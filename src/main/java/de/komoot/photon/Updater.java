package de.komoot.photon;

/**
 * @author felix
 */
public interface Updater {
    public void create(PhotonDoc doc);

    public void delete(String id);

    public void delete(Long id);

    public void finish();

    public void cleanManualRecords(String prefix);
}
