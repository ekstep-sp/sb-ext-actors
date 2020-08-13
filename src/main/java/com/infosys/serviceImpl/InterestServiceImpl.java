/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
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
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.model.CourseRecommendation;
import com.infosys.model.Topic;
import com.infosys.service.InterestService;
import com.infosys.service.TopicService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;

@Service
public class InterestServiceImpl implements InterestService {

	private static final int RECS_PER_INTEREST = 5;
	@Autowired
	TopicService topicService;

	@Autowired
	private AdminAccessControlServiceImpl accessControlService;

	private boolean accessControlEnabled;
	@Autowired
	private Environment environment;

	@PostConstruct
	private void initialize() {
		accessControlEnabled = environment.getProperty(LexConstants.ENABLE_ACCESS_CONTROL, Boolean.class);
	}

	@Override
	public List<CourseRecommendation> getInterestedCourses(String userId, String numberOfResources, String pageNumberStr)
			throws IOException {
		
		int pageNumber;
		int pageSize;
		
		try {
			pageNumber = Integer.valueOf(pageNumberStr);
			pageSize = Integer.valueOf(numberOfResources);
		} catch(NumberFormatException ex) {
			throw new BadRequestException("Page Size and Page Number should be numbers");
		}
		
		if (pageNumber < 0 || pageSize < 0) {
			throw new BadRequestException("Page Size and Page Number should be non negative");
		}
		
		if (userId.equals("")) {
			throw new BadRequestException("User Id cannot be empty");
		}
		
		List<String> userTopics = topicService.getUserTopics(userId).stream().map(Topic::getName)
				.collect(Collectors.toList());
		RestHighLevelClient client = ConnectionManager.getClient();
		HashMap<String, CourseRecommendation> recsHash = new HashMap<>();
		Response accessControlResponse;
		List<String> accessPaths = new ArrayList<>();
		if (accessControlEnabled) {
			accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
			accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
		}
		for (String topic : userTopics) {
			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			boolQuery.mustNot(QueryBuilders.termQuery("contentType.keyword", "Resource"));
			if (accessControlEnabled) {
				boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
			}
			// multi match type MOST_FIELDS ensures that if the interest is
			// present in multiple fields of a content then it will get a
			// higher score
			boolQuery.should(QueryBuilders.multiMatchQuery(topic, "_all").type(Type.MOST_FIELDS));

			SearchResponse response = client.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
							.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
							.source(new SearchSourceBuilder().query(boolQuery).size(RECS_PER_INTEREST)),
					RequestOptions.DEFAULT);

			for (SearchHit hit : response.getHits()) {
				Map<String, Object> courseMeta = hit.getSourceAsMap();
				double score = hit.getScore();
				String contentId = (String) courseMeta.get("identifier");

				// A course can be recommended because of multiple
				// interests, the final score of a recommendation
				// is the sum of the scores due to individual interests
				if (recsHash.containsKey(contentId)) {
					CourseRecommendation existingRec = recsHash.get(contentId);
					existingRec.setScore(existingRec.getScore() + score);
					existingRec.getReasonsForRecommendation().add(topic);
					recsHash.put(contentId, existingRec);
				} else {
					HashSet<String> reason = new HashSet<>();
					reason.add(topic);
					CourseRecommendation newRec = new CourseRecommendation(score, courseMeta, reason);
					recsHash.put(contentId, newRec);
				}
			}
		}

		// sort the recommendation in descending order of scores
		// CourseRecommendation implements comparable interface
		List<CourseRecommendation> recs = recsHash.values().stream().sorted().collect(Collectors.toList());
		return recs;
	}

}
