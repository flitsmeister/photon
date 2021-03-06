package de.komoot.photon;

/**
 * @author felix
 */
public interface Updater {
    public void create(PhotonDoc doc);

    public void update(PhotonDoc doc);

    public void delete(String id);

    public void finish();

    public void updateOrCreate(PhotonDoc updatedDoc);

    public void cleanManualRecords(String prefix);
}
