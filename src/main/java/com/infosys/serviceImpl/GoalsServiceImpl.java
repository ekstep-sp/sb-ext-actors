/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 *
 */
package com.infosys.serviceImpl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.CommonLearningGoals;
import com.infosys.model.ContentMeta;
import com.infosys.model.UserGoalProgress;
import com.infosys.model.UserLearningGoals;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.GoalsService;
import com.infosys.service.LearningHistoryService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import com.infosys.util.Util.DbInfo;

/**
 * @author Gaurav_Kumar48
 */
@Service
public class GoalsServiceImpl implements GoalsService {

	@Autowired
	ProgressRepository prRepository;

	@Autowired
	LearningHistoryService lhService;

	@Autowired
	UserUtilityService userUtilService;

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private Util.DbInfo userLearningGoalsDb = Util.dbInfoMap.get(LexJsonKey.USER_Learning_Goals_Table);
	private Util.DbInfo commonLearningGoalsDb = Util.dbInfoMap.get(LexJsonKey.Common_Learning_Goals_Table);
	private Util.DbInfo sharedGoalsDb = Util.dbInfoMap.get(LexJsonKey.USER_SHARED_GOALS);
	private Util.DbInfo sharedGoalsTrackerDb = Util.dbInfoMap.get(LexJsonKey.SHARED_GOALS_TRACKER);
	private Util.DbInfo userGoalsTrackerDb = Util.dbInfoMap.get(LexJsonKey.USER_GOALS_TRACKER);

	private CassandraConnectionManager connectionManager;

	public GoalsServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infosys.service.CohortService#removeLearningGoals() remove a
	 * learning goal of a user based on goalid and userid
	 */
	@Override
	public Response removeLearningGoals(Map<String, Object> goalsData) {
		String goalId = null;
		String userEmail = null;
		String goalType = null;
		Response contentResponse = new Response();
		try {
			goalId = ((String) goalsData.get("goal_id")).trim();
			userEmail = ((String) goalsData.get("user_email")).trim();
			goalType = ((String) goalsData.get("goal_type")).trim();
			Map<String, Object> goalToBeDeleted = new HashMap<String, Object>();
			goalToBeDeleted.put("user_email", userEmail);
			goalToBeDeleted.put("goal_id", UUID.fromString(goalId));
			goalToBeDeleted.put("goal_type", goalType);

			System.out.println(goalType + " " + goalId + " " + userEmail);

			if (goalType.equals("sharedby")) {
				executeBatchQueries(getDeleteStatementsForSharedGoal(goalToBeDeleted));
			} else {
				/*
				 * cassandraOperation.deleteRecordByProperties(userLearningGoalsDb.getKeySpace()
				 * ,userLearningGoalsDb.getTableName(), goalToBeDeleted);
				 */
				Delete.Where deleteUserGoals = QueryBuilder.delete()
						.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
						.where(eq("user_email", userEmail)).and(eq("goal_id", UUID.fromString(goalId)))
						.and(eq("goal_type", goalType));
				List<Statement> deleteList = new ArrayList<Statement>();
				deleteList.add(deleteUserGoals);
				this.executeBatchQueries(deleteList);
			}
			return contentResponse;

		} catch (ProjectCommonException e) {
			e.printStackTrace();
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_DELETE + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
	}

	// add/update user learning goals

	@SuppressWarnings({ "unchecked" })
	@Override
	public Response upsertLearningGoals_v1(Map<String, Object> userGoalsData) throws ParseException {
		Response contentResponse = new Response();
		String userEmail = null;
		List<String> goalContentList = null;
		String goalDesc = null;
		String goalId = null;
		String goalType = null;
		String goalTitle = null;
		List<Map<String, Object>> output = new ArrayList<Map<String, Object>>();
		try {

			userEmail = ((String) userGoalsData.get("user_email")).trim();
			for (Map<String, Object> goalsData : (List<Map<String, Object>>) userGoalsData.get("goal_data")) {
				List<Map<String, Object>> contentMessage = new ArrayList<Map<String, Object>>();
				goalId = (String) goalsData.get("goal_id");
				goalDesc = ((String) goalsData.get("goal_desc"));
				if (goalsData.get("goal_content_id") != null)
					goalContentList = (List<String>) goalsData.get("goal_content_id");
				goalType = ((String) goalsData.get("goal_type"));
				goalTitle = (String) goalsData.get("goal_title");
				List<Map<String, Object>> parentPrograms = new ArrayList<Map<String, Object>>();

				// removing duplicate elements if any
				goalContentList = goalContentList.stream().distinct().collect(Collectors.toList());
				List<Map<String, Object>> resourceMetaList = ElasticSearchUtil.searchDataByValues(
						LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
						LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, goalContentList,
						goalContentList.size());

				for (Iterator<String> iterator = goalContentList.iterator(); iterator.hasNext();) {
					String resourceId = iterator.next();

					// calling elastic search to get content meta data
					// Map<String, Object> resourceMeta = ElasticSearchUtil.getDataByIdentifier(
					// LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
					// LexProjectUtil.EsType.new_lex_search.getTypeName(),
					// resourceId);
					for (Map<String, Object> map : resourceMetaList) {
						if (map.containsKey("identifier")) {
							if (map.get("identifier").toString().equals(resourceId)) {

								if (map.containsKey("status")
										&& map.get("status").toString().toLowerCase().equals("live")) {
									if (map.containsKey("contentType")) {
										String contentType = map.get("contentType").toString().toLowerCase();
										if (contentType.equals("course") || contentType.equals("learning path")) {
											if (contentType.equals("course")) {
												if (map.containsKey("collections")) {
													parentPrograms = (List<Map<String, Object>>) map.get("collections");

													for (Map<String, Object> program : parentPrograms) {
														if (goalContentList
																.contains(program.get("identifier").toString())) {
															int courseIndex = goalContentList.indexOf(resourceId);
															int programIndex = goalContentList
																	.indexOf(program.get("identifier").toString());

															Map<String, Object> parentExists = new HashMap<String, Object>();
															if (courseIndex < programIndex) {
																StringBuilder message = new StringBuilder(
																		"Goal updated and ");
																message.append(map.get("name").toString());

																for (Map<String, Object> resourceMap : resourceMetaList) {

																	if (resourceMap.get("identifier").toString().equals(
																			program.get("identifier").toString())) {
																		message.append(" has been moved to ");
																		if (resourceMap.containsKey("name"))
																			message.append(
																					resourceMap.get("name").toString());
																	}
																}
																parentExists.put("result", "success");
																parentExists.put("message", message.toString());

															} else {
																parentExists.put("result", "failed");
																parentExists.put("message",
																		"Course cannot be added as it is already part of an existing program in this goal");
															}
															// remove course since its parent program is already in goal
															contentMessage.add(parentExists);
															iterator.remove();
															break;
														}
													}
												}
											}
										} else {
											// not a course or learning path
											iterator.remove();
											break;
										}
									}
									break;
								} else {
									// not a live course
									iterator.remove();
									break;
								}
							}
						}

					}

				}
				if (!goalContentList.isEmpty()) {
					Map<String, Object> objectToBeInserted = new HashMap<String, Object>();
					Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
					Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
					objectToBeInserted.put("user_email", userEmail);
					objectToBeInserted.put("goal_type", goalType);
					if (goalId != null && !goalId.isEmpty()) {
						objectToBeInserted.put("goal_id", UUID.fromString(goalId));
					} else {
						goalId = ProjectUtil.generateUniqueId();
						objectToBeInserted.put("goal_id", UUID.fromString(goalId));
						objectToBeInserted.put("created_on", timestamp);
					}
					if (goalTitle != null && !goalTitle.isEmpty()) {
						objectToBeInserted.put("goal_title", goalTitle);
					}
					if (goalDesc != null && !goalDesc.isEmpty()) {
						objectToBeInserted.put("goal_desc", goalDesc);
					}
					objectToBeInserted.put("last_updated_on", timestamp);
					objectToBeInserted.put("goal_content_id", goalContentList);
					contentResponse = cassandraOperation.upsertRecord(userLearningGoalsDb.getKeySpace(),
							userLearningGoalsDb.getTableName(), objectToBeInserted);
				} else {
					if (goalId != null && !goalId.isEmpty()) {
						Map<String, Object> goalToBeDeleted = new HashMap<String, Object>();
						goalToBeDeleted.put("user_email", userEmail);
						goalToBeDeleted.put("goal_id", UUID.fromString(goalId));
						// user can remove only custom user goal by this method
						goalToBeDeleted.put("goal_type", "user");
						cassandraOperation.deleteRecordsByProperties(userLearningGoalsDb.getKeySpace(),
								userLearningGoalsDb.getTableName(), goalToBeDeleted);
					}
				}
				Map<String, Object> finalOutput = new HashMap<String, Object>();
				finalOutput.put("goal_id", goalId);
				if (contentMessage.isEmpty()) {
					Map<String, Object> outputMap = new HashMap<String, Object>();
					outputMap.put("result", "success");
					outputMap.put("message", "success");
					contentMessage.add(outputMap);
				}
				finalOutput.put("output", contentMessage);
				output.add(finalOutput);
			}
		} catch (ProjectCommonException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		contentResponse.put(Constants.RESPONSE, output);
		return contentResponse;
	}

	// add/update user learning goals

	@SuppressWarnings({ "unchecked" })
	@Override
	public Response upsertLearningGoals(Map<String, Object> userGoalsData) throws ParseException {
		/*
		 * various goal types that are created by this method goal types in table
		 * user_learning_goals user- custom goal created by user for himself common-
		 * goal that are pre-created and listed for all users to choose from.
		 * tobeshared- goal created for sharing with others but not shared yet. this
		 * goal type is deleted once the goal has been shared with even one person.
		 * commonshared- choosing a common goal for sharing but not shared yet. this
		 * goal type is deleted once the goal has been shared with even one person.
		 * 
		 * now coming to goal_types in user_shared_goals. all goals that are shared are
		 * stored in this table. this has custom_shared- goals that has been created by
		 * sharer for others common_shared- common goals that has been shared by others.
		 */
		Response contentResponse = new Response();
		List<Map<String, Object>> output = new ArrayList<Map<String, Object>>();
		try {
			String userEmail = ((String) userGoalsData.get("user_email")).trim();
			for (Map<String, Object> goalsData : (List<Map<String, Object>>) userGoalsData.get("goal_data")) {
				Map<String, Object> objectMapForDb = prepareGoalData(userEmail, goalsData);
				Map<String, Object> finalOutput = new HashMap<String, Object>();
				if (objectMapForDb != null) {
					if (!((List<String>) objectMapForDb.get("goal_content_id")).isEmpty()) {
						if (objectMapForDb.get("goal_type").equals("share_with")) {
							objectMapForDb.remove("goal_type");
							finalOutput.put("self_shared", objectMapForDb.get("self_shared"));
							finalOutput.put("already_shared", objectMapForDb.get("already_shared"));
							finalOutput.put("user_count", objectMapForDb.get("user_count"));
							finalOutput.put("invalid_users", objectMapForDb.get("invalid_users"));
							if (((int) (objectMapForDb.get("self_shared")) == 1
									&& ((List<String>) objectMapForDb.get("user_list")).isEmpty())
									|| ((int) objectMapForDb.get("user_count") > 0)
									|| ((int) (objectMapForDb.get("invalid_share")) == 1)) {
								finalOutput.put("result", "failed");
								finalOutput.put("fail_reason", objectMapForDb.get("fail_reason"));
							} else {
								objectMapForDb.remove("self_shared");
								executeBatchQueries(getInsertStmtForGoalSharing(sharedGoalsDb.getKeySpace(),
										sharedGoalsDb.getTableName(), objectMapForDb));
							}
						} else if (objectMapForDb.get("goal_type").equals("shared")) {
							executeBatchQueries(insertStmtForUserActionSharedGoals(objectMapForDb));
						} else {
							if (objectMapForDb.containsKey("invalid_goal")
									&& (int) objectMapForDb.get("invalid_goal") == 1) {
								finalOutput.put("result", "failed");
								finalOutput.put("fail_reason", objectMapForDb.get("fail_reason"));
							} else {
								contentResponse = cassandraOperation.upsertRecord(userLearningGoalsDb.getKeySpace(),
										userLearningGoalsDb.getTableName(), objectMapForDb);
							}
						}
					} else {
						// for deleting a particular goal
						objectMapForDb.remove("goal_content_id");
						cassandraOperation.deleteRecordsByProperties(userLearningGoalsDb.getKeySpace(),
								userLearningGoalsDb.getTableName(), objectMapForDb);
					}
					if (!finalOutput.containsKey("result")) {
						finalOutput.put("result", "success");
					}
				} else {
					finalOutput.put("result", "failed");
				}

				finalOutput.put("goal_id", objectMapForDb.get("goal_id"));
				output.add(finalOutput);
				/*
				 * if (contentMessage.isEmpty()) { Map<String, String> outputMap = new
				 * HashMap<String, String>(); outputMap.put("result", "success");
				 * outputMap.put("message", "success"); contentMessage.add(outputMap); }
				 */
			}
		} catch (ProjectCommonException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		contentResponse.put(Constants.RESPONSE, output);
		return contentResponse;
	}

	// fetches the list of user goals barring common goals chosen by user

	@Override
	public Response getUserGoalsList(String userEmail) {
		Response contentResponse = new Response();
		String[] identifierCols = { "user_email", "goal_type" };
		Object[] identifierColValues = { userEmail, "user" };
		String[] columnRequired = { "goal_id", "goal_content_id", "goal_desc", "goal_type", "goal_title",
				"goal_duration" };
		// user learning goals
		contentResponse = cassandraOperation.getSpecifiedPropertiesRecords(userLearningGoalsDb.getKeySpace(),
				userLearningGoalsDb.getTableName(), identifierCols, identifierColValues, columnRequired);
		return contentResponse;
	}

	@Override
	public Response getUserGoalsList_v1(String userEmail) {
		Response contentResponse = new Response();
		String[] identifierCols = { "user_email", "goal_type" };
		Object[] identifierColValues = { userEmail, "user" };
		String[] columnRequired = { "goal_id", "goal_content_id", "goal_desc", "goal_type", "goal_title" };
		// user learning goals
		contentResponse = cassandraOperation.getSpecifiedPropertiesRecords(userLearningGoalsDb.getKeySpace(),
				userLearningGoalsDb.getTableName(), identifierCols, identifierColValues, columnRequired);
		return contentResponse;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Response getSharedGoalsList(String userEmail, String goalType) {
		Response contentResponse = new Response();
		Map<String, Object> identifierCols = new HashMap<String, Object>();
		identifierCols.put("user_email", userEmail);
		identifierCols.put("goal_type", goalType);

		List<Map<String, Object>> userGoalsData = null;
		if (goalType.equals("sharedby")) {
			// fetch shared goals by user
			contentResponse = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(), sharedGoalsTrackerDb.getTableName(),
					identifierCols);
			userGoalsData = (List<Map<String, Object>>) contentResponse.getResult().get("response");
			// fetch tobeshared goals
			identifierCols.put("goal_type", "tobeshared");
			contentResponse = getSharedGoals(userGoalsTrackerDb.getKeySpace(), userGoalsTrackerDb.getTableName(),
					identifierCols);
			List<Map<String, Object>> tobeSharedGoalsData = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");
			userGoalsData.addAll(tobeSharedGoalsData);
		} else {
			contentResponse = getSharedGoals(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName(), identifierCols);
			userGoalsData = (List<Map<String, Object>>) contentResponse.getResult().get("response");
		}
		if (userGoalsData != null && !userGoalsData.isEmpty()) {
			// sorting usergoals in last updated order
			Collections.sort(userGoalsData, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> m1, Map<String, Object> m2) {
					if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null) {
						return ((Date) m1.get("last_updated_on")).compareTo((Date) (m2.get("last_updated_on")));
					} else {
						return -1;
					}
				}
			}.reversed());
		}
		if (goalType.equals("sharedby")) {
			List<Map<String, Object>> sharedUserGoalProgress = organizeSharedGoals(userGoalsData);
			contentResponse.put(Constants.RESPONSE, sharedUserGoalProgress);
		} else {
			for (Iterator<Map<String, Object>> iterator = userGoalsData.iterator(); iterator.hasNext();) {
				Map<String, Object> map = iterator.next();
				map.remove("last_updated_on");
				if (goalType.equals("sharedwith")) {
					map.remove("goal_start_date");
					map.remove("goal_end_date");
				}
				List<String> resourceIds = (List<String>) map.get("goal_content_id");
				if (resourceIds != null && !resourceIds.isEmpty()) {
					List<Map<String, Object>> goalresources = ElasticSearchUtil.searchDataByValues(
							LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
							LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
							resourceIds.size());
					List<Map<String, Object>> goalsContent = new ArrayList<Map<String, Object>>();
					for (String resIdentifier : resourceIds) {
						for (Map<String, Object> resourceMap : goalresources) {
							if (resIdentifier.equalsIgnoreCase(resourceMap.get("identifier").toString())) {
								Map<String, Object> contentMap = new HashMap<String, Object>();
								contentMap.put("resource_name", resourceMap.get("name"));
								contentMap.put("resource_id", resourceMap.get("identifier"));
								contentMap.put("time_duration", resourceMap.get("duration"));
								contentMap.put("contentType", resourceMap.get("contentType"));
								contentMap.put("mimeType", resourceMap.get("mimeType"));
								goalsContent.add(contentMap);
								break;
							}
						}
					}
					map.put("resource", goalsContent);
				}
			}
			contentResponse.put(Constants.RESPONSE, userGoalsData);
		}

		return contentResponse;
	}

