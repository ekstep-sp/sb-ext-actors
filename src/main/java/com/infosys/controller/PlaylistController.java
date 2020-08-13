/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.PlaylistService;

@RestController
@CrossOrigin(origins = "*")
public class PlaylistController {
	@Autowired
	PlaylistService playlistService;

	@DeleteMapping("/v1/users/{user_id}/playlist/{playlistId}")
	public Response deleteUserPlaylist(@PathVariable("user_id") String userId, @RequestHeader("rootOrg") String rootOrg,
			@PathVariable("playlistId") String playlistId,
			@RequestParam(defaultValue = "user", required = false, name = "type") String type) {
		String url = "/v1/users/" + userId + "/playlist/" + playlistId;
		Map<String, Object> deleteMap = new HashMap<String, Object>();
		deleteMap.put("root_org", rootOrg.trim());
		deleteMap.put("user_id", userId.toLowerCase().trim());
		deleteMap.put("playlist_id", UUID.fromString(playlistId));
		Response resp = null;
		Long start = System.currentTimeMillis();
		if (type.equalsIgnoreCase("user")) {
			resp = playlistService.deletePlaylist(deleteMap);
		} else {
			resp = playlistService.deleteSharedPlaylist(deleteMap);
		}
		resp.setVer("v1");
		resp.setId("api.users.playlist.delete");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode responseCode = resp.getResponseCode();
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
		if (responseCode.getResponseCode() != 200) {
			params.setErr("Internal Server problem");
		}
		resp.setParams(params);
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return resp;
	}

	@PostMapping("/v1/users/{user_id}/playlist")
	public Response upsertUserPlaylist_v1(@RequestBody Request reqData, @PathVariable("user_id") String userId,
			@RequestHeader("rootOrg") String rootOrg,
			@RequestParam(defaultValue = "upsert", required = false, name = "type") String type) throws Exception {
		String url = "/v1/users/" + userId + "/playlist";
		Map<String, Object> reqMap = reqData.getRequest();
		reqMap.put("user_id", userId.toLowerCase().trim());
		reqMap.put("root_org", rootOrg.trim());
		reqMap.put("version", "v1");
		Response resp = null;
		Long start = System.currentTimeMillis();
		if (type.equalsIgnoreCase("share")) {
			resp = playlistService.sharePlaylist(reqMap);
		} else {
			reqMap.put("type", type);
			resp = playlistService.upsertPlaylist(reqMap);
		}
		resp.setVer("v1");
		resp.setId("api.users.playlists");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode responseCode = resp.getResponseCode();
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
		if (responseCode.getResponseCode() != 200) {
			params.setErr("Internal Server problem");
		}
		resp.setParams(params);
		// resp.put(Constants.RESPONSE, resp);
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return resp;
	}

	@PostMapping("/v2/users/{user_id}/playlist")
	public Response upsertUserPlaylist(@RequestBody Request reqData, @PathVariable("user_id") String userId,
			@RequestHeader("rootOrg") String rootOrg,
			@RequestParam(defaultValue = "upsert", required = false, name = "type") String type) throws Exception {
		String url = "/v2/users/" + userId + "/playlist";
		Map<String, Object> reqMap = reqData.getRequest();
		reqMap.put("user_id", userId.toLowerCase().trim());
		reqMap.put("root_org", rootOrg.trim());
		reqMap.put("version", "v2");
		Response resp = null;
		Long start = System.currentTimeMillis();
		if (type.equalsIgnoreCase("share")) {
			resp = playlistService.sharePlaylist(reqMap);
		} else {
			reqMap.put("type", type);
			resp = playlistService.upsertPlaylist(reqMap);
		}
		resp.setVer("v2");
		resp.setId("api.users.playlists");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode responseCode = resp.getResponseCode();
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
		if (responseCode.getResponseCode() != 200) {
			params.setErr("Internal Server problem");
		}
		resp.setParams(params);
		// resp.put(Constants.RESPONSE, resp);
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return resp;
	}

	/*
	 * this method will return user playlist depending upon type of playlist being
	 * asked like user playlist, playlist shared with user or recently added/updated
	 * resources in playlists of user
	 */

