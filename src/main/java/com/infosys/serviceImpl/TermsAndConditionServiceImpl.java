/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.datastax.driver.core.utils.UUIDs;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.cassandra.EmailToGroupModelPrimaryKeyModel;
import com.infosys.model.cassandra.UserAccessPathsModel;
import com.infosys.model.cassandra.UserAccessPathsPrimaryKeyModel;
import com.infosys.repository.EmailToGroupRepository;
import com.infosys.repository.UserAccessPathsRepository;
import com.infosys.service.TermsAndConditionService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.apache.commons.lang3.StringEscapeUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TermsAndConditionServiceImpl implements TermsAndConditionService {

	@Value("${appName}")
	private String appName;

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
	private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);
	private Util.DbInfo userTermsConditiondDb = Util.dbInfoMap.get(LexJsonKey.USER_TERMS_CONDITION_DB);
	private Util.DbInfo authorTermsConditiondDb = Util.dbInfoMap.get(LexJsonKey.AUTHOR_TERMS_CONDITION_DB);
	private Util.DbInfo termsConditiondDb = Util.dbInfoMap.get(LexJsonKey.TERMS_CONDITION_DB);
	private Util.DbInfo userBadgeDb = Util.dbInfoMap.get(LexJsonKey.UserBadges);
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	private String[] docNames = new String[] { "Europe DP", "Rest of world DP", "Generic T&C" };
	private String[] docNamesV2 = new String[] { "Generic T&C", "Data Privacy" };

	@Autowired
	UserAccessPathsRepository userAccessPathsRepo;
	@Autowired
	EmailToGroupRepository emailToGroupRepo;

	public TermsAndConditionServiceImpl() {
		Util.checkCassandraDbConnections(keyspace);
		Util.checkCassandraDbConnections(bodhiKeyspace);
	}

	@Override
	public boolean addUserTermsAcceptance(String userId, String userType, List<Map<String, Object>> termsAccepted)
			throws IOException {

		// Validating the userId
		Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
				userId);
		List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);
		if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
			ProjectLogger.log("Invalid userId " + userId);
			return false;
		}

		String emailId = (String) userDetails.get(0).get(JsonKey.EMAIL);
		System.out.println(userType);
		// Validating the userType
		if (!userType.equals("User") && !userType.equals("Author")) {
			ProjectLogger.log("Invalid userType " + userType);
			return false;
		}

		// Validating the terms
		if (termsAccepted.size() != 2) {
			return false;
		}
		// Validating user/author has chosen one Region Data Privacy and One Generic TnC
		boolean docFlag = false;
		boolean tncFlag = false;
		for (Map<String, Object> t : termsAccepted) {
			String docName = (String) t.get(LexJsonKey.DOC_NAME);
			docName = addDP(docName);
			double version = Double.parseDouble((String) t.get(JsonKey.VERSION));
			if (docName.equals(this.docNames[0]) || docName.equals(this.docNames[1])) {
				if (!docFlag)
					docFlag = true;
				else
					return false;
			}
			if (docName.equals(this.docNames[2])) {
				tncFlag = true;
			}
			// Checking document and versions exists in main DB
			Map<String, Object> request = new HashMap<>();
			request.put(LexJsonKey.DOC_NAME, docName);
			request.put(LexJsonKey.DOC_FOR, userType);
			request.put(JsonKey.VERSION, version);
			Response resp = cassandraOperation.getRecordsByProperties(termsConditiondDb.getKeySpace(),
					termsConditiondDb.getTableName(), request);
			List<Map<String, Object>> records = (List<Map<String, Object>>) resp.get(Constants.RESPONSE);
			if (records.size() == 0 || records.get(0) == null || records.get(0).isEmpty()) {
				ProjectLogger.log("Terms document not found in database");
				return false;
			}
		}
		if (!docFlag || !tncFlag) {
			ProjectLogger.log("Improper terms name");
			return false;
		}

		Timestamp dateAccepted = new Timestamp(System.currentTimeMillis());

		int insertCount = 0;

		// Inserting accepted terms in database
		for (Map<String, Object> terms : termsAccepted) {
			Map<String, Object> request = new HashMap<>();
			String docName = (String) terms.get(LexJsonKey.DOC_NAME);
			docName = addDP(docName);
			request.put(JsonKey.USER_ID, userId);
			request.put(LexJsonKey.DOC_NAME, docName);
			request.put(JsonKey.VERSION, Double.parseDouble((String) terms.get(JsonKey.VERSION)));
			request.put(LexJsonKey.DATE_ACCEPTED, dateAccepted);

			// Email added later on 9th August, 2018 by Krishnendu_C
			request.put(JsonKey.EMAIL, emailId);

			String keyspace;
			String table;
			if (userType.equals("User")) {
				keyspace = userTermsConditiondDb.getKeySpace();
				table = userTermsConditiondDb.getTableName();
			} else {
				keyspace = authorTermsConditiondDb.getKeySpace();
				table = authorTermsConditiondDb.getTableName();
			}

			Response resp = cassandraOperation.upsertRecord(keyspace, table, request);
			if (resp.getResponseCode().equals(ResponseCode.OK)) {
				insertCount++;
			}
		}

		if (userType.equals("User")) {
			UserAccessPathsModel defaultAccessPaths = new UserAccessPathsModel();
			UserAccessPathsPrimaryKeyModel defaultAccessPathsPrimaryKey = new UserAccessPathsPrimaryKeyModel();
			String rootOrg = System.getenv(LexConstants.ROOT_ORG);
			String org = System.getenv(LexConstants.ORG);
			defaultAccessPathsPrimaryKey.setRootOrg(rootOrg);
			defaultAccessPathsPrimaryKey.setOrg(org);
			defaultAccessPathsPrimaryKey.setUserId(UUID.fromString(userId));
			defaultAccessPathsPrimaryKey.setCasId(UUIDs.timeBased());

			defaultAccessPaths.setPrimaryKey(defaultAccessPathsPrimaryKey);
			Set<String> accessPaths = new HashSet<>(Arrays.asList(rootOrg, rootOrg + "/" + org));
			defaultAccessPaths.setAccessPaths(accessPaths);
			defaultAccessPaths.setTemporary(false);
			defaultAccessPaths.setTtl(0);
			try {
				userAccessPathsRepo.save(defaultAccessPaths);
			} catch (Exception e) {
				return false;
			}
		}

		// checking whether user has been mapped to any access groups before accepting
		// the tnc
		List<EmailToGroupModelPrimaryKeyModel> emailToGroups = emailToGroupRepo.findByEmail(emailId);
		if (!emailToGroups.isEmpty()) {
			List<String> groupIds = emailToGroups.stream().map(emailToGroup -> emailToGroup.getGroupIdentifier())
					.collect(Collectors.toList());
			System.out.println(groupIds.toString());

			RestHighLevelClient client = ConnectionManager.getClient();
			BulkRequest request = new BulkRequest();
			groupIds.forEach(item -> {
				UpdateRequest updateObj = new UpdateRequest();
				updateObj.index(LexProjectUtil.EsIndex.access_control_groups.getIndexName());
				updateObj.type(LexProjectUtil.EsType.access_control_group.getTypeName());
				updateObj.id(item);
				Map<String, Object> params = new HashMap<>();
				params.put("x", userId);
				updateObj.script(
						new Script(ScriptType.INLINE, "painless", "ctx._source['userIds'].add(params.x)", params));
				request.add(updateObj);
			});
			BulkResponse resObj = client.bulk(request, RequestOptions.DEFAULT);

			if (!resObj.hasFailures()) {
				emailToGroupRepo.deleteAll(emailToGroups);
			}
		}

		// Allocating fledgling badge if the user does not have it
		if (insertCount == 2) {
			if (userType.equals("User")) {
				// Checking and inserting new user badge
				Map<String, Object> userBadge = new HashMap<>();
				userBadge.put("email_id", emailId);
				userBadge.put("badge_id", "NewUser");
				Response resp = cassandraOperation.getRecordsByProperties(userBadgeDb.getKeySpace(),
						userBadgeDb.getTableName(), userBadge);
				// If not found then insert the badge
				List<Map<String, Object>> badgeDetails = (List<Map<String, Object>>) resp.get(Constants.RESPONSE);
				if (badgeDetails.size() == 0 || badgeDetails.get(0) == null || badgeDetails.get(0).isEmpty()) {
					userBadge.put("badge_type", "O");
					userBadge.put("first_received_date", dateAccepted);
					userBadge.put("last_received_date", dateAccepted);
					userBadge.put("progress", 100.0f);// float
					userBadge.put("progress_date", dateAccepted);
					userBadge.put("received_count", 1);
					resp = cassandraOperation.insertRecord(userBadgeDb.getKeySpace(), userBadgeDb.getTableName(),
							userBadge);
				}
				if (resp.getResponseCode().equals(ResponseCode.OK)) {
					return true;
				} else {
					ProjectLogger.log("Badge insertion failed");
					return false;
				}
			} else {
				return true;
			}
		} else {
			ProjectLogger.log("Terms and Condition acceptance failed");
			return false;
		}
	}

	@Override
	public Map<String, Object> addUserTermsAcceptanceV2(String userId, String userType, List<Map<String, Object>> termsAccepted)
			throws IOException {

		Map<String, Object> returnVal = new HashMap<>();

		// Validating the userId
		Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
				userId);
		List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);
		if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
			ProjectLogger.log("Invalid userId " + userId);
			returnVal.put(Constants.RESPONSE, "FAILED");
			returnVal.put("errorMessage", "Invalid userId");
			return returnVal;
		}

		String emailId = (String) userDetails.get(0).get(JsonKey.EMAIL);
		// System.out.println(userType);
		// Validating the userType
		if (!userType.equals("User") && !userType.equals("Author")) {
			ProjectLogger.log("Invalid userType " + userType);
			returnVal.put(Constants.RESPONSE, "FAILED");
			returnVal.put("errorMessage", "Invalid userType " + userType);
			return returnVal;
		}

		// Validating the terms
		if (termsAccepted.size() < 1 || termsAccepted.size() > 2) {
			returnVal.put(Constants.RESPONSE, "FAILED");
			returnVal.put("errorMessage", "User should accept one or two terms");
			return returnVal;
		}

		// Validating the terms with the master table
