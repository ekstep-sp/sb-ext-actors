/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;
import java.util.Map;

import org.bson.Document;

public interface ProgressRepositoryCustom {

	public Map<String, Object> getUserCourseProgressPaginated(String emailId, String status, String contentType,
			Integer page, Integer pageSize);

	public Map<String, Object> getUserCourseProgressNonPaginated(String emailId, String status, String contentType);

	public Map<String, Object> fetchUserContentListProgress(String emailId, List<String> contentIds);
	
	public Map<String, Object> fetchUserContentListMap(String emailId, List<String> contentIds);
	
	public List<Document> getCourseProgress(String emailId);
	
	public List<Document> getActiveUsersFromDB(String userEmail,List<String>resourceIds,long lastTs);
	
	public List<Document> getGoalProgressFromDB(List<String> userEmails,List<String>resourceIds);

}
