/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.infosys.model.ProgressData;
import com.infosys.repository.BadgeRepository;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import java.text.SimpleDateFormat;
import java.util.*;

@Repository
public class BadgeRepositoryImpl implements BadgeRepository {

	@Autowired
	ProgressRepository prRepository;

    @Autowired
    UserUtilityService userUtilService;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String userBadge = properties.getProperty(LexJsonKey.UserBadges);
	private String badgeMeta = properties.getProperty(LexJsonKey.AllBadges);
	private String userBadgeError = properties.getProperty(LexJsonKey.ErrorUserBadges);
	private CassandraConnectionManager connectionManager;
	private PreparedStatement inComplete = null;
	private PreparedStatement upsertCourseQuizTable = null;
	private PreparedStatement getUserBadge = null;
	private PreparedStatement getBadgeMeta = null;

	public BadgeRepositoryImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	public PreparedStatement getStatement(String name, String keyspaceName, String tableName) {
		PreparedStatement ret = null;
		if (name.equals("inComplete")) {
			if (inComplete == null)
				inComplete = connectionManager.getSession(keyspaceName).prepare("insert into " + tableName
						+ " (email_id, badge_id, badge_type, progress, first_received_date, received_count, last_received_date, progress_date) values(?,?,?,100.0,dateof(now()),1,dateof(now()),dateof(now())) IF NOT EXISTS;");
			ret = inComplete;
		} else if (name.equals("courseQuizBadgeUpsert")) {
			if (upsertCourseQuizTable == null)
				upsertCourseQuizTable = connectionManager.getSession(keyspaceName).prepare("update " + tableName
						+ " set first_received_date = ?, last_received_date= ?, progress = ?, badge_type = 'O', progress_date = dateof(now()), received_count = ? where email_id = ? and badge_id= ?");
			ret = upsertCourseQuizTable;
		} else if (name.equals("getUserBadges")) {
			if (getUserBadge == null)
				getUserBadge = connectionManager.getSession(keyspaceName).prepare(
						"select badge_id,progress from " + tableName + " where badge_id in ? and email_id = ?");
			ret = getUserBadge;
        } else if (name.equals("getBadgeMeta")) {
			if (getBadgeMeta == null)
				getBadgeMeta = connectionManager.getSession(keyspaceName).prepare(
						"select badge_id,threshold from " + tableName + " where badge_group = ? allow filtering");
			ret = getBadgeMeta;
		}
		return ret;
	}

	@Async
    public void insertInBadges(String courseId, List<String> programId, String userEmail, boolean parent) {
		// insert into badges
		this.insertCertificatesAndMedals(bodhiKeyspace, userBadge, courseId, userEmail, "C", 0);
		if (parent) {
            for (String program : programId)
                if (checkProgram(program, courseId, userEmail)) {
                    // insert medal
                    this.insertCertificatesAndMedals(bodhiKeyspace, userBadge, program, userEmail, "M", 0);

                }
		}
	}

	@SuppressWarnings("unchecked")
	private boolean checkProgram(String programId, String courseId, String email) {
		Boolean ret = false;
		try {
			List<String> ids = new ArrayList<String>();
            Map<String, Object> source = userUtilService.getMetaByIDandSource(programId,
					new String[] { "contentType", "children" });

			if (source.get("contentType").toString().toLowerCase().equals("learning path")) {
				for (Map<String, Object> child : (List<Map<String, Object>>) source.get("children")) {
					ids.add(child.get("identifier").toString());
				}

				List<ProgressData> result = prRepository.findProgressOfCourses(email, ids);
				int courseCount = 0;
				// to check if the record is already present in mongo
				boolean recentCourseFlag = false;
				for (ProgressData d : result) {
					if (d.getProgress() == 1) {
						ret = true;
						courseCount++;
						continue;
					} else if (d.getContentId().equals(courseId)) {
						recentCourseFlag = true;
						ret = true;
						courseCount++;
						continue;
					} else {
						ret = false;
						break;
					}
				}
				if (ret && recentCourseFlag && courseCount < ids.size()) {
					ret = false;
				} else if (ret && !recentCourseFlag && courseCount < ids.size() - 1) {
					ret = false;
				}
			}
		} catch (Exception e) {
			ProjectLogger.log("Error : Medal Validation failed :" + e.getMessage(), e);
			ret = false;
		}
		return ret;
	}

