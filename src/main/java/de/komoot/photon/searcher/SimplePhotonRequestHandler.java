package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.TagFilterQueryBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class SimplePhotonRequestHandler extends AbstractPhotonRequestHandler<PhotonRequest> implements PhotonRequestHandler<PhotonRequest> {
    public SimplePhotonRequestHandler(ElasticsearchSearcher elasticsearchSearcher) {
        super(elasticsearchSearcher);
    }

    @Override
    public TagFilterQueryBuilder buildQuery(PhotonRequest photonRequest) {
        TagFilterQueryBuilder queryBuilder = PhotonQueryBuilder.builder(photonRequest.getQuery(), photonRequest.getSearchLanguage()).
                withLocationBias(photonRequest.getLocationForBias(), photonRequest.getScaleForBias()).
                withBoundingBox(photonRequest.getBbox());
        if (photonRequest.isFuzzy()) {
            queryBuilder.withFuzzyMatch();
        }
        if (photonRequest.isLenient()) {
            queryBuilder.withLenientMatch();
        }
        return queryBuilder;
    }
}
