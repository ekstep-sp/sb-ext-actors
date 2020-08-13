/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;
import java.util.Map;

public interface ExerciseRepository {

	public List<Map<String,Object>> getAll(String userUUID,String contentId) throws Exception;
	
	public List<Map<String,Object>> getLatest(String userUUID,String contentId) throws Exception;
	
	public List<Map<String, Object>> getOne(String userUUID,String contentId, String submissionId) throws Exception;
	
	public List<Map<String, String>> getGroups(String educatorUUID) throws Exception;

    public Map<String, Object> getSubmissionsByGroups(String groupId, String contentId) throws Exception;
	
	public void validateUserID(String userUUID) throws Exception;
	
	public void validateContentID(String contentId) throws Exception;

	void validateSubmissionID(String submissionId) throws Exception;

    List<Map<String, Object>> getNotificationForUser(String userUUID) throws Exception;
}

