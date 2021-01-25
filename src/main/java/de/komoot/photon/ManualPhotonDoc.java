package de.komoot.photon;

import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import de.komoot.photon.nominatim.model.AddressType;

@Getter
@Setter
public class ManualPhotonDoc extends PhotonDoc  {

    private final static GeometryFactory geometryFactory = new GeometryFactory();
    final private long index;
    final private String prefix;

    public ManualPhotonDoc(String prefix, long index, Double latitude, Double longitude, String street, String houseNumber, String city, String postcode, String countryCode, Map<String, String> name, Map<String, String> extraValues) {
        super(
            index,
            prefix,
            index,
            "place",
            "house_number"
        );

        this.prefix = prefix;
        this.index = index;

        this.names(name);
        this.houseNumber(houseNumber);
        this.centroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        this.countryCode(countryCode);

        this.setAddressPartIfNew(AddressType.STREET, new HashMap<String, String>() {
            { put("name", street); }
        });

        this.setAddressPartIfNew(AddressType.CITY, new HashMap<String, String>() {
            { put("name", city); }
        });

        this.postcode(postcode);

        if (extraValues != null) {
            this.getContext().add(extraValues);
        }
    }

    public String getUid() {
        return this.prefix + ":" + String.valueOf(index);
    }
}
