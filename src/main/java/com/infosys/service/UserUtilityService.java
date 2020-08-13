/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.elasticsearch.search.SearchHits;
import org.sunbird.common.models.response.Response;

import com.infosys.model.CourseProgress;

public interface UserUtilityService {

	List<CourseProgress> getUserCourseProgress(String emailId, int pageSize);

	List<Map<String, Object>> getUserDataFromElastic(List<String> users);

	List<Map<String, Object>> getSearchDataFromActiveDirectory(String searchString);

	Response getRecordsByProperties(String keyspaceName, String tableName, Map<String, Object> propertyMap);

	Map<String, Object> verifyUsers(List<String> emails);

	byte[] getUserPhotoFromActiveDirectory(String mailId);

	List<Map<String, Object>> getUsersFromActiveDirectory(List<String> mailIds);

	Response getAllRecordsForColumns(String keySpaceName, String tableName, List<String> columnNames);

	Map<String, Object> getAssessmentMap(String artifactUrl);

	HttpResponse getFileForEmail(String url);

	File getImageFromContentStore() throws Exception;

	String insertFile(File f, String contentId);

	Map<String, Object> getMetaByIDandSource(String id, String[] source) throws IOException;

	List<Map<String, Object>> getUserIdsFromActiveDirectory(List<String> mailIds);

	Map<String, Object> getMailData();

	SearchHits getMetaByIDListandSource(List<String> id, String[] source) throws IOException;

	Boolean checkContentStore(String contentHost, String contentPort);

	String checkElasticSearch();

	String getValidationOptions();

	void insertRecordInElasticSearchAutocompleteIndex(Map<String, Object> requestMap);

	List<Map<String, Object>> getUserSuggestionsForQuery(String searchString) throws Exception;

	boolean validateRootOrgUser(String user, String rootOrg);

	// ---------------------------------------------------------------------------------------------

	public boolean validateUser(String rootOrg, String userId);

	public Map<String, Object> validateUsers(String rootOrg, List<String> userIds);

	public Map<String, Object> getUsersDataFromUserIds(String rootOrg, List<String> userIds, List<String> source);

	public Map<String, Object> getUserDataFromUserId(String rootOrg, String userId, List<String> source);

	public Map<String, Object> getUserEmailsFromUserIds(String rootOrg, List<String> userIds);

	public String getUserEmailFromUserId(String rootOrg, String userId);

	public Map<String, Object> validateAndFetchExistingAndNewUsers(String rootOrg, List<String> userIds);

}
