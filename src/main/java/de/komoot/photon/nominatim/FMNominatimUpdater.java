package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.OAPhotonDoc;
import de.komoot.photon.Updater;
import de.komoot.photon.nominatim.model.UpdateRow;
import org.apache.commons.dbcp.BasicDataSource;
import org.postgis.jts.JtsWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

    public void update(JSONArray create, JSONArray modify, JSONArray delete) {
        if (updateLock.tryLock()) {
            try {
                LOGGER.info(String.format("Starting %d news", create.length()));
                this.update(create, true);
                LOGGER.info(String.format("Starting %d updates", modify.length()));
                this.update(modify, false);
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
            updater.delete(String.valueOf(places.getLong(i)));
        }
    }

    public void update(JSONArray places, Boolean create) {
        for (int i = 0; i < places.length(); i++) {
            long placeId = places.getLong(i);
            final List<PhotonDoc> updatedDocs = exporter.getByPlaceId(places.getLong(i));
            boolean wasUseful = false;
            for (PhotonDoc updatedDoc : updatedDocs) {
                if (create && updatedDoc.isUsefulForIndex()) {
                    updater.create(updatedDoc);
                } else if (!create && updatedDoc.isUsefulForIndex()) {
                    updater.updateOrCreate(updatedDoc);
                    wasUseful = true;
                }
            }
            if (!create && !wasUseful) {
                // only true when rank != 30
                // if no documents for the place id exist this will likely cause moaning
                updater.delete(String.valueOf(placeId));
            }
        }
    }

    public void updateOpenAddresses(JSONArray addresses, int startIndex, Boolean clean) {
        if (updateLock.tryLock()) {
            try {
                if (clean) {
                    System.out.println("Cleaning old openaddress");
                    updater.cleanOpenaddresses();
                    updater.finish();
                    System.out.println("Cleaning finished");
                }

                System.out.println("Now importing " + addresses.length() + " addresses");
                for (int i = 0; i < addresses.length(); i++) {
                    if (i % 10000 == 0 && i > 0) {
                        System.out.println(i);
                        updater.finish();
                    }

                    String[] location = addresses.getString(i).split(",");

                    PhotonDoc doc = new OAPhotonDoc(
                        i + startIndex,
                        Double.parseDouble(location[0]),
                        Double.parseDouble(location[1]),
                        location[2],
                        location[3],
                        location[4],
                        location[5],
                        location[6]
                    );

                    doc.setCountry(exporter.getCountryNames(doc.getCountryCode().getAlpha2().toLowerCase()));

                    updater.updateOrCreate(doc);
                }
                updater.finish();
                System.out.println("Finished importing");
            } finally {
                updateLock.unlock();
            }
        }
    }
}
