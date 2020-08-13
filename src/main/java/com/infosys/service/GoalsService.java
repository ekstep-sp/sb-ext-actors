/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.sunbird.common.models.response.Response;

public interface GoalsService {
	Response removeLearningGoals(Map<String, Object> goalsData);

	Response upsertLearningGoals(Map<String, Object> goalsData) throws Exception;

	Response upsertLearningGoals_v1(Map<String, Object> goalsData) throws ParseException;

	Response getCommonGoals(String userEmail);

	Response getGoalsProgress_v1(String userEmail);

	Response getGoalsProgress(String userEmail) throws Exception;

	Response getUserGoalsList(String userEmail) throws Exception;

	Response getUpdatedResourceList(Map<String, Object> goalsData);

	Response getSharedGoalsList(String userEmail, String goalType) throws Exception;

	Response deleteSharedUserFromSharedGoal(Map<String, Object> sharedUserData) throws ParseException;

	Response getUserGoalsList_v1(String userEmail);

	Response getCountForSharedGoal(Map<String, Object> data);

	Response getSharedGoalStatus(Map<String, Object> data) throws Exception;

	Response getCommonGoals_v1(String userEmail);

	Response getOnboardingPrograms();

	Response isAddedAsCommonGoal(Map<String, Object> sharedGoalIdentifier);

	Response deleteContentUserGoal(Map<String, Object> goalsData);

	Response addContentUserGoal(Map<String, Object> goalsData);

	Response addCommonGoal(Map<String, Object> commonGoalMap) throws ParseException;

	Response updateCommonGoal(Map<String, Object> commonGoalMap) throws ParseException;

	List<String> getContentIdsForGoal(String userId, String goalId) throws Exception;
}
