/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.infosys.repository.AssessmentRepository;
import com.infosys.repository.BadgeRepository;
import com.infosys.repository.UserDataCorrectionRepository;
import com.infosys.service.UserDataCorrectionService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserDataCorrectionServiceImpl implements UserDataCorrectionService {

	@Autowired
	AssessmentRepository aRepository;

	@Autowired
	BadgeRepository bRepository;

	@Autowired
	UserDataCorrectionRepository uDRepository;

	@Autowired
	UserUtilityService userUtilService;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String assessmentContentUser = properties.getProperty(LexJsonKey.ASSESSMENT_BY_CONTENT_USER);
	private CassandraConnectionManager connectionManager;

	public UserDataCorrectionServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public String correctData(String emailId) {
		String ret = "Success";

		try {
			String statement = "select user_id,parent_source_id, max(result_percent) as result from "
					+ assessmentContentUser + " where user_id='" + emailId + "' group by user_id,parent_source_id";
			ResultSet assessmentData = connectionManager.getSession(bodhiKeyspace).execute(statement);

			Set<String> contentIds = new HashSet<>();
			for (Row assessment : assessmentData)
				if (!assessment.getString("parent_source_id").isEmpty()
						&& assessment.getDecimal("result").compareTo(BigDecimal.valueOf(60)) >= 0)
					contentIds.add(assessment.getString("parent_source_id"));

			Map<String, Map<String, Object>> contentMap = new HashMap<>();
			for (SearchHit content : userUtilService.getMetaByIDListandSource(new ArrayList<String>(contentIds),
					new String[] { "identifier", "contentType", "collections" })) {
				contentMap.put(content.getSourceAsMap().get("identifier").toString(), content.getSourceAsMap());
			}

			for (String contentId : contentIds) {
				Map<String, Object> tempSource = contentMap.get(contentId);
				boolean parent = ((List<Object>) tempSource.get("collections")).size() > 0 ? true : false;
				List<String> programId = new ArrayList<String>();
				for (Map<String, Object> collection : ((List<Map<String, Object>>) (tempSource.get("collections")))) {
					programId.add(collection.get("identifier").toString());
				}
				bRepository.insertInBadges(contentId, programId, emailId, parent);
			}

			uDRepository.insertCourseAndQuizBadge(emailId, "Course");
			uDRepository.insertCourseAndQuizBadge(emailId, "Quiz");
			uDRepository.insertNewUserBadge(emailId);

		} catch (Exception e) {
			ret = e.getMessage();
		}
		return ret;
	}
}