	@Override
	public Response insertCertificatesAndMedals(String keyspaceName, String tableName, String id, String email,
			String type, int retryCounter) {
		Response response = new Response();
		try {
			PreparedStatement statement = this.getStatement("inComplete", keyspaceName, tableName);
			BatchStatement batchStatement = new BatchStatement();
			batchStatement.add(statement.bind(email, id, type));
			Calendar cal = Calendar.getInstance();
            if (type.equals("M") & cal.get(Calendar.MONTH) == 11) {
                List<ProgressData> accessedDateList = prRepository.findFirstAccessedOn(email, id);
                if (accessedDateList.size() > 0) {
					cal.setTimeInMillis(accessedDateList.get(0).getFirstAccessedOn());
                    if (cal.get(Calendar.MONTH) == 11)
                        batchStatement.add(statement.bind(email, "Elf", "O"));
				}
			}
			connectionManager.getSession(keyspaceName).execute(batchStatement);
			response.put(Constants.RESPONSE, Constants.SUCCESS);
		} catch (Exception e) {
			ProjectLogger.log("Cassandra BADGE Insertion failed : " + e.getMessage(), e);
			if (retryCounter < 3)
				this.insertCertificatesAndMedals(keyspaceName, tableName, id, email, type, ++retryCounter);
			else if (retryCounter < 4)
				this.insertCertificatesAndMedals(keyspaceName, userBadgeError, id, email, type, ++retryCounter);
		}
		return response;
	}

	@Async
	@Override
	public void insertCourseAndQuizBadge(String email, String type, String contentId) {
		try {
			int count = this.getContentCompletedCount(email, type, contentId);
			PreparedStatement statementOnReciving = this.getStatement("courseQuizBadgeUpsert", bodhiKeyspace,
					userBadge);
			BatchStatement batchStatementUpdate = new BatchStatement();
            Map<String, Integer> meta = null;
			if (type.toLowerCase().equals("course")) {
                meta = this.getBadgeMeta("Course");
				Map<String, Float> badges = this
						.getUserExistingBadges(Arrays.asList(meta.keySet().toArray(new String[0])), email);
				for (String badgeId : meta.keySet()) {
					Float progress = (count * 100.0f / meta.get(badgeId));
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
			} else {
                meta = this.getBadgeMeta("Quiz");
				Map<String, Float> badges = this
						.getUserExistingBadges(Arrays.asList(meta.keySet().toArray(new String[0])), email);
				for (String badgeId : meta.keySet()) {
					Float progress = (count * 100.0f / meta.get(badgeId));
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
			}
			connectionManager.getSession(bodhiKeyspace).execute(batchStatementUpdate);
		} catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("Cassandra BADGE Insertion failed : " + e.getMessage(), e);
		}
	}

    private Map<String, Integer> getBadgeMeta(String badgeGroup) {
        Map<String, Integer> ret = new HashMap<>();
		try {
			PreparedStatement getStatement = this.getStatement("getBadgeMeta", bodhiKeyspace, badgeMeta);
			ResultSet rs = connectionManager.getSession(bodhiKeyspace).execute(getStatement.bind(badgeGroup));
			for (Row row : rs) {
				ret.put(row.getString("badge_id"), row.getInt("threshold"));
			}
        } catch (Exception e) {
			ProjectLogger.log("Cassandra Fetch Failed : " + e.getMessage(), e);
		}
		return ret;
	}

    private int getContentCompletedCount(String email, String type, String contentId) throws Exception {
		int count = 0;
		try {
			List<ProgressData> result = new ArrayList<>();
			if (type.toLowerCase().equals("course"))
				result = prRepository.findCompletedCourses(email);
			else
				result = prRepository.findCompletedQuizzes(email);
			for (ProgressData d : result) {
				if (!d.getContentId().equals(contentId)) {
					count += 1;
				}
			}
			count += 1;
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
	public void insertSanta(String userId) {
		try {
			PreparedStatement inStatement = this.getStatement("inComplete", bodhiKeyspace, userBadge);
			connectionManager.getSession(bodhiKeyspace).execute(inStatement.bind(userId, "Santa", "O"));
		} catch (Exception e) {
			ProjectLogger.log("Cassandra insertion Failed : " + e.getMessage(), e);
		}
	}
}
