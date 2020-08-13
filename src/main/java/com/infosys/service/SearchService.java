/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import org.sunbird.common.request.Request;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.model.ContentMeta;
import com.infosys.model.Filter;
import com.infosys.model.ObjectMeta;
import com.infosys.model.Topic;
import com.infosys.model.Track;
import com.infosys.util.LexProjectUtil;

@Service
public class SearchService {
	public List<ContentMeta> searchAPI(Request request) throws IOException {
		Map<String, Object> filters = request.getRequest();

		Filter filter = Filter.fromMap(filters);

		String contentType = filter.getContentType();
		String resourceType = filter.getResourceType();
		String query = filter.getQuery();

		Topic[] topics = filter.getConcepts();
		Track[] track = filter.getTracks();

		String trackNames[] = new String[track.length];
		String topicNames[] = new String[topics.length];

		for (int i = 0; i < track.length; i++) {
			trackNames[i] = track[i].getName();
			System.out.println(trackNames[i]);
		}

		for (int i = 0; i < topics.length; i++) {
			topicNames[i] = topics[i].getName();
			System.out.println(topicNames[i]);
		}

		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		String allField = "_all";

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
				.query(QueryBuilders.boolQuery().should(QueryBuilders.multiMatchQuery(query, allField)));
		if (!contentType.isEmpty()) {
			sourceBuilder.query(QueryBuilders.boolQuery().should(QueryBuilders.termQuery("contentType", contentType)));
			System.out.println("contentType ! null");
		}
		if (!resourceType.isEmpty()) {
			sourceBuilder
					.query(QueryBuilders.boolQuery().should(QueryBuilders.termQuery("resourceType", resourceType)));
			System.out.println("resourceType!null");
		}

		System.out.println(contentType + " " + resourceType);

		if (trackNames.length > 0 && trackNames[0] != null) {
			System.out.println("tracks ecxecuted");
			sourceBuilder.query(QueryBuilders.boolQuery().should(QueryBuilders.termsQuery("track.name", trackNames)));
		}
		if (topicNames.length > 0 && topicNames[0] != null) {
			System.out.println("topics ecxecuted");
			sourceBuilder
					.query(QueryBuilders.boolQuery().should(QueryBuilders.termsQuery("concepts.name", topicNames)));
		}

		SearchResponse response = requestBuilder
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.DFS_QUERY_THEN_FETCH)
						.source(sourceBuilder), RequestOptions.DEFAULT);

		System.out.println(response.getHits().totalHits);

		List<ContentMeta> contentMetaList = new ArrayList<ContentMeta>();

		for (SearchHit hit : response.getHits()) {
			String id = hit.getId();
			// System.out.println(id);
			Map<String, Object> map = getCourseMeta(id);
			if (!map.get("keywords").toString().isEmpty()) {
				ContentMeta contentMeta = ContentMeta.fromMap(map);
				contentMetaList.add(contentMeta);
			}
		}

		return contentMetaList;
	}

	public Map<String, Map<String, Integer>> getFiltersAndCount() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> allEntries = ElasticSearchUtil.searchData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
				LexProjectUtil.EsType.resource.getTypeName(), map, 50);

		Map<String, Map<String, Integer>> completeMap = new HashMap<String, Map<String, Integer>>();

		Map<String, Integer> contentTypeMap = new HashMap<String, Integer>();
		contentTypeMap.put("Resource", 0);
		contentTypeMap.put("Course", 0);
		contentTypeMap.put("", 0);

		Map<String, Integer> resourceTypeMap = new HashMap<String, Integer>();
		resourceTypeMap.put("", 0);
		resourceTypeMap.put("Story", 0);

		for (Map eachEntry : allEntries) {
			if (!eachEntry.get("keywords").toString().isEmpty()) {
				ContentMeta contentMeta = ContentMeta.fromMap(eachEntry);
				Map<String, Integer> trackMap = new HashMap<String, Integer>();
				Track[] trackObject = contentMeta.getTrack();
				List<String> newTrackNames = new ArrayList<String>();
				for (int i = 0; i < trackObject.length; i++) {
					if (!trackMap.containsKey(trackObject[i].getName())) {
						trackMap.put(trackObject[i].getName(), 1);
						newTrackNames.add(trackObject[i].getName());
					} else {
						int trackCount = trackMap.get(trackObject[i].getName());
						trackCount += 1;
						trackMap.put(trackObject[i].getName(), trackCount);
					}
				}
				if (!completeMap.containsKey("track")) {
					completeMap.put("track", trackMap);
				} else {
					// Set<String> trackKeySet = completeMap.get("track").keySet();
					for (String key : newTrackNames) {
						int trackCountInPresentIdentifier = trackMap.get(key);
						if (!completeMap.get("track").containsKey(key))
							completeMap.get("track").put(key, trackCountInPresentIdentifier);
						else {
							int presentTrackCount = completeMap.get("track").get(key);
							presentTrackCount += trackCountInPresentIdentifier;
							completeMap.get("track").put(key, presentTrackCount);
						}
					}
				}

				Map<String, Integer> topicMap = new HashMap<String, Integer>();
				ObjectMeta[] topicObject = contentMeta.getConcepts();
				List<String> newTopicNames = new ArrayList<String>();
				for (int i = 0; i < topicObject.length; i++) {
					if (!topicMap.containsKey(topicObject[i].getName())) {
						topicMap.put(topicObject[i].getName(), 1);
						newTopicNames.add(topicObject[i].getName());
					} else {
						int topicCount = topicMap.get(topicObject[i].getName());
						topicCount += 1;
						topicMap.put(topicObject[i].getName(), topicCount);
					}
				}
				if (!completeMap.containsKey("concepts")) {
					completeMap.put("concepts", topicMap);
				} else {
					// Set<String> trackKeySet = completeMap.get("track").keySet();
					for (String key : newTopicNames) {
						int trackCountInPresentIdentifier = topicMap.get(key);
						if (!completeMap.get("concepts").containsKey(key))
							completeMap.get("concepts").put(key, trackCountInPresentIdentifier);
						else {
							int presentTrackCount = completeMap.get("concepts").get(key);
							presentTrackCount += trackCountInPresentIdentifier;
							completeMap.get("concepts").put(key, presentTrackCount);
						}
					}
				}

				String contentType = contentMeta.getContentType();
				// System.out.println(contentType);
				if (contentType != null) {
					int contentTypeCount = 0;
					if (contentTypeMap.get(contentType) != null) {
						contentTypeCount = contentTypeMap.get(contentType);
					}
					contentTypeCount += 1;
					contentTypeMap.put(contentType, contentTypeCount);
				}

				String resourceType = contentMeta.getResourceType();
				System.out.println(resourceType);
				if (resourceType != null) {
					int resourceTypeCount = 0;
					if (resourceTypeMap.get(resourceType) != null) {
						resourceTypeCount = resourceTypeMap.get(resourceType);
					}
					resourceTypeCount += 1;
					resourceTypeMap.put(resourceType, resourceTypeCount);
				}

			}
		}

		completeMap.put("contentType", contentTypeMap);
		completeMap.put("resourceType", resourceTypeMap);

		return completeMap;
	}

	public Map<String, Object> getCourseMeta(String courseId) throws IOException {

		Map<String, Object> mappedObject = ElasticSearchUtil.getDataByIdentifier(
				LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), courseId);
		return mappedObject;

	}
}
