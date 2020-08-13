/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.util.Map;

import com.infosys.exception.ApplicationLogicError;

public interface AssessmentService {

	public Map<String,Object> submitAssessment(Map<String,Object> data);

	Map<String, Object> getAssessmentByContentUser(String course_id, String user_id) throws ApplicationLogicError;

	Map<String, Object> submitAssessmentByIframe(Map<String, Object> request) throws Exception;
	
}