	/*
	 * @SuppressWarnings("unchecked")
	 *
	 * @Override public Response getSharedGoalsList(String userEmail, String
	 * goalType) { Response contentResponse = new Response(); Map<String, Object>
	 * identifierCols = new HashMap<String, Object>();
	 * identifierCols.put("user_email", userEmail); identifierCols.put("goal_type",
	 * goalType);
	 *
	 * List<Map<String, Object>> userGoalsData = null; if
	 * (goalType.equals("sharedby")) { // fetch shared goals by user contentResponse
	 * = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(),
	 * sharedGoalsTrackerDb.getTableName(), identifierCols); userGoalsData =
	 * (List<Map<String, Object>>) contentResponse.getResult().get("response"); //
	 * fetch tobeshared goals identifierCols.put("goal_type", "tobeshared");
	 * contentResponse = getSharedGoals(userGoalsTrackerDb.getKeySpace(),
	 * userGoalsTrackerDb.getTableName(), identifierCols); List<Map<String, Object>>
	 * tobeSharedGoalsData = (List<Map<String, Object>>) contentResponse.getResult()
	 * .get("response"); userGoalsData.addAll(tobeSharedGoalsData); } else {
	 * contentResponse = getSharedGoals(sharedGoalsDb.getKeySpace(),
	 * sharedGoalsDb.getTableName(), identifierCols); userGoalsData =
	 * (List<Map<String, Object>>) contentResponse.getResult().get("response"); } if
	 * (userGoalsData != null && !userGoalsData.isEmpty()) { // sorting usergoals in
	 * last updated order Collections.sort(userGoalsData, new Comparator<Map<String,
	 * Object>>() {
	 *
	 * @Override public int compare(Map<String, Object> m1, Map<String, Object> m2)
	 * { if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null)
	 * { return ((Date) m1.get("last_updated_on")).compareTo((Date)
	 * (m2.get("last_updated_on"))); } else { return -1; } } }.reversed()); }
	 *
	 * for (Iterator<Map<String, Object>> iterator = userGoalsData.iterator();
	 * iterator.hasNext();) { Map<String, Object> map = iterator.next();
	 * map.remove("last_updated_on"); if (goalType.equals("sharedwith")) {
	 * map.remove("goal_start_date"); map.remove("goal_end_date"); } List<String>
	 * resourceIds = (List<String>) map.get("goal_content_id"); if (resourceIds !=
	 * null && !resourceIds.isEmpty()) { List<Map<String, Object>> goalresources =
	 * ElasticSearchUtil.searchDataByValues(
	 * ProjectUtil.EsIndex.new_lex_search.getIndexName(),
	 * ProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
	 * resourceIds.size()); List<Map<String, Object>> goalsContent = new
	 * ArrayList<Map<String, Object>>(); for (Map<String, Object> resourceMap :
	 * goalresources) { Map<String, Object> contentMap = new HashMap<String,
	 * Object>(); contentMap.put("resource_name", resourceMap.get("name"));
	 * contentMap.put("resource_id", resourceMap.get("identifier"));
	 * contentMap.put("time_duration", resourceMap.get("duration"));
	 * contentMap.put("contentType", resourceMap.get("contentType"));
	 * contentMap.put("mimeType", resourceMap.get("mimeType"));
	 * goalsContent.add(contentMap); } map.put("resource", goalsContent); } }
	 * contentResponse.put(Constants.RESPONSE, userGoalsData);
	 *
	 * return contentResponse; }
	 */

	// get the list of common goals barring those chosen already by user
	@Override
	public Response getCommonGoals_v1(String userEmail) {
		Response contentResponse = new Response();

		// fetching common learning goals
		contentResponse = cassandraOperation.getAllRecords(commonLearningGoalsDb.getKeySpace(),
				commonLearningGoalsDb.getTableName());

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> commonLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");

		Map<String, Object> userQueryData = new HashMap<String, Object>();
		userQueryData.put("user_email", userEmail);
		userQueryData.put("goal_type", "common");
		// user learning goals
		contentResponse = cassandraOperation.getRecordsByProperties(userLearningGoalsDb.getKeySpace(),
				userLearningGoalsDb.getTableName(), userQueryData);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> userLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		ObjectMapper mapper = new ObjectMapper();
		List<CommonLearningGoals> commonGoalsDetails = new ArrayList<CommonLearningGoals>();
		boolean flag = true; // default common goals not exist in user goals.
		for (Iterator<Map<String, Object>> iterator = commonLearningGoals.iterator(); iterator.hasNext();) {
			flag = true;
			Map<String, Object> learningGoal = iterator.next();
			for (Map<String, Object> userGoal : userLearningGoals) {
				if (userGoal.get("goal_content_id").equals(learningGoal.get("goal_content_id"))) {
					iterator.remove();
					flag = false;
					break;
				}
			}

			if (flag)
				commonGoalsDetails.add(mapper.convertValue(learningGoal, CommonLearningGoals.class));
		}
		contentResponse.put(Constants.RESPONSE, commonGoalsDetails);
		return contentResponse;
	}

	// get the list of common goals barring those chosen already by user
	@SuppressWarnings("unchecked")
	@Override
	public Response getCommonGoals(String userEmail) {
		Response contentResponse = new Response();

		// fetching common learning goals
		contentResponse = cassandraOperation.getAllRecords(commonLearningGoalsDb.getKeySpace(),
				commonLearningGoalsDb.getTableName());
		List<Map<String, Object>> commonLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		Map<String, Object> userQueryData = new HashMap<String, Object>();
		userQueryData.put("user_email", userEmail);
		userQueryData.put("goal_type", "common");
		userQueryData.put("keyspace_name", userGoalsTrackerDb.getKeySpace());
		userQueryData.put("table_name", userGoalsTrackerDb.getTableName());// user learning goals
		contentResponse = getDataForCommonGoals(userQueryData);
		List<Map<String, Object>> userLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		userQueryData.put("goal_type", "sharedby");
		userQueryData.put("keyspace_name", sharedGoalsTrackerDb.getKeySpace());
		userQueryData.put("table_name", sharedGoalsTrackerDb.getTableName());
		contentResponse = getDataForCommonGoals(userQueryData);

		List<Map<String, Object>> sharedLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");

		List<Map<String, Object>> commonGoalsData = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> notGrouped = new ArrayList<Map<String, Object>>();
		int userGoalFlag = 0; // default common goals not exist in user goals.
		int sharedGoalFlag = 0;
		for (Iterator<Map<String, Object>> iterator = commonLearningGoals.iterator(); iterator.hasNext();) {
			userGoalFlag = 0;
			sharedGoalFlag = 0;
			Map<String, Object> learningGoal = iterator.next();
			for (Map<String, Object> userGoal : userLearningGoals) {
				if ((userGoal.get("goal_id").equals(learningGoal.get("goal_id")))
						|| (userGoal.get("goal_content_id").equals(learningGoal.get("goal_content_id")))) {
					if (userGoal.get("goal_type").toString().equalsIgnoreCase("common")) {
						userGoalFlag = 1;
					} else {
						sharedGoalFlag = 2;
					}
					if (userGoalFlag > 0 && sharedGoalFlag > 0) {
						break;
					}
				}
			}
			if (sharedGoalFlag == 0) {
				for (Map<String, Object> sharedGoal : sharedLearningGoals) {
					if ((sharedGoal.get("goal_id").equals(learningGoal.get("goal_id")))
							|| (sharedGoal.get("goal_content_id").equals(learningGoal.get("goal_content_id")))) {
						sharedGoalFlag = 1;
						break;
					}
				}
			}
			if (userGoalFlag > 0 && sharedGoalFlag > 0) {
				iterator.remove();
				continue;
			}
			learningGoal.put("user_added", userGoalFlag);
			learningGoal.put("share_added", sharedGoalFlag);
			if (learningGoal.get("goal_group") != null) {
				boolean isAdded = false;
				for (Map<String, Object> cgData : commonGoalsData) {
					if (cgData.get("goal_group_title").toString().toLowerCase()
							.equals(learningGoal.get("goal_group").toString().toLowerCase())) {
						isAdded = true;
						List<Map<String, Object>> goalCollection = (List<Map<String, Object>>) cgData.get("goals");
						goalCollection.add(learningGoal);
						break;
					}
				}
				if (!isAdded) {
					List<Map<String, Object>> goalCollection = new ArrayList<Map<String, Object>>();
					goalCollection.add(learningGoal);
					Map<String, Object> goalGroup = new HashMap<String, Object>();
					goalGroup.put("goal_group_title", learningGoal.get("goal_group").toString());
					goalGroup.put("goals", goalCollection);
					commonGoalsData.add(goalGroup);
				}
			} else {
				notGrouped.add(learningGoal);

			}
		}
		if (!notGrouped.isEmpty()) {
			Map<String, Object> others = new HashMap<String, Object>();
			others.put("goal_group_title", "others");
			others.put("goals", notGrouped);
			commonGoalsData.add(others);
		}
		contentResponse.put(Constants.RESPONSE, commonGoalsData);
		return contentResponse;
	}

