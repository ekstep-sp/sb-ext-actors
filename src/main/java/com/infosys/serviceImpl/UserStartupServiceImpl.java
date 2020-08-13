/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infosys.exception.BadRequestException;
import com.infosys.service.AdminAccessControlService;
import com.infosys.service.TermsAndConditionService;
import com.infosys.service.UserRolesService;
import com.infosys.service.UserStartupService;

@Service
public class UserStartupServiceImpl implements UserStartupService {

	@Autowired
	UserRolesService userRoles;

	@Autowired
	AdminAccessControlService userGroups;

	@Autowired
	TermsAndConditionService tncService;

	@Override
	public Map<String, Object> getUserStartupDetails(String userId, boolean isAuthor)
			throws BadRequestException, Exception {
		System.out.println("started");
		List<String> roles = new ArrayList<>();
		List<String> groupIds = new ArrayList<>();
		Map<String, Object> tncMap = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		boolean tncStatus = false;

		UUID userUUID;
		try {
			userUUID = UUID.fromString(userId);
		} catch (ClassCastException | IllegalArgumentException e) {
			throw new BadRequestException("userId MUST BE A UUID");
		}

		CompletableFuture<List<String>> rolesFutures = CompletableFuture.supplyAsync(() -> {
			return userRoles.getUserRoles(userId);
		});

		CompletableFuture<List<String>> groupsFutures = CompletableFuture.supplyAsync(() -> {
			try {
				return userGroups.getAllGroupsForUser(userId);
			} catch (Exception e) {
				return null;
			}
		});

		CompletableFuture<Map<String, Object>> tncMapFuture = CompletableFuture.supplyAsync(() -> {
			if (isAuthor) {
				return tncService.checkVersionChangeV2(userId, "Author");
			} else {
				return tncService.checkVersionChangeV2(userId, "User");
			}
		});

		roles = rolesFutures.get();
		groupIds = groupsFutures.get();
		tncMap = tncMapFuture.get();

		if (tncMap == null) {
			resultMap.put("tncStatus", null);
		} else {
			tncStatus = (boolean) tncMap.get("isAccepted");
			resultMap.put("tncStatus", tncStatus);
		}
		resultMap.put("roles", roles);
		resultMap.put("group", groupIds);
		resultMap.put("rootOrg", "Infosys");
		resultMap.put("org", java.util.Arrays.asList("Infosys Ltd"));

		return resultMap;
	}

}