	@GetMapping("/v1/users/{user_id}/playlist")
	public Response getUserPlaylist_v1(@PathVariable("user_id") String userId, @RequestHeader("rootOrg") String rootOrg,
			@RequestParam(defaultValue = "user", required = false, name = "type") String type,
			@RequestParam(required = false, name = "count") Integer count,
			@RequestParam(defaultValue = "no_one", required = false, name = "sharedBy") String sharedBy,
			@RequestParam(defaultValue = "a4c013d7-9522-4dda-8a19-fe40a874b93b", required = false, name = "userPId") String userPId,
			@RequestParam(defaultValue = "a4c013d7-9522-4dda-8a19-fe40a874b93b", required = false, name = "sourcePId") String sourcePId)
			throws Exception {
		Response resp;
		String url = "/v1/users/" + userId + "/playlist";
		Map<String, Object> reqMap = new HashMap<String, Object>();
		reqMap.put("user_id", userId.toLowerCase().trim());
		reqMap.put("root_org", rootOrg.trim());
		reqMap.put("version", "v1");
		reqMap.put("type", type.toLowerCase());
		if (count == null) {
			// default resource count=20
			count = new Integer(20);
		}

		reqMap.put("count", count);
		Long start = System.currentTimeMillis();
		switch (type.toLowerCase()) {
		case "user":
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		case "recent":
			resp = playlistService.fetchRecentPlaylist(reqMap);
			break;
		case "share":
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		case "sync":
			reqMap.put("shared_by", sharedBy.toLowerCase());
			reqMap.put("playlist_id", userPId);
			reqMap.put("source_playlist_id", sourcePId);
			resp = playlistService.fetchResourceListForSyncing(reqMap);
			break;
		default:
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		}

		resp.setVer("v1");
		resp.setId("api.users.getplaylist");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return resp;
	}

	@GetMapping("/v2/users/{user_id}/playlist")
	public Response getUserPlaylist(@PathVariable("user_id") String userId, @RequestHeader("rootOrg") String rootOrg,
			@RequestParam(defaultValue = "user", required = false, name = "type") String type,
			@RequestParam(required = false, name = "count") Integer count,
			@RequestParam(defaultValue = "no_one", required = false, name = "sharedBy") String sharedBy,
			@RequestParam(defaultValue = "a4c013d7-9522-4dda-8a19-fe40a874b93b", required = false, name = "userPId") String userPId,
			@RequestParam(defaultValue = "a4c013d7-9522-4dda-8a19-fe40a874b93b", required = false, name = "sourcePId") String sourcePId)
			throws Exception {
		Response resp;
		String url = "/v2/users/" + userId + "/playlist";
		Map<String, Object> reqMap = new HashMap<String, Object>();
		reqMap.put("user_id", userId.toLowerCase().trim());
		reqMap.put("root_org", rootOrg.trim());
		reqMap.put("version", "v2");
		reqMap.put("type", type.toLowerCase());
		if (count == null) {
			// default resource count=20
			count = new Integer(20);
		}

		reqMap.put("count", count);
		Long start = System.currentTimeMillis();
		switch (type.toLowerCase()) {
		case "user":
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		case "recent":
			resp = playlistService.fetchRecentPlaylist(reqMap);
			break;
		case "share":
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		case "sync":
			reqMap.put("shared_by", sharedBy.toLowerCase());
			reqMap.put("playlist_id", userPId);
			reqMap.put("source_playlist_id", sourcePId);
			resp = playlistService.fetchResourceListForSyncing(reqMap);
			break;
		default:
			resp = playlistService.fetchUserPlaylist(reqMap);
			break;
		}

		resp.setVer("v2");
		resp.setId("api.users.getplaylist");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return resp;
	}

//	@GetMapping("/v1/users/{user_email}/playlist/{playlist_id}/content")
//	public ResponseEntity<List<String>> getContentForGoals(@PathVariable("user_email") String userId,
//			@PathVariable("playlist_id") String playlistId) {
//		try {
//			return new ResponseEntity<List<String>>(playlistService.getContentIdsForPlayList(userId, playlistId),
//					HttpStatus.OK);
//		} catch (Exception e) {
//			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}

	@GetMapping("/v1/users/{user_id}/playlist/{playlist_id}")
	public ResponseEntity<Map<String, Object>> getPlayListById_v1(@PathVariable("user_id") String userId,
			@RequestHeader("rootOrg") String rootOrg, @PathVariable("playlist_id") String playlistId) throws Exception {
		return new ResponseEntity<Map<String, Object>>(playlistService.getPlayListById_v1(rootOrg, userId, playlistId),
				HttpStatus.OK);
	}

	@GetMapping("/v2/users/{user_id}/playlist/{playlist_id}")
	public ResponseEntity<Map<String, Object>> getPlayListById(@PathVariable("user_id") String userId,
			@RequestHeader("rootOrg") String rootOrg, @PathVariable("playlist_id") String playlistId) throws Exception {
		return new ResponseEntity<Map<String, Object>>(playlistService.getPlayListById(rootOrg, userId, playlistId),
				HttpStatus.OK);
	}

	@GetMapping("/v1/users/{user_id}/playlists")
	public ResponseEntity<?> getUserPlayListByUserId(@PathVariable("user_id") String userId,
			@RequestHeader("rootOrg") String rootOrg) throws Exception {
		return new ResponseEntity<>(playlistService.getUserPlaylistWithoutMeta(rootOrg, userId), HttpStatus.OK);
	}
}
