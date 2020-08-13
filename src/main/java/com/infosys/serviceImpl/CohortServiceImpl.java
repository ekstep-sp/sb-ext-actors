/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 *
 */
package com.infosys.serviceImpl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.CohortUsers;
import com.infosys.model.ContentMeta;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.CohortService;
import com.infosys.service.ParentService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
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
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import com.infosys.elastic.common.ElasticSearchUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author Gaurav_Kumar48
 */
@Service
public class CohortServiceImpl implements CohortService {

	@Autowired
	ProgressRepository progressRepo;

	@Autowired
	UserUtilityService userUtilService;

	@Autowired
	ParentService parentSvc;

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private Util.DbInfo userLearningGoalsDb = Util.dbInfoMap.get(LexJsonKey.USER_Learning_Goals_Table);
	private Util.DbInfo assessmentsResultCourseDb = Util.dbInfoMap.get(LexJsonKey.Assessments_By_Course_Result_Table);
	private Util.DbInfo educatorsDb = Util.dbInfoMap.get(LexJsonKey.Educators_Table);
	// private String telemetryDBName =
	// properties.getProperty(LexJsonKey.Mongo_Telemetry_DB);
	// private String activeUsersCollection =
	// properties.getProperty(LexJsonKey.Telemetry_Progress_Collection);

	private CassandraConnectionManager connectionManager;

	public CohortServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	/*
	 * not used for now // this reads cohort properties file private Properties
	 * readCohortPropeties() { InputStream is = null; Properties prop = new
	 * Properties(); try { prop = new Properties(); is =
	 * this.getClass().getResourceAsStream("/Cohort.properties"); prop.load(is); }
	 * catch (FileNotFoundException e) { e.printStackTrace();
	 *
	 * } catch (IOException e) { e.printStackTrace(); } return prop; }
	 *
	 */

