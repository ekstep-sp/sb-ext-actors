/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.sunbird.common.models.util.ProjectLogger;

import com.fasterxml.uuid.impl.UUIDUtil;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.model.cassandra.EducatorGroupModel;
import com.infosys.model.cassandra.GroupUserModel;
import com.infosys.model.cassandra.UserExerciseLastByFeedbackModel;
import com.infosys.model.cassandra.UserExerciseLastModel;
import com.infosys.model.cassandra.UserExerciseModel;
import com.infosys.repository.EducatorGroupRepository;
import com.infosys.repository.ExerciseRepository;
import com.infosys.repository.GroupUserRepository;
import com.infosys.repository.UserExerciseLastByFeedbackRepository;
import com.infosys.repository.UserExerciseLastRepository;
import com.infosys.repository.UserExerciseRepository;
import com.infosys.repository.UserRepository;
import com.infosys.service.UserUtilityService;

@Repository
public class ExerciseRepositoryImpl implements ExerciseRepository {

	@Autowired
	UserRepository uRepository;

	@Autowired
	UserExerciseRepository uExerciseRepository;

	@Autowired
	UserExerciseLastRepository uExerciseLastRepository;

	@Autowired
	UserExerciseLastByFeedbackRepository uExerciseLastByFeebackRepository;

	@Autowired
	EducatorGroupRepository eGroupRepository;

	@Autowired
	GroupUserRepository gUserRepository;

	@Autowired
	UserUtilityService userUtilService;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public List<Map<String, Object>> getAll(String userUUID, String contentId) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		try {
			List<UserExerciseModel> result = uExerciseRepository.findByPrimaryKeyUserIdAndPrimaryKeyContentId(userUUID,
					contentId);

			for (UserExerciseModel row : result) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("submission_id", row.getPrimaryKey().getSubmissionId());
				temp.put("submission_time",
						row.getSubmissionTime() == null ? null : dateFormatter.format(row.getSubmissionTime()));
				temp.put("result_percent", row.getResultPercent());
				temp.put("submission_url", row.getSubmissionUrl());
				if (row.getFeedbackBy() == null) {
					temp.put("feedback_by", null);
				} else {
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getFeedbackBy());
					temp.put("feedback_by", names.get("firstname").toString() + " " + names.get("lastname").toString());
				}
				temp.put("feedback_type", row.getFeedbackType());
				temp.put("feedback_time",
						row.getFeedbackTime() == null ? null : dateFormatter.format(row.getFeedbackTime()));
				temp.put("feedback_url", row.getFeedbackUrl());
				temp.put("testcases_failed", row.getTestcasesFailed());
				temp.put("testcases_passed", row.getTestcasesPassed());
				temp.put("total_testcases", row.getTotalTestcases());
				temp.put("submission_type", row.getSubmissionType());
				ret.add(temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getLatest(String userUUID, String contentId) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		try {
			List<UserExerciseLastModel> result = uExerciseLastRepository
					.findByPrimaryKeyUserIdAndPrimaryKeyContentId(userUUID, contentId);

			for (UserExerciseLastModel row : result) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("submission_id", row.getSubmissionId());
				temp.put("submission_time",
						row.getSubmissionTime() == null ? null : dateFormatter.format(row.getSubmissionTime()));
				temp.put("result_percent", row.getResultPercent());
				temp.put("submission_url", row.getSubmissionUrl());
				temp.put("submitted_by", row.getPrimaryKey().getUserId());
				{
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getPrimaryKey().getUserId());
					temp.put("submitted_by_email", names.get("email").toString());
				}
				if (row.getFeedbackBy() == null) {
					temp.put("feedback_by", null);
				} else {
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getFeedbackBy());
					temp.put("feedback_by", names.get("firstname").toString() + " " + names.get("lastname").toString());
				}
				temp.put("feedback_type", row.getFeedbackType());
				temp.put("feedback_time",
						row.getFeedbackTime() == null ? null : dateFormatter.format(row.getFeedbackTime()));
				temp.put("feedback_url", row.getFeedbackUrl());
				temp.put("testcases_failed", row.getTestcasesFailed());
				temp.put("testcases_passed", row.getTestcasesPassed());
				temp.put("total_testcases", row.getTotalTestcases());
				temp.put("submission_type", row.getSubmissionType());
				if (row.getFeedbackTime() != null) {
					if (row.getSubmissionId().timestamp() > row.getFeedbackSubmissionId().timestamp()) {
						temp.put("is_feedback_for_older_sumbission", 1);
						temp.put("old_feedback_submission_id", row.getFeedbackSubmissionId());
					}
				}

