/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.BadRequestException;
import com.infosys.helper.ServiceFactory;
import com.infosys.service.SurveyService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Service
public class SurveyServiceImpl implements SurveyService {
	private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private Util.DbInfo surveyDb = Util.dbInfoMap.get(LexJsonKey.SURVEY_DB);

	@SuppressWarnings("unchecked")
	@Override
	public void submitSurvey(Request request) throws Exception {
		Map<String, Object> requestMap = request.getRequest();
		validations(requestMap);
		List<Map<String, Object>> insertMaps = createInsertMaps(requestMap);
		Map<String, Object> tableReqMap = new HashMap<>();
		tableReqMap.put("survey_id", Integer.valueOf(String.valueOf(requestMap.get("survey_id"))));
		tableReqMap.put("user_id", String.valueOf(requestMap.get("user_id")).toLowerCase());
		Response response = cassandraOperation.getRecordsByProperties(surveyDb.getKeySpace(), surveyDb.getTableName(),
				tableReqMap);
		List<Map<String, Object>> resultMaps = (List<Map<String, Object>>) response.getResult().get("response");
		if (resultMaps.size() > 0) {
			throw new BadRequestException("Survey already submitted");
		}
		try {
			cassandraOperation.batchInsert(surveyDb.getKeySpace(), surveyDb.getTableName(), insertMaps);
		} catch (Exception e) {
			throw new Exception("Error while submitting survey");
		}
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> createInsertMaps(Map<String, Object> requestMap) {
		Integer surveyId = Integer.valueOf(String.valueOf(requestMap.get("survey_id")));
		String userId = String.valueOf(requestMap.get("user_id")).toLowerCase();
		List<Map<String, Object>> questionAnswers = (List<Map<String, Object>>) requestMap.get("answers");

		List<Map<String, Object>> insertMaps = new ArrayList<>();

		for (Map<String, Object> questionAnswer : questionAnswers) {
			Map<String, Object> insertMap = new HashMap<>();
			insertMap.put("survey_id", surveyId);
			insertMap.put("user_id", userId);
			insertMap.put("question_id", Integer.valueOf(String.valueOf(questionAnswer.get("question_id"))));
			insertMap.put("answer", String.valueOf(questionAnswer.get("answer")));
			insertMaps.add(insertMap);
		}
		return insertMaps;
	}

	public void validations(Map<String, Object> requestMap) throws BadRequestException {
		if (requestMap.get("survey_id") == null || String.valueOf(requestMap.get("survey_id")).isEmpty()) {
			throw new BadRequestException("survey_id cannot be empty");
		}
		if (requestMap.get("user_id") == null || String.valueOf(requestMap.get("user_id")).trim().isEmpty()) {
			throw new BadRequestException("user_id cannot be empty");
		}
		if (requestMap.get("answers") == null) {
			throw new BadRequestException("answers cannot be empty");
		}
	}
}
