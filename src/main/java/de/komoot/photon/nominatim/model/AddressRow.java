package de.komoot.photon.nominatim.model;

import com.neovisionaries.i18n.CountryCode;
import lombok.Data;

import java.util.Map;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class AddressRow {
    private final Map<String, String> name;
    private final String osmKey;
    private final String osmValue;
    private final int rankAddress;

    public AddressType getAddressType(CountryCode countryCode) {
        if (countryCode == CountryCode.NL && rankAddress == 14) return null;
        if (countryCode == CountryCode.BE && rankAddress == 20) return null;

        return AddressType.fromRank(rankAddress);
    }

    private boolean isPostcode() {
        if ("place".equals(osmKey) && "postcode".equals(osmValue)) {
            return true;
        }

        return "boundary".equals(osmKey) && "postal_code".equals(osmValue);
    }

    public boolean isUsefulForContext() {
        return !name.isEmpty() && !isPostcode();
    }
}
