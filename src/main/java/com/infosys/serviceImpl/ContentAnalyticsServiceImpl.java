package com.infosys.serviceImpl;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.service.ContentAnalyticsService;
import com.infosys.util.Constants;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.SearchConstants;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentAnalyticsServiceImpl implements ContentAnalyticsService {
    ObjectMapper objectMapper = new ObjectMapper();
	Map<String, String> externalResourcesMap = null;
	@Value("${externalResource_value}")
	private String externalResource_value;
    @Override
    public Map<String, Object> getContentCountByStatus(String rootOrg, String org, String langCode, String contentStatus, String startDate, String endDate) throws IOException {
        // indexName is based on langCode e.g mlsearch_en
        String indexName = LexProjectUtil.EsIndex.multi_lingual_search_index.getIndexName() + SearchConstants.SEARCH_INDEX_LOCALE_DELIMITER + langCode;
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.STATUS, StringUtils.capitalize(contentStatus)))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.ROOT_ORG, rootOrg))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.ACCESS_PATHS, rootOrg + Constants.ML_SEARCH.ACCESS_PATHS_SEPARATOR + org));
        if (startDate != null && endDate != null) {
            // For Live content filtering to be done on publishedOn else lastUpdatedOn
            queryBuilder.filter(QueryBuilders.rangeQuery(contentStatus.equalsIgnoreCase(Constants.ML_SEARCH.STATUS_LIVE) ?
                    Constants.ML_SEARCH.PUBLISHED_ON : Constants.ML_SEARCH.LAST_UPDATED_ON)
                    .gte(startDate)
                    .lte(endDate));
        }
        AggregationBuilder builder = AggregationBuilders.terms(Constants.COUNT_CONTENT_AGG_KEY).field(Constants.ML_SEARCH.CONTENT_TYPE);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchResponse searchResponse = ConnectionManager.getClient().search(
                searchRequest.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
                        .searchType(SearchType.QUERY_THEN_FETCH)
                        .source(new SearchSourceBuilder().query(queryBuilder)
                                .size(0).from(0).aggregation(builder)),
                RequestOptions.DEFAULT);
        Terms data = searchResponse.getAggregations().get(Constants.COUNT_CONTENT_AGG_KEY);
        return data.getBuckets().stream()
                .collect(Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString, MultiBucketsAggregation.Bucket::getDocCount));
    }

    public ArrayList<String> getExternalResourcesList(String rootOrg, String org, String langCode, String startDate, String endDate , int searchSize , int offSet) throws IOException {
        // indexName is based on langCode e.g mlsearch_en
        String indexName = LexProjectUtil.EsIndex.multi_lingual_search_index.getIndexName() + SearchConstants.SEARCH_INDEX_LOCALE_DELIMITER + langCode;
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.STATUS, StringUtils.capitalize(Constants.ML_SEARCH.STATUS_LIVE)))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.IS_EXTERNAL, externalResource_value))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.CONTENT_TYPE, SearchConstants.RESOURCE))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.ROOT_ORG, rootOrg))
                .must(QueryBuilders.termQuery(Constants.ML_SEARCH.ACCESS_PATHS, rootOrg + Constants.ML_SEARCH.ACCESS_PATHS_SEPARATOR + org));
        if (startDate != null && endDate != null) {
            // For Live content filtering to be done on publishedOn
            queryBuilder.filter(QueryBuilders.rangeQuery(
                    Constants.ML_SEARCH.PUBLISHED_ON)
                    .gte(startDate)
                    .lte(endDate));
        }
        AggregationBuilder builder = AggregationBuilders.terms(Constants.COUNT_CONTENT_AGG_KEY).field(Constants.ML_SEARCH.CONTENT_TYPE);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchResponse searchResponse = ConnectionManager.getClient().search(
                searchRequest.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
                        .searchType(SearchType.QUERY_THEN_FETCH)
                        .source(new SearchSourceBuilder().query(queryBuilder)
                                .size(searchSize).from(offSet).aggregation(builder)),
                RequestOptions.DEFAULT);
        ArrayList<String> externalResourceIds = new ArrayList<>();
        // Creating list of External Resources
        Arrays.stream(searchResponse.getHits().getHits()).forEach(hit -> {
			try {
				externalResourcesMap = objectMapper.readValue(hit.getSourceAsString(), Map.class);
			} catch (IOException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
			}
			externalResourceIds.add(externalResourcesMap.get(Constants.IDENTIFIER));

        });
        return externalResourceIds;
    }
}
