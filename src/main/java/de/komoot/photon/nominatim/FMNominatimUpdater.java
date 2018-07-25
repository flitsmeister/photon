package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.*;

/**
 * Nominatim update logic
 *
 * @author felix
 */

public class FMNominatimUpdater {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FMNominatimUpdater.class);
    private final Integer minRank = 1;
    private final Integer maxRank = 30;
    private final JdbcTemplate template;
    private final NominatimConnector exporter;

    private Boolean updating = false;

    private Updater updater;

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public void update(JSONArray create, JSONArray modify, JSONArray delete) {
        updating = true;
        try {
            LOGGER.info(String.format("Starting %d news", create.length()));
            this.update(create);
            LOGGER.info(String.format("Starting %d updates", modify.length()));
            this.update(modify);
            LOGGER.info(String.format("Starting %d removes", delete.length()));
            this.remove(delete);

            LOGGER.info(String.format("Updating finised"));
            updater.finish();
        } finally {
            updating = false;
        }
    }

    public Boolean isUpdating() {
        return this.updating;
    }

    private void update(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            long placeId = places.getLong(i);
            final PhotonDoc doc = exporter.getByPlaceId(placeId);

            if (!doc.isUsefulForIndex())
                updater.delete(placeId);

            updater.updateOrCreate(doc);
        }
    }

    private void remove(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            updater.delete(places.getLong(i));
        }
    }

    private List<Map<String, Object>> getIndexSectors(Integer rank) {
        return template.queryForList("select geometry_sector,count(*) from placex where rank_search = ? " +
                "and indexed_status > 0 group by geometry_sector order by geometry_sector;", rank);
    }

    private List<UpdateRow> getIndexSectorPlaces(Integer rank, Integer geometrySector) {
        return template.query("select place_id, indexed_status from placex where rank_search = ?" +
                " and geometry_sector = ? and indexed_status > 0;", new Object[]{rank, geometrySector}, new RowMapper<UpdateRow>() {
            @Override
            public UpdateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
                UpdateRow updateRow = new UpdateRow();
                updateRow.setPlaceId(rs.getLong("place_id"));
                updateRow.setIndexdStatus(rs.getInt("indexed_status"));
                return updateRow;
            }
        });
    }

    /**
     */
    public FMNominatimUpdater(String host, int port, String database, String username, String password) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(JtsWrapper.class.getCanonicalName());
        dataSource.setDefaultAutoCommit(false);

        exporter = new NominatimConnector(host, port, database, username, password);
        template = new JdbcTemplate(dataSource);
    }
}