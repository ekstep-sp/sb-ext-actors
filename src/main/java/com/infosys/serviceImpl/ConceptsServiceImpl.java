/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.service.ConceptsService;
import com.infosys.util.LexProjectUtil;

@Service
public class ConceptsServiceImpl implements ConceptsService {

	@Override
	public Response getByIds(String[] ids) throws IOException {
		Response resp = new Response();
		resp.setTs(ProjectUtil.getFormattedDate());

		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		queryBuilder.must(QueryBuilders.termsQuery("id", ids));

		SearchResponse searchResponse = requestBuilder
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.topic_topic.getIndexName())
						.types(LexProjectUtil.EsType.topic_topic.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(queryBuilder)), RequestOptions.DEFAULT);

		if (searchResponse.getHits().totalHits == 0){
			List<Map<String, Object>> finalResult = new ArrayList<>();
			SearchResponse searchResponse1 = requestBuilder
					.search(new SearchRequest().indices(LexProjectUtil.EsIndex.topic_details.getIndexName())
							.types(LexProjectUtil.EsType.topic_details.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
							.source(new SearchSourceBuilder().query(queryBuilder)), RequestOptions.DEFAULT);
			Map<String, Object> resultMap = new HashMap<>();
			for (SearchHit hit : searchResponse1.getHits().getHits()) {
				Map<String, Object> map1 = hit.getSourceAsMap();
				resultMap.put("id", map1.get("id"));
				resultMap.put("name", map1.get("name"));
				resultMap.put("synonyms", map1.get("synonyms"));
				resultMap.put("related", Collections.emptyList());
				finalResult.add(resultMap);
			}
			resp.put("response", finalResult);
			return resp;
		}

		List<Map<String, Object>> finalResult = new ArrayList<>();

		for (SearchHit hit : searchResponse.getHits().getHits()) {
			Map<String, Object> map = hit.getSourceAsMap();
			List<String> relatedIds = new ArrayList<>();
			List<Map<String, Object>> tempMapList = new ArrayList<>();
			int cnt = 0;
			for (Map<String, Object> item : (List<Map<String, Object>>) map.get("related")) {
				if (cnt < 10) {
					relatedIds.add(String.valueOf(item.get("id")));
					Map<String, Object> tempMap = new HashMap<>();
					tempMap.put("score", item.get("score"));
					tempMap.put("id", item.get("id"));
					tempMapList.add(tempMap);
					cnt++;
				} else {
					break;
				}
			}

			queryBuilder = QueryBuilders.boolQuery();
			queryBuilder.must(QueryBuilders.termQuery("id", map.get("id")));

			Map<String, Object> resultMap = new HashMap<>();

			
			SearchResponse searchResponse1 = requestBuilder
					.search(new SearchRequest().indices(LexProjectUtil.EsIndex.topic_details.getIndexName())
							.types(LexProjectUtil.EsType.topic_details.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
							.source(new SearchSourceBuilder().query(queryBuilder)), RequestOptions.DEFAULT);

			if (searchResponse1.getHits().getHits().length > 0) {
				Map<String, Object> map1 = searchResponse1.getHits().getHits()[0].getSourceAsMap();
				resultMap.put("id", map1.get("id"));
				resultMap.put("name", map1.get("name"));
				resultMap.put("synonyms", map1.get("synonyms"));

				List<Map<String, Object>> relatedList = new ArrayList<>();
				queryBuilder = QueryBuilders.boolQuery();
				queryBuilder.must(QueryBuilders.termsQuery("id", relatedIds));
				SearchResponse searchResponse2 = requestBuilder
						.search(new SearchRequest().indices(LexProjectUtil.EsIndex.topic_details.getIndexName())
								.types(LexProjectUtil.EsType.topic_details.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
								.source(new SearchSourceBuilder().query(queryBuilder)), RequestOptions.DEFAULT);

				for (SearchHit hit1 : searchResponse2.getHits().getHits()) {
					Map<String, Object> map2 = hit1.getSourceAsMap();
					int pos = relatedIds.indexOf(String.valueOf(map2.get("id")));
					tempMapList.get(pos).put("name", map2.get("name"));
					tempMapList.get(pos).put("synonyms", map2.get("synonyms"));
					relatedList.add(tempMapList.get(pos));
				}

				Comparator<Map<String, Object>> mapComparator = new Comparator<Map<String, Object>>() {
					public int compare(Map<String, Object> m1, Map<String, Object> m2) {
						return ((Double) m2.get("score")).compareTo(((Double) m1.get("score")));
					}
				};

				Collections.sort(relatedList, mapComparator);

				resultMap.put("related", relatedList);
				finalResult.add(resultMap);
			}
		}
		resp.put("response", finalResult);
		return resp;
	}
}