//		boolean tncFlag = false;
//		boolean dpFlag = false;
		for (Map<String, Object> t : termsAccepted) {
			String docName = (String) t.get(LexJsonKey.DOC_NAME);
			double version = Double.parseDouble((String) t.get(JsonKey.VERSION));
			if (docName.equals(this.docNamesV2[0]) || docName.equals(this.docNamesV2[1])) {
//				if (docName.equals(this.docNamesV2[0])) {
//					tncFlag = true;
//				} else {
//					dpFlag = true;
//				}
				// Checking document and versions exists in main DB
				Map<String, Object> request = new HashMap<>();
				request.put(LexJsonKey.DOC_NAME, docName);
				request.put(LexJsonKey.DOC_FOR, userType);
				request.put(JsonKey.VERSION, version);
				Response resp = cassandraOperation.getRecordsByProperties(termsConditiondDb.getKeySpace(),
						termsConditiondDb.getTableName(), request);
				List<Map<String, Object>> records = (List<Map<String, Object>>) resp.get(Constants.RESPONSE);
				if (records.size() == 0 || records.get(0) == null || records.get(0).isEmpty()) {
					ProjectLogger.log("Terms document not found in database");
					returnVal.put(Constants.RESPONSE, "FAILED");
					returnVal.put("errorMessage", "Terms with specified version not found");
					return returnVal;
				}
			} else {
				ProjectLogger.log("Invalid terms name");
				returnVal.put(Constants.RESPONSE, "FAILED");
				returnVal.put("errorMessage", "Invalid Terms Name");
				return returnVal;
			}
//			if (tncFlag && dpFlag) {
//				break;
//			}
		}
