/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.repository.ProgressRepositoryCustom;
import com.mongodb.BasicDBObject;

@Repository
public class ProgressRepositoryImpl implements ProgressRepositoryCustom {

	private final MongoTemplate mongotemplate;

	@Autowired
	public ProgressRepositoryImpl(MongoTemplate mongotemplate) {
		this.mongotemplate = mongotemplate;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> getUserCourseProgressPaginated(String emailId, String status, String contentType,
			Integer page, Integer pageSize) {
		Map<String, Object> ret = new HashMap<>();
		try {
			Criteria matchCriteria = Criteria.where("empMailId").is(emailId);

			if (status != null)
				if (status.equals("completed"))
					matchCriteria.and("progress").is(1);
				else
					matchCriteria.and("progress").lt(1);

			if (contentType.toLowerCase().equals("learning path")) {
				matchCriteria.and("contentType").is("Learning Path");
			} else if (contentType.toLowerCase().equals("course")) {
				matchCriteria.and("contentType").is("Course");
			} else if (contentType.toLowerCase().equals("collection")) {
				matchCriteria.and("contentType").is("Collection");
			} else if (contentType.toLowerCase().equals("resource")) {
				matchCriteria.and("contentType").is("Resource").and("parent_collections").size(0);
			} else {
				matchCriteria.orOperator(
						Criteria.where("contentType")
								.in(Arrays.asList(new String[] { "Learning Path", "Course", "Collection" })),
						Criteria.where("contentType").is("Resource").and("parent_collections").size(0));
			}

			MatchOperation matchStage = Aggregation.match(matchCriteria);
			ProjectionOperation projectStage = Aggregation
					.project("thumbnail", "progress", "progress_children", "children_duration", "contentType",
							"last_ts")
					.andExpression("cid").as("identifier").andExpression("contentName").as("name")
					.andExpression("length").as("totalDuration").andExpression("children_ids").as("children")
					.andExpression("1-progress").as("pending").andExclude("_id");
			SortOperation sortStage = Aggregation.sort(new Sort(Direction.DESC, "last_ts"));
			GroupOperation groupStage = Aggregation.group().count().as("count").push("$$ROOT").as("results");
			ProjectionOperation finalProjectStage = Aggregation.project("count").and("results")
					.slice(pageSize, page * pageSize).as("results").andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage, sortStage, groupStage,
					finalProjectStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "user_content_progress_collection",
					Map.class);
			if (output.getMappedResults().size() > 0)
				ret = output.getMappedResults().get(0);
			else
				throw new Exception("No Data Found");
		} catch (Exception e) {
			ret.put("count", 0);
			ret.put("results", new ArrayList<Map<String, Object>>());
			ret.put("error", "No Data Found");
			ProjectLogger.log("Error in progress repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Object> getUserCourseProgressNonPaginated(String emailId, String status, String contentType) {
		Map<String, Object> ret = new HashMap<>();
		try {
			Criteria matchCriteria = Criteria.where("empMailId").is(emailId);

			if (status != null)
				if (status.equals("completed"))
					matchCriteria.and("progress").is(1);
				else
					matchCriteria.and("progress").lt(1);
			if (contentType.toLowerCase().equals("learning path")) {
				matchCriteria.and("contentType").is("Learning Path");
			} else if (contentType.toLowerCase().equals("course")) {
				matchCriteria.and("contentType").is("Course");
			} else if (contentType.toLowerCase().equals("collection")) {
				matchCriteria.and("contentType").is("Collection").and("parent_collections").size(0);
			} else if (contentType.toLowerCase().equals("resource")) {
				matchCriteria.and("contentType").is("Resource").and("parent_collections").size(0);
			} else {
				matchCriteria.orOperator(
						Criteria.where("contentType").in(Arrays.asList(new String[] { "Learning Path", "Course" })),
						Criteria.where("contentType").in(Arrays.asList(new String[] { "Resource", "Collection" }))
								.and("parent_collections").size(0));
			}

			MatchOperation matchStage = Aggregation.match(matchCriteria);
			ProjectionOperation projectStage = Aggregation
					.project("thumbnail", "progress", "progress_children", "children_duration", "contentType",
							"last_ts")
					.andExpression("cid").as("identifier").andExpression("contentName").as("name")
					.andExpression("length").as("totalDuration").andExpression("children_ids").as("children")
					.andExpression("1-progress").as("pending").andExclude("_id");
			SortOperation sortStage = Aggregation.sort(new Sort(Direction.DESC, "last_ts"));

			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage, sortStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "user_content_progress_collection",
					Map.class);

			if (output.getMappedResults().size() > 0) {
				ret.put("count", output.getMappedResults().size());
				ret.put("results", output.getMappedResults());
			} else
				throw new Exception("No Data Found");
		} catch (Exception e) {
			ret.put("count", 0);
			ret.put("results", new ArrayList<Map<String, Object>>());
			ret.put("error", "No Data Found");
			ProjectLogger.log("Error in progress repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> fetchUserContentListProgress(String emailId, List<String> contentIds) {
		Map<String, Object> ret = new HashMap<>();
		try {
			MatchOperation matchStage = Aggregation
					.match(Criteria.where("empMailId").is(emailId).and("cid").in(contentIds));
			ProjectionOperation projectStage = Aggregation
					.project("thumbnail", "progress", "progress_children", "children_duration", "contentType")
					.andExpression("cid").as("identifier").andExpression("contentName").as("name")
					.andExpression("length").as("totalDuration").andExpression("children_ids").as("children")
					.andExpression("1-progress").as("pending").andExpression("time_spent").as("timeSpent")
					.andExclude("_id");
			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "user_content_progress_collection",
					Map.class);
			int resultSize = output.getMappedResults().size();
			if (resultSize > 0) {
				Map<String, Object> resultMap = new HashMap<>();
				List<String> missingIds = new ArrayList<>();
				for (Map<String, Object> temp : output.getMappedResults()) {
					resultMap.put(temp.get("identifier").toString(), temp);
				}
				if (resultSize < contentIds.size()) {
					for (String id : contentIds) {
						if (!resultMap.containsKey(id))
							missingIds.add(id);
					}
				}
				ret.put("count", resultSize);
				ret.put("results", resultMap);
				ret.put("missingIds", missingIds);
			} else {
				ret.put("count", resultSize);
				ret.put("results", new HashMap<String, Object>());
				ret.put("missingIds", contentIds);
			}
		} catch (Exception e) {
			ret.put("count", 0);
			ret.put("results", new HashMap<String, Object>());
			ret.put("missingIds", new ArrayList<>());
			ret.put("error", "No Data Found");
			ProjectLogger.log("Error in progress repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> fetchUserContentListMap(String emailId, List<String> contentIds) {
		Map<String, Object> ret = new HashMap<>();
		try {
			Criteria matchCriteria = Criteria.where("empMailId").is(emailId);
			if (contentIds.size() > 0)
				matchCriteria.and("cid").in(contentIds);
			MatchOperation matchStage = Aggregation.match(matchCriteria);
			ProjectionOperation projectStage = Aggregation.project("cid", "empMailId", "progress").andExclude("_id");
			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "user_content_progress_collection",
					Map.class);
			int resultSize = output.getMappedResults().size();
			if (resultSize > 0) {
				for (Map<String, Object> temp : output.getMappedResults()) {
					ret.put(temp.get("cid").toString(), temp.get("progress").toString());
				}
			} else
				throw new Exception("No Data Found");
		} catch (Exception e) {
			ret.put("count", 0);
			ret.put("results", new HashMap<String, Object>());
			ret.put("error", "No Data Found");
			ProjectLogger.log("Error in progress repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@Override
	public List<Document> getCourseProgress(String emailId) {
		List<Document> ret = null;
		try {
			MatchOperation matchStage = Aggregation.match(Criteria.where("empMailId").is(emailId).and("contentType")
					.in(Arrays.asList("Course", "Learning Path")));
			ProjectionOperation projectStage = Aggregation
					.project("cid", "progress")
					.and(ConditionalOperators.when(Criteria.where("contentType").is("Course")).then("C").otherwise("M"))
					.as("badgeType")
					.and(ConditionalOperators.when(Criteria.where("has_assessment").is(true))
							.thenValueOf(ConditionalOperators.when(Criteria.where("progress").gte(0.65)).then(1)
									.otherwise(0))
							.otherwise(ConditionalOperators.when(Criteria.where("progress").gte(0.85)).then(1)
									.otherwise(0)))
					.as("select").andExclude("_id");
			MatchOperation finalMatchStage = Aggregation.match(Criteria.where("select").is(1));
			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage, finalMatchStage);

			// aggregation
			AggregationResults<Document> output = mongotemplate.aggregate(aggregation,
					"user_content_progress_collection", Document.class);
			ret = output.getMappedResults();
		} catch (Exception e) {
			ProjectLogger.log("Error in progress repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@Override
	public List<Document> getActiveUsersFromDB(String userEmail, List<String> resourceIds, long lastTs) {
		List<Document> ret = null;
		try {
			MatchOperation matchStage = Aggregation.match(
					Criteria.where("cid").in(resourceIds).and("empMailId").ne(userEmail).and("last_ts").gt(lastTs));
			ProjectionOperation projectStage = Aggregation.project("empMailId", "last_ts").andExclude("_id");
			SortOperation sortStage = Aggregation.sort(new Sort(Direction.DESC, "last_ts"));
			LimitOperation limitStage = Aggregation.limit(50);
			Aggregation aggregation = Aggregation.newAggregation(matchStage, projectStage, sortStage, limitStage);

			// aggregation
			AggregationResults<Document> output = mongotemplate.aggregate(aggregation,
					"user_content_progress_collection", Document.class);
			ret = output.getMappedResults();
		} catch (Exception e) {
			ProjectLogger.log("Error in method getActiveUsers in ProgressRepository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@Override
	public List<Document> getGoalProgressFromDB(List<String> userEmails, List<String> resourceIds) {
		List<Document> ret = null;
		try {
			MatchOperation matchStage = Aggregation
					.match(Criteria.where("empMailId").in(userEmails).and("cid").in(resourceIds));
			GroupOperation groupStage = Aggregation.group("empMailId").push(new BasicDBObject("progress", "$progress")
					.append("length", "$length").append("cid", "$cid").append("time_spent", "$time_spent")).as("data");
			Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);
			// aggregation
			AggregationResults<Document> output = mongotemplate.aggregate(aggregation,
					"user_content_progress_collection", Document.class);
			ret = output.getMappedResults();
		} catch (Exception e) {
			ProjectLogger.log("Error in method getGoalProgress in ProgressRepository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}
}
