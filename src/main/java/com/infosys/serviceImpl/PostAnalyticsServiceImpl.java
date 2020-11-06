package com.infosys.serviceImpl;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.service.PostAnalyticsService;
import com.infosys.util.Constants;
import com.infosys.util.LexProjectUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostAnalyticsServiceImpl implements PostAnalyticsService {

	@Override
	public List<Map<String, Object>> getPostAnalyticContent(String rootOrg, String org, String postKind, String postStatus, String startDate, String endDate, String[] includeFields) throws Exception {
		String indexName = LexProjectUtil.EsIndex.post.getIndexName();
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.STATUS, StringUtils.capitalize(postStatus)))
				.must(QueryBuilders.termQuery(Constants.POST_SEARCH.POST_KIND, StringUtils.capitalize(postKind)))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.ROOT_ORG, rootOrg))
				.must(QueryBuilders.termQuery(Constants.ML_SEARCH.ACCESS_PATHS, rootOrg + Constants.ML_SEARCH.ACCESS_PATHS_SEPARATOR + org));
		if (startDate != null && endDate != null) {
			// For Active content filtering to be done on dtPublished
			boolQueryBuilder.filter(getDateFilter(postStatus, startDate, endDate));
		}
		SearchRequest searchRequest = new SearchRequest(indexName);
		SearchResponse searchResponse = ConnectionManager.getClient().search(
				searchRequest.types(LexProjectUtil.EsType.post.getTypeName())
						.searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(boolQueryBuilder).fetchSource(includeFields, null)),
				RequestOptions.DEFAULT);
		return Arrays.stream(searchResponse.getHits().getHits()).map(SearchHit::getSourceAsMap).collect(Collectors.toList());

	}

	public RangeQueryBuilder getDateFilter(String contentStatus, String startDate, String endDate) {
		return QueryBuilders.rangeQuery(contentStatus.equalsIgnoreCase(Constants.POST_SEARCH.STATUS_ACTIVE) ?
				Constants.POST_SEARCH.PUBLISHED_ON : Constants.POST_SEARCH.CREATED_ON)
				.gte(startDate)
				.lte(endDate);
	}
}
