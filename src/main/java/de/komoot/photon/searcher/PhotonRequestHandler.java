package de.komoot.photon.searcher;

import de.komoot.photon.query.PhotonQueryBuilder;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.utils.ConvertToJson;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;

import java.util.List;

/**
 * Given a {@link PhotonRequest photon request}, execute the search, process it (for example, de-duplicate) and respond with results formatted in a list of {@link JSONObject json
 * object}s.
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonRequestHandler {

    private final BaseElasticsearchSearcher elasticsearchSearcher;
    private final List<String> supportedLanguages;

    public PhotonRequestHandler(BaseElasticsearchSearcher elasticsearchSearcher, List<String> supportedLanguages) {
        this.elasticsearchSearcher = elasticsearchSearcher;
        this.supportedLanguages = supportedLanguages;
    }

    public List<JSONObject> handle(PhotonRequest photonRequest) {
        PhotonQueryBuilder queryBuilder = buildQuery(photonRequest);
        // for the case of deduplication we need a bit more results, #300
        int limit = photonRequest.getLimit();
        int extLimit = limit > 1 ? (int) Math.round(photonRequest.getLimit() * 1.5) : 1;
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), extLimit, photonRequest.getDebug());
        List<JSONObject> resultJsonObjects = new ConvertToJson(photonRequest.getLanguage()).convert(results, photonRequest.getDebug());
        StreetDupesRemover streetDupesRemover = new StreetDupesRemover(photonRequest.getLanguage());
        resultJsonObjects = streetDupesRemover.execute(resultJsonObjects);
        if (resultJsonObjects.size() > limit) {
            resultJsonObjects = resultJsonObjects.subList(0, limit);
        }
        return resultJsonObjects;
    }

    public String dumpQuery(PhotonRequest photonRequest) {
        return buildQuery(photonRequest).buildQuery().toString();
    }

    public long total(PhotonRequest photonRequest) {
        PhotonQueryBuilder queryBuilder = buildQuery(photonRequest);
        SearchResponse results = elasticsearchSearcher.search(queryBuilder.buildQuery(), 0, false);
        return results.getHits().getTotalHits();
    }

   public PhotonQueryBuilder buildQuery(PhotonRequest photonRequest) {
        return PhotonQueryBuilder.
                builder(photonRequest.getQuery(), photonRequest.getSearchLanguage(), supportedLanguages, photonRequest.isLenient(), photonRequest.isFuzzy()).
                withTags(photonRequest.tags()).
                withKeys(photonRequest.keys()).
                withValues(photonRequest.values()).
                withoutTags(photonRequest.notTags()).
                withoutKeys(photonRequest.notKeys()).
                withoutValues(photonRequest.notValues()).
                withTagsNotValues(photonRequest.tagNotValues()).
                withLocationBias(photonRequest.getLocationForBias(), photonRequest.getScaleForBias(), photonRequest.getZoomForBias()).
                withBoundingBox(photonRequest.getBbox());
    }
}
