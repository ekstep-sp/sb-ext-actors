/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;
import java.util.Map;

import org.sunbird.common.models.response.Response;

public interface AssessmentRepository {

	public Map<String, Object> getAssessmentAnswerKey(String artifactUrl) throws Exception;

	public Map<String, Object> getQuizAnswerKey(Map<String, Object> quizMap) throws Exception;

	public Response insertQuizOrAssessment(String keyspaceName, String tableName, Map<String, Object> persist,
			Boolean isAssessment) throws Exception;

	public List<Map<String,Object>> getAssessmetbyContentUser(String courseId, String userId) ;  
}

