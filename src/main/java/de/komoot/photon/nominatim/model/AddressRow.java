package de.komoot.photon.nominatim.model;

import lombok.Data;

import java.util.Map;

import com.neovisionaries.i18n.CountryCode;

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
    public final int rankAddress;
    public final int adminLevel;

    public AddressRow(Map<String, String> name, String osmKey, String osmValue, int rankAddress, int adminLevel, CountryCode countryCode) {
        this.name = name;
        this.osmKey = osmKey;
        this.osmValue = osmValue;
        this.adminLevel = adminLevel;

        if (rankAddress == 18 && adminLevel == 10 && countryCode == CountryCode.NL) {
            this.rankAddress = 16;
        } else if (rankAddress == 16 && adminLevel == 8 && countryCode == CountryCode.NL) {
            this.rankAddress = 14;
        } else {
            this.rankAddress = rankAddress;
        }
    }

    public AddressType getAddressType() {
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
