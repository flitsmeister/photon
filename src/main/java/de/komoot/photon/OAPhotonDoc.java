package de.komoot.photon;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * denormalized doc with all information needed be dumped to elasticsearch
 *
 * @author christoph
 */
@Getter
@Setter
public class OAPhotonDoc extends PhotonDoc  {

    private final static GeometryFactory geometryFactory = new GeometryFactory();
    final private long index;

    public OAPhotonDoc(long index, Double latitude, Double longitude, String street, String houseNumber, String city, String postcode, String countryCode) {
        super(
            index,
            "O",
            index,
            "place",
            "house_number",
            Collections.<String, String>emptyMap(), // no name
            houseNumber,
            Collections.<String, String>emptyMap(), // no extratags
            (Envelope) null,
            0,
            0d, // importance
            CountryCode.getByCode(countryCode),
            geometryFactory.createPoint(new Coordinate(longitude, latitude)),
            0,
            30
        );
        this.index = index;

        this.setStreet(new HashMap<String, String>() {
            { put("name", street); }
        });

        this.setCity(new HashMap<String, String>() {
            { put("name", city); }
        });

        this.setPostcode(postcode);
    }

    public String getUid() {
        return "O:" + String.valueOf(index);
    }
}