	// get the list of active user for a resource
	@SuppressWarnings("unchecked")
	public Response getActiveUsers(String resourceId, String userEmail, int count) {
		Response contentResponse = new Response();
		Map<String, Object> finalOutput = new HashMap<String, Object>();
		try {
			Long start = System.currentTimeMillis();
            Map<String, Object> parentCollection = parentSvc.getCourseParents(resourceId);
			
			ProjectLogger.log("getactiveusers getparentstime" + ":"
					+ LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start), LoggerEnum.INFO);

            List<ContentMeta> parentCourses = new ArrayList<>();
			List<ContentMeta> parentLearningModules = new ArrayList<>();
			if (parentCollection != null && !parentCollection.isEmpty()) {
				if (parentCollection.get("courses") != null) {
					parentCourses = (List<ContentMeta>) parentCollection.get("courses");
				}
				if ((parentCourses == null || parentCourses.isEmpty()) && parentCollection.get("modules") != null) {
					parentLearningModules = (List<ContentMeta>) parentCollection.get("modules");
				}
			}
			List<String> resourceParentList = new ArrayList<String>();
			String similarGoalResourceId = resourceId;
			// for active users consider parent courses, then learning modules and then
			// resources
			// for similar goal users consider parent course
			if (parentCourses != null && !parentCourses.isEmpty()) {
				for (ContentMeta meta : parentCourses) {
					resourceParentList.add(meta.getIdentifier());
				}
				similarGoalResourceId = resourceParentList.get(0);
			} else if (parentLearningModules != null && !parentLearningModules.isEmpty()) {
				for (ContentMeta meta : parentLearningModules) {
					resourceParentList.add(meta.getIdentifier());
				}
			} else {
				resourceParentList.add(resourceId);
			}
			List<CohortUsers> activeUserCollection = new ArrayList<CohortUsers>();
			List<CohortUsers> usersSharingGoals = new ArrayList<CohortUsers>();
			int counter = 1;
			List<String> emails = new ArrayList<String>();
			Map<String, String> mailMap = new HashMap<String, String>();
			long machineTimestamp = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli();
			start = System.currentTimeMillis();
			List<Document> collection = progressRepo.getActiveUsersFromDB(userEmail, resourceParentList,
					machineTimestamp);
			/*
			ProjectLogger.log("getactiveusers frommongo" + ":"
					+ LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start), LoggerEnum.INFO);
			*/
			for (Document doc : collection) {
				if (doc.get("empMailId") != null) {
					String email = doc.get("empMailId").toString().toLowerCase();
					int isEmail = email.indexOf("@");
					if (isEmail > 1) {
						email = email.substring(0, isEmail);
						if (!emails.contains(email)) {
							emails.add(email);
							CohortUsers activeUser = new CohortUsers();
							activeUser.setEmail(doc.get("empMailId").toString().toLowerCase());
							double timestampData = Double.parseDouble(doc.get("last_ts").toString());
							long lastActive = (long) timestampData;
							// converting into minutes
							long timeLagInUse = (Instant.now().toEpochMilli() - lastActive) / (1000 * 60);
							// if user has accessed the resource within last 15 minutes
							if (timeLagInUse < 15) {
								activeUser.setDesc("Currently viewing");
							} else {
								// checking if user has been active in last 1 day then show no. of hours user
								// was active before
								long timeAgo = timeLagInUse / 1440;
								if (timeAgo < 1) {
									if (timeLagInUse / 60 < 2) {

										if (timeLagInUse < 60) {
											activeUser.setDesc("Last active " + timeLagInUse + " minutes ago");
										} else {
											activeUser.setDesc("Last active " + timeLagInUse / 60 + " hour ago");
										}
									} else {
										activeUser.setDesc("Last active " + timeLagInUse / 60 + " hours ago");
									}

								} else {
									if (timeAgo < 2)
										activeUser.setDesc("Last active " + timeAgo + " day ago");
									else
										activeUser.setDesc("Last active " + timeAgo + " days ago");
								}
							}
							activeUser.setUser_id("activeuser");
							activeUserCollection.add(activeUser);
							mailMap.put(activeUser.getEmail(), "activeuser");
							if (counter == count)
								break;
							counter++;
						}
					}
				}
			}
			// operator list is to specify certain operations like contains/gte/lte etc.
			List<Map<String, Object>> operatorList = new ArrayList<Map<String, Object>>();
			Map<String, Object> operatorCol1 = new HashMap<String, Object>();
			operatorCol1.put("Column", "goal_content_id");
			operatorCol1.put("Value", similarGoalResourceId);
			operatorCol1.put("Operation", "contains");
			operatorList.add(operatorCol1);

			// fetch user_id of users having similar goals
			Response respSharingGoals = cassandraOperation.getRecordsByPropertyOperator(
					userLearningGoalsDb.getKeySpace(), userLearningGoalsDb.getTableName(), operatorList);
			List<Map<String, Object>> sharingGoalsRecords = (List<Map<String, Object>>) respSharingGoals.getResult()
					.get("response");

			if (sharingGoalsRecords != null && !sharingGoalsRecords.isEmpty()) {
				// sorting users with similar goals based on last updated
				Collections.sort(sharingGoalsRecords, new Comparator<Map<String, Object>>() {
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

			counter = 1;
			for (Map<String, Object> similarGoalsRow : sharingGoalsRecords) {
				if (similarGoalsRow.get("user_email") != null) {
					String email = similarGoalsRow.get("user_email").toString().toLowerCase();
					String userName = email.substring(0, email.indexOf("@"));
					// if user is already in user active list or user himself in goal data then
					// ignore
					if (!email.equals(userEmail) && !emails.contains(userName)) {
						emails.add(userName);
						CohortUsers user = new CohortUsers();
						user.setUser_id("similargoal");
						user.setDesc("Has similar goal");
						user.setEmail(email);
						usersSharingGoals.add(user);
						mailMap.put(user.getEmail(), "similargoal");
						if (counter == count)
							break;
						counter++;
					}
				}
			}
			if (!emails.isEmpty()) {
				start = System.currentTimeMillis();
				List<Map<String, Object>> userGraphData = userUtilService.getUsersFromActiveDirectory(emails);

                ProjectLogger.log("getactiveusers from_graph" + ":"
						+ LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start), LoggerEnum.INFO);

                if (!userGraphData.isEmpty()) {
					for (Map<String, Object> map : userGraphData) {
						String officialEmail = map.getOrDefault("onPremisesUserPrincipalName","").toString().toLowerCase();
						if (mailMap.getOrDefault(officialEmail, "").equals("activeuser")) {
							for (CohortUsers user : activeUserCollection) {
								if (user.getEmail().equals(officialEmail)) {
									user.setFirst_name(
											map.get("givenName") != null ? map.get("givenName").toString() : "");
									user.setLast_name(map.get("surname") != null ? map.get("surname").toString() : "");
									user.setPhone_No(
											map.get("mobilePhone") != null ? map.get("mobilePhone").toString() : "0");
									user.setDepartment(
											map.get("department") != null ? map.get("department").toString() : "");
									user.setDesignation(
											map.get("jobTitle") != null ? map.get("jobTitle").toString() : "");
									user.setUserLocation(
											map.get("usageLocation") != null ? map.get("usageLocation").toString()
													: "");
									user.setCity(map.get("city") != null ? map.get("city").toString() : "");
									user.setUser_id(null);
									break;
								}

							}
						} else {
							for (CohortUsers user : usersSharingGoals) {
								if (user.getEmail().equals(officialEmail)) {
									user.setFirst_name(
											map.get("givenName") != null ? map.get("givenName").toString() : "");
									user.setLast_name(map.get("surname") != null ? map.get("surname").toString() : "");
									user.setPhone_No(
											map.get("mobilePhone") != null ? map.get("mobilePhone").toString() : "0");
									user.setDepartment(
											map.get("department") != null ? map.get("department").toString() : "");
									user.setDesignation(
											map.get("jobTitle") != null ? map.get("jobTitle").toString() : "");
									user.setUserLocation(
											map.get("usageLocation") != null ? map.get("usageLocation").toString()
													: "");
									user.setCity(map.get("city") != null ? map.get("city").toString() : "");
									user.setUser_id(null);
									break;
								}
							}
						}

					}
					// removing invalid employeeIds
					for (Iterator<CohortUsers> it = activeUserCollection.iterator(); it.hasNext();) {
						CohortUsers user = it.next();
						if (user.getUser_id() != null)
							it.remove();
					}
					for (Iterator<CohortUsers> it = usersSharingGoals.iterator(); it.hasNext();) {
						CohortUsers user = it.next();
						if (user.getUser_id() != null)
							it.remove();
					}
				}
			}
			finalOutput.put("active_users", activeUserCollection);
			finalOutput.put("similar_goals_users", usersSharingGoals);
			contentResponse.put(Constants.RESPONSE, finalOutput);
		} catch (Exception e) {
			ProjectLogger.log("Exception occured while processing user active records : " + e.getMessage(), e);
			e.printStackTrace();
			throw new ProjectCommonException(ResponseCode.invalidPropertyError.getErrorCode(),
					CassandraUtil.processExceptionForUnknownIdentifier(e), ResponseCode.CLIENT_ERROR.getResponseCode());
		}
		return contentResponse;

	}

	@SuppressWarnings("unchecked")
	public Response getListofSMEs(String resourceId, String userEmail, int count) {
		Response response = new Response();
		// will contain the list of all parents for that i.e. courses only
		List<Object> parentResourceList = new ArrayList<Object>();
		List<String> emails = new ArrayList<String>();
		Map<String, Object> smeData = new HashMap<String, Object>();
		List<CohortUsers> authors = new ArrayList<CohortUsers>();
		List<CohortUsers> educators = new ArrayList<CohortUsers>();
		List<CohortUsers> topPerformers = new ArrayList<CohortUsers>();
		Map<String, String> mailMap = new HashMap<String, String>();
		try {

			// get the meta of resource specifically for authors
			// in case of learning path get its children
			// fetching only those resource with status=LIVE
			Map<String, Object> searchData = new HashMap<>();
			searchData.put(JsonKey.IDENTIFIER, resourceId);
			searchData.put(JsonKey.STATUS, LexProjectUtil.Status.LIVE.getValue());
			List<Map<String, Object>> content = ElasticSearchUtil.searchMatchedData(
					LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(),
					searchData, null, 1);
			Map<String, Object> resourceMeta = null;
			if (content != null && !content.isEmpty()) {
				resourceMeta = content.get(0);
			}

			// Map<String, Object> resourceMeta = ElasticSearchUtil.getDataByIdentifier(
			// LexProjectUtil.EsIndex.bodhi.getIndexName(),
			// LexProjectUtil.EsType.resource.getTypeName(), resourceId);

			int counter = 1;
			if (resourceMeta != null && !resourceMeta.isEmpty()) {
				// remove these people from authors as they are temp authors
				String[] superAuthors = { "shyamprasadkr", "vikramp",
						"hanumesh", "swati", "hanadurrehman.main",
						"preethy.sreepriya" };
				List<String> authorsToExclude = new ArrayList<String>(Arrays.asList(superAuthors));
				if (resourceMeta.containsKey("creatorDetails")) {
					List<Map<String, Object>> creatorDetails = (List<Map<String, Object>>) resourceMeta
							.get("creatorDetails");
					authorsToExclude.add(userEmail); // removing user from authors list if user is author
					for (Map<String, Object> creator : creatorDetails) {
						if (!authorsToExclude.contains(creator.get("email").toString())) {
							if (creator.get("email").toString().contains("@")) {

								String email = creator.get("email").toString().toLowerCase()
										.substring(0, creator.get("email").toString().indexOf("@")).toLowerCase();
								if (!emails.contains(email)) {
									CohortUsers user = new CohortUsers();
									user.setEmail(email + "");
									emails.add(email);

									String[] name = creator.get("name").toString().split(" ");
									if (name.length > 0)
										user.setFirst_name(name[0]);
									if (name.length > 1)
										name = Arrays.copyOfRange(name, 1, name.length);
									user.setLast_name(String.join(" ", name));
									user.setDesc("Author");
									// setting a dummy user id for authors
									user.setUser_id("Auth1");
									mailMap.put(user.getEmail(), "author");
									authors.add(user);
									if (counter == count)
										break;
									counter++;
								}
							}
						}
					}
				}
				if (resourceMeta.containsKey("contentType")) {
					if (resourceMeta.get("contentType").toString().toLowerCase().equals("learning path")) {
						// get the children of resource in case it is learning path
						if (resourceMeta.containsKey("children")) {
							if (resourceMeta.get("children") != null) {
								// using this since at times list of null is coming
								List<?> childrenList = (List<?>) resourceMeta.get("children");
								for (Object childObj : childrenList) {
									if (childObj != null) {
										Map<String, Object> childMap = (Map<String, Object>) childObj;
										parentResourceList.add(childMap.get("identifier").toString());
									}
								}

							}
						}
					} else {
						// get the parents of resource
                        Map<String, Object> parentCollection = parentSvc.getCourseParents(resourceId);
						List<ContentMeta> parent = new ArrayList<ContentMeta>();
						if (parentCollection != null && !parentCollection.isEmpty()) {
							if (parentCollection.get("courses") != null
									&& ((List<ContentMeta>) parentCollection.get("courses")).size() > 0) {
								parent = (List<ContentMeta>) parentCollection.get("courses");
							} else if (parentCollection.get("modules") != null
									&& ((List<ContentMeta>) parentCollection.get("modules")).size() > 0) {
								parent = (List<ContentMeta>) parentCollection.get("modules");
							} else {
								parent = null;
							}
						}

						if (parent != null) {
							for (ContentMeta data : parent)
								parentResourceList.add(data.getIdentifier());
						}
					}
				}
			}
			if (parentResourceList.isEmpty()) {
				parentResourceList.add(resourceId);
			}
			// fetch educator for courses
			Response respEducator = getEducators(educatorsDb.getKeySpace(), educatorsDb.getTableName(),
					LexJsonKey.Educator_Course_Identifier, parentResourceList);
			List<Map<String, Object>> educatorRecords = (List<Map<String, Object>>) respEducator.getResult()
					.get("response");

			counter = 1;
			for (Map<String, Object> educatorRow : educatorRecords) {
				String email = educatorRow.get("educator_email").toString().contains("@")
						? educatorRow.get("educator_email").toString()
								.substring(0, educatorRow.get("educator_email").toString().indexOf("@")).toLowerCase()
						: educatorRow.get("educator_email").toString().toLowerCase();
				// if educator is author also ignore in educator list and remove user from
				// educator
				if (!emails.contains(email) && !userEmail.equals(email + "")) {
					CohortUsers user = new CohortUsers();
					user.setDesc("Educator");
					user.setEmail(email + "");
					// setting a dummy user id for educators
					user.setUser_id("Edu1");
					educators.add(user);
					emails.add(educatorRow.get("educator_email").toString().toLowerCase().contains("@")
							? (educatorRow.get("educator_email").toString().toLowerCase()).substring(0,
									educatorRow.get("educator_email").toString().indexOf("@"))
							: educatorRow.get("educator_email").toString().toLowerCase());
					mailMap.put(user.getEmail(), "educator");
					if (counter == count)
						break;
					counter++;
				}
			}
			/*
			 * get top peformers cutoff from cohort prop file int topPerformerCutOff = 90;
			 * Properties prop = this.readCohortPropeties(); if (prop != null &&
			 * !prop.isEmpty()) { topPerformerCutOff =
			 * Integer.parseInt(prop.getProperty("result_Percent_Cohort")); }
			 */
			// fetch top learners
			Response respTopLearners = getTopPerformers(assessmentsResultCourseDb.getKeySpace(),
					assessmentsResultCourseDb.getTableName(), LexJsonKey.Assessment_Result_Identifier,
					parentResourceList);
			List<Map<String, Object>> topLearnerRecords = (List<Map<String, Object>>) respTopLearners.getResult()
					.get("response");

			// sorting top learners based on date of assessment in desc
			Collections.sort(topLearnerRecords, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> m1, Map<String, Object> m2) {
					if (m1.get("ts_created") != null && m2.get("ts_created") != null) {
						return ((Date) m1.get("ts_created")).compareTo((Date) (m2.get("ts_created")));
					} else {
						return -1;
					}
				}
			}.reversed());

			// in assessment table user_id contains email id instead of userid
			counter = 1;
			for (Map<String, Object> topLearnerRow : topLearnerRecords) {
				if (topLearnerRow.get("user_id").toString().contains("@")) {
					String email = topLearnerRow.get("user_id").toString().toLowerCase().substring(0,
							topLearnerRow.get("user_id").toString().indexOf("@"));

					if (!emails.contains(email)
							&& !topLearnerRow.get("user_id").toString().toLowerCase().equals(userEmail)) {
						CohortUsers user = new CohortUsers();
						user.setDesc("Top Learner");
						user.setUser_id("top learner");
						user.setEmail(topLearnerRow.get("user_id").toString().toLowerCase());
						topPerformers.add(user);
						mailMap.put(topLearnerRow.get("user_id").toString().toLowerCase(), "toplearner");
						emails.add(email);
						if (counter == count)
							break;
						counter++;
					}
				}
			}
			// clear topLearnerRecords;
			topLearnerRecords = null;

			List<Map<String, Object>> userGraphData = userUtilService.getUsersFromActiveDirectory(emails);
			if (!userGraphData.isEmpty()) {
				for (Map<String, Object> map : userGraphData) {
					if (mailMap.containsKey(map.get("onPremisesUserPrincipalName").toString().toLowerCase())) {
						if (mailMap.get(map.get("onPremisesUserPrincipalName").toString().toLowerCase())
								.equals("author")) {
							for (CohortUsers user : authors) {
								if (user.getEmail() != null && !user.getEmail().isEmpty()) {
									if (user.getEmail()
											.equals(map.get("onPremisesUserPrincipalName").toString().toLowerCase())) {
										user.setFirst_name(
												map.get("givenName") != null ? map.get("givenName").toString() : "");
										user.setLast_name(
												map.get("surname") != null ? map.get("surname").toString() : "");
										user.setPhone_No(
												map.get("mobilePhone") != null ? map.get("mobilePhone").toString()
														: "0");
										user.setDepartment(
												map.get("department") != null ? map.get("department").toString() : "");
										user.setDesignation(
												map.get("jobTitle") != null ? map.get("jobTitle").toString() : "");
										user.setUserLocation(
												map.get("usageLocation") != null ? map.get("usageLocation").toString()
														: "");
										user.setCity(map.get("city") != null ? map.get("city").toString() : "");
										user.setUser_id(null);
										break;
									}
								}
							}
						} else if (mailMap.get(map.get("onPremisesUserPrincipalName").toString().toLowerCase())
								.equals("educator")) {
							for (CohortUsers user : educators) {
								if (user.getEmail() != null && !user.getEmail().isEmpty()) {
									if (user.getEmail()
											.equals(map.get("onPremisesUserPrincipalName").toString().toLowerCase())) {
										user.setFirst_name(
												map.get("givenName") != null ? map.get("givenName").toString() : "");
										user.setLast_name(
												map.get("surname") != null ? map.get("surname").toString() : "");
										user.setPhone_No(
												map.get("mobilePhone") != null ? map.get("mobilePhone").toString()
														: "0");
										user.setDepartment(
												map.get("department") != null ? map.get("department").toString() : "");
										user.setDesignation(
												map.get("jobTitle") != null ? map.get("jobTitle").toString() : "");
										user.setUserLocation(
												map.get("usageLocation") != null ? map.get("usageLocation").toString()
														: "");
										user.setCity(map.get("city") != null ? map.get("city").toString() : "");
										user.setUser_id(null);
										break;
									}
								}
							}
						} else {
							for (CohortUsers user : topPerformers) {
								if (user.getEmail() != null && !user.getEmail().isEmpty()) {
									if (user.getEmail()
											.equals(map.get("onPremisesUserPrincipalName").toString().toLowerCase())) {
										user.setFirst_name(
												map.get("givenName") != null ? map.get("givenName").toString() : "");
										user.setLast_name(
												map.get("surname") != null ? map.get("surname").toString() : "");
										user.setPhone_No(
												map.get("mobilePhone") != null ? map.get("mobilePhone").toString()
														: "0");
										user.setDepartment(
												map.get("department") != null ? map.get("department").toString() : "");
										user.setDesignation(
												map.get("jobTitle") != null ? map.get("jobTitle").toString() : "");
										user.setUserLocation(
												map.get("usageLocation") != null ? map.get("usageLocation").toString()
														: "");
										user.setCity(map.get("city") != null ? map.get("city").toString() : "");
										user.setUser_id(null);
										break;
									}
								}
							}
						}
					}
				}
				// removing invalid employeeIds
				for (Iterator<CohortUsers> it = authors.iterator(); it.hasNext();) {
					CohortUsers user = it.next();
					if (user.getUser_id() != null)
						it.remove();
				}
				for (Iterator<CohortUsers> it = educators.iterator(); it.hasNext();) {
					CohortUsers user = it.next();
					if (user.getUser_id() != null)
						it.remove();
				}
				for (Iterator<CohortUsers> it = topPerformers.iterator(); it.hasNext();) {
					CohortUsers user = it.next();
					if (user.getUser_id() != null)
						it.remove();
				}
			}

		} catch (Exception e) {
			ProjectLogger.log("Exception occured while processing list of SMEs : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.invalidPropertyError.getErrorCode(),
					CassandraUtil.processExceptionForUnknownIdentifier(e), ResponseCode.CLIENT_ERROR.getResponseCode());
		}
		smeData.put("authors", authors);
		smeData.put("educators", educators);
		smeData.put("highPerfomers", topPerformers);
		response.put(Constants.RESPONSE, smeData);
		return response;
	}

	private Response getTopPerformers(String keyspaceName, String tableName, String propertyName,
			List<Object> propertyValueList) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service getTopPerformers method started at ==" + startTime, LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Selection selection = QueryBuilder.select();
			// columns to fetch
			selection.column("user_id");
			selection.column("result_percent");
			selection.column("ts_created");
			Select selectQuery = selection.from(keyspaceName, tableName);
			Where selectWhere = selectQuery.where();
			Clause clause = QueryBuilder.in(propertyName, propertyValueList);
			selectWhere.and(clause);
			/*
			 * this is old code for finding top performers List<Integer> cutOffPercentList =
			 * new ArrayList<Integer>(); // passing the cut off percent as list(of numbers
			 * starting from base cutoff till // 100) // so as to be able to query greated
			 * than equal to // on date_created // select * from
			 * bodhi.user_assessments_course_result where parent_source_id in(list) and
			 * result_percent in (list) and date_created>timestamp for (int i =
			 * cutOffPercent; i <= 100; i++) { cutOffPercentList.add(i); } Clause
			 * operationClause = QueryBuilder.in("result_percent", cutOffPercentList);
			 * selectWhere.and(operationClause);
			 */
			// take 90 days data for top performers
			long machineTimestamp = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli();
			Clause dateValid = QueryBuilder.gte("ts_created", machineTimestamp);
			// limiting the no of rows to 200
			selectWhere.and(dateValid).limit(200);
			// current query select * from assessment_top_performer where parent_source_id
			// in ('a','b') and ts_created>'2018-01-01'
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
		ProjectLogger.log("Cassandra Service getTopPerformers method end at ==" + stopTime + " ,Total time elapsed = "
				+ elapsedTime, LoggerEnum.PERF_LOG);
		return response;
	}

	// this method is to fetch all educators for a course
	private Response getEducators(String keyspaceName, String tableName, String propertyName,
			List<Object> propertyValueList) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service getEducators method started at ==" + startTime, LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Select selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);
			Where selectWhere = selectQuery.where();
			Clause clause = QueryBuilder.in(propertyName, propertyValueList);
			selectWhere.and(clause);
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
				"Cassandra Service getEducators method end at ==" + stopTime + " ,Total time elapsed = " + elapsedTime,
				LoggerEnum.PERF_LOG);
		return response;
	}
}
