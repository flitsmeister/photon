package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp2.BasicDataSource;
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

public class FMNominatimUpdater extends NominatimUpdater {

    public FMNominatimUpdater(String host, int port, String database, String username, String password) {
        super(host, port, database, username, password);
    }

    public void update(JSONArray create, JSONArray modify, JSONArray delete, boolean interpolations) {
        if (updateLock.tryLock()) {
            try {
                LOGGER.info(String.format("Starting %d news", create.length()));
                if (interpolations) {
                    this.updateInterpolations(create);
                } else {
                    this.update(create, true);
                }
                LOGGER.info(String.format("Starting %d updates", modify.length()));
                if (interpolations) {
                    this.updateInterpolations(modify);
                } else {
                    this.update(modify, true);
                }
                LOGGER.info(String.format("Starting %d removes", delete.length()));
                this.remove(delete);

                LOGGER.info(String.format("Updating finised"));
                updater.finish();
            } finally {
                updateLock.unlock();
            }
        }
    }

    public Boolean isUpdating() {
        return updateLock.isLocked();
    }

    private void remove(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            long placeId = places.optLong(i);
            try {
                if (placeId == 0) {
                    updater.delete(places.getString(i));
                } else {
                    updater.delete(placeId);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Deleting of %d failed", places.getLong(i)));
                LOGGER.error(e.toString());
            }
        }
    }

    public void update(JSONArray places, Boolean create) {
        for (int i = 0; i < places.length(); i++) {
            long placeId = places.getLong(i);
            try {
                final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(places.getLong(i));
                boolean wasUseful = false;
                for (PhotonDoc updatedDoc : updatedDocs) {
                    if (create && updatedDoc.isUsefulForIndex()) {
                        updater.create(updatedDoc);
                    } else if (!create && updatedDoc.isUsefulForIndex()) {
                        updater.create(updatedDoc);
                        wasUseful = true;
                    }
                }
                if (!create && !wasUseful) {
                    // only true when rank != 30
                    // if no documents for the place id exist this will likely cause moaning
                    updater.delete(placeId);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Updating of %d failed", placeId));
                LOGGER.error(e.toString());
            }
        }
    }

    public void updateInterpolations(JSONArray places) {
        for (int i = 0; i < places.length(); i++) {
            long placeId = places.getLong(i);
            try {
                final List<PhotonDoc> updatedDocs = exporter.getInterpolationsByPlaceId(places.getLong(i));
                for (PhotonDoc updatedDoc : updatedDocs) {
                    updater.create(updatedDoc);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Updating of %d failed", placeId));
                LOGGER.error(e.toString());
            }
        }
    }
}
