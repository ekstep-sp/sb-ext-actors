/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.exception.BadRequestException;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.model.UserProfileDetails;
import com.infosys.repository.UserRepository;
import com.infosys.service.ProfileService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.PropertiesCache;

import java.util.*;

@Service
public class ProfileServiceImpl implements ProfileService {

	@Autowired
	UserUtilityService userUtilService;

	@Autowired
	UserRepository userRepo;

	private PropertiesCache properties = PropertiesCache.getInstance();
	private String userRoleTable = properties.getProperty(LexJsonKey.USER_PERSONALROLE);
	private String bodhiKeyspace = properties.getProperty(LexJsonKey.BODHI_DB_KEYSPACE);

	@Override
	public UserProfileDetails getUserData(String userEmail) throws Exception {
		if (userEmail == null || userEmail.length() == 0) {
			throw new BadRequestException("user email cannot be null or empty");
		}
		String emailForGraph = "";
		if (userEmail.contains("@")) {
			emailForGraph = userEmail.substring(0, userEmail.indexOf('@')).toLowerCase();
		} else {
			emailForGraph = userEmail.toLowerCase();
		}

		List<Map<String, Object>> userList = new ArrayList<>();
		if (userUtilService.getValidationOptions().toLowerCase().contains("graph"))
			userList = userUtilService.getUsersFromActiveDirectory(Arrays.asList(emailForGraph));
		UserProfileDetails user = null;
		if (userList != null && userList.size() > 0) {
			user = UserProfileDetails.fromMap(userList.get(0));
			if (user != null && user.getOnPremisesUserPrincipalName() != null
					&& !user.getOnPremisesUserPrincipalName().isEmpty()) {
				user.setOnPremisesUserPrincipalName(user.getOnPremisesUserPrincipalName().toLowerCase());
			}
		} else {
			user = new UserProfileDetails();
			Map<String, Object> userFromCassandra = userRepo.getUUIDFromEmail(
					userEmail.contains("@") ? userEmail.toLowerCase().replaceAll("@infosys.com", "@ad.infosys.com")
							: userEmail.toLowerCase() + "@ad.infosys.com");
			user.setGivenName(userFromCassandra.get("firstname").toString());
			user.setSurname(userFromCassandra.get("lastname").toString());
			user.setOnPremisesUserPrincipalName(userEmail.toLowerCase());
		}

		if (user == null) {
			throw new ResourceNotFoundException("User not found");
		}

		return user;
	}

