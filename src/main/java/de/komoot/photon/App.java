package de.komoot.photon;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.komoot.photon.elasticsearch.DatabaseProperties;
import de.komoot.photon.elasticsearch.Server;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.FMNominatimUpdater;
import de.komoot.photon.utils.CorsFilter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;

import static spark.Spark.*;
import org.json.*;


@Slf4j
public class App {

    public static void main(String[] rawArgs) throws Exception {
        // parse command line arguments
        CommandLineArgs args = new CommandLineArgs();
        final JCommander jCommander = new JCommander(args);
        try {
            jCommander.parse(rawArgs);
            if (args.isCorsAnyOrigin() && args.getCorsOrigin() != null) { // these are mutually exclusive
                throw new ParameterException("Use only one cors configuration type");
            }
        } catch (ParameterException e) {
            log.warn("could not start photon: " + e.getMessage());
            jCommander.usage();
            return;
        }

        // show help
        if (args.isUsage()) {
            jCommander.usage();
            return;
        }

        if (args.getJsonDump() != null) {
            startJsonDump(args);
            return;
        }

        boolean shutdownES = false;
        final Server esServer = new Server(args.getDataDirectory()).start(args.getCluster(), args.getTransportAddresses());
        try {
            Client esClient = esServer.getClient();

            log.info("Make sure that the ES cluster is ready, this might take some time.");
            esClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
            log.info("ES cluster is now ready.");

            if (args.isNominatimImport()) {
                shutdownES = true;
                startNominatimImport(args, esServer, esClient);
                return;
            }

            // Working on an existing installation.
            // Update the index settings in case there are any changes.
            esServer.updateIndexSettings();
            esClient.admin().cluster().prepareHealth().setWaitForYellowStatus().get();

            // no special action specified -> normal mode: start search API
            startApi(args, esClient);
        } finally {
            if (shutdownES) esServer.shutdown();
        }
    }


    /**
     * take nominatim data and dump it to json
     *
     * @param args
     */
    private static void startJsonDump(CommandLineArgs args) {
        try {
            final String filename = args.getJsonDump();
            final JsonDumper jsonDumper = new JsonDumper(filename, args.getLanguages(), args.getExtraTags());
            NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
            nominatimConnector.setImporter(jsonDumper);
            nominatimConnector.readEntireDatabase(args.getCountryCodes().split(","));
            log.info("json dump was created: " + filename);
        } catch (FileNotFoundException e) {
            log.error("cannot create dump", e);
        }
    }


    /**
     * take nominatim data to fill elastic search index
     *
     * @param args
     * @param esServer
     * @param esNodeClient
     */
    private static void startNominatimImport(CommandLineArgs args, Server esServer, Client esNodeClient) {
        DatabaseProperties dbProperties;
        try {
            dbProperties = esServer.recreateIndex(args.getLanguagesOrDefault()); // clear out previous data
        } catch (IOException e) {
            throw new RuntimeException("cannot setup index, elastic search config files not readable", e);
        }

        log.info("starting import from nominatim to photon with languages: " + String.join(",", dbProperties.getLanguages()));
        de.komoot.photon.elasticsearch.Importer importer = new de.komoot.photon.elasticsearch.Importer(esNodeClient, dbProperties.getLanguages(), args.getExtraTags());
        NominatimConnector nominatimConnector = new NominatimConnector(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        nominatimConnector.setImporter(importer);
        nominatimConnector.readEntireDatabase(args.getCountryCodes().split(","));

        log.info("imported data from nominatim to photon with languages: " + String.join(",", dbProperties.getLanguages()));
    }

    /**
     * start api to accept search requests via http
     *
     * @param args
     * @param esNodeClient
     */
    private static void startApi(CommandLineArgs args, Client esNodeClient) {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = new DatabaseProperties();
        dbProperties.loadFromDatabase(esNodeClient);
        if (!args.getLanguages().isEmpty()) {
            dbProperties.restrictLanguages(args.getLanguages().split(","));
        }

        port(args.getListenPort());
        ipAddress(args.getListenIp());

        String allowedOrigin = args.isCorsAnyOrigin() ? "*" : args.getCorsOrigin();
        if (allowedOrigin != null) {
            CorsFilter.enableCORS(allowedOrigin, "get", "*");
        } else {
            before((request, response) -> {
                response.type("application/json; charset=UTF-8"); // in the other case set by enableCors
            });
        }

        // setup search API
        get("api", new SearchRequestHandler("api", esNodeClient, dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("api/", new SearchRequestHandler("api/", esNodeClient, dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("reverse", new ReverseSearchRequestHandler("reverse", esNodeClient, dbProperties.getLanguages(), args.getDefaultLanguage()));
        get("reverse/", new ReverseSearchRequestHandler("reverse/", esNodeClient, dbProperties.getLanguages(), args.getDefaultLanguage()));

        // setup update API
        final FMNominatimUpdater nominatimUpdater = new FMNominatimUpdater(args.getHost(), args.getPort(), args.getDatabase(), args.getUser(), args.getPassword());
        Updater updater = new de.komoot.photon.elasticsearch.Updater(esNodeClient, dbProperties.getLanguages(), args.getExtraTags());
        nominatimUpdater.setUpdater(updater);

        post("/fm-nominatim-update", (Request request, Response response) -> {
            JSONObject changes = new JSONObject(request.body());
            JSONArray delete = changes.getJSONArray("delete");
            JSONArray create = changes.getJSONArray("create");
            JSONArray modify = changes.getJSONArray("modify");
            new Thread(() -> nominatimUpdater.update(create, modify, delete)).start();
            return "nominatim update started (more information in console output) ...";
        });

        get("/update-status", (Request request, Response response) -> {
            return nominatimUpdater.isUpdating() ? "updating" : "";
        });

        post("/upload-manual-records", (Request request, Response response) -> {
            JSONArray addresses = new JSONArray(request.body());
            int index = Integer.parseInt(request.queryParamOrDefault("index", "0"));
            String prefix = request.queryParams("prefix");
            new Thread(() -> nominatimUpdater.updateManualRecords(prefix, addresses, index, index == 0)).start();
            return "nominatim update started (more information in console output) ...";
        });
    }
}
