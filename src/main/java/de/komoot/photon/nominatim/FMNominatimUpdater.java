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

    private Updater updater;

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }

    public void update(JSONArray create, JSONArray modify, JSONArray delete) {
        this.insert(create);
        this.update(modify);
        this.remove(delete);

        updater.finish();
    }

    private void insert(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i);
            final PhotonDoc newDoc = exporter.getByOsmId(place.getLong("osm_id"), place.getString("osm_type"));

            if (newDoc.isUsefulForIndex())
                updater.create(newDoc);
        }
    }

    private void update(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i);
            final PhotonDoc updatedDoc = exporter.getByOsmId(place.getLong("osm_id"), place.getString("osm_type"));

            if (!updatedDoc.isUsefulForIndex())
                updater.delete(updatedDoc.getPlaceId());

            updater.updateOrCreate(updatedDoc);
        }
    }

    private void remove(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i);
            final PhotonDoc updatedDoc = exporter.getByOsmId(place.getLong("osm_id"), place.getString("osm_type"));

            // updater.delete(place.getPlaceId());
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