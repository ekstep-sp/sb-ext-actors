/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.infosys.model.ProgressData;
import com.infosys.repository.ProgressRepository;
import com.infosys.repository.UserDataCorrectionRepository;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Repository
public class UserDataCorrectionRepositoryImpl implements UserDataCorrectionRepository {

	@Autowired
	ProgressRepository prRepository;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String badgeMeta = properties.getProperty(LexJsonKey.AllBadges);
	private String userBadge = properties.getProperty(LexJsonKey.UserBadges);
	private CassandraConnectionManager connectionManager;
	private PreparedStatement upsertCourseQuizTable = null;
	private PreparedStatement insertUser = null;
	private PreparedStatement getUserBadge = null;
	private PreparedStatement getBadgeMeta = null;
	
	public UserDataCorrectionRepositoryImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	public PreparedStatement getStatement(String name, String keyspaceName, String tableName) {
//		System.out.print(tableName);
		PreparedStatement ret = null;
		if (name.equals("courseQuizBadgeUpsert")) {
			if (upsertCourseQuizTable == null)
				upsertCourseQuizTable = connectionManager.getSession(keyspaceName).prepare("update " + tableName
						+ " set first_received_date = ?, last_received_date= ?, progress = ?, badge_type = 'O', progress_date = dateof(now()), received_count = ? where email_id = ? and badge_id= ?");
			ret = upsertCourseQuizTable;
		} else if (name.equals("getUserBadges")) {
			if (getUserBadge == null)
				getUserBadge = connectionManager.getSession(keyspaceName).prepare(
						"select badge_id,progress from " + tableName + " where badge_id in ? and email_id = ?");
			ret = getUserBadge;
		} else if (name.equals("insertUser")) {
			if (insertUser == null)
				insertUser = connectionManager.getSession(keyspaceName).prepare("insert into " + tableName
						+ " (email_id, badge_id, badge_type, progress, first_received_date, received_count, last_received_date, progress_date) values(?,'NewUser','O',100.0,dateof(now()),1,dateof(now()),dateof(now())) IF NOT EXISTS;");
			ret = insertUser;
		}else if (name.equals("getBadgeMeta")) {
			if (getBadgeMeta == null)
				getBadgeMeta = connectionManager.getSession(keyspaceName).prepare(
						"select badge_id,threshold from " + tableName + " where badge_group = ? allow filtering");
			ret = getBadgeMeta;
		}
		return ret;
	}

	@Async
	@Override
	public void insertCourseAndQuizBadge(String email, String type) {
		try {
			int count = this.getContentCompletedCount(email, type);
			PreparedStatement statementOnReciving = this.getStatement("courseQuizBadgeUpsert", bodhiKeyspace,
					userBadge);
			BatchStatement batchStatementUpdate = new BatchStatement();
			Map<String, Integer> executionMap = null;
			if (type.toLowerCase().equals("course")) {
				executionMap = this.getBadgeMeta("Course");
			} else {
				executionMap = this.getBadgeMeta("Quiz");
			}
			Map<String, Float> badges = this
					.getUserExistingBadges(Arrays.asList(executionMap.keySet().toArray(new String[0])), email);
			for (String badgeId : executionMap.keySet()) {
				Float progress = (count * 100.0f / executionMap.get(badgeId));
				Float oldProgress = badges.getOrDefault(badgeId, 0.0f);
				if (oldProgress < 100) {
					if (progress >= 100) {
						Date d = new Date();
						batchStatementUpdate.add(statementOnReciving.bind(d, d, 100.0f, 1, email, badgeId));
					} else {
						Date d = new Date(0);
						batchStatementUpdate.add(statementOnReciving.bind(d, d, progress, 0, email, badgeId));
					}
				}
			}
			connectionManager.getSession(bodhiKeyspace).execute(batchStatementUpdate);
		} catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("Cassandra BADGE Insertion failed : " + e.getMessage(), e);
		}
	}
	
	private Map<String,Integer> getBadgeMeta(String badgeGroup){
		Map<String,Integer> ret=new HashMap<>();
		try {
			PreparedStatement getStatement = this.getStatement("getBadgeMeta", bodhiKeyspace, badgeMeta);
			ResultSet rs = connectionManager.getSession(bodhiKeyspace).execute(getStatement.bind(badgeGroup));
			for (Row row : rs) {
				ret.put(row.getString("badge_id"), row.getInt("threshold"));
			}
		}
		catch(Exception e) {
			ProjectLogger.log("Cassandra Fetch Failed : " + e.getMessage(), e);
		}
		return ret;
	}

	private int getContentCompletedCount(String email, String type) throws Exception {
		int count = 0;
		try {
			List<ProgressData> result = new ArrayList<>();
			if (type.toLowerCase().equals("course"))
				result = prRepository.findCompletedCourses(email);
			else
				result = prRepository.findCompletedQuizzes(email);
			count = result.size();
		} catch (Exception e) {
			ProjectLogger.log("Mongo Fetch Failed : " + e.getMessage(), e);
			throw new Exception("Couldn't fetch completed " + type);
		}
		return count;
	}

	private Map<String, Float> getUserExistingBadges(List<String> ids, String email) throws Exception {
		Map<String, Float> ret = new HashMap<>();
		try {
			PreparedStatement getStatement = this.getStatement("getUserBadges", bodhiKeyspace, userBadge);
			ResultSet rs = connectionManager.getSession(bodhiKeyspace).execute(getStatement.bind(ids, email));
			for (Row row : rs) {
				ret.put(row.getString("badge_id"), row.getFloat("progress"));
			}
		} catch (Exception e) {
			ProjectLogger.log("Cassandra Fetch Failed : " + e.getMessage(), e);
			throw new Exception("Couldn't fetch user Badges");
		}
		return ret;
	}

	@Override
	public void insertNewUserBadge(String email) {
		try {
			PreparedStatement statement = this.getStatement("insertUser", bodhiKeyspace, userBadge);
			BatchStatement batchStatement = new BatchStatement();
			batchStatement.add(statement.bind(email));
			connectionManager.getSession(bodhiKeyspace).execute(batchStatement);
		} catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("Cassandra BADGE Insertion failed : " + e.getMessage(), e);
		}
	}

}
