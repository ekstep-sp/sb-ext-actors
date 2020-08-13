/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.UnauthorizedException;
import com.infosys.service.AdminAccessControlService;
import com.infosys.util.ErrorGenerator;
import com.infosys.util.LexConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import javax.annotation.PostConstruct;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class AdminAccessControlController {

    @Autowired
    private Environment environment;
    @Autowired
    private AdminAccessControlService accessControlService;

    private boolean accessControlEnabled;

    @PostConstruct
    private void initialize() {
        accessControlEnabled = environment.getProperty(LexConstants.ENABLE_ACCESS_CONTROL, Boolean.class);
    }

    @GetMapping("/accesscontrol/user/{userId}")
    public ResponseEntity<Response> getUserAccess(@PathVariable String userId,
                                                  @RequestParam(required = false, name = "rootOrg") String rootOrg) {
        Response response = new Response();
        Date startTime = new Date();

        try {
            ProjectLogger.log("Fetching user Access started at " + startTime + " for userId : " + userId,
                    LoggerEnum.INFO);
            response = accessControlService.getUserAccess(userId, rootOrg);
            ProjectLogger.log("Fetching user Access ended at " + new Date() + " for userId : " + userId,
                    LoggerEnum.INFO);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.getUserAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.getUserAccess", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("GetUserAccess", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.getUserAccess", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/user/{userId}/content")
    public ResponseEntity<Response> getContentUserAccess(
            @RequestParam(required = false, name = "rootOrg") String rootOrg, @PathVariable String userId,
            @RequestParam(required = true, name = "contentIds") List<String> contentIds) {
        Response response = new Response();
        UUID uuid = UUID.randomUUID();
        if (accessControlEnabled) {
            try {
                ProjectLogger.log("Fetching content Access started at " + new Date() + " for userId : " + userId,
                        LoggerEnum.INFO);
                response = accessControlService.getContentUserAccess(userId, contentIds, rootOrg, uuid);
                ProjectLogger.log("Fetching content Access started at " + new Date() + " for userId : " + userId,
                        LoggerEnum.INFO);

            } catch (BadRequestException e) {
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                                "adminAccessControl.getContentUserAccess", "1", e.getMessage()),
                        HttpStatus.BAD_REQUEST);
            } catch (ApplicationLogicError e) {
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                                "adminAccessControl.getContentUserAccess", "1", e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                ProjectLogger.log("GetContentUserAccess", e);
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                                "adminAccessControl.getContentUserAccess", "1", "controller error"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            Map<String, Object> retMap = new HashMap<>();
            contentIds.forEach(item -> {
                Map<String, Object> tempMap = new HashMap<>();
                tempMap.put("hasAccess", true);
                retMap.put(item, tempMap);
            });
            response.put("response", retMap);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/users/contents")
    public ResponseEntity<Response> getContentUsersAccess(@RequestBody Request request, @RequestParam(required = false, name = "rootOrg") String rootOrg) {
        Response response = new Response();

        UUID uuid = UUID.randomUUID();
        if (accessControlEnabled) {
            Map<String, Object> req = request.getRequest();
            try {
                response = accessControlService.getContentUsersAccess(req,rootOrg,uuid);
            } catch (BadRequestException e) {
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                                "adminAccessControl.getContentUserAccess", "1", e.getMessage()),
                        HttpStatus.BAD_REQUEST);
            } catch (ApplicationLogicError e) {
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                                "adminAccessControl.getContentUserAccess", "1", e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                ProjectLogger.log("GetContentUserAccess", e);
                return new ResponseEntity<Response>(
                        ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                                "adminAccessControl.getContentUserAccess", "1", "controller error"),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } else {
            Map<String, Object> finalMap = new HashMap<>();
            Iterator<Map.Entry<String, Object>> iterator = request.getRequest().entrySet().iterator();
            try {
                while (iterator.hasNext()) {
                    Map<String, Object> retMap = new HashMap<>();
                    Map.Entry<String, Object> currItem = iterator.next();
                    ArrayList<String> currValue;
                    try {
                        currValue = ((ArrayList<String>) currItem.getValue());
                    } catch (Exception e) {
                        throw new Exception();
                    }
                    Map<String, Object> tempMap = new HashMap<>();
                    for (String s : currValue) {
                        tempMap.put("hasAccess", true);
                        retMap.put(s, tempMap);
                    }
                    finalMap.put(currItem.getKey(), retMap);
                }
            } catch (Exception e) {
                response.put("Error", "contentIds should be List<String>");
                return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
            }
            response.put("response", finalMap);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/user/{userId}/batch")
    public ResponseEntity<Response> getCohortForUser(@PathVariable String userId,
                                                     @RequestParam(required = false, name = "rootOrg") String rootOrg,
                                                     @RequestParam(required = false, name = "groupId") String groupId,
                                                     @RequestParam(required = false, name = "contentId") String contentId) {
        Response response = new Response();
        Date startTime = new Date();

        try {
            ProjectLogger.log("Fetching getCohortForUser started at " + startTime + " for userId : " + userId,
                    LoggerEnum.INFO);
            response = accessControlService.getCohortForUser(userId, rootOrg, contentId, groupId);
            ProjectLogger.log("Fetching getCohortForUser ended at " + new Date() + " for userId : " + userId,
                    LoggerEnum.INFO);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.getCohortForUser", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getCohortForUser", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("getCohortForUser", e);
            e.printStackTrace();
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getCohortForUser", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/paths/{adminId}")
    public ResponseEntity<Response> getAllAccessPaths(@PathVariable String adminId) {
        Response response = new Response();

        try {
            response = accessControlService.getAllAccessPaths(adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.getAllAccessPaths", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getAllAccessPaths", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("GetAllAccessPaths", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getAllAccessPaths", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/user")
    public ResponseEntity<Response> addUserAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.addUserAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.addUserAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.addUserAccess", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("AddUserAccess", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.addUserAccess", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/user")
    public ResponseEntity<Response> deleteUserAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.deleteUserAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.deleteUserAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.deleteUserAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("DeleteUserAccess", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.deleteUserAccess", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/content/{contentId}/{adminId}")
    public ResponseEntity<Response> getContentAccessPaths(@PathVariable String contentId,
                                                          @PathVariable String adminId) {
        Response response = new Response();

        try {
            response = accessControlService.getContentAccess(contentId, adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.getContentAccessPaths", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getContentAccessPaths", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("GetContentAccessPaths", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getContentAccessPaths", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/content")
    public ResponseEntity<Response> addContentAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.addContentAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.addContentAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("AddContentAccess", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/content")
    public ResponseEntity<Response> deleteContentAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.deleteContentAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.deleteContentAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.deleteContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("DeleteContentAccess", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.deleteContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/lock/{contentId}/{adminId}")
    public ResponseEntity<Response> lockContents(@PathVariable String contentId, @PathVariable String adminId) {
        Response response = new Response();

        try {
            response = accessControlService.lockContents(contentId, adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.getContentAccessPaths", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getContentAccessPaths", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("GetContentAccessPaths", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.getContentAccessPaths", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/unlock/{contentId}/{adminId}")
    public ResponseEntity<Response> unlockContents(@PathVariable String contentId, @PathVariable String adminId) {
        Response response = new Response();

        try {
            response = accessControlService.unlockContents(contentId, adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.unlockContents", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.unlockContents", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("UnlockContents", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.unlockContents", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/group")
    public ResponseEntity<Response> createGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.createGroup(requestBody);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.createGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            e.printStackTrace();
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.createGroup", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("createGroup ", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.createGroup", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/group")
    public ResponseEntity<Response> deleteGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.deleteGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.deleteGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.deleteGroup", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("deleteGroup ", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.deleteGroup", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/group/user")
    public ResponseEntity<Response> addUserToGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.addUserToGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.addUserToGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addUserToGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("addUserToGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addUserToGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/group/user")
    public ResponseEntity<Response> removeUserFromGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.removeUserFromGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.removeUserFromGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeUserFromGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("removeUserFromGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeUserFromGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/group/accesspath")
    public ResponseEntity<Response> addAccessPathToGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.addAccessPathToGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.addAccessPathToGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addAccessPathToGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("addAccessPathToGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addAccessPathToGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/group/accesspath")
    public ResponseEntity<Response> removeAccessPathFromGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.removeAccessPathFromGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                            "adminAccessControl.removeAccessPathFromGroup", "1", e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeAccessPathFromGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("removeAccessPathFromGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeAccessPathFromGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/group/{adminId}/{groupName}")
    public ResponseEntity<Response> fetchGroup(@PathVariable String groupName, @PathVariable String adminId) {
        Response response = new Response();
        try {
            response = accessControlService.fetchGroup(groupName, adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.fetchGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.fetchGroup", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (UnauthorizedException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.UNAUTHORIZED,
                    "adminAccessControl.fetchGroup", "1", e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("fetchGroup ", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                    "adminAccessControl.fetchGroup", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/group/all/{adminId}")
    public ResponseEntity<Response> fetchAllGroups(@PathVariable String adminId, @RequestParam(name = "pageNo", defaultValue = "0") int pageNo) {
        Response response = new Response();
        try {
            response = accessControlService.fetchAllGroups(adminId, pageNo);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.fetchAllGroups", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.fetchAllGroups", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("fetchGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.fetchAllGroups", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @GetMapping("/accesscontrol/admin/{adminId}")
    public ResponseEntity<Response> fetchAdminDetails(@PathVariable String adminId) {
        Response response = new Response();
        try {
            response = accessControlService.fetchAdminDetails(adminId);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.fetchAdminDetails", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.fetchAdminDetails", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("fetchGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.fetchAdminDetails", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/group/moderator")
    public ResponseEntity<Response> addModeratorToGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.addModeratorToGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.addModeratorToGroup", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addModeratorToGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("addModeratorToGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.addModeratorToGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @DeleteMapping("/accesscontrol/group/moderator")
    public ResponseEntity<Response> removeModeratorFromGroup(@RequestBody Request requestBody) {
        Response response = new Response();
        try {
            response = accessControlService.removeModeratorFromGroup(requestBody);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                            "adminAccessControl.removeModeratorFromGroup", "1", e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeModeratorFromGroup", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            ProjectLogger.log("removeModeratorFromGroup ", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.removeModeratorFromGroup", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());
        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

    @PostMapping("/accesscontrol/wrapper/group")
    public ResponseEntity<Response> groupContentAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.groupContentAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.groupContentAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.groupContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("updateContentAccess", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.groupContentAccess", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }


    @PostMapping("/accesscontrol/wrapper/content")
    public ResponseEntity<Response> updateContentAccess(@RequestBody Request request) {
        Response response = new Response();

        try {
            response = accessControlService.updateContentAccess(request);
        } catch (BadRequestException e) {
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData,
                    "adminAccessControl.updateContentAccess", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.updateContentAccess", "1", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("updateContentAccess", e);
            return new ResponseEntity<Response>(
                    ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
                            "adminAccessControl.updateContentAccess", "1", "controller error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setVer("v1");
        response.setId("api.accesscontrol");
        response.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(response, HttpStatus.OK);
    }

}