				ret.add(temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getOne(String userUUID, String contentId, String submissionId) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		try {
			List<UserExerciseModel> result = uExerciseRepository
					.findByPrimaryKeyUserIdAndPrimaryKeyContentIdAndPrimaryKeySubmissionId(userUUID, contentId,
							UUID.fromString(submissionId));
			for (UserExerciseModel row : result) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("submission_id", row.getPrimaryKey().getSubmissionId());
				temp.put("submission_time",
						row.getSubmissionTime() == null ? null : dateFormatter.format(row.getSubmissionTime()));
				temp.put("result_percent", row.getResultPercent());
				temp.put("submission_url", row.getSubmissionUrl());
				temp.put("submitted_by", row.getPrimaryKey().getUserId());
				{
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getPrimaryKey().getUserId());
					temp.put("submitted_by_email", names.get("email").toString());
				}
				if (row.getFeedbackBy() == null) {
					temp.put("feedback_by", null);
				} else {
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getFeedbackBy());
					temp.put("feedback_by", names.get("firstname").toString() + " " + names.get("lastname").toString());
				}
				temp.put("feedback_type", row.getFeedbackType());
				temp.put("feedback_time",
						row.getFeedbackTime() == null ? null : dateFormatter.format(row.getFeedbackTime()));
				temp.put("feedback_url", row.getFeedbackUrl());
				temp.put("testcases_failed", row.getTestcasesFailed());
				temp.put("testcases_passed", row.getTestcasesPassed());
				temp.put("total_testcases", row.getTotalTestcases());
				temp.put("submission_type", row.getSubmissionType());
				ret.add(temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public List<Map<String, String>> getGroups(String educatorUUID) throws Exception {
		List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
		try {
			List<EducatorGroupModel> result = eGroupRepository.findByPrimaryKeyEducatorId(educatorUUID);

			for (EducatorGroupModel row : result) {
				Map<String, String> temp = new HashMap<>();
				temp.put("group_id", row.getPrimaryKey().getGroupId());
				temp.put("group_name", row.getGroupName());
				ret.add(temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public Map<String, Object> getSubmissionsByGroups(String groupId, String contentId) throws Exception {
		Map<String, Object> ret = new HashMap<>();
		try {
			List<Map<String, Object>> dataList = new ArrayList<>();
			int feedbackCount = 0;
			List<UserExerciseLastModel> result = uExerciseLastRepository
					.findByPrimaryKeyUserIdInAndPrimaryKeyContentId(this.getUsersByGroup(groupId), contentId);

			for (UserExerciseLastModel row : result) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("submitted_by", row.getPrimaryKey().getUserId());
				{
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getPrimaryKey().getUserId());
					temp.put("submitted_by_email", names.get("email").toString());
					temp.put("submitted_by_name",
							names.get("firstname").toString() + " " + names.get("lastname").toString());
				}
				temp.put("submission_id", row.getSubmissionId());
				temp.put("submission_time",
						row.getSubmissionTime() == null ? null : dateFormatter.format(row.getSubmissionTime()));
				temp.put("result_percent", row.getResultPercent());
				temp.put("submission_url", row.getSubmissionUrl());
				if (row.getFeedbackBy() == null) {
					temp.put("feedback_by", null);
				} else {
					feedbackCount++;
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getFeedbackBy());
					temp.put("feedback_by", names.get("firstname").toString() + " " + names.get("lastname").toString());
				}
				temp.put("feedback_type", row.getFeedbackType());
				temp.put("feedback_time",
						row.getFeedbackTime() == null ? null : dateFormatter.format(row.getFeedbackTime()));
				temp.put("feedback_url", row.getFeedbackUrl());
				temp.put("testcases_failed", row.getTestcasesFailed());
				temp.put("testcases_passed", row.getTestcasesPassed());
				temp.put("total_testcases", row.getTotalTestcases());
				temp.put("submission_type", row.getSubmissionType());
				if (row.getFeedbackTime() != null) {
					if (row.getSubmissionId().timestamp() > row.getFeedbackSubmissionId().timestamp()) {
						temp.put("is_feedback_for_older_sumbission", 1);
						temp.put("old_feedback_submission_id", row.getFeedbackSubmissionId());
					}
				}

				dataList.add(temp);
			}
			ret.put("submissions", dataList);
			ret.put("feedback_count", feedbackCount);
			ret.put("submission_count", dataList.size());
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	private List<String> getUsersByGroup(String groupId) throws Exception {
		List<String> ret = new ArrayList<String>();
		try {
			List<GroupUserModel> result = gUserRepository.findByPrimaryKeyGroupId(UUID.fromString(groupId));

			for (GroupUserModel row : result) {
				ret.add(row.getPrimaryKey().getUserId());
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getNotificationForUser(String userUUID) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		try {
			Calendar cal = Calendar.getInstance();
			Date upperLimit = cal.getTime();
			cal.add(Calendar.DAY_OF_YEAR, -15);
			Date lowerLimit = cal.getTime();

			List<UserExerciseLastByFeedbackModel> result = uExerciseLastByFeebackRepository
					.findByPrimaryKeyUserIdAndPrimaryKeyFeedbackTimeGreaterThanAndPrimaryKeyFeedbackTimeLessThan(
							userUUID, lowerLimit, upperLimit);

			List<String> contentIdList = new ArrayList<>();
			for (UserExerciseLastByFeedbackModel row : result) {
				contentIdList.add(row.getPrimaryKey().getContentId());
			}

			SearchHits hits = userUtilService.getMetaByIDListandSource(contentIdList,
					new String[]{"name", "identifier"});
			Map<String, String> contentNames = new HashMap<>();
			for (SearchHit hit : hits) {
				Map<String, Object> source = hit.getSourceAsMap();
				contentNames.put(source.get("identifier").toString(), source.get("name").toString());
			}

			for (UserExerciseLastByFeedbackModel row : result) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("submission_id", row.getSubmissionId());
				temp.put("content_id", row.getPrimaryKey().getContentId());
				temp.put("content_name", contentNames.get(row.getPrimaryKey().getContentId()));
				{
					Map<String, Object> names = uRepository.getEmailFromUUID(row.getFeedbackBy());
					temp.put("feedback_by", names.get("firstname").toString() + " " + names.get("lastname").toString());
					temp.put("feedback_time",
							row.getFeedbackTime() == null ? null : dateFormatter.format(row.getFeedbackTime()));
				}
				if (row.getSubmissionId().timestamp() > row.getFeedbackSubmissionId().timestamp()) {
					temp.put("is_feedback_for_older_sumbission", 1);
					temp.put("old_feedback_submission_id", row.getFeedbackSubmissionId());
				}
				ret.add(temp);
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public void validateUserID(String userUUID) throws Exception {
		if (userUUID == null || userUUID.isEmpty())
			throw new InvalidDataInputException("Empty User Id");
		Map<String, Object> data = uRepository.getEmailFromUUID(userUUID);
		if (data.isEmpty())
			throw new InvalidDataInputException("Invalid User Id");
	}

	@Override
	public void validateContentID(String contentId) throws Exception {
		if (contentId == null || contentId.isEmpty())
			throw new InvalidDataInputException("Empty Exercise Id");
		
		SearchResponse response = ConnectionManager
				.getClient().search(
						new SearchRequest().indices("lexcontentindex").types("resource")
								.searchType(SearchType.QUERY_THEN_FETCH)
								.source(new SearchSourceBuilder()
										.query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_id", contentId)))
										.fetchSource(new String[] { "resourceType" }, new String[0])
										.size(1)),
						RequestOptions.DEFAULT);
		
		if (response.getHits().totalHits == 0 || !response.getHits().getAt(0).getSourceAsMap().get("resourceType").toString()
				.toLowerCase().equals("exercise"))
			throw new InvalidDataInputException("Incorrect Exercise Id");
	}

	@Override
	public void validateSubmissionID(String submissionId) throws Exception {
		if (UUIDUtil.typeOf(UUID.fromString(submissionId)) == null)
			throw new InvalidDataInputException("Invalid Submission Id!");
	}

}
