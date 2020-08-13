/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.helper.ServiceFactory;
import com.infosys.repository.BadgeRepository;
import com.infosys.service.UserRolesService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.PropertiesCache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRolesServiceImpl implements UserRolesService {

	
	@Autowired
	BadgeRepository bRepository;
	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
    private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
    private Util.DbInfo userRolesDb = Util.dbInfoMap.get(LexJsonKey.USER_ROLES_DB);
    private Util.DbInfo applicationPropertiesDb = Util.dbInfoMap.get(LexJsonKey.APPLICATION_PROPERTIES_TABLE);

	public UserRolesServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
	}

	public List<String> getUserRoles(String userId) throws ProjectCommonException {
		if (Calendar.getInstance().get(Calendar.MONTH) == 11) {
			if (userId.contains("@") && userId.contains("infosys"))
				bRepository.insertSanta(userId.substring(0, userId.indexOf('@')) + "@ad.infosys.com");
			else if(userId.contains("@"))
				bRepository.insertSanta(userId);
			else
				bRepository.insertSanta(userId+ "@ad.infosys.com");
		}
		List<String> defaultList = new ArrayList<String>();

		Response response = cassandraOperation.getRecordsByProperty(userRolesDb.getKeySpace(),
				userRolesDb.getTableName(), "userId", userId.toLowerCase());
		Map<String, Object> responseMap = response.getResult();
		List<Map<String, Object>> roles = (List<Map<String, Object>>) responseMap.get("response");

		Response defaultResponse = cassandraOperation.getRecordsByProperty(userRolesDb.getKeySpace(),
				userRolesDb.getTableName(), "userId", "defaultuser");
		Map<String, Object> defaultResponseMap = defaultResponse.getResult();
		List<Map<String, Object>> defaultRoles = (List<Map<String, Object>>) defaultResponseMap.get("response");
		
		/*Response defaultResponse = cassandraOperation.getRecordsByProperty(applicationPropertiesDb.getKeySpace(),
				applicationPropertiesDb.getTableName(), "key", "defaultuser");
		Map<String, Object> defaultResponseMap = defaultResponse.getResult();
		List<Map<String, Object>> defaultRoles = (List<Map<String, Object>>) defaultResponseMap.get("response");*/

		if (roles.isEmpty()) {

			if (defaultRoles.isEmpty()) {
				defaultList.add("author");
				defaultList.add("reviewer");
				defaultList.add("live_stream");
				return defaultList;
			}

			List<String> roleList = (List<String>) defaultRoles.get(0).get("roles");
			return roleList;
		}

		List<String> roleList = (List<String>) roles.get(0).get("roles");
		List<String> defaultRoleList = (List<String>) defaultRoles.get(0).get("roles");

		/*for (int i = 0; i < defaultRoleList.size(); i++) {
			if (!roleList.contains(defaultRoleList.get(i)))
				roleList.add(defaultRoleList.get(i));
		}*/
		
		roleList.removeAll(defaultRoleList);
		roleList.addAll(defaultRoleList);

		return roleList;
	}

	public void addPublishers(String userId, List<String> roles) {

		if (userId == null || roles == null)
			throw new ResourceNotFoundException("api contract not adhered");

		Response response = cassandraOperation.getRecordsByProperty(userRolesDb.getKeySpace(),
				userRolesDb.getTableName(), "userId", userId.toLowerCase());
		Map<String, Object> responseMap = response.getResult();

		List<String> rolesToBeAdded = new ArrayList<String>();

		List<Map<String, Object>> existingRoles = (List<Map<String, Object>>) responseMap.get("response");

		if (existingRoles == null)
			throw new ApplicationLogicError("response from sunbird erred");

		List<String> roleList;

		if (!existingRoles.isEmpty() && existingRoles.get(0) != null)
			roleList = (List<String>) existingRoles.get(0).get("roles");
		else
			roleList = new ArrayList<String>();

		if (roleList.isEmpty()) {
			rolesToBeAdded.add("author");
			rolesToBeAdded.add("reviewer");
		} else {
			for (int i = 0; i < roleList.size(); i++)
				rolesToBeAdded.add(roleList.get(i));
		}

		for (int i = 0; i < roles.size(); i++) {
			boolean alreadyPresentFlag = false;
			for (int j = 0; j < roleList.size(); j++) {
				if (roles.get(i).toLowerCase().equals(roleList.get(j).toLowerCase())) {
					alreadyPresentFlag = true;
					break;
				}
			}
			if (alreadyPresentFlag == false && !rolesToBeAdded.contains(roles.get(i))) {
				rolesToBeAdded.add(roles.get(i));
			}
		}

		Map<String, Object> requestMap = new HashMap<String, Object>();

		requestMap.put("userId", userId.toLowerCase());
		requestMap.put("roles", rolesToBeAdded);

		for (int i = 0; i < rolesToBeAdded.size(); i++)
			System.out.println(rolesToBeAdded.get(i));

		if (rolesToBeAdded != null && !rolesToBeAdded.isEmpty()) {
			try {
				cassandraOperation.upsertRecord(userRolesDb.getKeySpace(), userRolesDb.getTableName(), requestMap);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ApplicationLogicError("could not update database");
			}
		}
	}

	public void removePublishers(String userId, List<String> roles) {

		if (userId == null || roles == null)
			throw new ResourceNotFoundException("api contract not adhered");

		Response response = cassandraOperation.getRecordsByProperty(userRolesDb.getKeySpace(),
				userRolesDb.getTableName(), "userId", userId.toLowerCase());
		Map<String, Object> responseMap = response.getResult();

		List<String> rolesToBeRemoved = new ArrayList<String>();

		List<Map<String, Object>> existingRoles = (List<Map<String, Object>>) responseMap.get("response");

		if (existingRoles == null)
			throw new ApplicationLogicError("response from sunbird erred");

		List<String> roleList;

		if (!existingRoles.isEmpty() && existingRoles.get(0) != null)
			roleList = (List<String>) existingRoles.get(0).get("roles");
		else
			throw new ResourceNotFoundException("user does not have the role");

		for (int i = 0; i < roles.size(); i++) {
			boolean alreadyPresentFlag = false;
			for (int j = 0; j < roleList.size(); j++) {
				if (roles.get(i).toLowerCase().equals(roleList.get(j).toLowerCase())) {
					alreadyPresentFlag = true;
					break;
				}
			}
			if (alreadyPresentFlag == true) {
				rolesToBeRemoved.add(roles.get(i));
			} else {
				throw new ApplicationLogicError("no role access found for this user ");
			}
		}

		List<String> rolesToBeUpdated = new ArrayList<String>();

		for (int i = 0; i < roleList.size(); i++) {
			if (!rolesToBeRemoved.contains(roleList.get(i)))
				rolesToBeUpdated.add(roleList.get(i));
		}

		Map<String, Object> requestMap = new HashMap<String, Object>();

		requestMap.put("userId", userId.toLowerCase());
		requestMap.put("roles", rolesToBeUpdated);

		for (int i = 0; i < rolesToBeUpdated.size(); i++)
			System.out.println(rolesToBeUpdated.get(i));

		if (rolesToBeUpdated != null && !rolesToBeUpdated.isEmpty()) {
			try {
				cassandraOperation.upsertRecord(userRolesDb.getKeySpace(), userRolesDb.getTableName(), requestMap);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ApplicationLogicError("could not update database");
			}
		}
	}

}
