/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.service.GenericRecommendationService;
import com.infosys.service.ParentService;
import com.infosys.util.LexProjectUtil;

@Service
public class GenericRecommendationServiceImpl implements GenericRecommendationService {

	@Autowired
	ParentService parentsService;

	public List<Object> getPopularCourses(String numberOfResources, String pageNumber)
			throws JsonParseException, JsonMappingException, IOException {

		int pageNo = Integer.parseInt(pageNumber);
		int pageSize = Integer.parseInt(numberOfResources);

		SearchResponse response = ConnectionManager.getClient().search(
				new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder()
								.postFilter(QueryBuilders.termQuery("contentType.keyword", "Course")).size(pageSize)
								.from(pageNo * pageSize).sort("me_totalSessionsCount", SortOrder.DESC)),
				RequestOptions.DEFAULT);

		List<Object> allEntries = new ArrayList<Object>();

		for (SearchHit hit : response.getHits()) {
			Map<String, Object> individualEntryMap = hit.getSourceAsMap();
			allEntries.add(individualEntryMap);
		}
		return allEntries;
	}

	public List<Object> getNewCourses(String numberOfResources, String pageNumber, String contentType)
			throws JsonParseException, JsonMappingException, IOException {

		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		int pageNo = Integer.parseInt(pageNumber);
		int pageSize = Integer.parseInt(numberOfResources);

		BoolQueryBuilder boolQuery;
		System.out.println("contenttype " + contentType);
		if (!contentType.equals("")) {
			boolQuery = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("contentType.keyword", contentType));
		} else {
			boolQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("collections"))
					.mustNot(QueryBuilders.termQuery("contentType.keyword", "Resource"));
		}

		SearchResponse searchResponse = requestBuilder
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(boolQuery).sort("versionDate", SortOrder.DESC)
								.size(pageSize).from(pageNo)),
						RequestOptions.DEFAULT);

		List<Object> allEntries = new ArrayList<Object>();
		for (SearchHit hit : searchResponse.getHits()) {
			allEntries.add(hit.getSourceAsMap());
		}
		return allEntries;
	}

	public List<Object> getAssessments(String numberOfResources, String pageNumber)
			throws JsonParseException, JsonMappingException, IOException {

		int pageNo = Integer.parseInt(pageNumber);
		int pageSize = Integer.parseInt(numberOfResources);

		SearchResponse searchResponse = ConnectionManager.getClient()
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("resourceType.keyword", "HandsOnAssessment")).sort("versionDate", SortOrder.DESC)
								.size(pageSize).from(pageNo* pageSize)),
						RequestOptions.DEFAULT);

		List<Object> allEntries = new ArrayList<Object>();
		for (SearchHit hit : searchResponse.getHits()) {
			allEntries.add(hit.getSourceAsMap());
		}
		return allEntries;
	}

}