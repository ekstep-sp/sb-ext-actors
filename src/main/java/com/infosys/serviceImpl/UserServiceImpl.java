/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.elastic.dto.SearchDTO;
import com.infosys.exception.BadRequestException;
import com.infosys.helper.ServiceFactory;
import com.infosys.helper.UserUtility;
import com.infosys.model.LexUser;
import com.infosys.repository.UserRepository;
import com.infosys.service.UserService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	UserRepository userRepository;

	boolean isSSOEnabled = Boolean.parseBoolean(PropertiesCache.getInstance().getProperty(JsonKey.IS_SSO_ENABLED));
	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
	private EncryptionService encryptionService = org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
			.getEncryptionServiceInstance(null);
	private DecryptionService decryptionService = org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
			.getDecryptionServiceInstance(null);
	private SSOManager ssoManager = SSOServiceFactory.getInstance();

	public UserServiceImpl() {
		Util.checkCassandraDbConnections(keyspace);
	}

	@Override
	public Map<String, Object> findUUID(String email) throws Exception {
		Map<String, Object> responseMap = new HashMap<>();
		try {
			Map<String, Object> requestMap = new HashMap<>();
			requestMap.put("email", email);
			Response response = cassandraOperation.getRecordById(keyspace, "user", requestMap);
			List<Map<String, Object>> resultMaps = (List<Map<String, Object>>) response.getResult().get("response");
			if (resultMaps.size() == 0) {
				throw new BadRequestException("No user with this email exists");
			}
			Map<String, Object> resultMap = resultMaps.get(0);
			responseMap.put("email", email);
			responseMap.put("userId", resultMap.get("id"));
		} catch (BadRequestException badRequestException) {
			ProjectLogger.log(badRequestException.getMessage());
			throw badRequestException;
		} catch (Exception exception) {
			ProjectLogger.log(exception.getMessage());
			exception.printStackTrace();
			throw new Exception("Something went wrong");
		}
		return responseMap;
	}

	@Override
	public Response createUser(Map<String, Object> userData) throws IOException {
		ProjectLogger.log("create user method started..");
		Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
		Util.DbInfo addrDbInfo = Util.dbInfoMap.get(JsonKey.ADDRESS_DB);
		Util.DbInfo eduDbInfo = Util.dbInfoMap.get(JsonKey.EDUCATION_DB);
		Util.DbInfo jobProDbInfo = Util.dbInfoMap.get(JsonKey.JOB_PROFILE_DB);
		Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
		Util.DbInfo orgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
		Util.DbInfo usrExtIdDb = Util.dbInfoMap.get(JsonKey.USR_EXT_ID_DB);
		ProjectLogger.log("collected all the DB setup..");

		// Fecthing the entire request including REQUESTED BY and the actual JSON
		// request
		Map<String, Object> req = userData;
		Map<String, Object> requestMap = null;
		// Extracting the actual JSON request
		Map<String, Object> userMap = (Map<String, Object>) req.get(JsonKey.USER);

		// Can be used to send on boarding mail
		Map<String, Object> emailTemplateMap = new HashMap<>(userMap);

		userMap.put(JsonKey.CREATED_BY, req.get(JsonKey.REQUESTED_BY));
		userMap.put(JsonKey.LOGIN_ID, userMap.get(JsonKey.EMAIL));
		userMap.put(JsonKey.USERNAME, userMap.get(JsonKey.EMAIL));

		emailTemplateMap.put(JsonKey.USERNAME, userMap.get(JsonKey.LOGIN_ID));

		// Validating loginId/emailId uniqueness
		if (null != userMap.get(JsonKey.LOGIN_ID)) {
			String loginId = (String) userMap.get(JsonKey.LOGIN_ID);

			Response resultFrUserName = cassandraOperation.getRecordsByProperty(usrDbInfo.getKeySpace(),
					usrDbInfo.getTableName(), JsonKey.LOGIN_ID, loginId);
			if (!(((List<Map<String, Object>>) resultFrUserName.get(JsonKey.RESPONSE)).isEmpty())) {
				ProjectCommonException exception = new ProjectCommonException(
						ResponseCode.userNameAlreadyExistError.getErrorCode(),
						ResponseCode.userNameAlreadyExistError.getErrorMessage(),
						ResponseCode.CLIENT_ERROR.getResponseCode());
				throw exception;
			}
		}
		// validate root org and reg org
		userMap.put(JsonKey.ROOT_ORG_ID, JsonKey.DEFAULT_ROOT_ORG_ID);
		if (!ProjectUtil.isStringNullOREmpty((String) userMap.get(JsonKey.REGISTERED_ORG_ID))) {
			Response orgResponse = null;
			try {
				orgResponse = cassandraOperation.getRecordById(orgDb.getKeySpace(), orgDb.getTableName(),
						(String) userMap.get(JsonKey.REGISTERED_ORG_ID));
			} catch (Exception e) {
				ProjectLogger.log("Exception occured while verifying regOrgId during create user : ", e);
				throw new ProjectCommonException(ResponseCode.invalidOrgId.getErrorCode(),
						ResponseCode.invalidOrgId.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
			}
			List<Map<String, Object>> responseList = (List<Map<String, Object>>) orgResponse.get(JsonKey.RESPONSE);
			String rootOrgId = "";
			if (null != responseList && !(responseList.isEmpty())) {
				String orgId = (String) responseList.get(0).get(JsonKey.ID);
				Map<String, Object> orgMap = responseList.get(0);
				boolean isRootOrg = false;
				if (null != orgMap.get(JsonKey.IS_ROOT_ORG)) {
					isRootOrg = (boolean) orgMap.get(JsonKey.IS_ROOT_ORG);
				} else {
					isRootOrg = false;
				}
				if (isRootOrg) {
					rootOrgId = orgId;
				} else {
					String channel = (String) orgMap.get(JsonKey.CHANNEL);
					if (!ProjectUtil.isStringNullOREmpty(channel)) {
						Map<String, Object> filters = new HashMap<>();
						filters.put(JsonKey.CHANNEL, channel);
						filters.put(JsonKey.IS_ROOT_ORG, true);
						SearchDTO searchDTO = new SearchDTO();
						searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
						Map<String, Object> esResult = ElasticSearchUtil.complexSearch(searchDTO,
								LexProjectUtil.EsIndex.sunbird.getIndexName(),
								LexProjectUtil.EsType.organisation.getTypeName());
						if (ProjectUtil.isNotNull(esResult) && esResult.containsKey(JsonKey.CONTENT)
								&& ProjectUtil.isNotNull(esResult.get(JsonKey.CONTENT))
								&& !(((List<String>) esResult.get(JsonKey.CONTENT)).isEmpty())) {
							Map<String, Object> esContent = ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT))
									.get(0);
							rootOrgId = (String) esContent.get(JsonKey.ID);
						} else {
							throw new ProjectCommonException(
									ResponseCode.invalidRootOrgData.getErrorCode(), ProjectUtil
											.formatMessage(ResponseCode.invalidRootOrgData.getErrorMessage(), channel),
									ResponseCode.CLIENT_ERROR.getResponseCode());
						}
					} else {
						rootOrgId = JsonKey.DEFAULT_ROOT_ORG_ID;
					}
				}
				userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
			} else {
				throw new ProjectCommonException(ResponseCode.invalidOrgId.getErrorCode(),
						ResponseCode.invalidOrgId.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
			}
			// --------------------------------------------------
		} else {
			String provider = (String) userMap.get(JsonKey.PROVIDER);
			String rootOrgId = Util.getRootOrgIdFromChannel(provider);
			if (!ProjectUtil.isStringNullOREmpty(rootOrgId)) {
				userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
			}
		}

		/**
		 * set role as PUBLIC by default if role is empty in request body. And if roles
		 * are coming in request body, then check for PUBLIC role , if not present then
		 * add PUBLIC role to the list
		 *
		 */

		if (userMap.containsKey(JsonKey.ROLES)) {
			List<String> roles = (List<String>) userMap.get(JsonKey.ROLES);
			String msg = Util.validateRoles(roles);
			if (!msg.equalsIgnoreCase(JsonKey.SUCCESS)) {
				throw new ProjectCommonException(ResponseCode.invalidRole.getErrorCode(),
						ResponseCode.invalidRole.getErrorMessage(), ResponseCode.CLIENT_ERROR.getResponseCode());
			}
			if (!roles.contains(ProjectUtil.UserRole.PUBLIC.getValue())) {
				roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
				userMap.put(JsonKey.ROLES, roles);
			}
		} else {
			List<String> roles = new ArrayList<>();
			roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
			userMap.put(JsonKey.ROLES, roles);
		}
		ProjectLogger.log("User roles is===" + userMap.get(JsonKey.ROLES));
		String accessToken = "";
		if (isSSOEnabled) {
			try {
				String userId = "";
				Map<String, String> responseMap = ssoManager.createUser(userMap);
				userId = responseMap.get(JsonKey.USER_ID);
				accessToken = responseMap.get(JsonKey.ACCESSTOKEN);
				if (!ProjectUtil.isStringNullOREmpty(userId)) {
					userMap.put(JsonKey.USER_ID, userId);
					userMap.put(JsonKey.ID, userId);
				} else {
					ProjectCommonException exception = new ProjectCommonException(
							ResponseCode.userRegUnSuccessfull.getErrorCode(),
							ResponseCode.userRegUnSuccessfull.getErrorMessage(),
							ResponseCode.SERVER_ERROR.getResponseCode());
					throw exception;
				}
			} catch (Exception exception) {
				ProjectLogger.log(exception.getMessage(), exception);
				throw exception;
			}
		} else {
			userMap.put(JsonKey.USER_ID, OneWayHashing.encryptVal((String) userMap.get(JsonKey.USERNAME)));
			userMap.put(JsonKey.ID, OneWayHashing.encryptVal((String) userMap.get(JsonKey.USERNAME)));
		}

		userMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
		userMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());

		if (!ProjectUtil.isStringNullOREmpty((String) userMap.get(JsonKey.PASSWORD))) {
			emailTemplateMap.put(JsonKey.TEMPORARY_PASSWORD, userMap.get(JsonKey.PASSWORD));
			userMap.put(JsonKey.PASSWORD, OneWayHashing.encryptVal((String) userMap.get(JsonKey.PASSWORD)));
		} else {
			// create tempPassword
			String tempPassword = ProjectUtil.generateRandomPassword();
			userMap.put(JsonKey.PASSWORD, OneWayHashing.encryptVal(tempPassword));
			emailTemplateMap.put(JsonKey.TEMPORARY_PASSWORD, tempPassword);
		}
		try {
			// TODO: Verify this
			UserUtility.encryptUserData(userMap);
		} catch (Exception e1) {
			ProjectCommonException exception = new ProjectCommonException(
					ResponseCode.userDataEncryptionError.getErrorCode(),
					ResponseCode.userDataEncryptionError.getErrorMessage(),
					ResponseCode.SERVER_ERROR.getResponseCode());
			throw exception;
		}
		requestMap = new HashMap<>();
		requestMap.putAll(userMap);
		removeUnwanted(requestMap);
		Map<String, String> profileVisbility = new HashMap<>();
		for (String field : ProjectUtil.defaultPrivateFields) {
			profileVisbility.put(field, JsonKey.PRIVATE);
		}
		requestMap.put(JsonKey.PROFILE_VISIBILITY, profileVisbility);
		Response response = null;
		try {
			response = cassandraOperation.insertRecord(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), requestMap);
		} catch (ProjectCommonException exception) {
			throw exception;
		} finally {
			if (null == response && isSSOEnabled) {
				ssoManager.removeUser(userMap);
			}
		}
		response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
		if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
			if (userMap.containsKey(JsonKey.ADDRESS)) {
				List<Map<String, Object>> reqList = (List<Map<String, Object>>) userMap.get(JsonKey.ADDRESS);
				for (int i = 0; i < reqList.size(); i++) {
					Map<String, Object> reqMap = reqList.get(i);
					reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(i + 1));
					reqMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
					String encUserId = "";
					String encCreatedById = "";
					try {
						encUserId = encryptionService.encryptData((String) userMap.get(JsonKey.ID));
						encCreatedById = encryptionService.encryptData((String) userMap.get(JsonKey.CREATED_BY));
					} catch (Exception e) {
						ProjectCommonException exception = new ProjectCommonException(
								ResponseCode.userDataEncryptionError.getErrorCode(),
								ResponseCode.userDataEncryptionError.getErrorMessage(),
								ResponseCode.SERVER_ERROR.getResponseCode());
						throw exception;
					}
					reqMap.put(JsonKey.CREATED_BY, encCreatedById);
					reqMap.put(JsonKey.USER_ID, encUserId);
					try {
						cassandraOperation.insertRecord(addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), reqMap);
					} catch (Exception e) {
						ProjectLogger.log(e.getMessage(), e);
					}
				}
			}
			if (!ProjectUtil.isStringNullOREmpty((String) userMap.get(JsonKey.REGISTERED_ORG_ID))) {
				Response orgResponse = null;
				try {
					orgResponse = cassandraOperation.getRecordById(orgDb.getKeySpace(), orgDb.getTableName(),
							(String) userMap.get(JsonKey.REGISTERED_ORG_ID));
				} catch (Exception e) {
					ProjectLogger.log("Exception occured while verifying regOrgId during create user : ", e);
				}
				if (null != orgResponse
						&& (!((List<Map<String, Object>>) orgResponse.get(JsonKey.RESPONSE)).isEmpty())) {
					insertOrganisationDetails(userMap, usrOrgDb);
				} else {
					ProjectLogger.log("Reg Org Id :" + (String) userMap.get(JsonKey.REGISTERED_ORG_ID) + " for user id "
							+ userMap.get(JsonKey.ID) + " is not valid.");
				}
			}
			// update the user external identity data
			// ProjectLogger.log("User insertation for extrenal identity started--.....");
			// updateUserExtId(requestMap, usrExtIdDb);
			// ProjectLogger.log("User insertation for extrenal identity completed--.....");
		}

		// Validate and insert data in ES
		LexUser user = LexUser.fromMap(requestMap);
		Map<String, Object> data = user.toMap(true);
		List<String> missingFields = new ArrayList<>();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() == null
					|| (entry.getValue().getClass().equals(List.class) && ((List) entry.getValue()).isEmpty())
					|| (entry.getValue().getClass().equals(String.class) && ((String) entry.getValue()).isEmpty())) {
				missingFields.add(entry.getKey());
			}
		}
		data.put("missingFields", missingFields);
		try {
			System.out.println(new ObjectMapper().writeValueAsString(data));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// ElasticSearchUtil.createData(LexProjectUtil.EsIndex.sunbird.getIndexName(),
		// LexProjectUtil.EsType.user.getTypeName(), user.getId(), data);

		ProjectLogger.log("User created successfully.....");
		response.put(JsonKey.ACCESSTOKEN, accessToken);
		return response;

		// user created successfully send the onboarding mail
		// sendOnboardingMail(emailTemplateMap);
	}

	private void removeUnwanted(Map<String, Object> reqMap) {
		reqMap.remove(JsonKey.ADDRESS);
		reqMap.remove(JsonKey.EDUCATION);
		reqMap.remove(JsonKey.JOB_PROFILE);
		reqMap.remove(JsonKey.ORGANISATION);
		reqMap.remove(JsonKey.EMAIL_VERIFIED);
		reqMap.remove(JsonKey.PHONE_NUMBER_VERIFIED);
		reqMap.remove(JsonKey.REGISTERED_ORG);
		reqMap.remove(JsonKey.ROOT_ORG);
		reqMap.remove(JsonKey.IDENTIFIER);
		reqMap.remove(JsonKey.ORGANISATIONS);
		reqMap.remove(JsonKey.IS_DELETED);
		reqMap.remove(JsonKey.PHONE_VERIFIED);
	}

	private void insertOrganisationDetails(Map<String, Object> userMap, Util.DbInfo usrOrgDb) {

		Map<String, Object> reqMap = new HashMap<>();
		reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
		reqMap.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
		reqMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.REGISTERED_ORG_ID));
		reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
		reqMap.put(JsonKey.IS_DELETED, false);

		try {
			cassandraOperation.insertRecord(usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap);
		} catch (Exception e) {
			ProjectLogger.log(e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> getSeparationDate(String email) throws Exception {
		Map<String, Object> res = new HashMap<String, Object>();
		ProjectLogger.log("Fetch Requested Started at " + LocalDateTime.now().toString(), LoggerEnum.INFO);
		try {
			Util.DbInfo usrSepInfo = Util.dbInfoMap.get(LexJsonKey.USER_SEPERATION);
			Response response = cassandraOperation.getRecordsByProperty(usrSepInfo.getKeySpace(),
					usrSepInfo.getTableName(), LexJsonKey.TOPIC_USER_ID, email);
			res = response.getResult();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> resList = (List<Map<String, Object>>) res.get("response");
			System.out.println(res);
			System.out.println(resList.isEmpty());
			if (resList.isEmpty())
				throw new Exception("SERVICE:USER_NOT_FOUND");
		} catch (Exception e) {
			ProjectLogger.log("ERROR : " + e.getMessage(), LoggerEnum.ERROR);
			throw e;
		}
		ProjectLogger.log("Fetch Requested Ended at " + LocalDateTime.now().toString(), LoggerEnum.INFO);
		return res;
	}

	@Override
	public Map<String, Object> findUuids(List<String> emails) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			resultMap = userRepository.getUUIDsFromEmails(emails);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	@Override
	public Map<String, String> toEmails(List<String> ids) throws Exception {
		Map<String, String> result = userRepository.getEmailsFromUUIDS(ids);
		return result;
	}

	@Override
	public Map<String, Object> verifyUUIDs(List<String> ids) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		List<String> validUUIDs = new ArrayList<>();
		try {
			validUUIDs = userRepository.findUUIDs(ids);
			for (String id : validUUIDs) {
				ids.remove(id);
			}
			resultMap.put("valid_users", validUUIDs);
			resultMap.put("invalid_users", ids);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}
}