package de.komoot.photon.searcher;

import de.komoot.photon.elasticsearch.PhotonIndex;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class BaseElasticsearchSearcher {

    private Client client;

    public BaseElasticsearchSearcher(Client client) {
        this.client = client;
    }

    public SearchResponse search(QueryBuilder queryBuilder, Integer limit, Boolean debug) {
        TimeValue timeout = TimeValue.timeValueSeconds(7);
        SearchRequestBuilder builder = client.prepareSearch(PhotonIndex.NAME).
                setSearchType(SearchType.QUERY_AND_FETCH).
                setQuery(queryBuilder).
                setSize(limit).
                setTimeout(timeout);
        if (debug) {
            builder.setExplain(true);
        }
        return builder.execute().actionGet();
    }

}
