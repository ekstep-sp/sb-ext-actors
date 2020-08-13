/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.util.LexProjectUtil;

@RestController
public class MimeTypeController {

	@GetMapping("/get/mimetype/objects/list")
	public List<Map<String, Object>> getMimeTypeList() throws IOException {
		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		SearchRequest req = new SearchRequest();
		req.indices(LexProjectUtil.EsIndex.bodhi.getIndexName());
		req.types(LexProjectUtil.EsType.resource.getTypeName());
		req.searchType(SearchType.QUERY_THEN_FETCH);
		AggregationBuilder mimeTypeAggregationBuilder = AggregationBuilders.terms("mimeType_aggs")
				.field("mimeType.keyword").size(1000);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.aggregation(mimeTypeAggregationBuilder);
		req.source(sourceBuilder);

		SearchResponse response = requestBuilder.search(req, RequestOptions.DEFAULT);

		Terms conceptsAggregation = response.getAggregations().get("mimeType_aggs");
		Map<String, Long> mimeTypeMap = new HashMap<String, Long>();
		for (Terms.Bucket bucket : conceptsAggregation.getBuckets()) {
			if (bucket.getKeyAsString().isEmpty()) {
				continue;
			} else if (bucket.getDocCount() > 0) {
				System.out.println(bucket.getKeyAsString());
				mimeTypeMap.put(bucket.getKeyAsString(), bucket.getDocCount());
			}
		}

		Set<String> keySet = mimeTypeMap.keySet();

		List<Map<String, Object>> mimeTypeList = new ArrayList<Map<String, Object>>();

		for (String mimeType : keySet) {
			sourceBuilder = new SearchSourceBuilder();
			sourceBuilder.aggregation(mimeTypeAggregationBuilder);
			sourceBuilder.query(QueryBuilders.termQuery("mimeType.keyword", mimeType));
			sourceBuilder.size(1);
			req.source(sourceBuilder);

			SearchResponse tempResponse = requestBuilder.search(req, RequestOptions.DEFAULT);

			System.out.println(tempResponse.getHits().totalHits);

			for (SearchHit hit : tempResponse.getHits()) {
				Map<String, Object> hitMap = hit.getSourceAsMap();
				mimeTypeList.add(hitMap);
			}
		}
		return mimeTypeList;
	}
}
