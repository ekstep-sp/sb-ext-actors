/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.util.List;
import java.util.Map;


public interface ExerciseService {

    public List<Map<String, Object>> getAllData(String userId, String contentId, String userIdType) throws Exception;
    
    public List<Map<String, Object>> getLatestData(String userId, String contentId, String userIdType) throws Exception;
    
    public List<Map<String, Object>> getOneData(String userId, String contentId, String submissionId, String userIdType) throws Exception;
    
    public List<Map<String, String>> getEducatorGroups(String educatorId, String userIdType) throws Exception;

    public Map<String, Object> getSubmissionsByGroups(String groupId, String contentId) throws Exception;

    public List<Map<String, Object>> getExerciseNotification(String userId, String userIdType) throws Exception;

    String insertFeedback(Map<String, Object> meta, String contentId, String userId, String submissionId)
            throws Exception;

    String insertSubmission(Map<String, Object> meta, String contentId, String userId) throws Exception;
}
