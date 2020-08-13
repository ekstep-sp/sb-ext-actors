/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.model.CourseProgress;
import com.infosys.model.CourseRecommendation;
import com.infosys.service.UsageRecommendationService;
import com.infosys.service.UserService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class UsageRecommendationServiceImpl implements UsageRecommendationService {

	@Autowired
	UserUtilityService userUtilService;

	@Autowired
	UserService userService;

	private static final int REC_LIST_SIZE = 15;

	@Autowired
	private AdminAccessControlServiceImpl accessControlService;

	private boolean accessControlEnabled;
	@Autowired
	private Environment environment;

	@PostConstruct
	private void initialize() {
		accessControlEnabled = environment.getProperty(LexConstants.ENABLE_ACCESS_CONTROL, Boolean.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CourseRecommendation> getUsageRecommendations(String emailId, String pageSizeStr, String pageNumberStr) {
		
		int pageNumber;
		int pageSize;
		
		try {
			pageNumber = Integer.valueOf(pageNumberStr);
			pageSize = Integer.valueOf(pageSizeStr);
		} catch(NumberFormatException ex) {
			throw new BadRequestException("Page Size and Page Number should be numbers");
		}
		
		if (pageNumber < 0 || pageSize < 0) {
			throw new BadRequestException("Page Size and Page Number should be non negative");
		}
		
		try {
			List<CourseProgress> progressList = userUtilService.getUserCourseProgress(emailId, 10);
			HashMap<String, String> idToName = new HashMap<>();
			for (CourseProgress progress : progressList) {
				idToName.put(progress.identifier, progress.name);
			}

			RestHighLevelClient client = ConnectionManager.getClient();
			HashMap<String, HashMap<String, Object>> recommendationHash = new HashMap<>();
			HashSet<String> itemIds = new HashSet<>();
			HashMap<String, Map<String, Object>> idToContentMeta = new HashMap<>();

			List<Map<String, Object>> result = ElasticSearchUtil.searchDataByValues("usagerec-index", "usagerec",
					"item_x", progressList.stream().map(item -> item.identifier).collect(Collectors.toList()), 10);

			for (Map<String, Object> item : result) {
				String itemXName = idToName.get((String) item.get("item_x"));
				List<Map<String, Object>> recsList = (List<Map<String, Object>>) item.get("recommendations");
				recsList = recsList.subList(0, Math.min(3, recsList.size()));
				for (Map<String, Object> recItem : recsList) {
					itemIds.add((String) recItem.get("item_y"));
					HashMap<String, Object> existingRec = (HashMap<String, Object>) recommendationHash
							.get(recItem.get("item_y"));
					if (existingRec != null) {
						existingRec.put("score",
								(double) existingRec.get("score") + (double) recItem.get("conviction"));
						((HashSet<String>) existingRec.get("reasonsForRecommendation")).add(itemXName);
						recommendationHash.put((String) recItem.get("item_y"), existingRec);
					} else {
						HashMap<String, Object> courseRec = new HashMap<>();
						courseRec.put("score", recItem.get("conviction"));
						courseRec.put("course", recItem.get("item_y"));
						courseRec.put("reasonsForRecommendation", new HashSet<>(Arrays.asList(itemXName)));
						recommendationHash.put((String) recItem.get("item_y"), courseRec);
					}
				}

			}

			System.out.println("Hash size " + recommendationHash.size());
			if (recommendationHash.size() == 0) {
				return new ArrayList<>();
			}

			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			if (accessControlEnabled) {
				if (!emailId.endsWith(".com")) {
					emailId += ".com";
				}
				String userId = (String) userService.findUUID(emailId).get("userId");
				Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
				List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
				boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
			}

			for (String id : itemIds) {
				boolQuery.should(QueryBuilders.termQuery("identifier", id));
			}

			
			SearchResponse response = client.search(new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
					.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
					.source(new SearchSourceBuilder()
							.fetchSource(new String[] { "name", "identifier", "description", "resourceType", "contentType",
							"appIcon", "artifactUrl", "children", "mimeType", "creatorContacts", "downloadUrl",
							"duration", "me_totalSessionsCount", "size", "complexityLevel", "lastUpdatedOn",
							"resourceCategory" }, null)
							.query(boolQuery)
							.size(pageSize)),
					RequestOptions.DEFAULT);
			
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				idToContentMeta.put((String) map.get("identifier"), map);
			}

			HashMap<String, CourseRecommendation> courseRecHash = new HashMap<>();
			recommendationHash.forEach((k, v) -> {
				CourseRecommendation rec = new CourseRecommendation();
				rec.setScore((double) v.get("score"));
				rec.setCourse(idToContentMeta.get(v.get("course")));
				rec.setReasonsForRecommendation((HashSet<String>) v.get("reasonsForRecommendation"));
				courseRecHash.put(k, rec);
			});

			List<CourseRecommendation> recs = courseRecHash.values().stream().sorted()
					.filter(recommendation -> recommendation.getCourse() != null).collect(Collectors.toList());

			return recs;
		} catch (Exception e) {
//			e.printStackTrace();
			ProjectLogger.log("Error : " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Map<String, List<CourseRecommendation>> getUsageRecommendationsForCourses(String[] contentIds)
			throws IOException {
		Map<String, List<CourseRecommendation>> usageRecsForIds = new HashMap<>();
		RestHighLevelClient client = ConnectionManager.getClient();
		for (String contentId : contentIds) {
			ArrayList<CourseRecommendation> usageRecsForId = new ArrayList<>();

			SearchResponse response = client.search(new SearchRequest().indices(Util.ARL_INDEX)
					.types(Util.ARL_INDEX_TYPE)
					.source(new SearchSourceBuilder()
							.query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("item_x", contentId)))
							.sort(SortBuilders.fieldSort("conviction").order(SortOrder.DESC)).size(REC_LIST_SIZE)),
					RequestOptions.DEFAULT);

			for (SearchHit hit : response.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				Map<String, Object> courseMeta = ElasticSearchUtil.getDataByIdentifier(
						LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(),
						(String) map.get("item_y"));
				if (courseMeta == null || !courseMeta.containsKey("contentType"))
					continue;
				if (!courseMeta.get("contentType").equals("Resource")) {
					double conviction = (double) map.get("conviction");
					usageRecsForId.add(new CourseRecommendation(conviction, courseMeta, null));
				}
			}

			usageRecsForIds.put(contentId, usageRecsForId);
		}
		return usageRecsForIds;
	}
}