/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.util.List;
import java.util.Map;

public interface LearningHistoryService {

	public Map<String, Object> getUserCourseProgress(String emailId, Integer pageNumber, Integer pageSize,
			String status, Integer version, String contentType) throws Exception;
	
	public List<Map<String, Object>> getUserContentListProgress(String emailId, List<String> contentIds) throws Exception;
	
	public Map<String, Object> getContentListProgress(String emailId ,List<String> contentIds);
}
