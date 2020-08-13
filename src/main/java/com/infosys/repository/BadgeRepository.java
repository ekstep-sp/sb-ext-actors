/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import org.sunbird.common.models.response.Response;

import java.util.List;

public interface BadgeRepository {

	public void insertInBadges(String courseId, List<String> programId, String userEmail, boolean parent);

	public Response insertCertificatesAndMedals(String keyspaceName, String tableName, String id, String email,
			String type, int retryCounter);
	
	public void insertCourseAndQuizBadge(String email, String type, String contentId);

	public void insertSanta(String userId) ;  
}

