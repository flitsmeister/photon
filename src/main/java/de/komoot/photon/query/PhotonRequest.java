package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Envelope;
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
    private final double scale;
    private Envelope bbox;
    private String search_language;
    private Boolean fuzzy;
    private Boolean lenient;

    public PhotonRequest(String query, int limit, Envelope bbox, Point locationForBias, double scale, String language, String search_language, Boolean fuzzy, Boolean lenient) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        this.scale = scale;
        this.language = language;
        this.bbox = bbox;
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

    public Envelope getBbox() {
        return bbox;
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
