/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.helper.ServiceFactory;
import com.infosys.repository.AssessmentRepository;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Repository
public class AssessmentRepositoryImpl implements AssessmentRepository {

	@Autowired
	ProgressRepository prRepository;
	
	@Autowired
	UserUtilityService userUtilService;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private Util.DbInfo assessmentView = Util.dbInfoMap.get(LexJsonKey.ASSESSMENT_BY_CONTENT_USER);
	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private CassandraConnectionManager connectionManager;
	private PreparedStatement inAssessmentTable = null;
	private PreparedStatement inQuizTable = null;
	private PreparedStatement getCountAndMax = null;

	public AssessmentRepositoryImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	public PreparedStatement getStatement(String name, String keyspaceName, String tableName) {
		PreparedStatement ret = null;
		if (name.equals("assessmentInsert")) {
			if (inAssessmentTable == null)
				inAssessmentTable = connectionManager.getSession(keyspaceName).prepare("insert into " + tableName
						+ " (parent_source_id,result_percent,date_created,ts_created,id,pass_percent,source_id,source_title,user_id,correct_count,incorrect_count,not_answered_count,parent_content_type) values(?,?,?,?,now(),60,?,?,?,?,?,?,?);");
			ret = inAssessmentTable;
		} else if (name.equals("quizInsert")) {
			if (inQuizTable == null)
				inQuizTable = connectionManager.getSession(keyspaceName).prepare("insert into " + tableName
						+ " (result_percent,date_created,ts_created,id,pass_percent,source_id,source_title,user_id,correct_count,incorrect_count,not_answered_count) values(?,?,?,now(),60,?,?,?,?,?,?);");
			ret = inQuizTable;
		} else if (name.equals("getCountAndMax")) {
			if (getCountAndMax == null)
				getCountAndMax = connectionManager.getSession(keyspaceName).prepare("Select count(*) as Count, Max(result_percent) as Max_result from " + tableName
						+ " where user_id=? and parent_source_id=?;");
			ret = getCountAndMax;
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getAssessmentAnswerKey(String artifactUrl) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		try {
			Map<String, Object> m = userUtilService.getAssessmentMap(artifactUrl);
			for (Map<String, Object> question : (List<Map<String, Object>>) m.get("questions")) {
				List<String> correctOption = new ArrayList<String>();
				if (question.containsKey("questionType")) {
					if (question.get("questionType").toString().toLowerCase().equals("mtf")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if ((boolean) options.get("isCorrect"))
								correctOption.add(options.get("optionId").toString() + "-"
										+ options.get("text").toString().toLowerCase() + "-"
										+ options.get("match").toString().toLowerCase());
						}
					} else if (question.get("questionType").toString().toLowerCase().equals("fitb")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if ((boolean) options.get("isCorrect"))
								correctOption.add(options.get("optionId").toString() + "-"
										+ options.get("text").toString().toLowerCase());
						}
					} else if (question.get("questionType").toString().toLowerCase().equals("mcq-sca")
							|| question.get("questionType").toString().toLowerCase().equals("mcq-mca")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if ((boolean) options.get("isCorrect"))
								correctOption.add(options.get("optionId").toString());
						}
					}
				} else {
					for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
						if ((boolean) options.get("isCorrect"))
							correctOption.add(options.get("optionId").toString());
					}
				}
				ret.put(question.get("questionId").toString(), correctOption);
			}
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getQuizAnswerKey(Map<String, Object> quizMap) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		try {
			for (Map<String, Object> question : (List<Map<String, Object>>) quizMap.get("questions")) {
				List<String> correctOption = new ArrayList<String>();
				if (question.containsKey("questionType")) {
					if (question.get("questionType").toString().toLowerCase().equals("mtf")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							correctOption.add(options.get("optionId").toString() + "-"
									+ options.get("text").toString().toLowerCase() + "-"
									+ options.get("match").toString().toLowerCase());
						}
					} else if (question.get("questionType").toString().toLowerCase().equals("fitb")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							correctOption.add(options.get("optionId").toString() + "-"
									+ options.get("text").toString().toLowerCase());
						}
					} else if (question.get("questionType").toString().toLowerCase().equals("mcq-sca")
							|| question.get("questionType").toString().toLowerCase().equals("mcq-mca")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if ((boolean) options.get("isCorrect"))
								correctOption.add(options.get("optionId").toString());
						}
					}
				} else {
					for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
						if ((boolean) options.get("isCorrect"))
							correctOption.add(options.get("optionId").toString());
					}
				}
				ret.put(question.get("questionId").toString(), correctOption);
			}
		} catch (Exception e) {
			throw new Exception("Problem fetching answers");
		}
		return ret;
	}

	public Response insertQuizOrAssessment(String keyspaceName, String tableName, Map<String, Object> persist,
			Boolean isAssessment) throws Exception {
		Response response = new Response();
		try {
			Date record = new Date();
			BatchStatement batchStatement = new BatchStatement();
			PreparedStatement statement;
			if (isAssessment) {
				statement = this.getStatement("assessmentInsert", keyspaceName, tableName);

				batchStatement.add(statement.bind(persist.get("parent"), new BigDecimal((Double) persist.get("result")),
						formatter.parse(formatter.format(record)), record, persist.get("sourceId"),
						persist.get("title"), persist.get("email"), persist.get("correct"), persist.get("incorrect"),
						persist.get("blank"), persist.get("parentContentType")));
			} else {
				statement = this.getStatement("quizInsert", keyspaceName, tableName);

				batchStatement.add(statement.bind(new BigDecimal((Double) persist.get("result")),
						formatter.parse(formatter.format(record)), record, persist.get("sourceId"),
						persist.get("title"), persist.get("email"), persist.get("correct"), persist.get("incorrect"),
						persist.get("blank")));
			}
			connectionManager.getSession(keyspaceName).execute(batchStatement);

			response.put(Constants.RESPONSE, Constants.SUCCESS);
		} catch (Exception e) {
			throw new Exception("Cassandra QUIZ OR ASSESSMENT Insertion failed.");
		}
		return response;
	}

	@Override
	public List<Map<String, Object>> getAssessmetbyContentUser(String courseId, String userId) {
		// get all submission data from cassandra
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("parent_source_id", courseId);
		requestMap.put("user_id", userId);
		// fetching the assessments of a user on a Course from the Assessment_by_content
		// user_view
		Response response = cassandraOperation.getRecordsByProperties(assessmentView.getKeySpace(),
				assessmentView.getTableName(), requestMap);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> assessmentResults = (List<Map<String, Object>>) response.getResult().get("response");
		return assessmentResults;
	}

}
