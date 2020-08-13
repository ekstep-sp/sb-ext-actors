/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;

import com.datastax.driver.core.utils.UUIDs;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.BadRequestException;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.CommonLearningGoals;
import com.infosys.service.CommonLearningGoalsService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Service
public class CommonLearningGoalsServiceImpl implements CommonLearningGoalsService {

	@Autowired
	UserUtilityService userUtilService;
	
	private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private Util.DbInfo commonLearningGoalsDb = Util.dbInfoMap.get(LexJsonKey.Common_Learning_Goals_Table);

	@SuppressWarnings("unchecked")
	@Override
	public void createCommonGoal(Map<String, Object> requestMap) throws Exception {
		List<Map<String, Object>> commonGoalsMap = (List<Map<String, Object>>) requestMap.get("goals");
		Set<List<String>> goalContentIds = fetchCommonGoalsContentId();
		for (Map<String, Object> commonGoalMap : commonGoalsMap) {
			CommonLearningGoals commonGoal = CommonLearningGoals.fromMap(commonGoalMap);
			validations(commonGoal, goalContentIds);
			if (!commonGoalMap.containsKey("id") || commonGoalMap.get("id").toString().trim().equals("")) {
				commonGoalMap.put("id", UUIDs.timeBased());
			} else {
				commonGoalMap.put("id", UUID.fromString(String.valueOf(commonGoalMap.get("id"))));
			}
			commonGoalMap.put("created_on", new Date());
		}
		cassandraOperation.batchInsert(commonLearningGoalsDb.getKeySpace(), commonLearningGoalsDb.getTableName(),
				commonGoalsMap);
	}

	@SuppressWarnings("unchecked")
	public Set<List<String>> fetchCommonGoalsContentId() {
		Response response = userUtilService.getAllRecordsForColumns(commonLearningGoalsDb.getKeySpace(),
				commonLearningGoalsDb.getTableName(), Arrays.asList(new String[] {"goal_content_id"}));
		List<Map<String, Object>> resultMaps = (List<Map<String, Object>>) response.getResult().get("response");
		Set<List<String>> goalContentIds = new HashSet<>();
		for (Map<String, Object> resultMap : resultMaps) {
			List<String> goalContentIdsFromDb = (List<String>) resultMap.get("goal_content_id");
			goalContentIds.add(goalContentIdsFromDb);
		}
		return goalContentIds;
	}

	public void validations(CommonLearningGoals commonGoal, Set<List<String>> goalContentIdsFromDB) {
		if (commonGoal.getGoal_content_id() == null || commonGoal.getGoal_content_id().size() == 0) {
			throw new BadRequestException("goal_content_id cannot be empty");
		} else if (commonGoal.getGoal_desc() == null || commonGoal.getGoal_desc().trim().isEmpty()) {
			throw new BadRequestException("goal_desc cannot be empty");
		} else if (commonGoal.getGoal_group() == null || commonGoal.getGoal_group().trim().isEmpty()) {
			throw new BadRequestException("goal_group cannot be empty");
		} else if (commonGoal.getGoal_title() == null || commonGoal.getGoal_title().trim().isEmpty()) {
			throw new BadRequestException("goal_title cannot be empty");
		}
		// check if similar goals already exists
		if (goalContentIdsFromDB.contains(commonGoal.getGoal_content_id())) {
			throw new BadRequestException("Common goal with goal_content_ids \'" + commonGoal.getGoal_content_id()
					+ "\' already exists in table.");
		}
		for (String goalContentId : commonGoal.getGoal_content_id()) {
			if (goalContentId.trim().isEmpty()) {
				throw new BadRequestException("goal_content_id cannot be empty");
			}
		}
		List<String> goalContentIds = commonGoal.getGoal_content_id();
		goalContentIds.replaceAll(String::trim);
		commonGoal.setGoal_content_id(goalContentIds);
		commonGoal.setGoal_desc(commonGoal.getGoal_desc().trim());
		commonGoal.setGoal_group(commonGoal.getGoal_group().trim());
		commonGoal.setGoal_title(commonGoal.getGoal_title().trim());
	}
}
