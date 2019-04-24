package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;

import java.io.Serializable;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequest implements Serializable {
    private String query;
    private Integer limit;
    private Point locationForBias;
    private String language;
    private String search_language;
    private final double scale;
    private Boolean fuzzy;
    private Boolean lenient;

    public PhotonRequest(String query, int limit, Point locationForBias, double scale, String language, String search_language, Boolean fuzzy, Boolean lenient) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.scale = scale;
        this.language = language;
        this.search_language = search_language;
        this.fuzzy = fuzzy;
        this.lenient = lenient;
    }

    public String getQuery() {
        return query;
    }

    public Integer getLimit() {
        return limit;
    }

    public Point getLocationForBias() {
        return locationForBias;
    }

    public double getScaleForBias() {
        return scale;
    }

    public String getLanguage() {
        return language;
    }

    public String getSearchLanguage() {
        return search_language;
    }

    public Boolean isFuzzy() {
        return fuzzy;
    }

    public Boolean isLenient() {
        return lenient;
    }
}
