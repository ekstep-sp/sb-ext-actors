/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import com.infosys.elastic.helper.ConnectionManager;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BodhiElasticSearchUtil {

	public static List<Map<String, Object>> getAllRecords() throws IOException {

		SearchResponse searchResponse = ConnectionManager.getClient()
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()).size(100000).from(0))
						, RequestOptions.DEFAULT);

		List<Map<String, Object>> allRecordList = new ArrayList<Map<String, Object>>();

		for (SearchHit searchHit : searchResponse.getHits())
			allRecordList.add(searchHit.getSourceAsMap());

		return allRecordList;
	}

}
