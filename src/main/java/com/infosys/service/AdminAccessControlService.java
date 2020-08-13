/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminAccessControlService {
    Response getUserAccess(String userId, String rootOrg) throws Exception;

    Response getContentUserAccess(String userId, List<String> contentIds, String rootOrg, UUID uuid) throws Exception;

    Response getCohortForUser(String userId, String rootOrg, String contentId, String groupId) throws Exception;

    Response createGroup(Request requestBody) throws Exception;

    Response deleteGroup(Request requestBody) throws Exception;

    Response addUserToGroup(Request requestBody) throws Exception;

    Response removeUserFromGroup(Request requestBody) throws Exception;

    Response addAccessPathToGroup(Request requestBody) throws Exception;

    Response removeAccessPathFromGroup(Request requestBody) throws Exception;

    Response getAllAccessPaths(String adminId) throws Exception;

    Response addUserAccess(Request request) throws Exception;

    Response getContentAccess(String contentId, String aId) throws Exception;

    Response deleteUserAccess(Request request) throws Exception;

    Response lockContents(String contentId, String aId) throws Exception;

    Response deleteContentAccess(Request request) throws Exception;

    Response addContentAccess(Request request) throws Exception;

    Response unlockContents(String contentId, String aId) throws Exception;

    Response fetchGroup(String groupIdentifier, String adminUUID) throws Exception;

    Response fetchAllGroups(String adminUUID, int pageNo) throws Exception;

    Response fetchAdminDetails(String adminId) throws Exception;

    Response addModeratorToGroup(Request requestBody) throws Exception;

    Response removeModeratorFromGroup(Request requestBody) throws Exception;

    Response updateContentAccess(Request request) throws Exception;

    Response groupContentAccess(Request request) throws Exception;

    List<String> getAllGroupsForUser(String userId) throws Exception;

    Response getContentUsersAccess(Map<String, Object> req, String rootOrg, UUID uuid) throws  Exception;
}