	@Override
	public UserProfileDetails getUserDataByUUID(String userUUID) throws Exception {

		Map<String, Object> email = userRepo.getEmailFromUUID(userUUID);

		if (email == null || email.isEmpty()) {
			throw new BadRequestException("user email cannot be null or empty");
		}

		String userEmail = email.get("email").toString().toLowerCase();
		if (userEmail.contains("@")) {
			userEmail = userEmail.substring(0, userEmail.indexOf('@'));
		}

		List<Map<String, Object>> userList = new ArrayList<>();
		if (userUtilService.getValidationOptions().toLowerCase().contains("graph"))
			userList = userUtilService.getUsersFromActiveDirectory(Arrays.asList(userEmail));
		UserProfileDetails user = null;
		if (userList != null && userList.size() > 0) {
			user = UserProfileDetails.fromMap(userList.get(0));
			if (user != null && user.getOnPremisesUserPrincipalName() != null
					&& !user.getOnPremisesUserPrincipalName().isEmpty()) {
				user.setOnPremisesUserPrincipalName(user.getOnPremisesUserPrincipalName().toLowerCase());
			}
		} else {
			Map<String, Object> userFromCassandra = userRepo.getUUIDFromEmail(email.get("email").toString());
			user = new UserProfileDetails();
			user.setGivenName(userFromCassandra.get("firstname").toString());
			user.setSurname(userFromCassandra.get("lastname").toString());
			user.setOnPremisesUserPrincipalName(email.get("email").toString().toLowerCase());
		}

		if (user == null) {
			throw new ResourceNotFoundException("User not found");
		}

		return user;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getMultipleUserData(List<String> userEmails) throws Exception {
		List<String> users = new ArrayList<String>();
		for (String userEmail : userEmails) {
			if (userEmail.contains("@"))
				userEmail = userEmail.substring(0, userEmail.indexOf('@'));
			users.add(userEmail.toLowerCase());
		}
		Map<String, Object> allValues = new HashMap<>();
		List<String> missingIds = new ArrayList<>();
		{
			List<Map<String, Object>> userList = new ArrayList<>();
			if (userUtilService.getValidationOptions().toLowerCase().contains("graph"))
				userList = userUtilService.getUsersFromActiveDirectory(users);
			for (Map<String, Object> user : userList) {
				user.put("mail", user.get("mail").toString().toLowerCase());
				if (user.get("onPremisesUserPrincipalName") != null
						&& !user.get("onPremisesUserPrincipalName").toString().isEmpty())
					user.put("onPremisesUserPrincipalName",
							user.get("onPremisesUserPrincipalName").toString().toLowerCase());
				allValues.put(user.get("mail").toString(), user);
			}
			for (String userEmail : userEmails) {
				if (!allValues.containsKey((userEmail.contains("@")
						? userEmail.substring(0, userEmail.indexOf('@')).toLowerCase() + "@infosys.com"
						: userEmail.toLowerCase() + "@infosys.com"))) {
					missingIds.add(userEmail.toLowerCase());
				}
			}
			if (missingIds.size() > 0) {
				List<String> cassandraIds = new ArrayList<>();
				for (String id : missingIds) {
					if (!id.contains("@")) {
						id = id + "@ad.infosys.com";
					} else {
						id = id.replaceAll("@infosys.com", "@ad.infosys.com");
					}
					cassandraIds.add(id);
				}
				missingIds = cassandraIds;
				Map<String, Object> idsfromCassandra = userRepo.getUUIDsFromEmails(missingIds);
				for (String userEmail : idsfromCassandra.keySet()) {
					Map<String, String> temp = new HashMap<>();
					temp.put("surname", ((Map<String, String>) idsfromCassandra.get(userEmail)).get("firstname"));
					temp.put("givenName", ((Map<String, String>) idsfromCassandra.get(userEmail)).get("lastname"));
					allValues.put(userEmail.toLowerCase(), temp);
				}
			}
		}
		List<Map<String, Object>> ret = new ArrayList<>();
		for (String userId : users) {
			Map<String, Object> user = (Map<String, Object>) allValues.get(userId + "@infosys.com");
			if (user == null) {
				continue;
			}
			String temp = user.get("department") == null ? "" : user.get("department").toString();
			user.put("department", temp);

			temp = user.get("jobTitle") == null ? "" : user.get("jobTitle").toString();
			user.put("jobTitle", temp);

			temp = user.get("surname") == null ? "" : user.get("surname").toString();
			user.put("surname", temp);

			temp = user.get("givenName") == null ? "" : user.get("givenName").toString();
			user.put("givenName", temp);
			ret.add(user);
		}

		for (String userId : missingIds) {
			Map<String, Object> user = (Map<String, Object>) allValues.get(userId);
			if (user == null) {
				continue;
			}
			String temp = user.get("surname") == null ? "" : user.get("surname").toString();
			user.put("surname", temp);

			temp = user.get("givenName") == null ? "" : user.get("givenName").toString();
			user.put("givenName", temp);

			user.put("onPremisesUserPrincipalName", userId.toLowerCase());
			ret.add(user);

		}
		return ret;
	}

	@Override
	public byte[] getUserPhoto(String userEmail) {
		if (userEmail.contains("@"))
			userEmail = userEmail.substring(0, userEmail.indexOf('@'));
		return userUtilService.getUserPhotoFromActiveDirectory(userEmail);

	}

	@Override
	public byte[] getUserPhotoByUUID(String userUUID) throws Exception {
		Map<String, Object> email = userRepo.getEmailFromUUID(userUUID);

		if (email == null || email.isEmpty()) {
			throw new BadRequestException("user email cannot be null or empty");
		}
		String userEmail = email.get("email").toString();
		if (userEmail.contains("@")) {
			userEmail = userEmail.substring(0, userEmail.indexOf('@'));
		}
		if (userEmail.contains("@"))
			userEmail = userEmail.substring(0, userEmail.indexOf('@'));
		return userUtilService.getUserPhotoFromActiveDirectory(userEmail);

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getRole(String userEmail) throws Exception {
		List<String> roles = new ArrayList<String>();
		try {
			if (userEmail == null || userEmail.length() == 0) {
				throw new BadRequestException("user email cannot be null or empty");
			}

			Map<String, Object> propertyMap = new HashMap<String, Object>();
			propertyMap.put("userid", userEmail.toLowerCase());
			Response result = userUtilService.getRecordsByProperties(bodhiKeyspace, userRoleTable, propertyMap);
			if (!((List<Map<String, Object>>) result.get("response")).isEmpty())
				roles = (List<String>) ((Map<String, Object>) (((List<Map<String, Object>>) result.get("response"))
						.get(0))).get("roles");
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return roles;
	}

	@Override
	public String getEmailByUUID(String userUUID) throws Exception {
		Map<String, Object> email = userRepo.getEmailFromUUID(userUUID);
		if (email.get("email") == null)
			throw new InvalidDataInputException("Invalid user id");
		return email.get("email").toString().toLowerCase();

	}

	@Override
	public Map<String, Object> fetchUserData(String idValue, String idType) throws Exception {
		return userRepo.getUserDetails(idValue, idType);
	}

}