	// get the goal progress along with components progress of each user goal

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Response getGoalsProgress(String userEmail) {
		Response contentResponse = new Response();
		Map<String, Object> output = new HashMap<String, Object>();
		try {
			Map<String, Object> goalIdentifierData = new HashMap<String, Object>();
			goalIdentifierData.put("user_email", userEmail);
			contentResponse = getUserGoals(userGoalsTrackerDb.getKeySpace(), userGoalsTrackerDb.getTableName(), "user",
					goalIdentifierData);
			List<Map<String, Object>> userLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");
			contentResponse = getGoalsSharedWithMe(userEmail);
			List<Map<String, Object>> sharedLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");
			for (Map<String, Object> sharedMap : sharedLearningGoals) {
				userLearningGoals.add(sharedMap);
			}

			ObjectMapper mapper = new ObjectMapper();
			List<UserLearningGoals> userGoalsDetails = new ArrayList<UserLearningGoals>();
			// sorting goals in last updated order
			Collections.sort(userLearningGoals, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> m1, Map<String, Object> m2) {
					if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null) {
						return ((Date) m1.get("last_updated_on")).compareTo((Date) (m2.get("last_updated_on")));
					} else {
						return -1;
					}
				}
			}.reversed());
			for (Map<String, Object> userGoal : userLearningGoals) {
				userGoalsDetails.add(mapper.convertValue(userGoal, UserLearningGoals.class));
			}
			for (Iterator itr = userGoalsDetails.iterator(); itr.hasNext();) {
				UserLearningGoals goal = (UserLearningGoals) itr.next();
				double totalGoalTime = 0d;
				double totalReadTime = 0d;
				if (goal.getGoal_type().equalsIgnoreCase("common_shared")
						|| goal.getGoal_type().equalsIgnoreCase("custom_shared")) {
					goal.setGoal_type("shared");
				}
				List<String> resourceIds = new ArrayList(goal.getGoal_content_id());
				Collections.reverse(resourceIds);
				if (resourceIds != null && !resourceIds.isEmpty()) {
					List<Map<String, Object>> goalresources = ElasticSearchUtil.searchDataByValues(
							LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
							LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
							resourceIds.size());
					Map<String, Object> progressMap = new HashMap<String, Object>();
					progressMap.put("user_list", Arrays.asList(userEmail));
					progressMap.put("goal_content_id", resourceIds);
					List<Map<String, Object>> progressMongoDataMapData = getProgressOfGoalContent(progressMap);
					List<UserGoalProgress> goalProgCollection = new ArrayList<UserGoalProgress>();

					for (String resourceIdentifier : resourceIds) {
						for (Map<String, Object> resourceMap : goalresources) {
							if (resourceMap.get("identifier").toString().equalsIgnoreCase(resourceIdentifier)) {
								UserGoalProgress goalProgressData = new UserGoalProgress();
								// to check if resource is in mongo . if in mongo take mongo db resource time
								// else take elastic time
								boolean resourceFound = false;
								goalProgressData.setResource_id((String) resourceMap.get("identifier"));
								goalProgressData.setResource_name((String) resourceMap.get("name"));
								goalProgressData.setContentType((String) resourceMap.get("contentType"));
								goalProgressData.setMimeType((String) resourceMap.get("mimeType"));
								goalProgressData.setTime_left(Float.parseFloat(resourceMap.get("duration").toString()));
								goalProgressData
										.setTotal_duration(Float.parseFloat(resourceMap.get("duration").toString()));
								goalProgressData.setPending(1);
								if (progressMongoDataMapData != null && !progressMongoDataMapData.isEmpty()) {
									List<Map<String, Object>> resProgressMongoData = new ArrayList<Map<String, Object>>();
									resProgressMongoData = (List<Map<String, Object>>) progressMongoDataMapData.get(0)
											.get("data");
									for (Map<String, Object> tempMap : resProgressMongoData) {
										if (tempMap.get("cid").toString()
												.equalsIgnoreCase(resourceMap.get("identifier").toString())) {
											double progress = Double.parseDouble(tempMap.get("progress").toString());
											double resourceTime = Double.parseDouble(tempMap.get("length").toString());
											double timeLeft = resourceTime - (resourceTime * progress);
											totalReadTime += resourceTime * progress;
											goalProgressData.setResource_progress((float) progress);
											goalProgressData.setTime_left((float) timeLeft);
											goalProgressData.setPending(1 - (float) progress);
											totalGoalTime += resourceTime;
											resourceFound = true;
											break;
										}
									}
								}
								if (!resourceFound) {
									totalGoalTime += Double.parseDouble(resourceMap.get("duration").toString());
								}
								goalProgCollection.add(goalProgressData);
							}
						}
					}
					goal.setResource_progress(goalProgCollection);
					goal.setTotal_goal_duration((float) totalGoalTime);
					goal.setTotal_read_duration((float) totalReadTime);
					if (totalGoalTime == 0) {
						totalGoalTime = 1;
					}
					goal.setGoalProgress((float) totalReadTime / (float) totalGoalTime);
				}
			}
			/*
			 * old way of getting resource progress String serverIp =
			 * System.getenv(JsonKey.SERVER_IP); if (serverIp == null) {
			 * ProjectLogger.log("Server ip address for course progress url is null");
			 * serverIp = ""; } final String courseProgressUrl = "http://" +
			 * serverIp + ":8090/user/dashboard/courses/details/" + userEmail; RestTemplate
			 * restTemplate = new RestTemplate();
			 * userGoalsDetails.parallelStream().forEach((userGoal) -> {
			 * List<UserGoalProgress> goalProgCollection = new
			 * ArrayList<UserGoalProgress>();
			 * userGoal.setResource_progress(goalProgCollection); if
			 * (userGoal.getGoal_content_id() != null &&
			 * !userGoal.getGoal_content_id().isEmpty()) { String apiResultData = ""; try {
			 * apiResultData = restTemplate.postForObject(courseProgressUrl,
			 * userGoal.getGoal_content_id(), String.class); } catch (Exception e) {
			 * ProjectLogger.
			 * log("Exception occured while calling telemetry service for goal progress : "
			 * + e.getMessage(), e); } List<Map<String, Object>> goalContentMeta =
			 * ElasticSearchUtil.searchDataByValues(
			 * ProjectUtil.EsIndex.new_lex_search.getIndexName(),
			 * ProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER,
			 * userGoal.getGoal_content_id(), userGoal.getGoal_content_id().size()); Map[]
			 * goalProgressObjects = null; if (apiResultData != null &&
			 * !apiResultData.isEmpty()) { try { goalProgressObjects =
			 * mapper.readValue(apiResultData, Map[].class); } catch (IOException e) {
			 * ProjectLogger.log(
			 * "Exception occured while processing user goal progress record  : " +
			 * e.getMessage(), e); } } else { goalProgressObjects = new HashMap[0]; } for
			 * (Map<String, Object> resourceMeta : goalContentMeta) { UserGoalProgress
			 * goalProgressData = new UserGoalProgress();
			 * goalProgressData.setResource_id((String) resourceMeta.get("identifier"));
			 * goalProgressData.setResource_name((String) resourceMeta.get("name"));
			 * goalProgressData.setContentType((String) resourceMeta.get("contentType"));
			 * goalProgressData.setMimeType((String) resourceMeta.get("mimeType")); Float
			 * resourceDuration = Float.parseFloat(resourceMeta.get("duration").toString());
			 * goalProgressData.setTotal_duration(resourceDuration);
			 * userGoal.setTotal_goal_duration(userGoal.getTotal_goal_duration() +
			 * resourceDuration); goalProgCollection.add(goalProgressData); for (Map
			 * progressData : goalProgressObjects) { if
			 * (progressData.get("identifier").toString().equals(goalProgressData.
			 * getResource_id())) { goalProgressData
			 * .setTime_left(Float.parseFloat(progressData.get("timeLeft").toString()));
			 * goalProgressData.setResource_progress(
			 * Float.parseFloat(progressData.get("progress").toString()));
			 * goalProgressData.setPending(Float.parseFloat(progressData.get("pending").
			 * toString()));
			 * userGoal.setTotal_read_duration(userGoal.getTotal_read_duration() +
			 * resourceDuration -
			 * Float.parseFloat(progressData.get("timeLeft").toString())); break; } } } if
			 * (userGoal.getTotal_goal_duration() < 1) {
			 * userGoal.setTotal_goal_duration(1.0f); }
			 * userGoal.setGoalProgress(userGoal.getTotal_read_duration() /
			 * userGoal.getTotal_goal_duration());
			 *
			 * userGoal.setResource_progress(goalProgCollection); if
			 * (userGoal.getGoal_type().equalsIgnoreCase("common_shared") ||
			 * userGoal.getGoal_type().equalsIgnoreCase("custom_shared")) {
			 * userGoal.setGoal_type("shared"); } } });
			 */
			List<UserLearningGoals> completedGoals = new ArrayList<UserLearningGoals>();
			List<UserLearningGoals> goalsInProgress = new ArrayList<UserLearningGoals>();
			for (UserLearningGoals goal : userGoalsDetails) {
				if (goal.getGoalProgress() == 1) {
					completedGoals.add(goal);
				} else {
					goalsInProgress.add(goal);
				}
			}
			output.put("completed_goals", completedGoals);
			output.put("goals_in_progress", goalsInProgress);
			contentResponse.put(Constants.RESPONSE, output);
		} catch (ProjectCommonException e) {
			ProjectLogger.log("Exception occured while processing user goals progress : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		return contentResponse;
	}

	@Override
	public Response getGoalsProgress_v1(String userEmail) {
		Response contentResponse = new Response();
		String identifierCol = "user_email";
		Map<String, Object> output = new HashMap<String, Object>();
		try {
			Map<String, Object> goalToBeFetched = new HashMap<String, Object>();
			goalToBeFetched.put(identifierCol, userEmail);
			Map<String, Object> userQueryData = new HashMap<String, Object>();
			userQueryData.put("user_email", userEmail);
			// user learning goals
			contentResponse = cassandraOperation.getRecordsByProperties(userLearningGoalsDb.getKeySpace(),
					userLearningGoalsDb.getTableName(), userQueryData);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> userLearningGoals = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");
			ObjectMapper mapper = new ObjectMapper();
			List<UserLearningGoals> userGoalsDetails = new ArrayList<UserLearningGoals>();
			// sorting goals in last updated order
			Collections.sort(userLearningGoals, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> m1, Map<String, Object> m2) {
					if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null) {
						return ((Date) m1.get("last_updated_on")).compareTo((Date) (m2.get("last_updated_on")));
					} else {
						return -1;
					}
				}
			}.reversed());
			for (Map<String, Object> userGoal : userLearningGoals) {
				userGoalsDetails.add(mapper.convertValue(userGoal, UserLearningGoals.class));
			}
			userGoalsDetails.forEach((userGoal) -> {
				List<UserGoalProgress> goalProgCollection = new ArrayList<UserGoalProgress>();
				userGoal.setResource_progress(goalProgCollection);
				if (userGoal.getGoal_content_id() != null && !userGoal.getGoal_content_id().isEmpty()) {
					List<Map<String, Object>> goalProgData = new ArrayList<Map<String, Object>>();
					try {
						goalProgData = lhService.getUserContentListProgress(userEmail.toLowerCase(),
								userGoal.getGoal_content_id());

						if (goalProgData != null && !goalProgData.isEmpty()) {

							for (Map<String, Object> progressData : goalProgData) {

								UserGoalProgress goalProgressData = new UserGoalProgress();
								goalProgressData.setResource_id((String) progressData.get("identifier"));
								goalProgressData
										.setTime_left(Float.parseFloat(progressData.get("timeLeft").toString()));
								goalProgressData.setResource_name((String) progressData.get("name"));
								goalProgressData.setResource_progress(
										Float.parseFloat(progressData.get("progress").toString()));
								goalProgressData.setTotal_duration(
										Float.parseFloat(progressData.get("totalDuration").toString()));
								goalProgressData.setPending(Float.parseFloat(progressData.get("pending").toString()));

								userGoal.setGoalProgress(userGoal.getGoalProgress()
										+ Float.parseFloat(progressData.get("progress").toString()));
								goalProgCollection.add(goalProgressData);
							}
							if (goalProgData.size() > 0) {
								userGoal.setGoalProgress(userGoal.getGoalProgress() / goalProgData.size() * 1.0f);
							}

							userGoal.setResource_progress(goalProgCollection);
						}
					} catch (Exception e) {
						ProjectLogger.log(
								"Exception occured while processing user goal progress record to  : " + e.getMessage(),
								e);
						e.printStackTrace();
						throw new ProjectCommonException(ResponseCode.invalidPropertyError.getErrorCode(),
								CassandraUtil.processExceptionForUnknownIdentifier(e),
								ResponseCode.CLIENT_ERROR.getResponseCode());

					}

				}
			});

			List<UserLearningGoals> completedGoals = new ArrayList<UserLearningGoals>();
			List<UserLearningGoals> goalsInProgress = new ArrayList<UserLearningGoals>();
			for (UserLearningGoals goal : userGoalsDetails) {
				if (goal.getGoalProgress() == 1) {
					completedGoals.add(goal);
				} else {
					goalsInProgress.add(goal);
				}
			}
			output.put("completed_goals", completedGoals);
			output.put("goals_in_progress", goalsInProgress);
			contentResponse.put(Constants.RESPONSE, output);
		} catch (ProjectCommonException e) {
			ProjectLogger.log("Exception occured while processing user goals progress : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		return contentResponse;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Response getCountForSharedGoal(Map<String, Object> data) {
		Response contentResponse = new Response();
		contentResponse = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(), sharedGoalsTrackerDb.getTableName(), data);
		List<Map<String, Object>> sharedGoalData = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		Map<String, Object> finalOutput = new HashMap<>();
		String goalId = null;
		int accepted = 0;
		int rejected = 0;
		int noActionTaken = 0;
		if (!sharedGoalData.isEmpty()) {
			goalId = sharedGoalData.get(0).get("goal_id").toString();
			for (Map<String, Object> map : sharedGoalData) {
				int status = (int) map.get("status");
				if (status == 1) {
					accepted++;
				} else if (status == -1) {
					rejected++;
				} else {
					noActionTaken++;
				}
			}
		}
		finalOutput.put("accepted", accepted);
		finalOutput.put("rejected", rejected);
		finalOutput.put("pending", noActionTaken);
		finalOutput.put("goal_id", goalId);
		contentResponse.put(Constants.RESPONSE, finalOutput);
		return contentResponse;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Response deleteSharedUserFromSharedGoal(Map<String, Object> sharedUserData) throws ParseException {
		Response contentResponse = new Response();
		List<Map<String, Object>> output = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> goalData : (List<Map<String, Object>>) sharedUserData.get("goal_data")) {
			Map<String, Object> finalOutput = new HashMap<String, Object>();
			finalOutput.put("goal_id", goalData.get("goal_id"));
			try {
				String userEmail = ((String) sharedUserData.get("user_email"));
				goalData.put("user_email", userEmail);
				executeBatchQueries(generateDelStmtForSharedGoal(goalData));
				long sharedGoalCount = sharedGoalCount(goalData);
				if (sharedGoalCount == 0) {
					Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
					Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
					Map<String, Object> insertionMap = new HashMap<>();
					insertionMap.put("goal_id", UUID.fromString(goalData.get("goal_id").toString()));
					insertionMap.put("goal_title", goalData.get("goal_title"));
					insertionMap.put("goal_desc", goalData.get("goal_desc"));
					insertionMap.put("goal_duration", goalData.get("goal_duration"));
					insertionMap.put("goal_content_id", goalData.get("goal_content_id"));
					insertionMap.put("created_on", timestamp);
					insertionMap.put("last_updated_on", timestamp);
					insertionMap.put("user_email", userEmail);
					if (goalData.get("goal_type").toString().equalsIgnoreCase("custom_shared")) {
						insertionMap.put("goal_type", "tobeshared");
					} else {
						insertionMap.put("goal_type", "commonshared");
					}
					contentResponse = cassandraOperation.upsertRecord(userLearningGoalsDb.getKeySpace(),
							userLearningGoalsDb.getTableName(), insertionMap);
				}
				finalOutput.put("result", "success");
			} catch (ProjectCommonException e) {
				finalOutput.put("result", "failed");
				ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
				contentResponse.setResponseCode(responseCode);
				return contentResponse;
			}
			output.add(finalOutput);
		}
		contentResponse.put(Constants.RESPONSE, output);
		return contentResponse;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Response getUpdatedResourceList(Map<String, Object> goalsData) {
		Response contentResponse = new Response();
		List<String> goalContentList = null;
		if (goalsData.get("goal_content_id") != null) {
			goalContentList = (List<String>) goalsData.get("goal_content_id");
		}
		contentResponse.put(Constants.RESPONSE, checkForParentsInGoals(goalContentList));

		return contentResponse;
	}

	/*
	 * this method will check for parent resources in goals and if it exists and
	 * then make appropriate optimizations
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> checkForParentsInGoals(List<String> goalContentList) {
		Map<String, Object> output = new HashMap<String, Object>();
		ParentsServiceImpl parentObj = new ParentsServiceImpl();
		List<ContentMeta> parentCourses = null;
		List<ContentMeta> parentLearningModules = null;
		List<ContentMeta> parentLearningPath = null;
		// getting meta data of all resources in goal content list
		int totalSuggestedTime = 0;
		goalContentList = goalContentList.stream().distinct().collect(Collectors.toList());
		List<Map<String, Object>> resourceMetaList = ElasticSearchUtil.searchDataByValues(
				LexProjectUtil.EsIndex.new_lex_search.getIndexName(), LexProjectUtil.EsType.new_lex_search.getTypeName(),
				JsonKey.IDENTIFIER, goalContentList, goalContentList.size());
		List<String> resourceRemovalMessages = new ArrayList<String>();
		for (Iterator<String> iterator = goalContentList.iterator(); iterator.hasNext();) {
			String resourceId = iterator.next();
			boolean parentExists = false;
			int duration = 0;
			for (Map<String, Object> map : resourceMetaList) {
				if (map.containsKey("identifier")) {
					if (map.get("identifier").toString().equals(resourceId)) {
						if (map.containsKey("status") && map.get("status").toString().toLowerCase().equals("live")) {
							if (map.get("duration") != null) {
								duration = (int) (map.get("duration"));
							}
							String contentType = (String) map.get("contentType");
							if (contentType != null) {
								contentType = contentType.toLowerCase();
								if (contentType.equals("collection")) {
									contentType = "learning module";
								} else if (contentType.equals("learning path")) {
									contentType = "program";
								}
							} else {
								contentType = "";
							}
							Map<String, Object> parentData = parentObj.getAllParents(resourceId);
							/*
							 * if in goal list there exists either parent(learning path,course or learning
							 * module) of this resource id and if its already added then don't add current
							 * resource if current resource is already added but user is also trying to add
							 * parent of this resource then remove the current resource and notify the user
							 * that current resource has been replaced by its parent in the goal.
							 */
							if (parentData != null && !parentData.isEmpty()) {
								if (parentData.get("learningPaths") != null
										&& ((List<ContentMeta>) parentData.get("learningPaths")).size() > 0) {
									parentLearningPath = (List<ContentMeta>) parentData.get("learningPaths");
									for (ContentMeta learningPath : parentLearningPath) {
										if (goalContentList.contains(learningPath.getIdentifier())) {
											parentExists = true;
											if (goalContentList.indexOf(resourceId) < goalContentList
													.indexOf(learningPath.getIdentifier())) {
												resourceRemovalMessages.add("Goal updated and \"" + map.get("name")
														+ "\" " + contentType + " has been moved to \""
														+ learningPath.getName() + "\" program");
											} else {
												resourceRemovalMessages.add("\"" + map.get("name") + "\" " + contentType
														+ " cannot be added to this goal as its parent program \""
														+ learningPath.getName() + "\" is already part of this goal");
											}
											break;
										}
									}
									if (parentExists) {
										// one parent of resource in goallist is enough to
										// remove that goal from goallist
										iterator.remove();
										break;
									}
								}
								if (parentData.get("courses") != null
										&& ((List<ContentMeta>) parentData.get("courses")).size() > 0) {
									parentCourses = (List<ContentMeta>) parentData.get("courses");
									for (ContentMeta course : parentCourses) {
										if (goalContentList.contains(course.getIdentifier())) {
											parentExists = true;
											if (goalContentList.indexOf(resourceId) < goalContentList
													.indexOf(course.getIdentifier())) {
												resourceRemovalMessages.add("Goal updated and \"" + map.get("name")
														+ "\" " + contentType + " has been moved to \""
														+ course.getName() + "\" course.");
											} else {

												resourceRemovalMessages.add("\"" + map.get("name") + "\" " + contentType
														+ " cannot be added to this goal as its parent course \""
														+ course.getName() + "\" is already part of this goal");
											}
											break;
										}
									}
									if (parentExists) {
										iterator.remove();
										break;
									}
								}
								if (parentData.get("modules") != null
										&& ((List<ContentMeta>) parentData.get("modules")).size() > 0) {
									parentLearningModules = (List<ContentMeta>) parentData.get("modules");
									for (ContentMeta module : parentLearningModules) {
										if (goalContentList.contains(module.getIdentifier())) {
											parentExists = true;
											if (goalContentList.indexOf(resourceId) < goalContentList
													.indexOf(module.getIdentifier())) {
												resourceRemovalMessages.add("Goal updated and \"" + map.get("name")
														+ "\" " + contentType + " has been moved to \""
														+ module.getName() + "\" learning module.");
											} else {
												resourceRemovalMessages.add("\"" + map.get("name") + "\" " + contentType
														+ " cannot be added to this goal as its parent learning module \""
														+ module.getName() + "\" is already part of this goal");
											}
											if (parentExists) {
												iterator.remove();
												break;
											}
										}
									}
								}
							}
						} else {
							iterator.remove();
							break;
						}
					}
				}
			}
			if (!parentExists) {
				totalSuggestedTime += duration;
			}
		}
		// int hours = totalSuggestedTime / 3600;
		// int minutes = (totalSuggestedTime % 3600) / 60;
		// int seconds=totalSuggestedTime % 60;
		// converting duration from seconds to hours
		output.put("suggested_time", totalSuggestedTime);
		output.put("resource_list", goalContentList);
		output.put("goal_message", resourceRemovalMessages);
		return output;
	}

	/*
	 * this method is used to execute multiple queries as a batch statement thereby
	 * ensuring atomicity.
	 */
	private Response executeBatchQueries(List<Statement> statements) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service executeBatchQuery method in goals  started at ==" + startTime,
				LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			final BatchStatement batchStatement = new BatchStatement();
			for (Statement stmt : statements) {
				batchStatement.add(stmt);
			}
			connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(batchStatement);
			response.put(Constants.RESPONSE, Constants.SUCCESS);
		} catch (Exception e) {
			ProjectLogger
					.log("Exception occurred while executing batch queries in CohortServiceImpl.executeBatchQueries "
							+ " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Cassandra Service executeBatchQueries method end at ==" + stopTime
				+ " ,Total time elapsed = " + elapsedTime, LoggerEnum.PERF_LOG);
		return response;

	}

	@SuppressWarnings("unchecked")
	private List<Statement> getInsertStmtForGoalSharing(String keyspaceName, String tableName,
			Map<String, Object> sharedGoalsMap) throws ParseException {
		List<Statement> statements = new ArrayList<Statement>();
		Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
		Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
		for (String email : (List<String>) sharedGoalsMap.get("user_list")) {
			Insert insertSharedGoal = QueryBuilder.insertInto(keyspaceName, tableName)
					.value("shared_with", email.toLowerCase()).value("goal_id", sharedGoalsMap.get("goal_id"))
					.value("goal_type", sharedGoalsMap.get("shared_goal_type"))
					.value("goal_content_id", sharedGoalsMap.get("goal_content_id"))
					.value("goal_title", sharedGoalsMap.get("goal_title"))
					.value("goal_desc", sharedGoalsMap.get("goal_desc"))
					.value("shared_by", sharedGoalsMap.get("shared_by")).value("shared_on", timestamp)
					.value("status", 0).value("goal_duration", sharedGoalsMap.get("goal_duration"))
					.value("status_message", null).value("last_updated_on", timestamp);
			statements.add(insertSharedGoal);
		}
		if (sharedGoalsMap.containsKey("is_delete") && ((int) sharedGoalsMap.get("is_delete")) == 1) {
			Delete.Where tobeSharedGoal = QueryBuilder.delete()
					.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
					.where(eq("user_email", sharedGoalsMap.get("shared_by")))
					.and(eq("goal_type", sharedGoalsMap.get("old_goal_type")))
					.and(eq("goal_id", UUID.fromString(sharedGoalsMap.get("goal_id").toString())));
			statements.add(tobeSharedGoal);
		}
		return statements;
	}

	private List<Statement> insertStmtForUserActionSharedGoals(Map<String, Object> goalData) throws ParseException {
		List<Statement> statements = new ArrayList<Statement>();
		if (goalData != null) {
			if (goalData.get("user_action").toString().toLowerCase().equals("accept")) {
				/*
				 * Insert insertSharedUserGoal = QueryBuilder
				 * .insertInto(userLearningGoalsDb.getKeySpace(),*
				 * userLearningGoalsDb.getTableName()) .value("user_email",*
				 * goalData.get("user_email")).value("goal_type", goalData.get("goal_type"))
				 * .value("goal_id", goalData.get("goal_id")).value("created_on",*
				 * goalData.get("created_on")) .value("goal_content_id",*
				 * goalData.get("goal_content_id")) .value("goal_desc",*
				 * goalData.get("goal_desc")).value("goal_title", goalData.get("goal_title"))
				 * .value("goal_duration", goalData.get("goal_duration")) .value("shared_by",*
				 * goalData.get("shared_by")) .value("goal_start_date",*
				 * goalData.get("goal_start_date")) .value("goal_end_date",*
				 * goalData.get("goal_end_date")) .value("last_updated_on",*
				 * goalData.get("last_updated_on"));
				 *
				 * statements.add(insertSharedUserGoal);
				 */
				Update.Where updateStmt = QueryBuilder.update(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName())
						.with(QueryBuilder.set("status", 1))
						.and(QueryBuilder.set("goal_start_date", goalData.get("goal_start_date")))
						.and(QueryBuilder.set("goal_end_date", goalData.get("goal_end_date")))
						.and(QueryBuilder.set("last_updated_on", goalData.get("last_updated_on")))
						.and(QueryBuilder.set("status_message", null))
						.where(eq("shared_with", goalData.get("user_email")))
						.and(eq("goal_type", goalData.get("shared_goal_type").toString()))
						.and(eq("goal_id", goalData.get("goal_id")))
						.and(eq("shared_by", goalData.get("shared_by").toString()));
				statements.add(updateStmt);

				if (goalData.get("user_commongoalid") != null
						&& !goalData.get("user_commongoalid").toString().isEmpty()) {
					Delete.Where deleteStmt = QueryBuilder.delete()
							.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
							.where(eq("user_email", goalData.get("user_email"))).and(eq("goal_type", "common"))
							.and(eq("goal_id", UUID.fromString(goalData.get("user_commongoalid").toString())));
					statements.add(deleteStmt);
				}
			} else {
				Update.Where updateStmt = QueryBuilder.update(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName())
						.with(QueryBuilder.set("status", -1))
						.and(QueryBuilder.set("status_message", goalData.get("status_message")))
						.and(QueryBuilder.set("last_updated_on", goalData.get("last_updated_on")))
						.where(eq("shared_with", goalData.get("user_email")))
						.and(eq("goal_type", goalData.get("shared_goal_type").toString()))
						.and(eq("goal_id", goalData.get("goal_id")))
						.and(eq("shared_by", goalData.get("shared_by").toString()));
				statements.add(updateStmt);
			}
		}
		return statements;
	}

	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> prepareGoalData(String userEmail, Map<String, Object> goalsData) {
		Map<String, Object> preparedGoalData = new HashMap<String, Object>();
		List<String> goalContentList = new ArrayList<>();
		String goalDesc = null;
		String goalId = null;
		String goalType = null;
		String goalTitle = null;
		int goalDuration = 0;
		String goalStartDate = null;
		// this flag will be used in checking
		// if common goals exists in method checkIfExistsAsUserGoal
		boolean goalIdFlag = true;
		try {
			goalId = (String) goalsData.get("goal_id");
			goalDesc = ((String) goalsData.get("goal_desc"));
			goalTitle = (String) goalsData.get("goal_title");
			if (goalsData.get("goal_content_id") != null) {
				goalContentList = (List<String>) goalsData.get("goal_content_id");
			}
			goalType = ((String) goalsData.get("goal_type")).toLowerCase();
			// removing duplicate elements if any

			// checkForParentsInGoals(goalContentList);

			// for navigator
			// goalcontentlist may be empty. in that case fetch goalcontent list from db
			if (goalContentList.isEmpty()
					&& (goalType.equalsIgnoreCase("common") || goalType.equalsIgnoreCase("commonshared"))) {
				Map<String, Object> navigatorGoalMap = fetchCommonGoalDataByGoalID(goalId);
				if (navigatorGoalMap != null && !navigatorGoalMap.isEmpty()) {
					goalTitle = (String) navigatorGoalMap.get("goal_title");
					goalDesc = (String) navigatorGoalMap.get("goal_desc");
					goalContentList = (List<String>) navigatorGoalMap.get("goal_content_id");
					// also setting goalsData Map
					goalsData.put("goal_content_id", goalContentList);
					goalsData.put("goal_title", goalTitle);
					goalsData.put("goal_desc", goalDesc);
					goalIdFlag = false;
				} else {
					preparedGoalData.put("invalid_goal", 1);
					preparedGoalData.put("fail_reason", "invalid goal id.");
					preparedGoalData.put("goal_content_id", Arrays.asList("invalid goal id"));
					preparedGoalData.put("goal_type", goalType);
					return preparedGoalData;
				}

			}
			if (goalContentList.isEmpty()) {
				// prepare for deleting user goal
				preparedGoalData.put("user_email", userEmail);
				preparedGoalData.put("goal_id", UUID.fromString(goalId));
				preparedGoalData.put("goal_content_id", goalContentList);
				preparedGoalData.put("goal_type", goalType);
			} else {
				goalContentList = goalContentList.stream().distinct().collect(Collectors.toList());
				Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
				Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
				if (goalType.equals("share_with")) {
					List<String> sharedUsers = (List<String>) goalsData.get("user_list");
					preparedGoalData.put("self_shared", 0);
					preparedGoalData.put("user_count", 0);
					preparedGoalData.put("fail_reason", "");
					preparedGoalData.put("user_list", sharedUsers);
					preparedGoalData.put("shared_by", userEmail);
					preparedGoalData.put("shared_goal_type", goalsData.get("shared_goal_type").toString());
					/*
					 * after sharing delete this particular tobeshared goal from user_learning_goals
					 */
					if (goalsData.containsKey("is_delete")) {
						preparedGoalData.put("is_delete", goalsData.get("is_delete"));
					}
					if (goalsData.containsKey("old_goal_type")) {
						preparedGoalData.put("old_goal_type", goalsData.get("old_goal_type"));
					}
					preparedGoalData.put("shared_on", timestamp);
					// if goalId is null fail sharing.
					if (goalId == null || goalId.isEmpty()) {
						preparedGoalData.put("invalid_share", 1);
						preparedGoalData.put("fail_reason", "no goal id");
					} else {
						preparedGoalData.put("invalid_share", 0);
						preparedGoalData.put("goal_id", UUID.fromString(goalId));
						Map<String, Object> usersCheck = userUtilService.verifyUsers(sharedUsers);
						preparedGoalData.put("invalid_users", usersCheck.get("invalid_users"));
						sharedUsers = (List<String>) usersCheck.get("valid_users");
						if (!sharedUsers.isEmpty()) {
							int sharedUserCount = sharedUsers.size();
							if (sharedUserCount <= 50) {
								Map<String, Object> alreadySharedMap = new HashMap<>();
								alreadySharedMap.put("goal_id", goalsData.get("goal_id"));
								alreadySharedMap.put("user_email", userEmail);
								alreadySharedMap.put("goal_type", "sharedby");
								alreadySharedMap.put("shared_goal_type", goalsData.get("shared_goal_type").toString());
								Response contentResponse = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(),
										sharedGoalsTrackerDb.getTableName(), alreadySharedMap);
								List<Map<String, Object>> sharedUserNames = (List<Map<String, Object>>) contentResponse
										.getResult().get("response");
								List<String> preSharedUsers = new ArrayList<>();
								for (Map<String, Object> userName : sharedUserNames) {
									if (userName.get("status") != null && (int) userName.get("status") == 1) {
										if (sharedUsers.contains(userName.get("shared_with").toString())) {
											// removing username from sharedlist if already shared and accepted by user
											sharedUsers.remove(userName.get("shared_with").toString());
											preSharedUsers.add(userName.get("shared_with").toString());
										}
									}
								}
								if (sharedUsers.contains(userEmail)) {
									preparedGoalData.put("self_shared", 1);
									sharedUsers.remove(userEmail);
								}
								preparedGoalData.put("already_shared", preSharedUsers);
								// if no of users exceed 50 then set usercount 1
								// and fail goal sharing
								sharedUserCount = sharedUsers.size() + sharedUserNames.size();
								if (sharedUserCount > 50) {
									preparedGoalData.put("user_count", sharedUserCount - 50);
								}
							} else {
								// if no of users exceed 50 then set usercount 1
								// and fail goal sharing
								preparedGoalData.put("user_count", sharedUserCount - 50);
								preparedGoalData.put("fail_reason", "exceeded limit of 50");
							}
						} else {
							preparedGoalData.put("invalid_share", 1);
							preparedGoalData.put("fail_reason", "all emails invalid");
						}
						preparedGoalData.put("user_list", sharedUsers);
					}
				} else {
					preparedGoalData.put("user_email", userEmail);
					if (goalId == null || goalId.isEmpty()) {
						preparedGoalData.put("created_on", timestamp);
						if (goalType.equalsIgnoreCase("common") || goalType.equalsIgnoreCase("commonshared")) {
							Map<String, String> commonGoalDetails = fetchCommonGoalId(goalsData);
							if (!commonGoalDetails.containsKey("goal_id")) {
								preparedGoalData.put("invalid_goal", 1);
								preparedGoalData.put("fail_reason", "invalid goal id");
							} else {
								goalId = commonGoalDetails.get("goal_id");
								preparedGoalData.put("goal_title", commonGoalDetails.get("goal_title"));
								preparedGoalData.put("goal_desc", commonGoalDetails.get("goal_desc"));
							}

						}
					}
					if (goalId != null && !goalId.isEmpty()) {
						goalsData.put("user_email", userEmail);
						goalsData.put("goal_id", goalId);
						/*
						 * a common goal is considered added if the user has either added that common
						 * goal or that particular common goal has been shared with user and user has
						 * accepted that shared common goal.
						 */
						if (goalType.equals("common")) {
							goalsData.put("goal_id_flag", goalIdFlag);
							String existingGoal = checkIfExistsAsUserGoal(goalsData);
							if (!existingGoal.isEmpty()) {
								preparedGoalData.put("invalid_goal", 1);
								preparedGoalData.put("fail_reason", "already existing goal");
							}
						}
						preparedGoalData.put("goal_id", UUID.fromString(goalId));
						if (goalType.equalsIgnoreCase("common") || goalType.equalsIgnoreCase("commonshared")) {
							preparedGoalData.put("created_on", timestamp);
						}
					} else {
						goalId = ProjectUtil.generateUniqueId();
						preparedGoalData.put("goal_id", UUID.fromString(goalId));
					}
					if (goalType.equals("shared")) {
						preparedGoalData.put("shared_by", goalsData.get("shared_by"));
						preparedGoalData.put("last_updated_on", timestamp);
						preparedGoalData.put("created_on", timestamp);
						preparedGoalData.put("user_action", goalsData.get("user_action"));
						preparedGoalData.put("status_message", goalsData.get("status_message"));
						preparedGoalData.put("shared_goal_type", goalsData.get("shared_goal_type"));
						preparedGoalData.put("user_commongoalid", goalsData.get("user_commongoalid"));
					}
				}
				preparedGoalData.put("goal_type", goalType);

				if (goalTitle != null && !goalTitle.isEmpty() && !preparedGoalData.containsKey("goal_title")) {
					preparedGoalData.put("goal_title", goalTitle);
				}
				if (goalDesc != null && !goalDesc.isEmpty() && !preparedGoalData.containsKey("goal_desc")) {
					preparedGoalData.put("goal_desc", goalDesc);
				}
				preparedGoalData.put("last_updated_on", timestamp);
				preparedGoalData.put("goal_content_id", goalContentList);

				// if user is sharing goal and there is no goal duration
				// fail goal sharing
				if (goalsData.containsKey("goal_duration") && goalsData.get("goal_duration") != null
						&& ((int) goalsData.get("goal_duration")) != 0) {
					goalDuration = (int) goalsData.get("goal_duration");
					goalStartDate = (String) goalsData.get("goal_start_date");
					// goal duration is in days
					preparedGoalData.put("goal_duration", goalDuration);
					if (!goalType.equals("tobeshared") && !(goalType.equals("commonshared"))) {
						Date endDate = null;
						Date startDate = null;
						Calendar cal = Calendar.getInstance();
						Timestamp goalStartTs = null;
						if (goalStartDate != null) {
							goalStartTs = new Timestamp(Long.parseLong(goalStartDate));
							startDate = goalStartTs;
							// startDate = ProjectUtil.getDateFormatter().parse(goalStartTs.toString());
						} else {
							startDate = parsedTimeStamp;
							goalStartTs = timestamp;
						}
						preparedGoalData.put("goal_start_date", goalStartTs);
						cal.setTime(startDate);
						cal.add(Calendar.DATE, goalDuration);
						endDate = cal.getTime();
						Date currentDate = Calendar.getInstance().getTime();
						if (endDate.before(currentDate)) {
							endDate = currentDate;
						}
						preparedGoalData.put("goal_end_date", new Timestamp(endDate.getTime()));
					}
				}
			}
		} catch (Exception ex) {
			preparedGoalData = null;
		}
		return preparedGoalData;
	}

	private Response getSharedGoals(String keySpaceName, String tableName, Map<String, Object> identifierCols) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service getGoalsShared method started for " + tableName + " at ==" + startTime,
				LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Select select = QueryBuilder.select().all().from(keySpaceName, tableName);
			// share_with means goals shared by me
			if (identifierCols.get("goal_type").equals("sharedwith")) {
				select.where(eq("shared_with", identifierCols.get("user_email"))).and(eq("status", 0));
			} else if (identifierCols.get("goal_type").equals("sharedby")) {
				select.where(eq("shared_by", identifierCols.get("user_email")));
				if (identifierCols.containsKey("shared_goal_type") && identifierCols.get("shared_goal_type") != null) {
					select.where(eq("goal_type", identifierCols.get("shared_goal_type").toString()));
				}
				if (identifierCols.containsKey("goal_id") && identifierCols.get("goal_id") != null) {
					select.where(eq("goal_id", UUID.fromString(identifierCols.get("goal_id").toString())));
				}
			} else {
				// fetch data from mv_user_goals_tracker
				List<String> goalTypes = Arrays.asList("tobeshared", "commonshared");
				Selection selection = QueryBuilder.select();
				select = selection.from(userGoalsTrackerDb.getKeySpace(), userGoalsTrackerDb.getTableName());
				Where selectWhere = select.where();
				Clause goalTypeClause = QueryBuilder.in("goal_type", goalTypes);
				Clause userIdentifierClause = QueryBuilder.eq("user_email", identifierCols.get("user_email"));
				selectWhere.and(goalTypeClause);
				selectWhere.and(userIdentifierClause);
			}
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(keySpaceName).execute(select);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Cassandra Service getGoalsShared method end at ==" + stopTime + " ,Total time elapsed = "
				+ elapsedTime, LoggerEnum.PERF_LOG);
		return response;
	}

	private Response getUserGoals(String keyspaceName, String tableName, String goalType,
			Map<String, Object> identifierData) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra call for getUserGoals method started at ==" + startTime, LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Selection selection = QueryBuilder.select();
			Select selectQuery = selection.from(keyspaceName, tableName);
			Where selectWhere = selectQuery.where();
			if (goalType.equals("user")) {
				List<String> goalTypes = Arrays.asList("user", "common", "shared");
				Clause goalTypeClause = QueryBuilder.in("goal_type", goalTypes);
				Clause userIdentifierClause = QueryBuilder.eq("user_email", identifierData.get("user_email"));
				selectWhere.and(goalTypeClause);
				selectWhere.and(userIdentifierClause);
			} else {
				Clause goalTypeClause = QueryBuilder.eq("goal_type", "tobeshared");
				Clause userIdentifierClause = QueryBuilder.eq("user_email", identifierData.get("user_email"));
				selectWhere.and(goalTypeClause);
				selectWhere.and(userIdentifierClause);
			}
			ProjectLogger.log("Query: " + selectQuery, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log(
				"Cassandra call for getUserGoals method end at ==" + stopTime + " ,Total time elapsed = " + elapsedTime,
				LoggerEnum.PERF_LOG);
		return response;
	}

	/*
	 * private void getGoalProgress(List<Map<String, Object>> sharedGoalProgress,*
	 * List<String> goalContentIds) { String serverIp =*
	 * System.getenv(LexJsonKey.SERVER_IP); if (serverIp == null) {
	 * ProjectLogger.log("Server ip address for course progress url is null");
	 * serverIp = ""; } String courseProgressUrl = "http://" +
	 * serverIp* + ":8090/user/dashboard/courses/details/"; RestTemplate
	 * restTemplate = new RestTemplate();ObjectMapper mapper = new ObjectMapper();
	 * sharedGoalProgress.parallelStream().forEach((sharedGoal) -> {
	 * sharedGoal.put("goal_progress", 0f); sharedGoal.put("total_goal_duration",
	 * 0f);sharedGoal.put("total_read_duration", 0f); if (sharedGoal.get("status")*
	 * != null && (int) sharedGoal.get("status") == 1) { List<UserGoalProgress>*
	 * goalProgCollection = new ArrayList<UserGoalProgress>(); String apiResultData
	 * = "";try { apiResultData = restTemplate.postForObject(courseProgressUrl +*
	 * sharedGoal.get("shared_with"), goalContentIds, String.class); } catch*
	 * (Exception e) { ProjectLogger.log(
	 * "Exception occured while calling telemetry service for goal progress : " +*
	 * e.getMessage(), e); } if (apiResultData != null && !apiResultData.isEmpty())
	 * {try {
	 *
	 * @SuppressWarnings("rawtypes") Map[] goalProgressObjects =*
	 * mapper.readValue(apiResultData, Map[].class);
	 *
	 * for (@SuppressWarnings("rawtypes") Map progressData : goalProgressObjects) {
	 *
	 * UserGoalProgress goalProgressData = new UserGoalProgress();
	 * goalProgressData.setResource_id((String) progressData.get("identifier"));
	 * goalProgressData.setTime_left(Float.parseFloat(progressData.get("timeLeft").
	 * toString()));goalProgressData.setResource_name((String)*
	 * progressData.get("name")); goalProgressData
	 * .setResource_progress(Float.parseFloat(progressData.get("progress").toString(
	 * )));Float resourceDuration =*
	 * Float.parseFloat(progressData.get("totalDuration").toString());
	 * goalProgressData.setTotal_duration(resourceDuration);
	 * goalProgressData.setPending(Float.parseFloat(progressData.get("pending").
	 * toString()));sharedGoal.put("total_goal_duration", (float)*
	 * sharedGoal.get("total_goal_duration") + resourceDuration);
	 * sharedGoal.put("total_read_duration", (float)*
	 * sharedGoal.get("total_read_duration") + resourceDuration -*
	 * Float.parseFloat(progressData.get("timeLeft").toString()));
	 * goalProgCollection.add(goalProgressData); } if (goalProgressObjects.length >
	 * 0) {sharedGoal.put("goal_progress", (float)*
	 * sharedGoal.get("total_read_duration") / (float)*
	 * sharedGoal.get("total_goal_duration")); } sharedGoal.put("resource_progress",
	 * goalProgCollection);} catch (IOException e) { ProjectLogger.log(
	 * "Exception occured while processing user goal progress record to  : " +*
	 * e.getMessage(), e); e.printStackTrace(); throw new*
	 * ProjectCommonException(ResponseCode.invalidPropertyError.getErrorCode(),
	 * CassandraUtil.processExceptionForUnknownIdentifier(e),
	 * ResponseCode.CLIENT_ERROR.getResponseCode());
	 * 
	 * } } }else { sharedGoal.put("resource_progress", null);} }); }
	 *
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Response getSharedGoalStatus(Map<String, Object> data) {
		Response contentResponse = new Response();
		contentResponse = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(), sharedGoalsTrackerDb.getTableName(), data);
		List<Map<String, Object>> sharedGoalData = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		Map<String, Object> finalOutput = new HashMap<>();
		if (!sharedGoalData.isEmpty()) {
			finalOutput = trackGoalsShared(sharedGoalData);
		}
		contentResponse.put(Constants.RESPONSE, finalOutput);
		return contentResponse;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> trackGoalsShared(List<Map<String, Object>> sharedGoalData) {
		Map<String, Object> finalOutput = new HashMap<String, Object>();
		Map<String, Object> goalData = new HashMap<String, Object>();
		List<Map<String, Object>> acceptedUserProgress = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> rejectedByUser = new ArrayList<>();
		List<Map<String, Object>> noActionByUser = new ArrayList<>();
		List<String> acceptedUserList = new ArrayList<>();
		Double totalGoalTime = 0d;
		if (!sharedGoalData.isEmpty()) {
			Map<String, Object> map = sharedGoalData.get(0);
			goalData.put("goal_id", map.get("goal_id"));
			goalData.put("goal_desc", map.get("goal_desc"));
			goalData.put("goal_title", map.get("goal_title"));
			goalData.put("goal_duration", map.get("goal_duration"));
			goalData.put("goal_content_id", map.get("goal_content_id"));
			List<String> resourceIds = (List<String>) map.get("goal_content_id");
			if (resourceIds != null && !resourceIds.isEmpty()) {
				List<Map<String, Object>> goalresources = ElasticSearchUtil.searchDataByValues(
						LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
						LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
						resourceIds.size());
				List<Map<String, Object>> goalsContent = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> resourceMap : goalresources) {
					Map<String, Object> contentMap = new HashMap<String, Object>();
					contentMap.put("resource_name", resourceMap.get("name"));
					contentMap.put("resource_id", resourceMap.get("identifier"));
					contentMap.put("time_duration", resourceMap.get("duration"));
					contentMap.put("contentType", resourceMap.get("contentType"));
					contentMap.put("mimeType", resourceMap.get("mimeType"));
					goalsContent.add(contentMap);
					totalGoalTime += Double.parseDouble(resourceMap.get("duration").toString());
				}
				goalData.put("resource", goalsContent);
				goalData.put("total_goal_time", totalGoalTime);
			}
		}
		for (Map<String, Object> map : sharedGoalData) {
			// if user accepted the shared goal
			// 1 -accepted ; 0= no //action ; -1= rejected
			Map<String, Object> userProgressTracker = new HashMap<String, Object>();
			List<Map<String, Object>> resourceProgress = new ArrayList<Map<String, Object>>();
			int sharedGoalStatus = (int) map.get("status");
			userProgressTracker.put("status", sharedGoalStatus);
			userProgressTracker.put("shared_with", map.get("shared_with"));
			userProgressTracker.put("last_updated_on", map.get("last_updated_on"));
			if (sharedGoalStatus == 1) {
				userProgressTracker.put("goal_start_date", map.get("goal_start_date"));
				userProgressTracker.put("goal_end_date", map.get("goal_end_date"));
				acceptedUserList.add(map.get("shared_with").toString());
				for (Map<String, Object> resourceMap : (List<Map<String, Object>>) goalData.get("resource")) {
					Map<String, Object> contentMap = new HashMap<String, Object>();
					contentMap.put("resource_id", resourceMap.get("resource_id"));
					contentMap.put("contentType", resourceMap.get("contentType"));
					contentMap.put("mimeType", resourceMap.get("mimeType"));
					contentMap.put("time_left", resourceMap.get("time_duration"));
					contentMap.put("resource_name", resourceMap.get("resource_name"));
					contentMap.put("resource_progress", 0);
					contentMap.put("pending", 1);
					contentMap.put("resource_duration", resourceMap.get("time_duration"));
					resourceProgress.add(contentMap);
				}

				userProgressTracker.put("resource_progress_tracker", resourceProgress);
				userProgressTracker.put("goal_progress", 0d);
				acceptedUserProgress.add(userProgressTracker);
			} else if (sharedGoalStatus == -1) {
				userProgressTracker.put("status_message", map.get("status_message"));
				rejectedByUser.add(userProgressTracker);
			} else {
				noActionByUser.add(userProgressTracker);
			}
		}
		Map<String, Object> progressMap = new HashMap<>();
		progressMap.put("goal_content_id", goalData.get("goal_content_id"));
		progressMap.put("user_list", acceptedUserList);
		List<Map<String, Object>> progressData = getProgressOfGoalContent(progressMap);
		if (progressData != null && !progressData.isEmpty()) {
			List<Map<String, Object>> mongoDbData = new ArrayList<Map<String, Object>>();

			for (Map<String, Object> map : acceptedUserProgress) {
				Double totalReadTime = 0d;
				for (Map<String, Object> prgMap : progressData) {
					if (prgMap.get("_id").equals(map.get("shared_with").toString())) {
						mongoDbData = (List<Map<String, Object>>) prgMap.get("data");
						for (Map<String, Object> resourceTrack : (List<Map<String, Object>>) map
								.get("resource_progress_tracker")) {
							for (Map<String, Object> mongoResMap : mongoDbData) {
								if (mongoResMap.get("cid").toString()
										.equals(resourceTrack.get("resource_id").toString())) {
									Double progress = Double.parseDouble(mongoResMap.get("progress").toString());
									Double resourceTime = Double.parseDouble(mongoResMap.get("length").toString());
									double timeLeft = resourceTime - (resourceTime * progress);
									totalReadTime += resourceTime * progress;
									resourceTrack.put("resource_progress", progress);
									resourceTrack.put("time_left", timeLeft);

									resourceTrack.put("pending", 1 - progress);
									break;
								}
							}
						}
						break;
					}
				}
				map.put("goal_progress", totalReadTime / totalGoalTime);
			}

		}

		// sort shared user progress in descending order
		Collections.sort(acceptedUserProgress, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> m1, Map<String, Object> m2) {
				if (m1.get("goal_progress") != null && m2.get("goal_progress") != null) {
					return ((Double) m1.get("goal_progress")).compareTo((Double) m2.get("goal_progress"));
				} else {
					return -1;
				}
			}
		}.reversed());
		finalOutput.put("accepted", acceptedUserProgress);
		finalOutput.put("rejected", rejectedByUser);
		finalOutput.put("pending", noActionByUser);
		return finalOutput;
	}

	/*
	 * this method is used to generate delete statements to be executed as batch
	 * query for cassandra regarding getDeleteStatementsForSharedGoal this method
	 * deletes for all users a particular shared goal even if they have accepted
	 * that goal
	 */
	@SuppressWarnings("unchecked")
	private List<Statement> getDeleteStatementsForSharedGoal(Map<String, Object> userGoalMap) {
		List<Statement> statements = new ArrayList<Statement>();
		Response contentResponse = getSharedGoals(sharedGoalsTrackerDb.getKeySpace(),
				sharedGoalsTrackerDb.getTableName(), userGoalMap);
		List<Map<String, Object>> sharedUserGoalsData = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		for (Map<String, Object> sharedUser : sharedUserGoalsData) {
			Delete.Where sharedGoal = QueryBuilder.delete()
					.from(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName())
					.where(eq("shared_with", sharedUser.get("shared_with")))
					.and(eq("goal_id", UUID.fromString(userGoalMap.get("goal_id").toString())));
			statements.add(sharedGoal);

			// delete particular shared goal accepted by other users
			if (sharedUser.get("status") != null && ((int) sharedUser.get("status")) == 1) {
				Delete.Where acceptedSharedGoal = QueryBuilder.delete()
						.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
						.where(eq("user_email", sharedUser.get("shared_with"))).and(eq("goal_type", "shared"))
						.and(eq("goal_id", UUID.fromString(userGoalMap.get("goal_id").toString())));

				statements.add(acceptedSharedGoal);
			}
		}
		return statements;
	}

	/*
	 * this method is used to delete a shared goal for a list of particular users
	 */
	@SuppressWarnings("unchecked")
	private List<Statement> generateDelStmtForSharedGoal(Map<String, Object> sharedUsersData) {
		List<Statement> statements = new ArrayList<Statement>();
		for (String sharedUser : (List<String>) sharedUsersData.get("user_list")) {
			Delete.Where sharedGoal = QueryBuilder.delete()
					.from(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName())
					.where(eq("shared_with", sharedUser)).and(eq("goal_type", sharedUsersData.get("goal_type")))
					.and(eq("goal_id", UUID.fromString(sharedUsersData.get("goal_id").toString())))
					.and(eq("shared_by", sharedUsersData.get("user_email").toString()));
			statements.add(sharedGoal);
			/*
			 * // delete particular shared goal accepted by other users Delete.Where*
			 * acceptedSharedGoal = QueryBuilder.delete()
			 * .from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
			 * .where(eq("user_email", sharedUser)).and(eq("goal_type", "shared"))
			 * .and(eq("goal_id",*
			 * UUID.fromString(sharedUsersData.get("goal_id").toString())));
			 * statements.add(acceptedSharedGoal);
			 */
		}
		return statements;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getProgressOfGoalContent(Map<String, Object> data) {
		List<Map<String, Object>> contentMap = new ArrayList<Map<String, Object>>();
		List<String> goalContentId = (List<String>) data.get("goal_content_id");
		List<String> userList = (List<String>) data.get("user_list");
		try {

			/*
			 * delete this commented part once code is stable and final for spring data for
			 * mongodb MongoClient mongoClient =
			 * MongoConnectionManager.CONNECTION.getClient(); MongoDatabase db =
			 * mongoClient.getDatabase(telemetryDBName); MongoCollection<Document>
			 * collection = db.getCollection(activeUsersCollection);
			 * AggregateIterable<Document> result = collection .aggregate(Arrays.asList( new
			 * Document("$match", new Document("cid", new Document("$in",
			 * goalContentId)).append("empMailId", new Document("$in", userList))), new
			 * Document("$group", new Document("_id", "$empMailId").append("data", new
			 * Document("$push", new Document("progress", "$progress").append("length",
			 * "$length") .append("cid", "$cid") .append("time_spent", "$time_spent"))))));
			 * 
			 */
			ObjectMapper mapper = new ObjectMapper();
			List<Document> result = prRepository.getGoalProgressFromDB(userList, goalContentId);
			for (Document doc : result) {
				contentMap.add(mapper.readValue(doc.toJson(), Map.class));
			}
		} catch (Exception e) {
			ProjectLogger.log("Exception occurred in CohortsServiceImpl getProgressOfGoalContent : " + e.getMessage(),
					e);
			contentMap = null;
		}
		return contentMap;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> organizeSharedGoals(List<Map<String, Object>> sharedGoalData) {
		List<Map<String, Object>> sharedUserGoalProgress = new ArrayList<Map<String, Object>>();
		// List<Map<String,Object>> finalOutput=new ArrayList<Map<String,Object>>();
		LinkedHashMap<Object, List<Map<String, Object>>> goalsSharedByMe = sharedGoalData.stream()
				.collect(Collectors.groupingBy(g -> g.get("goal_id"), LinkedHashMap::new, Collectors.toList()));

		for (Entry<Object, List<Map<String, Object>>> entry : goalsSharedByMe.entrySet()) {
			Map<String, Object> output = new HashMap<String, Object>();
			List<Map<String, Object>> userProgress = new ArrayList<Map<String, Object>>();
			for (Iterator<Map<String, Object>> itr = entry.getValue().iterator(); itr.hasNext();) {
				Map<String, Object> sharedUserData = itr.next();
				if (!output.containsKey("goal_id")) {
					output.put("goal_id", entry.getKey().toString());
					output.put("goal_desc", sharedUserData.get("goal_desc"));
					output.put("goal_title", sharedUserData.get("goal_title"));
					output.put("goal_duration", sharedUserData.get("goal_duration"));
					output.put("goal_content_id", sharedUserData.get("goal_content_id"));
					if (sharedUserData.containsKey("goal_type")) {
						output.put("goal_type", sharedUserData.get("goal_type"));
					} else {
						output.put("goal_type", "sharedby");
					}
					List<String> resourceIds = (List<String>) sharedUserData.get("goal_content_id");
					if (resourceIds != null && !resourceIds.isEmpty()) {
						List<Map<String, Object>> goalresources = ElasticSearchUtil.searchDataByValues(
								LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
								LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
								resourceIds.size());
						List<Map<String, Object>> goalsContent = new ArrayList<Map<String, Object>>();
						for (String resIdentifier : resourceIds) {
							for (Map<String, Object> resourceMap : goalresources) {
								if (resIdentifier.equalsIgnoreCase(resourceMap.get("identifier").toString())) {
									Map<String, Object> contentMap = new HashMap<String, Object>();
									contentMap.put("resource_name", resourceMap.get("name"));
									contentMap.put("resource_id", resourceMap.get("identifier"));
									contentMap.put("time_duration", resourceMap.get("duration"));
									contentMap.put("contentType", resourceMap.get("contentType"));
									contentMap.put("mimeType", resourceMap.get("mimeType"));
									goalsContent.add(contentMap);
									break;
								}
							}
						}
						output.put("resource", goalsContent);
					}
				}
				if (sharedUserData.containsKey("goal_type") && ((sharedUserData.get("goal_type").equals("tobeshared"))
						|| (sharedUserData.get("goal_type").equals("commonshared")))) {
					// don't add user progress as there is no
				} else {
					Map<String, Object> userProgressTracker = new HashMap<String, Object>();
					userProgressTracker.put("status", sharedUserData.get("status"));
					userProgressTracker.put("goal_start_date", sharedUserData.get("goal_start_date"));
					userProgressTracker.put("goal_end_date", sharedUserData.get("goal_end_date"));
					userProgressTracker.put("shared_with", sharedUserData.get("shared_with"));
					userProgress.add(userProgressTracker);
				}
			}
			output.put("shared_users_progress", userProgress);
			sharedUserGoalProgress.add(output);
		}
		return sharedUserGoalProgress;
	}

	private Response getDataForCommonGoals(Map<String, Object> goalData) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service getDataForCommonGoals method started for "
				+ goalData.get("table_name").toString() + " at ==" + startTime, LoggerEnum.PERF_LOG);
		Response response = new Response();
		Select select = null;
		try {
			// share_with means goals shared by me
			Selection selection = QueryBuilder.select();
			selection.column("goal_content_id");
			selection.column("goal_title");
			selection.column("goal_desc");
			selection.column("goal_id");
			if (goalData.get("goal_type").equals("sharedby")) {
				select = selection.from(goalData.get("keyspace_name").toString(),
						goalData.get("table_name").toString());
				select.where(eq("shared_by", goalData.get("user_email"))).and(eq("goal_type", "common_shared"));
			} else {
				// fetch data from mv_user_goals_tracker
				List<String> goalTypes = Arrays.asList("common", "commonshared");
				selection.column("goal_type");
				select = selection.from(goalData.get("keyspace_name").toString(),
						goalData.get("table_name").toString());
				// select = selection.from(goalData.get("keyspace_name").toString(),
				// goalData.get("table_name").toString());
				Where selectWhere = select.where();
				Clause goalTypeClause = QueryBuilder.in("goal_type", goalTypes);
				Clause userIdentifierClause = QueryBuilder.eq("user_email", goalData.get("user_email"));
				selectWhere.and(goalTypeClause);
				selectWhere.and(userIdentifierClause);
			}
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(goalData.get("keyspace_name").toString()).execute(select);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + goalData.get("table_name").toString() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Cassandra Service getGoalsShared method end at ==" + stopTime + " ,Total time elapsed = "
				+ elapsedTime, LoggerEnum.PERF_LOG);
		return response;
	}

	private long sharedGoalCount(Map<String, Object> identifierCols) {
		long goalCount = 0;
		try {
			Select select = QueryBuilder.select().countAll().from(sharedGoalsTrackerDb.getKeySpace(),
					sharedGoalsTrackerDb.getTableName());
			// share_with means goals shared by me
			select.where(eq("shared_by", identifierCols.get("user_email")));
			select.where(eq("goal_type", identifierCols.get("goal_type").toString()));
			select.where(eq("goal_id", UUID.fromString(identifierCols.get("goal_id").toString())));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(sharedGoalsTrackerDb.getKeySpace()).execute(select);
			Row row = results.one();
			if (row != null) {
				Object countVal = row.getObject(0);
				goalCount = (long) countVal;
			}
		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + sharedGoalsTrackerDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return goalCount;
	}

	private Response getGoalsSharedWithMe(String userEmail) {
		Response response = new Response();
		try {
			Selection selection = QueryBuilder.select();
			selection.column("shared_with").as("user_email");
			selection.column("goal_type");
			selection.column("goal_id");
			selection.column("goal_title");
			selection.column("goal_duration");
			selection.column("goal_start_date");
			selection.column("goal_end_date");
			selection.column("shared_by");
			selection.column("goal_content_id");
			selection.column("last_updated_on");
			Select select = selection.from(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName());
			// share_with means goals shared with me and status =1 i..e accepted
			select.where(eq("shared_with", userEmail)).and(eq("status", 1));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(sharedGoalsTrackerDb.getKeySpace()).execute(select);
			response = CassandraUtil.createResponse(results);

		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + sharedGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return response;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Response getOnboardingPrograms() {
		Response contentResponse = fetchOnboardingPrograms();
		List<Map<String, Object>> resourceListMap = (List<Map<String, Object>>) contentResponse.getResult()
				.get("response");
		List<Map<String, Object>> resourceMetaList = new ArrayList<Map<String, Object>>();
		Map<String, Object> output = new HashMap<>();
		if (resourceListMap != null && !resourceListMap.isEmpty()) {
			Set<String> resourceIds = new HashSet<String>();
			for (Map<String, Object> resMap : resourceListMap) {
				resourceIds.addAll((List<String>) resMap.get("goal_content_id"));
			}
			resourceMetaList.addAll(ElasticSearchUtil.searchDataByValues(
					LexProjectUtil.EsIndex.new_lex_search.getIndexName(), LexProjectUtil.EsType.new_lex_search.getTypeName(),
					JsonKey.IDENTIFIER, new ArrayList<String>(resourceIds), resourceIds.size()));
		}
		output.put("onboarding_data", resourceMetaList);
		contentResponse.put(Constants.RESPONSE, output);
		return contentResponse;
	}

	private Response fetchOnboardingPrograms() {
		Response response = new Response();
		try {
			Select select = QueryBuilder.select().column("goal_content_id").from(commonLearningGoalsDb.getKeySpace(),
					commonLearningGoalsDb.getTableName());
			select.where(eq("goal_group", "Onboarding Program"));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(commonLearningGoalsDb.getKeySpace()).execute(select);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + commonLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return response;
	}

	/*
	 * this method checks whether the shared common goal is already part of user
	 * common goals.
	 */
	@Override
	public Response isAddedAsCommonGoal(Map<String, Object> reqData) {
		Response contentResponse = new Response();
		String goalId = checkIfExistsAsUserGoal(reqData);
		contentResponse.put("user_commongoalid", goalId);
		return contentResponse;
	}

	/*
	 * this method checks whether the shared common goal is already part of user
	 * common goals.
	 */
	@SuppressWarnings("unchecked")
	private String checkIfExistsAsUserGoal(Map<String, Object> commonGoalMap) {
		String goalId = "";
		// this flag will tell whether to check by goalcontentid or goalid
		boolean goalIdFlag = true;
		try {

			if (commonGoalMap.containsKey("goal_id_flag")) {
				goalIdFlag = (boolean) commonGoalMap.get("goal_id_flag");
			}

			/*
			 * first check if the common goal user is trying to add has already been added
			 * by user.
			 */
			Select select = QueryBuilder.select().column("goal_id").column("goal_title").column("goal_content_id")
					.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName());

			if (goalIdFlag) {
				select.where(eq("user_email", commonGoalMap.get("user_email"))).and(eq("goal_type", "common"));
			} else {
				select.where(eq("user_email", commonGoalMap.get("user_email"))).and(eq("goal_type", "common"))
						.and(eq("goal_id", UUID.fromString(commonGoalMap.get("goal_id").toString())));
			}
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(select);

			if (commonGoalMap.get("goal_content_id") != null) {
				for (Row row : results) {
					if ((((List<String>) commonGoalMap.get("goal_content_id"))
							.equals(row.getList("goal_content_id", String.class)))
							|| (commonGoalMap.get("goal_id") != null && commonGoalMap.get("goal_id").toString()
									.equalsIgnoreCase(row.getUUID("goal_id").toString()))) {
						goalId = row.getUUID("goal_id").toString();
						break;
					}
				}
			}
			/*
			 * if the common goal user is trying to add has not been added by user, then
			 * check if this common goal has been shared with him by someone.
			 */
			if (goalId == "") {
				Select sharedGoalselect = QueryBuilder.select().column("goal_id").column("goal_content_id")
						.from(sharedGoalsDb.getKeySpace(), sharedGoalsDb.getTableName());
				if (goalIdFlag) {
					sharedGoalselect.where(eq("shared_with", commonGoalMap.get("user_email")))
							.and(eq("goal_type", "common_shared")).and(eq("status", 1));
				} else {
					sharedGoalselect.where(eq("shared_with", commonGoalMap.get("user_email")))
							.and(eq("goal_type", "common_shared"))
							.and(eq("goal_id", UUID.fromString(commonGoalMap.get("goal_id").toString())))
							.and(eq("status", 1));
				}
				ProjectLogger.log("Query: " + sharedGoalselect, LoggerEnum.DEBUG);
				ResultSet sharedGoalResult = connectionManager.getSession(sharedGoalsDb.getKeySpace())
						.execute(sharedGoalselect);

				if (commonGoalMap.get("goal_content_id") != null) {
					for (Row row : sharedGoalResult) {
						if (commonGoalMap.get("goal_id").toString().equalsIgnoreCase(row.getUUID("goal_id").toString())
								|| (((List<String>) commonGoalMap.get("goal_content_id"))
										.equals(row.getList("goal_content_id", String.class)))) {
							goalId = row.getUUID("goal_id").toString();
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return goalId;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> fetchCommonGoalId(Map<String, Object> commonGoalMap) {
		Map<String, String> goalData = new HashMap<String, String>();
		try {
			Select select = QueryBuilder.select().column("id").column("goal_title").column("goal_content_id")
					.column("goal_desc")
					.from(commonLearningGoalsDb.getKeySpace(), commonLearningGoalsDb.getTableName());
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(commonLearningGoalsDb.getKeySpace()).execute(select);

			for (Row row : results) {
				if (((List<String>) commonGoalMap.get("goal_content_id"))
						.equals(row.getList("goal_content_id", String.class))) {
					goalData.put("goal_id", row.getUUID("id").toString());
					goalData.put("goal_title", row.getString("goal_title"));
					goalData.put("goal_desc", row.getString("goal_desc"));
					break;
				}
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + commonLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return goalData;
	}

	/*
	 * this method deletes a resource (resource can be anything) from a user goal
	 * 
	 */
	@Override
	public Response deleteContentUserGoal(Map<String, Object> deleteMap) {
		Response contentResponse = new Response();
		Map<String, Object> output = new HashMap<String, Object>();

		try {
			List<String> goalContentList = fetchParticularGoalContent(deleteMap);
			// if content exists in current goal then delete
			if (goalContentList.contains(deleteMap.get("content_id"))) {
				if (goalContentList.size() > 1) {
					goalContentList.remove(deleteMap.get("content_id"));
					deleteMap.put("goal_content_id", goalContentList);
					updateGoalContent(deleteMap);
					output.put("goal_deleted", 0);
					output.put("content_deleted", 1);
					output.put("message", "content deleted.");

				} else {
					// if there is only one resource, then delete that goal itself
					Delete.Where deleteUserGoals = QueryBuilder.delete()
							.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
							.where(eq("user_email", deleteMap.get("user_email")))
							.and(eq("goal_id", UUID.fromString(deleteMap.get("goal_id").toString())))
							.and(eq("goal_type", deleteMap.get("goal_type")));
					ProjectLogger.log("Query: " + deleteUserGoals, LoggerEnum.DEBUG);
					connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(deleteUserGoals);
					output.put("goal_deleted", 1);
					output.put("content_deleted", 1);
					output.put("message", "goal deleted as it was only content in goal.");

				}
			} else {
				// there is no such goal
				output.put("goal_deleted", 0);
				output.put("content_deleted", 0);
				output.put("message", "this content does not exists in goal.");

			}

		} catch (ProjectCommonException ex) {
			contentResponse.setResponseCode(ResponseCode.SERVER_ERROR);
		}

		contentResponse.put("result", output);
		// contentResponse.put("user_commongoalid", goalId);
		return contentResponse;
	}

	/*
	 * this method adds a resource(resource can be anything) to a user goal
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Response addContentUserGoal(Map<String, Object> updateGoalMap) {
		Response contentResponse = new Response();
		Map<String, Object> output = new HashMap<String, Object>();
		try {
			List<String> goalContentList = fetchParticularGoalContent(updateGoalMap);
			if (goalContentList != null && !goalContentList.contains(updateGoalMap.get("content_id"))) {
				goalContentList.add((String) updateGoalMap.get("content_id"));
				Map<String, Object> parentResourceMap = checkForParentsInGoals(goalContentList);
				List<String> newResourceList = (List<String>) parentResourceMap.get("resource_list");
				if (!newResourceList.contains(updateGoalMap.get("content_id"))) {
					output.put("result", "failed");
					output.put("message", parentResourceMap.get("goal_message"));
				} else {
					updateGoalMap.put("goal_content_id", newResourceList);
					updateGoalContent(updateGoalMap);
					output.put("result", "success");
					output.put("message", parentResourceMap.get("goal_message"));
				}
			} else {
				output.put("result", "failed");
				output.put("message", "this content already exists in goal.");
			}

		} catch (ProjectCommonException ex) {
			contentResponse.setResponseCode(ResponseCode.SERVER_ERROR);
		}

		contentResponse.put("result", output);
		return contentResponse;
	}

	/*
	 * this method is used to update user goal when we have complete contentlist of
	 * a user goal
	 */

	private void updateGoalContent(Map<String, Object> goalData) {
		try {
			Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
			Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
			Update update = QueryBuilder.update(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName());
			update.with(QueryBuilder.set("goal_content_id", goalData.get("goal_content_id")))
					.and(QueryBuilder.set("last_updated_on", timestamp));
			update.where(eq("user_email", goalData.get("user_email"))).and(eq("goal_type", goalData.get("goal_type")))
					.and(eq("goal_id", UUID.fromString(goalData.get("goal_id").toString())));
			ProjectLogger.log("Query: " + update, LoggerEnum.DEBUG);
			connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(update);

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_UPDATE + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
	}

	/*
	 * this method fetches a particular goal contentlist
	 */
	private List<String> fetchParticularGoalContent(Map<String, Object> goalData) {
		List<String> goalContent = new ArrayList<>();
		try {
			Select select = QueryBuilder.select().column("goal_content_id").from(userLearningGoalsDb.getKeySpace(),
					userLearningGoalsDb.getTableName());
			select.where(eq("user_email", goalData.get("user_email"))).and(eq("goal_type", goalData.get("goal_type")))
					.and(eq("goal_id", UUID.fromString(goalData.get("goal_id").toString())));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(select);

			for (Row row : results) {
				goalContent = row.getList("goal_content_id", String.class);
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return goalContent;
	}

	/*
	 * this method adds a common goal for a user if goal does not exist else throw a
	 * message.
	 * 
	 */
	@Override
	public Response addCommonGoal(Map<String, Object> commonGoalMap) throws ParseException {
		Response contentResponse = new Response();
		try {
			Map<String, Object> commonGoalData = fetchCommonGoalData(commonGoalMap);
			if ((int) commonGoalData.get("new_goal") == 1) {
				Map<String, Object> dataToBeInserted = new HashMap<String, Object>();
				Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
				Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
				dataToBeInserted.put("user_email", commonGoalMap.get("user_email"));
				dataToBeInserted.put("goal_type", commonGoalMap.get("goal_type"));
				dataToBeInserted.put("goal_id", UUID.fromString(commonGoalMap.get("goal_id").toString()));
				dataToBeInserted.put("created_on", timestamp);
				dataToBeInserted.put("last_updated_on", timestamp);
				dataToBeInserted.put("goal_title", commonGoalData.get("goal_title"));
				dataToBeInserted.put("goal_desc", commonGoalData.get("goal_desc"));
				dataToBeInserted.put("goal_content_id", commonGoalData.get("goal_content_id"));
				dataToBeInserted.put("goal_duration", commonGoalMap.get("goal_duration"));
				dataToBeInserted.put("goal_start_date", timestamp);
				Date endDate = null;
				Date startDate = timestamp;
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);
				cal.add(Calendar.DATE, (int) commonGoalMap.get("goal_duration"));
				endDate = cal.getTime();
				Date currentDate = Calendar.getInstance().getTime();
				if (endDate.before(currentDate)) {
					endDate = currentDate;
				}
				dataToBeInserted.put("goal_end_date", new Timestamp(endDate.getTime()));
				contentResponse = cassandraOperation.upsertRecord(userLearningGoalsDb.getKeySpace(),
						userLearningGoalsDb.getTableName(), dataToBeInserted);
				contentResponse.put("response", "SUCCESS");
			} else if ((int) commonGoalData.get("new_goal") == 0) {
				contentResponse.put("response", "FAILED");
				contentResponse.setResponseCode(ResponseCode.alreadyExists);
				contentResponse.put("reason", "goal already exists.");
			} else {
				contentResponse.put("response", "FAILED");
				contentResponse.setResponseCode(ResponseCode.invalidData);
				contentResponse.put("reason", "no common goal with this id.");
			}
		} catch (ProjectCommonException ex) {
			contentResponse.setResponseCode(ResponseCode.SERVER_ERROR);
		}

		// contentResponse.put("result", output);
		return contentResponse;
	}

	/*
	 * this method updates duration for common goal for a user
	 *
	 */
	@Override
	public Response updateCommonGoal(Map<String, Object> commonGoalMap) throws ParseException {
		Response contentResponse = new Response();
		try {
			int result = updateDurationGoal(commonGoalMap);
			if (result == 0) {
				contentResponse.put("response", "FAILED");
				contentResponse.put("reason", "goal does not exist.");
				contentResponse.setResponseCode(ResponseCode.invalidData);
			} else {
				contentResponse.put("response", "SUCCESS");
			}
		} catch (ProjectCommonException ex) {
			contentResponse.setResponseCode(ResponseCode.SERVER_ERROR);
		}
		return contentResponse;
	}

	/*
	 * this method check if a particular common goal identified by goalid has been
	 * added by user/or shared with him. if yes then it shows goal already existing
	 * else it adds that common goal
	 */
	private Map<String, Object> fetchCommonGoalData(Map<String, Object> commonGoalMap) {

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("goal_id", commonGoalMap.get("goal_id"));
		result.put("new_goal", -1);
		try {
			String goalId = "";
			/*
			 * first check if the common goal user is trying to add has already been added
			 * by user.
			 */
			Select select = QueryBuilder.select().column("goal_id").from(userLearningGoalsDb.getKeySpace(),
					userLearningGoalsDb.getTableName());
			select.where(eq("user_email", commonGoalMap.get("user_email"))).and(eq("goal_type", "common"))
					.and(eq("goal_id", UUID.fromString(commonGoalMap.get("goal_id").toString())));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(select);
			Row commonGoalRow = results.one();
			if (commonGoalRow != null) {
				goalId = commonGoalRow.getUUID("goal_id").toString();
				result.put("new_goal", 0);
			}

			/*
			 * if the common goal user is trying to add has not been added by user, then
			 * check if this common goal has been shared with him by someone.
			 */
			if (goalId == "") {
				Select sharedGoalselect = QueryBuilder.select().column("goal_id").from(sharedGoalsDb.getKeySpace(),
						sharedGoalsDb.getTableName());
				sharedGoalselect.where(eq("shared_with", commonGoalMap.get("user_email")))
						.and(eq("goal_type", "common_shared"))
						.and(eq("goal_id", UUID.fromString(commonGoalMap.get("goal_id").toString())))
						.and(eq("status", 1));
				ProjectLogger.log("Query: " + sharedGoalselect, LoggerEnum.DEBUG);
				ResultSet sharedGoalResult = connectionManager.getSession(sharedGoalsDb.getKeySpace())
						.execute(sharedGoalselect);

				for (Row row : sharedGoalResult) {
					goalId = row.getUUID("goal_id").toString();
					result.put("new_goal", 0);
					break;
				}

			}

			if (goalId == "") {
				Select commonGoalSelect = QueryBuilder.select().column("id").column("goal_title")
						.column("goal_content_id").column("goal_desc")
						.from(commonLearningGoalsDb.getKeySpace(), commonLearningGoalsDb.getTableName());
				commonGoalSelect.where(eq("id", UUID.fromString(commonGoalMap.get("goal_id").toString())));
				ProjectLogger.log("Query: " + commonGoalSelect, LoggerEnum.DEBUG);
				ResultSet commonGoalResultSet = connectionManager.getSession(commonLearningGoalsDb.getKeySpace())
						.execute(commonGoalSelect);
				Row datarow = commonGoalResultSet.one();
				if (datarow != null) {
					result.put("new_goal", 1);
					result.put("goal_title", datarow.getString("goal_title"));
					result.put("goal_desc", datarow.getString("goal_desc"));
					result.put("goal_content_id", datarow.getList("goal_content_id", String.class));
				}
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return result;
	}

	/*
	 * this method is used to update duration
	 */
	private int updateDurationGoal(Map<String, Object> goalData) {
		int result = 0;
		try {
			Select select = QueryBuilder.select().all().from(userLearningGoalsDb.getKeySpace(),
					userLearningGoalsDb.getTableName());
			select.where(eq("user_email", goalData.get("user_email"))).and(eq("goal_type", goalData.get("goal_type")))
					.and(eq("goal_id", UUID.fromString(goalData.get("goal_id").toString())));
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(select);
			Row commonGoalRow = results.one();
			if (commonGoalRow != null) {
				Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
				Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
				Date endDate = null;
				Date startDate = commonGoalRow.getTimestamp("goal_start_date");
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);
				cal.add(Calendar.DATE, (int) goalData.get("goal_duration"));
				endDate = cal.getTime();
				Date currentDate = Calendar.getInstance().getTime();
				if (endDate.before(currentDate)) {
					endDate = currentDate;
				}

				Update.Where updateStmt = QueryBuilder
						.update(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName())
						.with(QueryBuilder.set("goal_duration", goalData.get("goal_duration")))
						.and(QueryBuilder.set("goal_end_date", endDate.getTime()))
						.and(QueryBuilder.set("last_updated_on", timestamp))
						.where(eq("user_email", goalData.get("user_email")))
						.and(eq("goal_type", goalData.get("goal_type").toString()))
						.and(eq("goal_id", UUID.fromString(goalData.get("goal_id").toString())));
				ProjectLogger.log("Query: " + updateStmt, LoggerEnum.DEBUG);
				connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(updateStmt);
				result = 1;
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_UPDATE + userLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return result;
	}

	private Map<String, Object> fetchCommonGoalDataByGoalID(String goalID) {
		Map<String, Object> goalData = new HashMap<>();
		try {
			Select select = QueryBuilder.select().column("id").column("goal_title").column("goal_content_id")
					.column("goal_desc")
					.from(commonLearningGoalsDb.getKeySpace(), commonLearningGoalsDb.getTableName());
			Where selectWhere = select.where(eq("id", UUID.fromString(goalID)));
			ProjectLogger.log("Query: " + selectWhere, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(commonLearningGoalsDb.getKeySpace()).execute(selectWhere);

			for (Row row : results) {
				goalData.put("goal_id", row.getUUID("id").toString());
				goalData.put("goal_title", row.getString("goal_title"));
				goalData.put("goal_desc", row.getString("goal_desc"));
				goalData.put("goal_content_id", row.getList("goal_content_id", String.class));
				break;
			}

		} catch (Exception e) {
			ProjectLogger.log(
					Constants.EXCEPTION_MSG_FETCH + commonLearningGoalsDb.getTableName() + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return goalData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getContentIdsForGoal(String userId, String goalId) throws Exception {

		List<String> ret = new ArrayList<>();

		String goalType = "";
		DbInfo db = null;
		String userField = "";

		Select select = QueryBuilder.select().column("goal_type")
				.from(userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName()).limit(1);
		Where selectWhere = select.where(eq("user_email", userId))
				.and(in("goal_type", Arrays.asList("common", "user", "commonshared", "tobeshared")))
				.and(eq("goal_id", UUID.fromString(goalId)));
		ResultSet results = connectionManager.getSession(userLearningGoalsDb.getKeySpace()).execute(selectWhere);

		for (Row row : results.all()) {
			goalType = row.getString("goal_type");
			db = userLearningGoalsDb;
			userField = "user_email";
		}

		if (db == null) {
			select = QueryBuilder.select().column("goal_type")
					.from(sharedGoalsTrackerDb.getKeySpace(), sharedGoalsTrackerDb.getTableName()).limit(1);
			selectWhere = select.where(eq("shared_by", userId))
					.and(in("goal_type", Arrays.asList("common_shared", "custom_shared")))
					.and(eq("goal_id", UUID.fromString(goalId)));
			results = connectionManager.getSession(sharedGoalsTrackerDb.getKeySpace()).execute(selectWhere);

			for (Row row : results.all()) {
				goalType = row.getString("goal_type");
				db = sharedGoalsTrackerDb;
				userField = "shared_by";
			}
		}

		if (db != null) {
			select = QueryBuilder.select().column("goal_content_id").from(db.getKeySpace(), db.getTableName()).limit(1);
			selectWhere = select.where(eq(userField, userId)).and(eq("goal_type", goalType))
					.and(eq("goal_id", UUID.fromString(goalId)));
			results = connectionManager.getSession(db.getKeySpace()).execute(selectWhere);

			for (Row row : results.all()) {
				ret = (List<String>) row.getObject("goal_content_id");
			}

		} else
			throw new Exception("No Goal Found!");
		return ret;
	}
}
