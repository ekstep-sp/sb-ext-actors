/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
© 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
under the law.

Highly Confidential
 
*/

package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.model.ProgressData;
import com.infosys.model.cassandra.ContentProgressModel;
import com.infosys.model.cassandra.InfyMeDownloadDataModel;
import com.infosys.repository.ContentProgressRepository;
import com.infosys.repository.InfyMeDownloadDataRepository;
import com.infosys.repository.ProgressRepository;
import com.infosys.repository.UserRepository;
import com.infosys.service.TodoService;
import com.infosys.util.Constants;

@Service
public class TodoServiceImpl implements TodoService {

	private RestHighLevelClient elasticClient = ConnectionManager.getClient();

	private ImmutableMap<String, String> contentIdMessageMap = ImmutableMap.of("lex_auth_012759681209475072315",
			"Watch Nandan's video", "lex_auth_012759640813477888305", "Watch UBP's video");

	@Autowired
	InfyMeDownloadDataRepository infyMeDownloadDataRepo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	ProgressRepository progressRepo;

	
	@Autowired
	ContentProgressRepository contentProgressRepo;

	@Override
	public List<Map<String, Object>> getTodoInfo(String userId, String groupId, String uidType) throws Exception {

		List<Map<String, Object>> todoInfo = new ArrayList<>();
		Map<String, Object> userDataMap = new HashMap<String, Object>();
		String emailId = "";
		String uuid = "";

		try {
			if (uidType.equals("email")) {
				emailId = userId;
				userDataMap = userRepo.getUUIDFromEmail(emailId);
				uuid = userDataMap.get("id").toString();
			} else {
				uuid = userId;
				userDataMap = userRepo.getEmailFromUUID(uuid);
				if (userDataMap.isEmpty())
					throw new InvalidDataInputException("EmailId Doesn't exists");
				emailId = userDataMap.get("email").toString();
			}

			this.infyMeDownload(todoInfo, emailId);

			// checks if the videos has been watched
			this.checkResourceCompletion(todoInfo, uuid);

			// checks page feedback has been provided
			this.checkFeedbackSubmitted(todoInfo, "ocm", uuid);

		} catch (InvalidDataInputException ex) {
			Map<String, Object> infyMe = new HashMap<>();
			infyMe.put("name", "Download InfyMe app");

			infyMe.put("completed", false);
			infyMe.put("contentId", null);
			todoInfo.add(infyMe);

			Map<String, Object> contentCompletion = new HashMap<>();
			contentCompletion.put("name", "Watch Nandan's video");
			contentCompletion.put("completed", false);
			contentCompletion.put("comtentId", "lex_auth_012602584559599616485");
			todoInfo.add(contentCompletion);
			contentCompletion = new HashMap<>();
			contentCompletion.put("name", "Watch UBP's video");
			contentCompletion.put("completed", false);
			contentCompletion.put("comtentId", "lex_auth_012759640813477888305");
			todoInfo.add(contentCompletion);

			Map<String, Object> feedbackSubmitted = new HashMap<>();
			feedbackSubmitted.put("name", "Provide Feedback");
			feedbackSubmitted.put("completed", false);
			feedbackSubmitted.put("contentId", null);
			todoInfo.add(feedbackSubmitted);

		}
		return todoInfo;
	}


