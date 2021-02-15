package com.infosys.serviceImpl;

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
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentAnalyticsServiceImpl implements ContentAnalyticsService {

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
			queryBuilder.filter(getDateFilter(contentStatus, startDate, endDate));

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

	@Override
	public List<Map<String, Object>> getExternalResourcesList(String rootOrg, String org, String langCode, String startDate, String endDate, int searchSize, int offSet, String[] includeFields,String search_query) throws IOException {
		// indexName is based on langCode e.g mlsearch_en
		String indexName = LexProjectUtil.EsIndex.multi_lingual_search_index.getIndexName() + SearchConstants.SEARCH_INDEX_LOCALE_DELIMITER + langCode;
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.queryStringQuery(search_query).field("name").allowLeadingWildcard(true).type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.STATUS, StringUtils.capitalize(Constants.ML_SEARCH.STATUS_LIVE)))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.IS_EXTERNAL, "true"))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.CONTENT_TYPE, SearchConstants.RESOURCE))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.ROOT_ORG, rootOrg))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.ACCESS_PATHS, rootOrg + Constants.ML_SEARCH.ACCESS_PATHS_SEPARATOR + org));
		if (startDate != null && endDate != null) {
			// For Live content filtering to be done on publishedOn
			queryBuilder.filter(getDateFilter(Constants.ML_SEARCH.STATUS_LIVE, startDate, endDate));
		}
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchResponse searchResponse = ConnectionManager.getClient().search(
				searchRequest.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
						.searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(queryBuilder).fetchSource(includeFields, null)
								.size(searchSize).from(offSet)),
				RequestOptions.DEFAULT);
		// Returning list of External Resources
		return Arrays.stream(searchResponse.getHits().getHits()).map(SearchHit::getSourceAsMap).collect(Collectors.toList());

	}

	public RangeQueryBuilder getDateFilter(String contentStatus, String startDate, String endDate) {
		return QueryBuilders.rangeQuery(contentStatus.equalsIgnoreCase(Constants.ML_SEARCH.STATUS_LIVE) ?
				Constants.ML_SEARCH.PUBLISHED_ON : Constants.ML_SEARCH.LAST_UPDATED_ON)
				.gte(startDate)
				.lte(endDate);
	}

	@Override
	public List<Map<String, Object>> getContentList(Map<String, Map<String, Map<String, Object>>> searchFilters, String langCode, int searchSize, int offSet, String[] includeFields, String search_query) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		String indexName = LexProjectUtil.EsIndex.multi_lingual_search_index.getIndexName() + SearchConstants.SEARCH_INDEX_LOCALE_DELIMITER + langCode;
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.queryStringQuery(search_query).field("name").allowLeadingWildcard(true).type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
		setSearchFilters(queryBuilder, searchFilters);
		SearchResponse searchResponse = getSearchResponse(indexName, LexProjectUtil.EsType.new_lex_search.getTypeName(),
				new SearchSourceBuilder().query(queryBuilder).fetchSource(includeFields, null).size(searchSize).from(offSet));
		// Returning list of filtered content
		return Arrays.stream(searchResponse.getHits().getHits()).map(SearchHit::getSourceAsMap).collect(Collectors.toList());
	}

	public void setSearchFilters(BoolQueryBuilder queryBuilder, Map<String, Map<String, Map<String, Object>>> searchFilters) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Map<String, Object> mustTermFiters = searchFilters.get(SearchConstants.MUST).get(SearchConstants.TERM);
		mustTermFiters.forEach((key, value) -> queryBuilder.must(QueryBuilders.termQuery(key, value)));
		Map<String, Object> mustNotTermFiters = searchFilters.get(SearchConstants.MUST_NOT).get(SearchConstants.TERM);
		mustNotTermFiters.forEach((key, value) -> queryBuilder.mustNot(QueryBuilders.termQuery(key, value)));
		Map<String, Object> rangeFilters = searchFilters.get(SearchConstants.FILTERS).get(SearchConstants.RANGE);
		for (Map.Entry<String, Object> entry : rangeFilters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(key);
			Map<?, ?> rangeMap = (Map<?, ?>) value;
			for (Map.Entry<?, ?> mapEntry : rangeMap.entrySet()) {
				Object rangeKey = mapEntry.getKey();
				Object rangeValue = mapEntry.getValue();
				rangeQueryBuilder.getClass().getMethod(rangeKey.toString(), Object.class).invoke(rangeQueryBuilder, rangeValue);
			}
			queryBuilder.filter(rangeQueryBuilder);
		}
	}

	public SearchResponse getSearchResponse(String indexName, String indexType, SearchSourceBuilder searchSourceBuilder) throws IOException {
		SearchRequest searchRequest = new SearchRequest(indexName);
		searchRequest.types(indexType)
				.searchType(SearchType.QUERY_THEN_FETCH)
				.source(searchSourceBuilder);
		return ConnectionManager.getClient().search(
				searchRequest,
				RequestOptions.DEFAULT);
	}
}