//		if (!tncFlag || !dpFlag) {
//			ProjectLogger.log("Improper terms name");
//			return false;
//		}

		Timestamp dateAccepted = new Timestamp(System.currentTimeMillis());

		int insertCount = 0;

		// Inserting accepted terms in database
		for (Map<String, Object> terms : termsAccepted) {
			Map<String, Object> request = new HashMap<>();
			String docName = (String) terms.get(LexJsonKey.DOC_NAME);
			docName = addDP(docName);
			request.put(JsonKey.USER_ID, userId);
			request.put(LexJsonKey.DOC_NAME, docName);
			request.put(JsonKey.VERSION, Double.parseDouble((String) terms.get(JsonKey.VERSION)));
			request.put(LexJsonKey.DATE_ACCEPTED, dateAccepted);

			// Email added later on 9th August, 2018 by Krishnendu_C
			request.put(JsonKey.EMAIL, emailId);

			String keyspace;
			String table;
			if (userType.equals("User")) {
				keyspace = userTermsConditiondDb.getKeySpace();
				table = userTermsConditiondDb.getTableName();
			} else {
				keyspace = authorTermsConditiondDb.getKeySpace();
				table = authorTermsConditiondDb.getTableName();
			}

			Response resp = cassandraOperation.upsertRecord(keyspace, table, request);
			if (resp.getResponseCode().equals(ResponseCode.OK)) {
				insertCount++;
			}
		}

		// adding default access paths for user.

		//appName check to skip fledling badge and access paths code to run in infy tq
		if (!appName.equals("Infy-TQ")) {
			if (userType.equals("User")) {
				if (userAccessPathsRepo.findByPrimaryKeyUserId(UUID.fromString(userId)).isEmpty()) {
					UserAccessPathsModel defaultAccessPaths = new UserAccessPathsModel();
					UserAccessPathsPrimaryKeyModel defaultAccessPathsPrimaryKey = new UserAccessPathsPrimaryKeyModel();
					String rootOrg = System.getenv(LexConstants.ROOT_ORG);
					String org = System.getenv(LexConstants.ORG);
					defaultAccessPathsPrimaryKey.setRootOrg(rootOrg);
					defaultAccessPathsPrimaryKey.setOrg(org);
					defaultAccessPathsPrimaryKey.setUserId(UUID.fromString(userId));
					defaultAccessPathsPrimaryKey.setCasId(UUIDs.timeBased());

					defaultAccessPaths.setPrimaryKey(defaultAccessPathsPrimaryKey);
					Set<String> accessPaths = new HashSet<>(Arrays.asList(rootOrg, rootOrg + "/" + org));
					defaultAccessPaths.setAccessPaths(accessPaths);
					defaultAccessPaths.setTemporary(false);
					defaultAccessPaths.setTtl(0);
					try {
						userAccessPathsRepo.save(defaultAccessPaths);
					} catch (Exception e) {
						returnVal.put(Constants.RESPONSE, "FAILED");
						returnVal.put("errorMessage", "Access Path Error");
						return returnVal;
					}
				}
			}

			// checking whether user has been mapped to any access groups before accepting
			// the tnc
			List<EmailToGroupModelPrimaryKeyModel> emailToGroups = emailToGroupRepo.findByEmail(emailId);
			if (!emailToGroups.isEmpty()) {
				List<String> groupIds = emailToGroups.stream().map(emailToGroup -> emailToGroup.getGroupIdentifier())
						.collect(Collectors.toList());

				RestHighLevelClient client = ConnectionManager.getClient();
				BulkRequest request = new BulkRequest();
				groupIds.forEach(item -> {
					UpdateRequest updateObj = new UpdateRequest();
					updateObj.index(LexProjectUtil.EsIndex.access_control_groups.getIndexName());
					updateObj.type(LexProjectUtil.EsType.access_control_group.getTypeName());
					updateObj.id(item);
					Map<String, Object> params = new HashMap<>();
					params.put("x", userId);
					updateObj.script(
							new Script(ScriptType.INLINE, "painless", "ctx._source['userIds'].add(params.x)", params));
					request.add(updateObj);
				});
				BulkResponse resObj = client.bulk(request, RequestOptions.DEFAULT);

				if (!resObj.hasFailures()) {
					emailToGroupRepo.deleteAll(emailToGroups);
				}
			}

			// Allocating fledgling badge if the user does not have it if (insertCount == 2)
			// {
			if (userType.equals("User")) {
				// Checking and inserting new user badge
				Map<String, Object> userBadge = new HashMap<>();
				userBadge.put("email_id", emailId);
				userBadge.put("badge_id", "NewUser");
				Response resp = cassandraOperation.getRecordsByProperties(userBadgeDb.getKeySpace(),
						userBadgeDb.getTableName(), userBadge);
				// If not found then insert the badge
				List<Map<String, Object>> badgeDetails = (List<Map<String, Object>>) resp.get(Constants.RESPONSE);
				if (badgeDetails.size() == 0 || badgeDetails.get(0) == null || badgeDetails.get(0).isEmpty()) {
					userBadge.put("badge_type", "O");
					userBadge.put("first_received_date", dateAccepted);
					userBadge.put("last_received_date", dateAccepted);
					userBadge.put("progress", 100.0f);// float
					userBadge.put("progress_date", dateAccepted);
					userBadge.put("received_count", 1);
					resp = cassandraOperation.insertRecord(userBadgeDb.getKeySpace(), userBadgeDb.getTableName(),
							userBadge);
				}
				if (!resp.getResponseCode().equals(ResponseCode.OK)) {
					ProjectLogger.log("Badge insertion failed");
					returnVal.put(Constants.RESPONSE, "FAILED");
					returnVal.put("errorMessage", "Badge insertion failed");
					return returnVal;
				}
			}
		}

		if (insertCount == termsAccepted.size()) {
			returnVal.put(Constants.RESPONSE, "SUCCESS");
			returnVal.put("errorMessage", "");
			return returnVal;
		} else {
			returnVal.put(Constants.RESPONSE, "FAILED");
			returnVal.put("errorMessage", "Terms insert failed");
			return returnVal;
		}
	}

	@Override
	public Map<String, Object> checkVersionChange(String userId, String userType) {

		// Validating the userId if present
		if (!userId.equals("default")) {
			Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
					userId);
			List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);
			if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
				ProjectLogger.log("Invalid userId " + userId);
				return null;
			}
		}

		// Validating the user type
		if (!userType.equals("User") && !userType.equals("Author")) {
			ProjectLogger.log("Invalid userType " + userType);
			return null;
		}

		// Get the latest t&c for all the docs based on user type
		Map<String, Object> latest = new HashMap<>();
		for (String docName : docNames) {
			Map<String, Object> properties = new HashMap<>();
			properties.put("docName", docName);
			properties.put("docFor", userType);

			Response cassandraResponse = cassandraOperation.getRecordsByProperties(termsConditiondDb.getKeySpace(),
					termsConditiondDb.getTableName(), properties);

			List<Map<String, Object>> termsAndCondition = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);

			// If terms and conditions are not found in DB
			if (termsAndCondition == null || termsAndCondition.isEmpty()) {
				ProjectLogger
						.log("Terms and Conditions of " + docName + "for " + userType + "is not present in database");
				return null;
			}

			Map<String, Object> docs = termsAndCondition.get(0);

			// Storing the latest document
			String[] data = new String[4];
			// Stores the latest version of the document
			data[0] = Double.toString((double) docs.get(JsonKey.VERSION.toLowerCase()));
			// Stores the latest content
			data[1] = StringEscapeUtils.unescapeHtml4((String) docs.get(LexJsonKey.DOC_TEXT.toLowerCase()));
			// Will store the accepted version of the document if available
			data[2] = "";
			// Will store the date when accepted by the user if available
			data[3] = "";
			latest.put(docName, data);
		}

		ProjectLogger.log("Terms and Conditions are fetched from database");

		// Return values
		Map<String, Object> ret = new HashMap<>();
		boolean changeFlag = false;

		// Used to store the region user does not choose
		String otherDoc = "";

		if (userId.equals("default")) {
			// for first time user all the latest doc is sent
			changeFlag = true;
			ret = latest;
		} else {
			// Get the user acceptance based on user Type
			String keyspace;
			String table;
			if (userType.equals("User")) {
				keyspace = userTermsConditiondDb.getKeySpace();
				table = userTermsConditiondDb.getTableName();
			} else {
				keyspace = authorTermsConditiondDb.getKeySpace();
				table = authorTermsConditiondDb.getTableName();
			}

			Response cassandraResponse = cassandraOperation.getRecordsByProperty(keyspace, table, JsonKey.USER_ID,
					userId);
			List<Map<String, Object>> userAcceptance = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);

			// If user has not accepted any terms and conditions, return the latest versions
			// of all
			// the three documents
			if (userAcceptance == null || userAcceptance.isEmpty()) {
				changeFlag = true;
				ret = latest;
			} else {
				// Sorting it based on the date accepted descending and Version descending
				Collections.sort(userAcceptance, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> m1, Map<String, Object> m2) {
						Double d1 = (double) m1.get(JsonKey.VERSION);
						Double d2 = (double) m2.get(JsonKey.VERSION);
						Timestamp t1 = new Timestamp(((Date) m1.get(LexJsonKey.DATE_ACCEPTED)).getTime());
						Timestamp t2 = new Timestamp(((Date) m2.get(LexJsonKey.DATE_ACCEPTED)).getTime());
						int ret = t2.compareTo(t1);
						if (ret != 0) {
							return ret;
						} else {
							return d2.compareTo(d1);
						}
					}
				});

				boolean docFlag = false;
				boolean compare = false;
				boolean tncFlag = false;

				for (Map<String, Object> docs : userAcceptance) {
					String docName = (String) docs.get(LexJsonKey.DOC_NAME.toLowerCase());
					// Getting which region DP user has chosen recently
					if (docName.equals(this.docNames[0])) {
						otherDoc = this.docNames[1];
					} else if (docName.equals(this.docNames[1])) {
						otherDoc = this.docNames[0];
					} else if (!tncFlag && docName.equals(this.docNames[2])) {
						tncFlag = true;
						compare = true;
					}
					// Storing the latest document of the region user has not selected
					// As it will be displayed as it is
					if (!docFlag && !otherDoc.isEmpty()) {
						docFlag = true;
						String[] otherData = (String[]) latest.get(otherDoc);
						ret.put(otherDoc, otherData);
						compare = true;
					}
					// Comparing the user accepted version and latest version, if user has not
					// accepted
					// latest version changeFlag will become true
					if (compare) {
						String[] currentData = (String[]) latest.get(docName);
						double acceptedVersion = (double) docs.get(JsonKey.VERSION.toLowerCase());
						Date acceptedDate = (Date) docs.get(LexJsonKey.DATE_ACCEPTED.toLowerCase());
						double latestVersion = Double.parseDouble(currentData[0]);
						if (latestVersion > acceptedVersion) {
							changeFlag = true;
						}
						currentData[2] = Double.toString(acceptedVersion);
						currentData[3] = ProjectUtil.getDateFormatter().format(acceptedDate);
						ret.put(docName, currentData);
						compare = false;
					}
					// Once both tnc version and dp version is checked it breaks the loop
					if (tncFlag && docFlag) {
						break;
					}
				}
			}

		}
		Map<String, Object> formattedResponse = new HashMap<>();
		List<Map<String, Object>> terms = new ArrayList<>();

		// Preparing the terms to be sent to user
		for (Map.Entry<String, Object> m : ret.entrySet()) {
			Map<String, Object> term = new HashMap<>();
			String name = m.getKey();
			boolean accepted = true;
			if (otherDoc.equals("")) {
				// Default user so accepted is false
				accepted = false;
			} else {
				// as user has not chosen the otherDoc when it match with the current doc in the
				// loop
				// the is accepted becomes false for that data privacy doc
				// so it will be true for the accepted DP and generic TnC and false for other DP
				accepted = !otherDoc.equals(name);
			}
			name = removeDP(name);
			term.put(JsonKey.NAME, name);
			String[] data = (String[]) m.getValue();
			term.put(LexJsonKey.IS_ACCEPTED, accepted);
			term.put(JsonKey.VERSION, data[0]);
			term.put(LexJsonKey.ACCEPTED_VERSION, data[2]);
			term.put(LexJsonKey.ACCEPTED_DATE, data[3]);
			term.put(JsonKey.VERSION, data[0]);
			term.put(JsonKey.CONTENT, data[1]);
			terms.add(term);
		}

		formattedResponse.put(LexJsonKey.TERMS_AND_CONDTIONS, terms);
		// Change flag decides whether there is a version change,
		// If change is there user needs to accept so is accepted becomes false
		formattedResponse.put(LexJsonKey.IS_ACCEPTED, !changeFlag);

		return formattedResponse;
	}

	@Override
	public Map<String, Object> checkVersionChangeV2(String userId, String userType) {

		// Validating the userId if present
		if (!userId.equals("default")) {
			Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
					userId);
			List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);
			if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
				ProjectLogger.log("Invalid userId " + userId);
				return null;
			}
		}

		// Validating the user type
		if (!userType.equals("User") && !userType.equals("Author")) {
			ProjectLogger.log("Invalid userType " + userType);
			return null;
		}

		// Get the latest t&c for Generic T&C
		Map<String, Object> latest = new HashMap<>();
		for (String docName : this.docNamesV2) {
			latest.put(docName, getLatest(docName, userType));
		}
		ProjectLogger.log("Terms and Conditions are fetched from database");

		// Return values
		Map<String, Object> ret = new HashMap<>();

		if (userId.equals("default")) {
			// for first time user all the latest doc is sent
			ret = latest;
		} else {
			// Get the user acceptance based on user Type
			String keyspace;
			String table;
			if (userType.equals("User")) {
				keyspace = userTermsConditiondDb.getKeySpace();
				table = userTermsConditiondDb.getTableName();
			} else {
				keyspace = authorTermsConditiondDb.getKeySpace();
				table = authorTermsConditiondDb.getTableName();
			}

			Response cassandraResponse = cassandraOperation.getRecordsByProperty(keyspace, table, JsonKey.USER_ID,
					userId);
			List<Map<String, Object>> userAcceptance = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);

			// If user has not accepted any terms and conditions, return the latest versions
			// of all the documents
			if (userAcceptance == null || userAcceptance.isEmpty()) {
				ret = latest;
			} else {
				// Sorting it based on the date accepted descending and Version descending
				Collections.sort(userAcceptance, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> m1, Map<String, Object> m2) {
						Double d1 = (double) m1.get(JsonKey.VERSION);
						Double d2 = (double) m2.get(JsonKey.VERSION);
						Timestamp t1 = new Timestamp(((Date) m1.get(LexJsonKey.DATE_ACCEPTED)).getTime());
						Timestamp t2 = new Timestamp(((Date) m2.get(LexJsonKey.DATE_ACCEPTED)).getTime());
						int ret = t2.compareTo(t1);
						if (ret != 0) {
							return ret;
						} else {
							return d2.compareTo(d1);
						}
					}
				});

				boolean dpFlag = false;
				boolean tncFlag = false;

				for (Map<String, Object> docs : userAcceptance) {
					String docName = (String) docs.get(LexJsonKey.DOC_NAME.toLowerCase());
					// Checking what version user has chosen
//					if ((!tncFlag && docName.equals(this.docNamesV2[0]))
//							|| (!dpFlag && docName.equals(this.docNamesV2[1]))) {
					/**
					 * Instead of allowing only Data Privacy as above Checking Europe and Rest Of
					 * The World as well
					 */
					List<String> dpDocs = Arrays.asList(this.docNamesV2[1], this.docNames[0], this.docNames[1]);
					if ((!tncFlag && docName.equals(this.docNamesV2[0])) || (!dpFlag && dpDocs.contains(docName))) {
						if (!dpDocs.contains(docName)) {
							tncFlag = true;
						} else {
							dpFlag = true;
							docName = this.docNamesV2[1];
						}
						// Comparing the user accepted version and latest version, if user has not
						// accepted latest version changeFlag will become true
						String[] currentData = (String[]) latest.get(docName);
						if (currentData != null) {
							double acceptedVersion = (double) docs.get(JsonKey.VERSION.toLowerCase());
							Date acceptedDate = (Date) docs.get(LexJsonKey.DATE_ACCEPTED.toLowerCase());
							currentData[2] = Double.toString(acceptedVersion);
							currentData[3] = ProjectUtil.getDateFormatter().format(acceptedDate);
							ret.put(docName, currentData);
						}
					}
					// Once tnc and dp version is checked it breaks the loop
					if (tncFlag && dpFlag) {
						break;
					}
				}
				// If user has not accepted any TnC, add it
				if (!tncFlag) {
					String docName = this.docNamesV2[0];
					ret.put(docName, latest.get(docName));
				}
				// If user has not accepted any DP, add it
				if (!dpFlag) {
					String docName = this.docNamesV2[1];
					ret.put(docName, latest.get(docName));
				}
			}
		}

		Map<String, Object> formattedResponse = new HashMap<>();
		List<Map<String, Object>> terms = new ArrayList<>();

		// Preparing the terms to be sent to user
		boolean isAccepted = true;
		for (Map.Entry<String, Object> m : ret.entrySet()) {
			Map<String, Object> term = new HashMap<>();
			String name = m.getKey();
			term.put(JsonKey.NAME, name);
			String[] tncData = (String[]) m.getValue();
			boolean accepted = !tncData[2].equals("")
					&& Double.parseDouble(tncData[0]) <= Double.parseDouble(tncData[2]);
			isAccepted = isAccepted && accepted;
			term.put(LexJsonKey.IS_ACCEPTED, accepted);
			term.put(JsonKey.VERSION, tncData[0]);
			term.put(LexJsonKey.ACCEPTED_VERSION, tncData[2]);
			term.put(LexJsonKey.ACCEPTED_DATE, tncData[3]);
			term.put(JsonKey.CONTENT, tncData[1]);
			terms.add(term);
		}

		formattedResponse.put(LexJsonKey.TERMS_AND_CONDTIONS, terms);
		// Change flag decides whether there is a version change,
		// If change is there user needs to accept so is accepted becomes false
		formattedResponse.put(LexJsonKey.IS_ACCEPTED, isAccepted);

		return formattedResponse;
	}

	private String addDP(String docName) {
		if (docName.equals("Europe")) {
			docName = "Europe DP";
		} else if (docName.equals("Rest of world")) {
			docName = "Rest of world DP";
		}
		return docName;
	}

	private String removeDP(String docName) {
		if (docName.equals("Europe DP")) {
			docName = "Europe";
		} else if (docName.equals("Rest of world DP")) {
			docName = "Rest of world";
		}
		return docName;
	}

	private String[] getLatest(String docName, String userType) {
		Map<String, Object> properties = new HashMap<>();
		properties.put("docName", docName);
		properties.put("docFor", userType);

		Response cassandraResponse = cassandraOperation.getRecordsByProperties(termsConditiondDb.getKeySpace(),
				termsConditiondDb.getTableName(), properties);

		List<Map<String, Object>> termsAndCondition = (List<Map<String, Object>>) cassandraResponse
				.get(Constants.RESPONSE);

		// If terms and conditions are not found in DB
		if (termsAndCondition == null || termsAndCondition.isEmpty()) {
			return null;
		} else {
			Map<String, Object> docs = termsAndCondition.get(0);

			// Storing the latest document
			String[] data = new String[4];
			// Stores the latest version of the document
			data[0] = Double.toString((double) docs.get(JsonKey.VERSION.toLowerCase()));
			// Stores the latest content
			data[1] = StringEscapeUtils.unescapeHtml4((String) docs.get(LexJsonKey.DOC_TEXT.toLowerCase()));
			// Will store the accepted version of the document if available
			data[2] = "";
			// Will store the date when accepted by the user if available
			data[3] = "";
			return data;
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void userPostProcessing(String rootOrg, String org, String userId, String userType) {

		// Validating the userId
	/*	Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
				userId);
		List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);
		if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
			ProjectLogger.log("Invalid userId " + userId);
			throw new ResourceNotFoundException("User id not found");
		}

		Timestamp dateAccepted = new Timestamp(System.currentTimeMillis());
		String emailId = (String) userDetails.get(0).get(JsonKey.EMAIL);
*/
		if (userType.equals("User")) {
			if (userAccessPathsRepo.findByPrimaryKeyUserId(UUID.fromString(userId)).isEmpty()) {
				UserAccessPathsModel defaultAccessPaths = new UserAccessPathsModel();
				UserAccessPathsPrimaryKeyModel defaultAccessPathsPrimaryKey = new UserAccessPathsPrimaryKeyModel();
				
				defaultAccessPathsPrimaryKey.setRootOrg(rootOrg);
				defaultAccessPathsPrimaryKey.setOrg(org);
				defaultAccessPathsPrimaryKey.setUserId(UUID.fromString(userId));
				defaultAccessPathsPrimaryKey.setCasId(UUIDs.timeBased());

				defaultAccessPaths.setPrimaryKey(defaultAccessPathsPrimaryKey);
				Set<String> accessPaths = new HashSet<>(Arrays.asList(rootOrg, rootOrg + "/" + org));
				defaultAccessPaths.setAccessPaths(accessPaths);
				defaultAccessPaths.setTemporary(false);
				defaultAccessPaths.setTtl(0);
				userAccessPathsRepo.save(defaultAccessPaths);

			}
		}

		// checking whether user has been mapped to any access groups before accepting
		// the tnc
		/*List<EmailToGroupModelPrimaryKeyModel> emailToGroups = emailToGroupRepo.findByEmail(emailId);
		if (!emailToGroups.isEmpty()) {
			List<String> groupIds = emailToGroups.stream().map(emailToGroup -> emailToGroup.getGroupIdentifier())
					.collect(Collectors.toList());

			RestHighLevelClient client = ConnectionManager.getClient();
			BulkRequest request = new BulkRequest();
			groupIds.forEach(item -> {
				UpdateRequest updateObj = new UpdateRequest();
				updateObj.index(LexProjectUtil.EsIndex.access_control_groups.getIndexName());
				updateObj.type(LexProjectUtil.EsType.access_control_group.getTypeName());
				updateObj.id(item);
				Map<String, Object> params = new HashMap<>();
				params.put("x", userId);
				updateObj.script(
						new Script(ScriptType.INLINE, "painless", "ctx._source['userIds'].add(params.x)", params));
				request.add(updateObj);
			});
			BulkResponse resObj = null;
			try {
				resObj = client.bulk(request, RequestOptions.DEFAULT);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw new ApplicationLogicError("Access path mapping fetch failed");
			}

			if (!resObj.hasFailures()) {
				emailToGroupRepo.deleteAll(emailToGroups);
			}
		}

		// Allocating fledgling badge if the user does not have it if (insertCount == 2)
		// {
		if (userType.equals("User")) {
			// Checking and inserting new user badge
			Map<String, Object> userBadge = new HashMap<>();
			userBadge.put("email_id", emailId);
			userBadge.put("badge_id", "NewUser");
			Response resp = cassandraOperation.getRecordsByProperties(userBadgeDb.getKeySpace(),
					userBadgeDb.getTableName(), userBadge);
			// If not found then insert the badge
			List<Map<String, Object>> badgeDetails = (List<Map<String, Object>>) resp.get(Constants.RESPONSE);
			if (badgeDetails.size() == 0 || badgeDetails.get(0) == null || badgeDetails.get(0).isEmpty()) {
				userBadge.put("badge_type", "O");
				userBadge.put("first_received_date", dateAccepted);
				userBadge.put("last_received_date", dateAccepted);
				userBadge.put("progress", 100.0f);// float
				userBadge.put("progress_date", dateAccepted);
				userBadge.put("received_count", 1);
				resp = cassandraOperation.insertRecord(userBadgeDb.getKeySpace(), userBadgeDb.getTableName(),
						userBadge);
			}
			if (!resp.getResponseCode().equals(ResponseCode.OK)) {
				ProjectLogger.log("Badge insertion failed");
				throw new ApplicationLogicError("Badge insertion failed");
			}
		}
		*/
	}

	
	
}