	private void checkFeedbackSubmitted(List<Map<String, Object>> todoInfo, String contentId, String userId)
			throws IOException {

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("feedback_type", contentId))
				.must(QueryBuilders.matchQuery("user_id", userId)));
		SearchResponse response = elasticClient.search(
				new SearchRequest().indices("lex_feedback").types("feedback").source(sourceBuilder),
				RequestOptions.DEFAULT);
		Map<String, Object> feedbackSubmitted = new HashMap<>();
		feedbackSubmitted.put("name", "Provide Feedback");
		if (response.getHits().totalHits == 0) {
			feedbackSubmitted.put("completed", false);
		} else {
			feedbackSubmitted.put("completed", true);
		}
		feedbackSubmitted.put("contentId", null);
		todoInfo.add(feedbackSubmitted);
	}

	private void infyMeDownload(List<Map<String, Object>> todoInfo, String emailId) {

		List<InfyMeDownloadDataModel> userDownloadData = infyMeDownloadDataRepo
				.findByKeyBucketAndKeyEmail("common_bucket", emailId);

		Map<String, Object> infyMe = new HashMap<>();
		infyMe.put("name", "Download InfyMe app");

		if (userDownloadData.isEmpty()) {
			infyMe.put("completed", false);
		} else {
			infyMe.put("completed", true);
		}
		infyMe.put("contentId", null);
		todoInfo.add(infyMe);

	}

	private void checkResourceCompletion(List<Map<String, Object>> todoInfo, String emailId) {

		List<String> contentIds = new ArrayList<String>(contentIdMessageMap.keySet());
		List<Map<String, Object>> resourceCompletionList = new ArrayList<Map<String, Object>>();
		List<ProgressData> progressDataList = progressRepo.findProgressOfCourses(emailId, contentIds);
		for (ProgressData progressData : progressDataList) {
			String contentId = progressData.getContentId();
			contentIds.remove(contentId);
			String todoText = contentIdMessageMap.get(contentId);
			Map<String, Object> contentCompletion = new HashMap<>();
			contentCompletion.put("name", todoText);
			contentCompletion.put("completed", true);
			contentCompletion.put("contentId", contentId);
			
			if (todoText.toLowerCase().contains("nandan"))
				resourceCompletionList.add(0, contentCompletion);
			else
				resourceCompletionList.add(contentCompletion);
		}
		for (String contentId : contentIds) {
			Map<String, Object> contentCompletion = new HashMap<>();
			String todoText = contentIdMessageMap.get(contentId);
			contentCompletion.put("name", contentIdMessageMap.get(contentId));
			contentCompletion.put("completed", false);
			contentCompletion.put("contentId", contentId);
			if (todoText.toLowerCase().contains("nandan"))
				resourceCompletionList.add(0, contentCompletion);
			else
				resourceCompletionList.add(contentCompletion);
		}
		todoInfo.addAll(resourceCompletionList);

	}

	private void checkResourceCompletionUsingCassandra(List<Map<String, Object>> todoInfo, String userUUID) {

		List<String> contentIds = new ArrayList<String>(contentIdMessageMap.keySet());
		List<Map<String, Object>> resourceCompletionList = new ArrayList<Map<String, Object>>();
		List<ContentProgressModel> progressDataList = contentProgressRepo.findProgressByUserIdAndContentId(Constants.INFOSYS_ROOTORG,userUUID,
				"Resource", contentIds);
		for (ContentProgressModel progressData : progressDataList) {
			Float progress = progressData.getProgress();
			if (progress.floatValue() == 1) {
				String contentId = progressData.getPrimaryKey().getContentId();
				contentIds.remove(contentId);
				Map<String, Object> contentCompletion = new HashMap<>();
				String todoText = contentIdMessageMap.get(contentId);
				contentCompletion.put("name", todoText);
				contentCompletion.put("completed", true);
				contentCompletion.put("contentId", contentId);
				// This was done as nandan video todo data has to be first in the list
				if (todoText.toLowerCase().contains("nandan"))
					resourceCompletionList.add(0, contentCompletion);
				else
					resourceCompletionList.add(contentCompletion);
			}

		}
		for (String contentId : contentIds) {
			Map<String, Object> contentCompletion = new HashMap<>();
			String todoText = contentIdMessageMap.get(contentId);
			contentCompletion.put("name", contentIdMessageMap.get(contentId));
			contentCompletion.put("completed", false);
			contentCompletion.put("contentId", contentId);
			if (todoText.toLowerCase().contains("nandan"))
				resourceCompletionList.add(0, contentCompletion);
			else
				resourceCompletionList.add(contentCompletion);
		}
		todoInfo.addAll(resourceCompletionList);

	}
	
	

	@Override
	public List<Map<String, Object>> getTodoInfoUsingNewContentProgress(String userId, String groupId) throws Exception {

		List<Map<String, Object>> todoInfo = new ArrayList<>();
		Map<String, Object> userDataMap = new HashMap<String, Object>();
		String emailId = "";
		String uuid = "";

		try {

			uuid = userId;
			userDataMap = userRepo.getEmailFromUUID(uuid);
			if (userDataMap.isEmpty())
				throw new InvalidDataInputException("EmailId Doesn't exists");
			emailId = userDataMap.get("email").toString();

			this.infyMeDownload(todoInfo, emailId);

			// checks if the videos has been watched
			this.checkResourceCompletionUsingCassandra(todoInfo, uuid);;

			// checks page feedback has been provided
			this.checkFeedbackSubmitted(todoInfo, "ocm", uuid);

		} catch (InvalidDataInputException ex) {
			Map<String, Object> infyMe = new HashMap<>();
			infyMe.put("name", "Download InfyMe app");

			infyMe.put("completed", false);
			infyMe.put("contentId", null);
			todoInfo.add(infyMe);

			Map<String, Object> contentCompletion = new HashMap<>();
			contentCompletion.put("name", "Watch Nandan's video");
			contentCompletion.put("completed", false);
			contentCompletion.put("comtentId", "lex_auth_012602584559599616485");
			todoInfo.add(contentCompletion);
			contentCompletion = new HashMap<>();
			contentCompletion.put("name", "Watch UBP's video");
			contentCompletion.put("completed", false);
			contentCompletion.put("comtentId", "lex_auth_012759640813477888305");
			todoInfo.add(contentCompletion);

			Map<String, Object> feedbackSubmitted = new HashMap<>();
			feedbackSubmitted.put("name", "Provide Feedback");
			feedbackSubmitted.put("completed", false);
			feedbackSubmitted.put("contentId", null);
			todoInfo.add(feedbackSubmitted);

		}
		return todoInfo;
	}

}
