/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.thread;

import com.infosys.util.LexProjectUtil;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


public class CourseListThread implements Callable<List<Map<String, Object>>> {

    String indexName;
    String typeName;
    Client requestBuilder;
    int pageNumber;
    int coursesToBeReturned;


    public CourseListThread(Client requestBuilder, String indexName, String typeName, int coursesToBeReturned,int pageNumber) {
        this.requestBuilder = requestBuilder;
        this.indexName = indexName;
        this.typeName = typeName;
        this.coursesToBeReturned = coursesToBeReturned;

    }


    @Override
    public List<Map<String, Object>> call() throws Exception {
        SearchRequestBuilder contentRequestBuilder = requestBuilder
                .prepareSearch(LexProjectUtil.EsIndex.bodhi.getIndexName())
                .setTypes(LexProjectUtil.EsType.resource.getTypeName()).setSearchType(SearchType.QUERY_THEN_FETCH);

        SearchResponse courseSearchResponse = contentRequestBuilder.setQuery(QueryBuilders.termQuery("contentType.keyword", "Course")).setFrom(0).setSize(100)
                .get();

        List<Map<String, Object>> courseList = new ArrayList<Map<String, Object>>();

        for (SearchHit searchHit : courseSearchResponse.getHits()) {
            Map<String, Object> hit = searchHit.getSourceAsMap();
            courseList.add(hit);
        }
        return courseList;
    }
}


