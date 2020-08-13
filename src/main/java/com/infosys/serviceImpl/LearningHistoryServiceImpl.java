/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.LearningHistoryService;

@Service
public class LearningHistoryServiceImpl implements LearningHistoryService {

	@Autowired
	ProgressRepository progRepository;

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getUserCourseProgress(String emailId, Integer pageNumber, Integer pageSize,
			String status, Integer version, String contentType) throws Exception{
		
		//Validating Parameters
		this.validateParams(emailId);
		
		Map<String, Object> ret = null;
		if (version == 2)
			ret = progRepository.getUserCourseProgressPaginated(emailId, status, contentType, pageNumber, pageSize);
		else
			ret = progRepository.getUserCourseProgressNonPaginated(emailId, status, contentType);
		if (!ret.get("count").toString().equals("0")) {
			ret.put("results", this.calculateTimeLeftAndProgress((List<Map<String, Object>>) ret.get("results")));
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> calculateTimeLeftAndProgress(List<Map<String, Object>> courseList) {
		for (Map<String, Object> m : courseList) {
			if (!m.containsKey("generated")) {
				String contentType = m.get("contentType").toString();
				if (contentType.toLowerCase().equals("course") || contentType.toLowerCase().equals("collection")) {
					if (m.get("progress").equals(1)) {
						m.put("timeLeft", 0);
					} else {
						List<String> childList = (List<String>) m.get("children");
						float timeLeft = 0f;
						for (String child : childList) {
							timeLeft += Float.parseFloat(
									((Map<String, Object>) m.get("children_duration")).get(child).toString())
									* (1 - Float.parseFloat(
											((Map<String, Object>) m.get("progress_children")).get(child).toString()));
						}
						m.put("timeLeft", timeLeft);
					}
				} else {
					m.put("timeLeft", ((1 - Float.parseFloat(m.get("progress").toString()))
							* Float.parseFloat(m.get("totalDuration").toString())));
				}
				m.put("progress", Math.round(Float.parseFloat(m.get("progress").toString()) * 1000) / 1000f);
				m.put("pending", Math.round(Float.parseFloat(m.get("pending").toString()) * 1000) / 1000f);
				m.remove("progress_children");
				m.remove("children_duration");
			}
		}

		return courseList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getUserContentListProgress(String emailId, List<String> contentIds)
			throws Exception {
		Map<String, Object> data = new HashMap<>();
		if (contentIds.size() > 0)
			data = progRepository.fetchUserContentListProgress(emailId, contentIds);
		else
			throw new Exception("Empty Request");
		Map<String, Object> tempData = (Map<String, Object>) data.get("results");
		if (((List<String>) data.get("missingIds")).size() > 0)
			tempData.putAll(this.getFromElasticSearch((List<String>) data.get("missingIds")));

		return this.calculateTimeLeftAndProgress(this.getOrderedData(contentIds, tempData));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getFromElasticSearch(List<String> ids) {
		Map<String, Object> source = new HashMap<>();
		try {
			SearchResponse response = ConnectionManager.getClient()
					.search(new SearchRequest().indices("lexcontentindex").types("resource")
							.searchType(
									SearchType.QUERY_THEN_FETCH)
							.source(new SearchSourceBuilder()
									.fetchSource(new String[] { "name", "contentType", "duration", "thumbnail",
											"identifier", "children" }, new String[0])
									.query(QueryBuilders.boolQuery().must(QueryBuilders.termsQuery("_id", ids)))
									.size(ids.size())),
							RequestOptions.DEFAULT);
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> temp = hit.getSourceAsMap();
				temp.put("totalDuration", temp.get("duration"));
				temp.put("timeLeft", temp.get("duration"));
				temp.remove("dration");
				temp.put("progress", 0);
				temp.put("pending", 1);
				temp.put("generated", true);
				temp.put("timeSpent", 0);
				List<String> childIds = new ArrayList<>();
				for (Map<String, Object> children : (List<Map<String, Object>>) temp.get("children"))
					childIds.add(children.get("identifier").toString());
				temp.put("children", childIds);
				source.put(temp.get("identifier").toString(), temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return source;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getOrderedData(List<String> inputIds, Map<String, Object> data) {
		List<Map<String, Object>> source = new ArrayList<>();
		for (String id : inputIds)
			if (data.get(id) != null)
				source.add((Map<String, Object>) data.get(id));
		return source;
	}

	@Override
	public Map<String, Object> getContentListProgress(String emailId, List<String> contentIds) {
		return progRepository.fetchUserContentListMap(emailId, contentIds);
	}

	
	private void validateParams(String emailId) {
		if (!emailId.matches("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$")) {
			throw new InvalidDataInputException("Bad Request");
		}
	}
}
