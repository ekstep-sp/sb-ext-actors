/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
© 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
under the law.

Highly Confidential
 
*/

package com.infosys.serviceImpl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.repository.UserRepository;
import com.infosys.search.GeneralMultiLingualIntegratedSearchService;
import com.infosys.service.AdminAccessControlService;
import com.infosys.service.PlaylistService;
import com.infosys.service.SearchTemplateService;
import com.infosys.service.UserService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class PlaylistServiceImpl implements PlaylistService {

	@Autowired
	UserUtilityService userUtilService;

	@Autowired
	UserRepository userRepo;

	@Autowired
	UserService userSvc;

	@Autowired
	AdminAccessControlService accessService;

	@Autowired
	SearchTemplateService searchTemplateService;

	@Autowired
	GeneralMultiLingualIntegratedSearchService searchSvc;

	@Autowired
	Environment environment;

	@Autowired
	RestTemplate restTemplate;

	private static PropertiesCache properties = PropertiesCache.getInstance();
	private static String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private static Util.DbInfo userPlaylistDb = Util.dbInfoMap.get(LexJsonKey.USER_PLAYLIST_DB);
	private static Util.DbInfo recentPlaylistDb = Util.dbInfoMap.get(LexJsonKey.RECENT_PLAYLIST_DB);
	private static Util.DbInfo sharedPlaylistDb = Util.dbInfoMap.get(LexJsonKey.SHARED_PLAYLIST_DB);
	private String notifSvcIp = null;
	private String notifSvcPort = null;

	private CassandraConnectionManager connectionManager;

	public PlaylistServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	@PostConstruct
	public void init() {
		notifSvcIp = environment.getProperty("notification.service.ip");
		notifSvcPort = environment.getProperty("notification.service.port");
	}

	/*
	 * this method is used to delete an entire playlist of a particular user based
	 * on user_email and playlist_id it deletes that particular playlistid from
	 * user_playlist and resources from playlist_recent while updating
	 * source_playlist_id (playlist created from shared playlist with source as this
	 * source_playlist_id ) to null.
	 */
	@Override
	public Response deletePlaylist(Map<String, Object> playlistData) {
		Response contentResponse = new Response();
		try {
			Map<String, String> tableName = new HashMap<String, String>();
			tableName.put("userPlaylist", userPlaylistDb.getTableName());
			tableName.put("playlistRecent", recentPlaylistDb.getTableName());
			return executeBatchQueries(
					this.getDeletePlaylistStmts(userPlaylistDb.getKeySpace(), tableName, playlistData));
		} catch (ProjectCommonException e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_DELETE + userPlaylistDb.getTableName() + " : " + e.getMessage(),
					e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
	}

	/*
	 * this method is used to fetch user playlist from user_playlist table for a
	 * particular user denoted by input parameter userEmail or playlist shared with
	 * a user by colleagues from playlist_shared table
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Response fetchUserPlaylist(Map<String, Object> playlistData) throws Exception {
		Response contentResponse = new Response();
		try {
			if (!userUtilService.validateUser(playlistData.get("root_org").toString(),
					playlistData.get("user_id").toString())) {
				throw new InvalidDataInputException("invalid.user");
			}
//			System.out.println("==============Step 2: USER VALIDATION DONE for USER ID : "
//					+ playlistData.get("user_id").toString() + " ====================== ");

			if (playlistData.get("type").toString().equalsIgnoreCase("user")) {
				contentResponse = getUserPlaylist(userPlaylistDb.getKeySpace(), userPlaylistDb.getTableName(),
						playlistData.get("user_id").toString(), playlistData.get("root_org").toString());

//				System.out.println("==============Step 3: USER PLAYLIST DATA FETCHED =====================");

			} else {
				contentResponse = getUserPlaylist(sharedPlaylistDb.getKeySpace(), sharedPlaylistDb.getTableName(),
						playlistData.get("user_id").toString(), playlistData.get("root_org").toString());

//				System.out.println("==============Step 3: SHARED PLAYLIST DATA FETCHED =====================");
			}
			List<Map<String, Object>> userPlaylistData = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");

			if (userPlaylistData != null && !userPlaylistData.isEmpty()) {
//				System.out.println(
//						"============== PLAYLIST DATA:" + userPlaylistData.toString() + " =====================");

				// sorting playlist in last updated order
				Collections.sort(userPlaylistData, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> m1, Map<String, Object> m2) {
						if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null) {
							return ((Date) m1.get("last_updated_on")).compareTo((Date) (m2.get("last_updated_on")));
						} else {
							return -1;
						}
					}
				}.reversed());

//				System.out.println("============== Step 4: SORTED PLAYLIST DATA:" + userPlaylistData.toString()
//						+ " =====================");

				// how many shared playlists to fetch
				int resourceCount = (int) playlistData.get("count");
				if (!playlistData.get("type").toString().equalsIgnoreCase("user")
						&& resourceCount < userPlaylistData.size()) {
					userPlaylistData = userPlaylistData.subList(0, resourceCount);
				}
				// ********************----------------------------*****************************
				Set<String> contentIds = new HashSet<>();
				Set<String> userIdsToReplace = new HashSet<>();
				for (Iterator<Map<String, Object>> iterator = userPlaylistData.iterator(); iterator.hasNext();) {
					Map<String, Object> map = iterator.next();
					map.remove("root_org");
					contentIds.addAll((List<String>) map.get("resource_ids"));

					if (map.containsKey("user_id")) {
						userIdsToReplace.add(map.get("user_id").toString());
					}
					if (map.containsKey("shared_by") && map.get("shared_by") != null) {
						userIdsToReplace.add(map.get("shared_by").toString());
					}
					if (map.containsKey("shared_with") && map.get("shared_with") != null) {
						userIdsToReplace.add(map.get("shared_with").toString());
					}
				}
				String uID = playlistData.get("user_id").toString();
				Map<String, Object> statusData = new HashMap<>();
				this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(), uID,
						new ArrayList<>(contentIds), statusData, true, false);

//				System.out.println("==============Step 5: ACCESS STATUS CHECK FOR PLAYLIST DATA: rootOrg - "
//						+ playlistData.get("root_org").toString() + " userId - " + uID + " =====================");

				Map<String, Object> userIdToDataMap = new HashMap<>();

//				System.out.println(
//						"==============Step 6: Replacing ANY user id WITH USER EMAIL/DATA IN RESPONSE:=====================");

				if (playlistData.containsKey("version")) {
					if (playlistData.get("version").toString().equalsIgnoreCase("v1")) {
						userIdToDataMap = userUtilService.getUserEmailsFromUserIds(
								playlistData.get("root_org").toString(), new ArrayList<>(userIdsToReplace));
					} else {
						userIdToDataMap = userUtilService.getUsersDataFromUserIds(
								playlistData.get("root_org").toString(), new ArrayList<>(userIdsToReplace),
								Arrays.asList("first_name", "last_name", "email"));
					}
				}
				for (Iterator<Map<String, Object>> iterator = userPlaylistData.iterator(); iterator.hasNext();) {
					Map<String, Object> map = iterator.next();

					// convert user_id to user_email
					if (map.containsKey("user_id")) {
						if (playlistData.containsKey("version")) {
							if (playlistData.get("version").toString().equalsIgnoreCase("v1")) {

								map.put("user_email", userIdToDataMap.get(map.get("user_id").toString()));
								map.remove("user_id");

							} else {
								Map<String, Object> result = (Map<String, Object>) userIdToDataMap
										.get(map.get("user_id").toString());
								Map<String, Object> userObject = new HashMap<>();
								if (result != null) {
									userObject.put("user_email", result.get("email"));
									userObject.put("name", this.getDisplayName(result));
									userObject.put("user_id", map.get("user_id").toString());
								}

								map.put("user", userObject);
								map.remove("user_id");
							}
						}
					}
					if (map.containsKey("shared_by")
							&& (map.get("shared_by") != null && !map.get("shared_by").toString().isEmpty())) {
						if (playlistData.containsKey("version")) {
							if (playlistData.get("version").toString().equalsIgnoreCase("v1")) {
								map.put("shared_by", userIdToDataMap.get(map.get("shared_by").toString()));
							} else {
								Map<String, Object> result = (Map<String, Object>) userIdToDataMap
										.get(map.get("shared_by").toString());

								Map<String, Object> userObject = new HashMap<>();
								if (result != null) {
									userObject.put("user_email", result.get("email"));
									userObject.put("name", this.getDisplayName(result));
									userObject.put("user_id", map.get("shared_by").toString());
								}

								map.put("shared_by", userObject);
							}
						}
					}
					if (map.containsKey("shared_with")
							&& (map.get("shared_with") != null && !map.get("shared_with").toString().isEmpty())) {
						if (playlistData.containsKey("version")) {
							if (playlistData.get("version").toString().equalsIgnoreCase("v1")) {
								map.put("shared_with", userIdToDataMap.get(map.get("shared_with").toString()));
							} else {
								Map<String, Object> result = (Map<String, Object>) userIdToDataMap
										.get(map.get("shared_with").toString());

								Map<String, Object> userObject = new HashMap<>();
								if (result != null) {
									userObject.put("user_email", result.get("email"));
									userObject.put("name", this.getDisplayName(result));
									userObject.put("user_id", map.get("shared_with").toString());
								}

								map.put("shared_with", userObject);
							}
						}
					}
					map.remove("last_updated_on");
					List<String> resourceIds = (List<String>) map.get("resource_ids");
					if (resourceIds != null && !resourceIds.isEmpty()) {
						// ********************----------------------------*****************************

						// List<Map<String, Object>> playlistResources =
						// ElasticSearchUtil.searchDataByValues(
						// LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
						// LexProjectUtil.EsType.new_lex_search.getTypeName(),
						// JsonKey.IDENTIFIER, resourceIds, resourceIds.size());

						// ********************----------------------------*****************************
						List<Map<String, Object>> playlistContent = new ArrayList<Map<String, Object>>();
						if (!statusData.isEmpty()) {
							for (String resourceId : resourceIds) {
								Map<String, Object> contentMap = new HashMap<String, Object>();
								if (statusData.containsKey(resourceId)) {
									contentMap.put("name",
											((Map<String, Object>) statusData.get(resourceId)).get("name"));
									contentMap.put("identifier",
											((Map<String, Object>) statusData.get(resourceId)).get("identifier"));
									contentMap.put("duration",
											((Map<String, Object>) statusData.get(resourceId)).get("duration"));
									contentMap.put("contentType",
											((Map<String, Object>) statusData.get(resourceId)).get("contentType"));
									contentMap.put("mimeType",
											((Map<String, Object>) statusData.get(resourceId)).get("mimeType"));
									contentMap.put("appIcon",
											((Map<String, Object>) statusData.get(resourceId)).get("appIcon"));
									contentMap.put("description",
											((Map<String, Object>) statusData.get(resourceId)).get("description"));
									contentMap.put("status",
											((Map<String, Object>) statusData.get(resourceId)).get("status"));
									contentMap.put("hasAccess",
											((Map<String, Object>) statusData.get(resourceId)).get("hasAccess"));

									if (((Map<String, Object>) statusData.get(resourceId))
											.containsKey("averageRating")) {
										Float avgRating = 0f;
										Map<String, Object> rating = (Map<String, Object>) ((Map<String, Object>) statusData
												.get(resourceId)).getOrDefault("averageRating", new HashMap<>());
										if (!rating.isEmpty()) {
											if (rating.containsKey(playlistData.get("root_org").toString())) {
												avgRating = Float.parseFloat(
														rating.get(playlistData.get("root_org").toString()).toString());
											}
										}
										contentMap.put("averageRating", avgRating);
									}
									if (((Map<String, Object>) statusData.get(resourceId)).containsKey("totalRating")) {
										Integer totalRating = 0;
										Map<String, Object> rating = (Map<String, Object>) ((Map<String, Object>) statusData
												.get(resourceId)).getOrDefault("totalRating", new HashMap<>());
										if (!rating.isEmpty()) {
											if (rating.containsKey(playlistData.get("root_org").toString())) {
												totalRating = Integer.parseInt(
														rating.get(playlistData.get("root_org").toString()).toString());
											}
										}
										contentMap.put("totalRating", totalRating);
									}
								}
								playlistContent.add(contentMap);
							}
						}
						map.put("resource", playlistContent);
						map.put("resource_ids", resourceIds);
					}
				}
			}
			contentResponse.put(Constants.RESPONSE, userPlaylistData);
		} catch (

		ProjectCommonException e) {
			ProjectLogger.log("Exception occured while processing user playlist : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		return contentResponse;
	}

	/*
	 * this method is used to create/update a playlist for a user as well as copy a
	 * shared playlist and create new playlist from copied playlist it takes as
	 * input a Map of Objects and inserts appropriate values into user_playlist as
	 * well as playlist_recent table
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Response upsertPlaylist(Map<String, Object> playlistData) throws Exception {
		Response contentResponse = new Response();
		List<String> changedResources = new ArrayList<String>();
		Map<String, Object> playlistMap = null;
		Map<String, String> tableName = null;
		Map<String, Object> objectToBeInserted = null;
		try {

			try {
				UUID.fromString(playlistData.get("user_id").toString());
			} catch (Exception e) {
				throw new InvalidDataInputException("invalid.useruuid");
			}

			if (!userUtilService.validateUser(playlistData.get("root_org").toString(),
					playlistData.get("user_id").toString())) {
				throw new InvalidDataInputException("invalid.user");
			}
			playlistMap = this.prepareUpsertDataForPlaylist(playlistData);
			tableName = (Map<String, String>) playlistMap.get("tableName");
			objectToBeInserted = (Map<String, Object>) playlistMap.get("insertion_object");
			if (objectToBeInserted.get("last_updated_on") == null) {
				throw new BadRequestException("Invalid data as input");
			}
		} catch (BadRequestException e) {
			ProjectLogger.log("Exception occured in PlaylistServiceImpl.upsertPlaylist : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}

		if (playlistData.get("user_action").toString().equalsIgnoreCase("create")) {
			changedResources = (List<String>) objectToBeInserted.get("resource_ids");
		} else {
			changedResources = (List<String>) playlistData.get("changed_resources");

			if (changedResources == null || changedResources.isEmpty()) {
				throw new InvalidDataInputException("contentToBeUpdated.emptyOrNull");
			}
		}
		if (playlistData.get("type").equals("upsert")) {
			if (playlistData.get("user_action").toString().equalsIgnoreCase("delete")) {
				contentResponse = executeBatchQueries(this.deletePlaylistResources(userPlaylistDb.getKeySpace(),
						tableName, objectToBeInserted, changedResources));
			} else {

				if (playlistData.get("user_action").toString().equalsIgnoreCase("create")) {
					this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(),
							playlistData.get("user_id").toString(), (List<String>) playlistData.get("resource_ids"),
							new HashMap<>(), false, false);
				} else {
					this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(),
							playlistData.get("user_id").toString(), changedResources, new HashMap<>(), false, false);
				}
				contentResponse = executeBatchQueries(this.insertPlaylistStatements(userPlaylistDb.getKeySpace(),
						tableName, objectToBeInserted, changedResources));
			}
		} else {

			if (objectToBeInserted.containsKey("shared_by")) {
				try {
					UUID.fromString(objectToBeInserted.get("shared_by").toString());
				} catch (Exception e) {
					throw new InvalidDataInputException("invalid.sharedByUUID");
				}
			}

			this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(),
					playlistData.get("user_id").toString(), changedResources, new HashMap<>(), false, false);
			contentResponse = executeBatchQueries(this.getQueriesForCopyingSharedPlaylist(userPlaylistDb.getKeySpace(),
					tableName, objectToBeInserted));
		}
		if (objectToBeInserted.get("playlist_id") != null) {
			contentResponse.put("playlist_id", objectToBeInserted.get("playlist_id"));
		}
		contentResponse.put(Constants.RESPONSE, "success");
		return contentResponse;
	}

	/*
	 * this method is used to fetch the resources listed in all playlists of a
	 * particular user denoted by input parameter userEmail and fetched data from
	 * playlist_recent table. this method is used for showing resources in last
	 * updated order on home page
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Response fetchRecentPlaylist(Map<String, Object> playlistData) throws Exception {
		Response contentResponse = new Response();
		try {
			if (!userUtilService.validateUser(playlistData.get("root_org").toString(),
					playlistData.get("user_id").toString())) {
				throw new InvalidDataInputException("invalid.user");
			}
			int resourceCount = (int) (playlistData.get("count"));
			contentResponse = getRecentlyAddedResources(recentPlaylistDb.getKeySpace(), recentPlaylistDb.getTableName(),
					playlistData.get("user_id").toString(), playlistData.get("root_org").toString());
			List<Map<String, Object>> userPlaylistData = (List<Map<String, Object>>) contentResponse.getResult()
					.get("response");

			List<Map<String, Object>> playlistContent = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> newPlayListContent = new ArrayList<>();
			if (userPlaylistData != null && !userPlaylistData.isEmpty()) {

				// sorting resources in last updated order i.e recently updated resources
				// in playlist
				Collections.sort(userPlaylistData, new Comparator<Map<String, Object>>() {
					@Override
					public int compare(Map<String, Object> m1, Map<String, Object> m2) {
						if (m1.get("last_updated_on") != null && m2.get("last_updated_on") != null) {
							return ((Date) m1.get("last_updated_on")).compareTo((Date) (m2.get("last_updated_on")));
						} else {
							return -1;
						}
					}
				}.reversed());

				List<String> resourceIds = new ArrayList<String>();

				int counter = 1;
				for (Map<String, Object> map : userPlaylistData) {
					map.remove("root_org");
					if (!resourceIds.contains(map.get("resource_id").toString())) {
						if (counter <= resourceCount) {
							resourceIds.add(map.get("resource_id").toString());
							counter++;
						}
					}
				}

				// ********************----------------------------*****************************
				String uID = playlistData.get("user_id").toString();
				Map<String, Object> statusData = new HashMap<>();
				this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(), uID, resourceIds,
						statusData, true, false);
//				playlistResources = ElasticSearchUtil.searchDataByValues(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
//				LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourceIds,
//				resourceIds.size());
				// ********************----------------------------*****************************

				if (!statusData.isEmpty()) {
					for (String resourceId : resourceIds) {
						Map<String, Object> contentMap = new HashMap<String, Object>();
						if (statusData.containsKey(resourceId)) {
							contentMap.putAll((Map<String, Object>) statusData.get(resourceId));

							if (contentMap.containsKey("averageRating")) {
								Float avgRating = 0f;
								Map<String, Object> rating = (Map<String, Object>) (contentMap
										.getOrDefault("averageRating", new HashMap<>()));
								if (!rating.isEmpty()) {
									if (rating.containsKey(playlistData.get("root_org").toString())) {
										avgRating = Float.parseFloat(
												rating.get(playlistData.get("root_org").toString()).toString());
									}
								}
								contentMap.put("averageRating", avgRating);
							}
							if (contentMap.containsKey("totalRating")) {
								Integer totalRating = 0;
								Map<String, Object> rating = (Map<String, Object>) (contentMap
										.getOrDefault("totalRating", new HashMap<>()));
								if (!rating.isEmpty()) {
									if (rating.containsKey(playlistData.get("root_org").toString())) {
										totalRating = Integer.parseInt(
												rating.get(playlistData.get("root_org").toString()).toString());
									}
								}
								contentMap.put("totalRating", totalRating);
							}

						} else {
							contentMap.put("identifier", resourceId);
						}
						playlistContent.add(contentMap);
					}
					// sorting to preserve the recently updated list order
					// as elastic search for all the resources at once does not preserve order
					if (playlistContent != null && playlistContent.size() > 1) {
						Collections.sort(playlistContent, new Comparator<Map<String, Object>>() {
							@Override
							public int compare(Map<String, Object> m1, Map<String, Object> m2) {
								return Integer.compare(resourceIds.indexOf(m1.get("identifier").toString()),
										resourceIds.indexOf(m2.get("identifier").toString()));
							}
						});
					}
					for (Map<String, Object> content : playlistContent) {
						if (content.keySet().size() > 1) {
							newPlayListContent.add(content);
						}
					}
				}

			}
			contentResponse.put(Constants.RESPONSE, newPlayListContent);
		} catch (ProjectCommonException e) {
			ProjectLogger.log("Exception occured in fetchRecentPlaylist : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		return contentResponse;
	}

	/*
	 * this method is used to insert rows into playlist_shared for the users with
	 * whom playlist is shared by the creator
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Response sharePlaylist(Map<String, Object> playlistData) throws Exception {
		Response contentResponse = new Response();
		String rootOrg = null;
		String sharedBy = null;
		String playlistId = null;
		List<String> playlistResourceIds = new ArrayList<String>();
		List<String> sharedWith = new ArrayList<String>();
		String visibility = null;
		String playlistTitle = null;
		Map<String, Object> responseData = new HashMap<String, Object>();
		try {
			rootOrg = ((String) playlistData.get("root_org")).trim();
			sharedBy = ((String) playlistData.get("user_id")).trim();

			try {
				UUID.fromString(sharedBy);
			} catch (Exception e) {
				throw new InvalidDataInputException("invalid.useruuid");
			}

			if (!userUtilService.validateUser(rootOrg, sharedBy)) {
				throw new InvalidDataInputException("invalid.user");
			}

			playlistId = (String) playlistData.get("playlist_id");
			playlistTitle = (String) playlistData.get("playlist_title");
			playlistResourceIds = (List<String>) playlistData.get("resource_ids");
			// ***************------------------------*********************************
			String uID = playlistData.get("user_id").toString();
			Map<String, Object> statusData = new HashMap<>();
			this.checkForAccessAndRetiredStatus(playlistData.get("root_org").toString(), uID, playlistResourceIds,
					statusData, false, false);

			// ***************------------------------*********************************
			sharedWith = (List<String>) playlistData.get("shared_with");

			List<String> invalidUsers = new ArrayList<>();
			List<String> validSharedUsers = new ArrayList<>();
			for (String userId : sharedWith) {
				try {
					UUID.fromString(userId);
				} catch (Exception e) {
					invalidUsers.add(userId);
					sharedWith.remove(userId);
				}
			}

			if (sharedWith.contains(sharedBy)) {
				if (sharedWith.size() == 1)
					throw new InvalidDataInputException("self.shared");
				else
					sharedWith.remove(sharedBy);
			}

			Map<String, Object> map = userUtilService.validateAndFetchExistingAndNewUsers(rootOrg, sharedWith);
			if (map == null || map.isEmpty()) {
				invalidUsers.addAll(sharedWith);
			} else {

				invalidUsers = (List<String>) map.get("invalid_users");
				validSharedUsers = (List<String>) map.get("valid_users");
			}
			visibility = (playlistData.get("visibility") != null ? (String) playlistData.get("visibility") : "private");
			responseData.put("InvalidUsers", invalidUsers);
			List<String> unauthorizedUsers = new ArrayList<>();
			this.checkForAccessStatus(validSharedUsers, playlistResourceIds, statusData, rootOrg);
			for (String user : validSharedUsers) {
				if (statusData.containsKey(user)) {
					if (!(boolean) statusData.get(user)) {
						unauthorizedUsers.add(user);
					}
				}
			}
			if (unauthorizedUsers.size() > 0) {
				for (String user : unauthorizedUsers) {
					if (validSharedUsers.contains(user)) {
						validSharedUsers.remove(user);
					}
				}

				Map<String, Object> userData = userUtilService.getUsersDataFromUserIds(rootOrg, unauthorizedUsers,
						Arrays.asList("first_name", "last_name", "email"));

				if (playlistData.get("version").toString().equalsIgnoreCase("v1")) {
					List<String> emails = new ArrayList<>();
					for (String uId : unauthorizedUsers) {
						if (userData.containsKey(uId)) {
							if (((Map<String, Object>) userData.get(uId)).get("email") != null
									&& !((Map<String, Object>) userData.get(uId)).get("email").toString().isEmpty()) {
								emails.add(((Map<String, Object>) userData.get(uId)).get("email").toString());
							} else {
								emails.add(uId);
							}
						}
					}
					responseData.put("UnauthorizedUsers", emails);
				} else {
					List<Map<String, Object>> unauthorizedUserDetails = new ArrayList<>();
					for (String uId : unauthorizedUsers) {
						Map<String, Object> data = new HashMap<>();
						if (userData.containsKey(uId)) {
							Map<String, Object> details = (Map<String, Object>) userData.get(uId);

							data.put("user_id", uId);
							data.put("name", this.getDisplayName(details));
							data.put("email", details.get("email"));

						} else {
							data.put("user_id", uId);
						}
						unauthorizedUserDetails.add(data);
					}
					responseData.put("UnauthorizedUsers", unauthorizedUserDetails);
				}
			}

			validSharedUsers.addAll((List<String>) map.get("new_users"));
			if (validSharedUsers != null && !validSharedUsers.isEmpty() && playlistResourceIds != null
					&& !playlistResourceIds.isEmpty()) {
				Map<String, Object> objectToBeInserted = new HashMap<String, Object>();
				Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
				Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
				objectToBeInserted.put("root_org", rootOrg);
				objectToBeInserted.put("shared_by", sharedBy);
				objectToBeInserted.put("playlist_id", UUID.fromString(playlistId));
				objectToBeInserted.put("playlist_title", playlistTitle);
				objectToBeInserted.put("shared_on", timestamp);
				objectToBeInserted.put("last_updated_on", timestamp);
				objectToBeInserted.put("resource_ids", playlistResourceIds);
				objectToBeInserted.put("shared_with", validSharedUsers);
				objectToBeInserted.put("visibility", visibility);
				Map<String, String> tableName = new HashMap<String, String>();
				tableName.put("playlistShared", sharedPlaylistDb.getTableName());
				tableName.put("userPlaylist", userPlaylistDb.getTableName());

				contentResponse = executeBatchQueries(
						this.getSharedPlaylistStatements(userPlaylistDb.getKeySpace(), tableName, objectToBeInserted));

				// put notification events in kafka
				this.putNotificationEventInKafka(rootOrg, sharedBy, validSharedUsers, playlistTitle,
						playlistData.get("message") == null ? null : playlistData.get("message").toString());

			} else {
				ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
				contentResponse.setResponseCode(responseCode);
				responseData.put("result", "failed");
				contentResponse.put(Constants.RESPONSE, responseData);
				return contentResponse;
			}

		} catch (ProjectCommonException e) {
			ProjectLogger.log("Exception occured in sharePlaylist : " + e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			contentResponse.setResponseCode(responseCode);
			return contentResponse;
		}
		responseData.put("result", "success");
		contentResponse.put(Constants.RESPONSE, responseData);
		return contentResponse;
	}

	private void putNotificationEventInKafka(String rootOrg, String sharedBy, List<String> sharedWith,
			String playListTitle, String message) {

		if (message == null || message.isEmpty())
			message = "";

		Map<String, Object> requestBody = new HashMap<>();

		requestBody.put("event-id", "share_playlist");

		Map<String, Object> tagValuePair = new HashMap<>();
		tagValuePair.put("#contentTitle", playListTitle);
//		tagValuePair.put("#targetUrl", "url");
		tagValuePair.put("#message", message);
		requestBody.put("tag-value-pair", tagValuePair);

		Map<String, List<String>> recipients = new HashMap<>();
		recipients.put("sharedWith", sharedWith);
		recipients.put("sharedBy", Arrays.asList(sharedBy));
		requestBody.put("recipients", recipients);

		String url = "http://" + this.notifSvcIp + ":" + this.notifSvcPort + "/v1/notification/event";
		HttpHeaders headers = new HttpHeaders();
		headers.set("rootOrg", rootOrg);
		try {
			restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(requestBody, headers), Void.class);
		} catch (Exception e) {
			// do nothing
		}
	}

	/*
	 * this method fetches two resource lists that is user playlists resources( that
	 * has been copied from the shared playlist ) and current resource list of
	 * source shared playlist and it returns these two list to choose what to sync
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Response fetchResourceListForSyncing(Map<String, Object> playlistMap) throws Exception {
		Response contentResponse = new Response();
		Map<String, Object> resultOutput = new HashMap<String, Object>();
		Map<String, Object> dataMap = new HashMap<String, Object>();

		if (!userUtilService.validateUser(playlistMap.get("root_org").toString(),
				playlistMap.get("user_id").toString())) {
			throw new InvalidDataInputException("invalid.user");
		}
		if (playlistMap.get("source_playlist_id") != null && playlistMap.get("source_playlist_id").toString() != "") {
			dataMap.put("root_org", playlistMap.get("root_org"));
			dataMap.put("user_id", playlistMap.get("user_id"));
			dataMap.put("playlist_id", UUID.fromString(playlistMap.get("playlist_id").toString()));
			List<String> resourcesInUserPlaylist = this.fetchResourcesInPlaylist(userPlaylistDb.getKeySpace(),
					userPlaylistDb.getTableName(), dataMap);
			dataMap.put("user_id", playlistMap.get("shared_by").toString());
			dataMap.put("playlist_id", UUID.fromString(playlistMap.get("source_playlist_id").toString()));
			List<String> resourcesInSharedPlaylist = this.fetchResourcesInPlaylist(userPlaylistDb.getKeySpace(),
					userPlaylistDb.getTableName(), dataMap);
			List<Map<String, Object>> userPlaylistResources = ElasticSearchUtil.searchDataByValues(
					LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
					LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, resourcesInUserPlaylist,
					resourcesInUserPlaylist.size());
			boolean isSame = false;
			if (resourcesInUserPlaylist.size() == resourcesInSharedPlaylist.size()) {
				if (resourcesInUserPlaylist.containsAll(resourcesInSharedPlaylist)
						&& resourcesInSharedPlaylist.containsAll(resourcesInUserPlaylist)) {
					isSame = true;
				}
			}
			resultOutput.put("isSame", isSame);
			List<Map<String, Object>> userPlaylistContent = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> sharedPlaylistContent = null;
			for (Map<String, Object> resourceMap : userPlaylistResources) {
				Map<String, Object> contentMap = new HashMap<String, Object>();
				contentMap.put("resource_name", resourceMap.get("name"));
				contentMap.put("resource_id", resourceMap.get("identifier"));
				contentMap.put("time_duration", resourceMap.get("duration"));
				contentMap.put("status", resourceMap.get("status"));

				if (resourceMap.containsKey("averageRating")) {
					Float avgRating = 0f;
					Map<String, Object> rating = (Map<String, Object>) (resourceMap.getOrDefault("averageRating",
							new HashMap<>()));
					if (!rating.isEmpty()) {
						if (rating.containsKey(playlistMap.get("root_org").toString())) {
							avgRating = Float.parseFloat(rating.get(playlistMap.get("root_org").toString()).toString());
						}
					}
					contentMap.put("averageRating", avgRating);
				}
				if (resourceMap.containsKey("totalRating")) {
					Integer totalRating = 0;
					Map<String, Object> rating = (Map<String, Object>) (resourceMap.getOrDefault("totalRating",
							new HashMap<>()));
					if (!rating.isEmpty()) {
						if (rating.containsKey(playlistMap.get("root_org").toString())) {
							totalRating = Integer
									.parseInt(rating.get(playlistMap.get("root_org").toString()).toString());
						}
					}
					contentMap.put("totalRating", totalRating);
				}
				userPlaylistContent.add(contentMap);
			}
			if (!isSame) {
				sharedPlaylistContent = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> sharedPlaylistResources = ElasticSearchUtil.searchDataByValues(
						LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
						LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER,
						resourcesInSharedPlaylist, resourcesInSharedPlaylist.size());

				List<String> notInUserPlaylist = new ArrayList<>();
				List<String> notInSharedPlaylist = new ArrayList<>();
				// not in user playlist but in shared playlist
				for (String resourceId : resourcesInSharedPlaylist) {
					if (!resourcesInUserPlaylist.contains(resourceId)) {
						notInUserPlaylist.add(resourceId);
					}
				}
				// in user playlist but not in shared playlist
				for (String resourceId : resourcesInUserPlaylist) {
					if (!resourcesInSharedPlaylist.contains(resourceId)) {
						notInSharedPlaylist.add(resourceId);
					}
				}
				for (Map<String, Object> sharedResourceMap : sharedPlaylistResources) {
					Map<String, Object> contentMap = new HashMap<String, Object>();
					contentMap.put("resource_name", sharedResourceMap.get("name"));
					contentMap.put("resource_id", sharedResourceMap.get("identifier"));
					contentMap.put("time_duration", sharedResourceMap.get("duration"));
					contentMap.put("status", sharedResourceMap.get("status"));

					if (sharedResourceMap.containsKey("averageRating")) {
						Float avgRating = 0f;
						Map<String, Object> rating = (Map<String, Object>) (sharedResourceMap
								.getOrDefault("averageRating", new HashMap<>()));
						if (!rating.isEmpty()) {
							if (rating.containsKey(playlistMap.get("root_org").toString())) {
								avgRating = Float
										.parseFloat(rating.get(playlistMap.get("root_org").toString()).toString());
							}
						}
						contentMap.put("averageRating", avgRating);
					}
					if (sharedResourceMap.containsKey("totalRating")) {
						Integer totalRating = 0;
						Map<String, Object> rating = (Map<String, Object>) (sharedResourceMap
								.getOrDefault("totalRating", new HashMap<>()));
						if (!rating.isEmpty()) {
							if (rating.containsKey(playlistMap.get("root_org").toString())) {
								totalRating = Integer
										.parseInt(rating.get(playlistMap.get("root_org").toString()).toString());
							}
						}
						contentMap.put("totalRating", totalRating);
					}

					sharedPlaylistContent.add(contentMap);
				}
				resultOutput.put("shared_resources", sharedPlaylistContent);
				resultOutput.put("notInUser", notInUserPlaylist);
				resultOutput.put("notInShared", notInSharedPlaylist);

			} else {
				// both lists are same
				sharedPlaylistContent = userPlaylistContent;
			}
			resultOutput.put("user_resources", userPlaylistContent);
		} else {
			resultOutput.put("message", "Sync operation not possible since source playlist has been deleted!");
		}
		contentResponse.put(Constants.RESPONSE, resultOutput);
		return contentResponse;
	}

	/*
	 * this method is used t o delete a playlist that has been shared with a user
	 * but ignored by the user
	 */
	@Override
	public Response deleteSharedPlaylist(Map<String, Object> playlistMap) {
		Response resp = new Response();
		resp = this.deleteIgnoredSharedPlaylist(sharedPlaylistDb.getKeySpace(), sharedPlaylistDb.getTableName(),
				playlistMap);
		return resp;
	}

	/*
	 * not used moved to userdatautil
	 * 
	 * private Map<String, Object> verifyUsers(List<String> emails) { Set<String>
	 * users = new HashSet<String>(); Set<String> userEmails = new
	 * HashSet<String>(); ArrayList<String> validUsers = new ArrayList<>(); if
	 * (emails != null && !emails.isEmpty()) { // removing duplicates from list by
	 * using a Set<String> for (String email : emails) { if (email.contains("@")) {
	 * users.add(email.toLowerCase().substring(0, email.indexOf("@"))); }
	 * userEmails.add(email.toLowerCase()); } List<Map<String, Object>>
	 * userGraphData = new UserDataUtil() .getUsersFromActiveDirectory(new
	 * ArrayList<String>(users)); if (userGraphData != null &&
	 * !userGraphData.isEmpty()) { for (Map<String, Object> map : userGraphData) {
	 * if (userEmails.contains(map.get("onPremisesUserPrincipalName").toString().
	 * toLowerCase())) {
	 * validUsers.add(map.get("onPremisesUserPrincipalName").toString().toLowerCase(
	 * )); } } } userEmails.removeAll(validUsers); } Map<String, Object>
	 * userValidityMap = new HashMap<String, Object>();
	 * userValidityMap.put("valid_users", validUsers);
	 * userValidityMap.put("invalid_users", userEmails); return userValidityMap; }
	 */

	/*
	 * this method generates a list of statement where one upsert is performed by
	 * removing resources from list of resources in user_playlist tables and
	 * resources are deleted from playlist_recent table
	 * 
	 */

	private List<Statement> deletePlaylistResources(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap, List<String> changedResources) {
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(getPlaylistInsertStmt(keyspaceName, tableName.get("userPlaylist"), playlistMap));
		statements.addAll(deleteRecentPlaylistStatements(keyspaceName, tableName, playlistMap, changedResources));
		return statements;
	}

	/*
	 * this method is used to generate a list of insert statements for inserting
	 * into playlist_recent. For multiple resource in a playlist multiple resources
	 * are created.
	 */
	private List<Statement> insertPlaylistStatements(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap, List<String> changedResources) {
		List<Statement> statements = new ArrayList<Statement>();
		statements.add(this.getPlaylistInsertStmt(keyspaceName, tableName.get("userPlaylist"), playlistMap));
		for (String resourceId : changedResources) {
			Insert insertRecentPlaylistStmt = QueryBuilder.insertInto(keyspaceName, tableName.get("playlistRecent"))
					.value("root_org", playlistMap.get("root_org")).value("user_id", playlistMap.get("user_id"))
					.value("playlist_id", playlistMap.get("playlist_id")).value("resource_id", resourceId)
					.value("last_updated_on", playlistMap.get("last_updated_on"));
			statements.add(insertRecentPlaylistStmt);
		}
		return statements;
	}

	/*
	 * this method is used to get update statement to update the copied playlist
	 * with updated resource ids after syncing
	 */
	private List<String> fetchResourcesInPlaylist(String keyspaceName, String tableName,
			Map<String, Object> playlistMap) {
		List<String> currentResourceList = null;
		try {
			Select.Where select = QueryBuilder.select().column("resource_ids").from(keyspaceName, tableName)
					.where(eq("root_org", playlistMap.get("root_org"))).and(eq("user_id", playlistMap.get("user_id")))
					.and(eq("playlist_id", playlistMap.get("playlist_id")));

			ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(select);
			currentResourceList = results.one().getList("resource_ids", String.class);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return currentResourceList;

	}

	@SuppressWarnings("unchecked")
	private List<Statement> getInsertStmtForSharedPlaylist(String keyspaceName, String tableName,
			Map<String, Object> playlistMap) {
		List<Statement> statements = new ArrayList<Statement>();
		for (String id : (List<String>) playlistMap.get("shared_with")) {
			Insert insertSharedPlaylist = QueryBuilder.insertInto(keyspaceName, tableName)
					.value("root_org", playlistMap.get("root_org")).value("shared_with", id.toLowerCase())
					.value("playlist_id", playlistMap.get("playlist_id"))
					.value("resource_ids", playlistMap.get("resource_ids"))
					.value("playlist_title", playlistMap.get("playlist_title"))
					.value("shared_by", playlistMap.get("shared_by")).value("shared_on", playlistMap.get("shared_on"))
					.value("visibility", playlistMap.get("visibility"))
					.value("last_updated_on", playlistMap.get("last_updated_on"));
			statements.add(insertSharedPlaylist);
		}
		return statements;
	}

	private List<Statement> getSharedPlaylistStatements(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap) {
		List<Statement> statements = this.getInsertStmtForSharedPlaylist(keyspaceName,
				tableName.get("playlistShared").toString(), playlistMap);
		Update.Where updateStmt = QueryBuilder.update(keyspaceName, tableName.get("userPlaylist"))
				.with(QueryBuilder.set("isshared", 1)).where(eq("root_org", playlistMap.get("root_org")))
				.and(eq("user_id", playlistMap.get("shared_by")))
				.and(eq("playlist_id", playlistMap.get("playlist_id")));
		statements.add(updateStmt);
		return statements;

	}

	/*
	 * this method returns an insert statement to create a new playlist
	 */
	private Statement getPlaylistInsertStmt(String keyspaceName, String tableName, Map<String, Object> playlistMap) {
		Insert insertPlaylistStmt = QueryBuilder.insertInto(keyspaceName, tableName);
		for (Entry<String, Object> entry : playlistMap.entrySet()) {
			if (entry.getValue() != null) {
				insertPlaylistStmt.value(entry.getKey(), entry.getValue());
			}
		}
		return insertPlaylistStmt;

	}

	/*
	 * this method is used to generate a list of delete statements to be executed as
	 * batch query on cassandra for deleting a particular playlist of a user
	 */
	private List<Statement> getDeletePlaylistStmts(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap) {
		List<Statement> statements = new ArrayList<Statement>();

		if (!userUtilService.validateUser(playlistMap.get("root_org").toString(),
				playlistMap.get("user_id").toString())) {
			throw new InvalidDataInputException("invalid.user");
		}
		Delete.Where deleteUserPlaylist = QueryBuilder.delete().from(keyspaceName, tableName.get("userPlaylist"))
				.where(eq("root_org", playlistMap.get("root_org"))).and(eq("user_id", playlistMap.get("user_id")))
				.and(eq("playlist_id", playlistMap.get("playlist_id")));
		statements.add(deleteUserPlaylist);
		Delete.Where deleteRecentPlaylist = QueryBuilder.delete().from(keyspaceName, tableName.get("playlistRecent"))
				.where(eq("root_org", playlistMap.get("root_org"))).and(eq("user_id", playlistMap.get("user_id")))
				.and(eq("playlist_id", playlistMap.get("playlist_id")));
		statements.add(deleteRecentPlaylist);
		statements.addAll(this.getUpdateSourcePlaylistIdStmt(keyspaceName, tableName.get("userPlaylist"),
				(UUID) playlistMap.get("playlist_id"), playlistMap.get("root_org").toString()));
		return statements;
	}

	/*
	 * this method is used to generate update statements for updating
	 * source_playlist_id to null in case a shared playlist is deleted by the owner
	 * of shared playlist.
	 */
	private List<Statement> getUpdateSourcePlaylistIdStmt(String keyspaceName, String tableName, UUID playlistId,
			String rootOrg) {
		List<Statement> statements = new ArrayList<Statement>();
		// fetch the list of useremail and playlist id who have copied shared playlist
		// for their use
		Selection selection = QueryBuilder.select();
		// columns to fetch
		selection.column("root_org");
		selection.column("user_id");
		selection.column("playlist_id");
		Select selectQuery = selection.from(keyspaceName, tableName);
		Where selectWhere = selectQuery.where(eq("source_playlist_id", playlistId));
		ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(selectWhere);
		Iterator<Row> rowIterator = results.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Update.Where updateStmt = QueryBuilder.update(keyspaceName, tableName)
					.with(QueryBuilder.set("source_playlist_id", null)).where(eq("root_org", row.getString("root_org")))
					.and(eq("user_id", row.getString("user_id"))).and(eq("playlist_id", row.getUUID("playlist_id")));
			statements.add(updateStmt);
		}
		return statements;
	}

	/*
	 * this method fetches playlist data based on keyspace name and table name three
	 * tables it refers to are user_playlist(stores self created playlist of user)
	 * playlist_shared (playlist shared with a user by his friends and colleagues)
	 */

	private Response getUserPlaylist(String keyspaceName, String tableName, String keyValue, String rootOrg) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service getUserPlaylist method started for " + tableName + " at ==" + startTime,
				LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Select select = QueryBuilder.select().all().from(keyspaceName, tableName);
			if (!tableName.equalsIgnoreCase(sharedPlaylistDb.getTableName())) {
				select.where(eq("root_org", rootOrg)).and(eq("user_id", keyValue));
//				select.where(eq("user_email", keyValue));
			} else {
				select.where(eq("root_org", rootOrg)).and(eq("shared_with", keyValue));
			}
			ProjectLogger.log("Query: " + select, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(keyspaceName).execute(select);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Cassandra Service getUserPlaylist method end at ==" + stopTime + " ,Total time elapsed = "
				+ elapsedTime, LoggerEnum.PERF_LOG);
		return response;
	}

	/*
	 * this method is used to execute multiple queries as a batch statement thereby
	 * ensuring atomicity.
	 */
	private Response executeBatchQueries(List<Statement> statements) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log("Cassandra Service executeBatchQuery method in playlist  started at ==" + startTime,
				LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			final BatchStatement batchStatement = new BatchStatement();
			for (Statement stmt : statements) {
				batchStatement.add(stmt);
			}
			connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(batchStatement);
			response.put(Constants.RESPONSE, Constants.SUCCESS);
		} catch (Exception e) {
			ProjectLogger
					.log("Exception occurred while executing batch queries in PlaylistServiceImpl.executeBatchQueries "
							+ " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Cassandra Service executeBatchQueries method end at ==" + stopTime
				+ " ,Total time elapsed = " + elapsedTime, LoggerEnum.PERF_LOG);
		return response;

	}

	/*
	 * this method is used to generate delete statements to be executed as batch
	 * query for cassandra regarding playlist_recent
	 */
	private List<Statement> deleteRecentPlaylistStatements(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap, List<String> changedResources) {
		List<Statement> statements = new ArrayList<Statement>();
		for (String resourceId : changedResources) {
			Delete.Where deleteRecentPlaylist = QueryBuilder.delete()
					.from(keyspaceName, tableName.get("playlistRecent"))
					.where(eq("root_org", playlistMap.get("root_org"))).and(eq("user_id", playlistMap.get("user_id")))
					.and(eq("playlist_id", UUID.fromString(playlistMap.get("playlist_id").toString())))
					.and(eq("resource_id", resourceId));
			statements.add(deleteRecentPlaylist);
		}

		return statements;

	}

	/*
	 * this method generate queries for copying a shared playlist(i.e. accepting a
	 * shared playlist) to make a personal playlist i.e. insertion statments and
	 * deletion statement
	 * 
	 */
	@SuppressWarnings("unchecked")
	private List<Statement> getQueriesForCopyingSharedPlaylist(String keyspaceName, Map<String, String> tableName,
			Map<String, Object> playlistMap) {
		List<Statement> stmtList = new ArrayList<>();
		stmtList.add(this.deletePlaylistSharedWithUser(keyspaceName, tableName.get("playlistShared"), playlistMap));
		Select.Where select = QueryBuilder.select().column("user_id").from(keyspaceName, tableName.get("userPlaylist"))
				.where((eq("root_org", playlistMap.get("root_org")))).and((eq("user_id", playlistMap.get("shared_by"))))
				.and(eq("playlist_id", playlistMap.get("source_playlist_id")));
		ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(select);
		if (results.all().size() < 1) {
			playlistMap.remove("source_playlist_id");
		}
		stmtList.addAll(this.insertPlaylistStatements(keyspaceName, tableName, playlistMap,
				(List<String>) playlistMap.get("resource_ids")));
		return stmtList;
	}

	/*
	 * this method is used to delete a record from shared playlist of an user after
	 * an user accepts the shared playlist and thus it becomes part of his/her
	 * playlist.
	 * 
	 */

	private Statement deletePlaylistSharedWithUser(String keyspaceName, String tableName,
			Map<String, Object> playlistMap) {

		Delete.Where deleteStmt = QueryBuilder.delete().from(keyspaceName, tableName)
				.where(eq("root_org", playlistMap.get("root_org").toString()))
				.and(eq("shared_with", playlistMap.get("user_id").toString()))
				.and(eq("playlist_id", playlistMap.get("source_playlist_id")));
		return deleteStmt;
	}

	/*
	 * this method prepares a map object that contains all data that needs to be
	 * inserted while creating/updating a playlist or copying a shared playlist
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> prepareUpsertDataForPlaylist(Map<String, Object> playlistData) throws Exception {
		Map<String, Object> upsertMap = new HashMap<String, Object>();
		Map<String, String> tableName = new HashMap<String, String>();
		tableName.put("userPlaylist", userPlaylistDb.getTableName());
		tableName.put("playlistRecent", recentPlaylistDb.getTableName());
		tableName.put("playlistShared", sharedPlaylistDb.getTableName());
		upsertMap.put("tableName", tableName);

		// removing duplicate elements if any
		List<String> playlistResourceIds = ((List<String>) playlistData.get("resource_ids")).stream().distinct()
				.collect(Collectors.toList());
		Map<String, Object> objectToBeInserted = new HashMap<String, Object>();
		if (playlistResourceIds != null && !playlistResourceIds.isEmpty()
				&& playlistData.get("playlist_title") != null) {

			Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(ProjectUtil.getFormattedDate());
			Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());
			objectToBeInserted.put("root_org", playlistData.get("root_org"));
			objectToBeInserted.put("user_id", playlistData.get("user_id"));

			if (playlistData.get("playlist_id") != null) {
				objectToBeInserted.put("playlist_id", UUID.fromString(playlistData.get("playlist_id").toString()));
			} else {
				objectToBeInserted.put("playlist_id", UUID.fromString(ProjectUtil.generateUniqueId()));
				objectToBeInserted.put("created_on", timestamp);
			}
			if (playlistData.get("playlist_title") != null) {
				objectToBeInserted.put("playlist_title", playlistData.get("playlist_title").toString());
			}
			objectToBeInserted.put("last_updated_on", timestamp);
			objectToBeInserted.put("resource_ids", playlistResourceIds);
			objectToBeInserted.put("shared_by", playlistData.get("shared_by"));
			objectToBeInserted.put("source_playlist_id",
					playlistData.get("source_playlist_id") != null
							? UUID.fromString(playlistData.get("source_playlist_id").toString())
							: null);
			objectToBeInserted.put("isShared", playlistData.get("isShared"));
			objectToBeInserted.put("visibility",
					playlistData.get("visibility") != null ? playlistData.get("visibility") : "private");
			upsertMap.put("insertion_object", objectToBeInserted);
		} else {
			throw new BadRequestException("Invalid data as input");
		}
		return upsertMap;
	}

	/*
	 * this method deleted those playlists that has been shared with a user but
	 * rejected by the user
	 */
	private Response deleteIgnoredSharedPlaylist(String keyspaceName, String tableName,
			Map<String, Object> playlistMap) {
		Response response = new Response();
		try {
			if (!userUtilService.validateUser(playlistMap.get("root_org").toString(),
					playlistMap.get("user_id").toString())) {
				throw new InvalidDataInputException("invalid.user");
			}
			Delete.Where deleteStmt = QueryBuilder.delete().from(keyspaceName, tableName)
					.where(eq("root_org", playlistMap.get("root_org").toString()))
					.and(eq("shared_with", playlistMap.get("user_id").toString()))
					.and(eq("playlist_id", playlistMap.get("playlist_id")));
			connectionManager.getSession(keyspaceName).execute(deleteStmt);
			response.put(Constants.RESPONSE, Constants.SUCCESS);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_DELETE + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		return response;
	}

	/*
	 * this method fetches resources that has been added recently across playlists
	 * of a user i.e. recently added playlist resources for home page
	 */

	private Response getRecentlyAddedResources(String keyspaceName, String tableName, String keyValue, String rootOrg) {
		long startTime = System.currentTimeMillis();
		ProjectLogger.log(
				"Playlist service getRecentlyAddedResources method started for " + tableName + " at ==" + startTime,
				LoggerEnum.PERF_LOG);
		Response response = new Response();
		try {
			Selection selection = QueryBuilder.select();
			// columns to fetch
			selection.column("resource_id");
			selection.column("last_updated_on");
			Select selectQuery = selection.from(keyspaceName, tableName);
			Where selectWhere = selectQuery.where(eq("root_org", rootOrg)).and(eq("user_id", keyValue));
			ProjectLogger.log("Query: " + selectWhere, LoggerEnum.DEBUG);
			ResultSet results = connectionManager.getSession(keyspaceName).execute(selectWhere);
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
			throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
					ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		ProjectLogger.log("Playlist service getRecentlyAddedResources method end at ==" + stopTime
				+ " ,Total time elapsed = " + elapsedTime, LoggerEnum.PERF_LOG);
		return response;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getContentIdsForPlayList(String userId, String playlistId) throws Exception {

		List<String> ret = new ArrayList<>();

		Select select = QueryBuilder.select().column("resource_ids")
				.from(userPlaylistDb.getKeySpace(), userPlaylistDb.getTableName()).limit(1);
		Where selectWhere = select.where(eq("user_email", userId)).and(eq("playlist_id", UUID.fromString(playlistId)));
		ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(selectWhere);

		for (Row row : results.all()) {
			ret = (List<String>) row.getObject("resource_ids");
		}
		return ret;
	}

	/**
	 * @param uID
	 * @param contentList
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked" })
	private void checkForAccessAndRetiredStatus(String rootOrg, String uID, List<String> contentList,
			Map<String, Object> statusData, boolean isDataRequired, boolean isListDataRequired) throws Exception {

		List<Map<String, Object>> playlistResources = ElasticSearchUtil.searchDataByValues(
				LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
				LexProjectUtil.EsType.new_lex_search.getTypeName(), JsonKey.IDENTIFIER, contentList,
				contentList.size());
		if (!isDataRequired) {
			for (Map<String, Object> data : playlistResources) {
				if ("Channel".equalsIgnoreCase(data.get("contentType").toString())
						|| "Knowledge Board".equalsIgnoreCase(data.get("contentType").toString())
						|| "Learning Journeys".equalsIgnoreCase(data.get("contentType").toString())) {
					throw new InvalidDataInputException("invalid.content");
				}
			}
		}
		if (playlistResources != null && !playlistResources.isEmpty()) {
			for (Map<String, Object> data : playlistResources) {
				statusData.put(data.get("identifier").toString(), data);
			}
			UUID uuid = UUID.randomUUID();
			Response accessDataResponse = accessService.getContentUserAccess(uID, contentList, rootOrg, uuid);
			Map<String, Object> result = accessDataResponse.getResult();
			Map<String, Object> accessData = (Map<String, Object>) result.get("response");

			for (String content : contentList) {
				if (statusData.containsKey(content)) {
					Map<String, Object> meta = (Map<String, Object>) statusData.get(content);
					boolean hasAccess = false;
					if (((Map<String, Object>) accessData.get(content)) == null
							|| ((Map<String, Object>) accessData.get(content)).isEmpty()) {
						hasAccess = false;
					} else {
						hasAccess = (boolean) ((Map<String, Object>) accessData.get(content)).get("hasAccess");
					}
					if (!isDataRequired && !hasAccess) {
						throw new InvalidDataInputException("content.accessrestricted");
					} else if (!isDataRequired && meta.get("status").toString().equalsIgnoreCase("deleted")) {
						throw new InvalidDataInputException("content.deleted");
					} else if (!isDataRequired && meta.get("status").toString().equalsIgnoreCase("expired")) {
						throw new InvalidDataInputException("content.expired");
					} else if (meta.get("status").toString().equalsIgnoreCase("live")
							|| meta.get("status").toString().equalsIgnoreCase("marked for deletion")) {

						if (meta.containsKey("expiryDate") && meta.get("expiryDate") != null
								&& !meta.get("expiryDate").toString().isEmpty()) {
							SimpleDateFormat formatterDateTime = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
							Date expiryDate = formatterDateTime.parse(meta.get("expiryDate").toString());
							Date currentDate = new Date();

							if (!isDataRequired && expiryDate.before(currentDate)) {
								throw new InvalidDataInputException("content.expired");
							} else if (isDataRequired && expiryDate.before(currentDate)) {
								meta.put("status", "Expired");
							}
						}
					}
					meta.put("hasAccess", hasAccess);
				} else {
					if (!isDataRequired) {
						throw new InvalidDataInputException("invalid.LexId");
					}
				}
			}
			if (isListDataRequired) {
				List<Map<String, Object>> listData = new ArrayList<>();
				for (String contentId : statusData.keySet()) {
					listData.add((Map<String, Object>) statusData.get(contentId));
				}
				statusData.clear();
				statusData.put("meta", listData);
			}
		} else {
			if (contentList != null && !contentList.isEmpty()) {
				throw new InvalidDataInputException("content.metaNotFound");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void checkForAccessStatus(List<String> uID, List<String> contentList, Map<String, Object> statusData,
			String rootOrg) throws Exception {

		List<Map<String, Object>> allContentResponse = new ArrayList<>();
		Map<String, Object> requestBody = new HashMap<>();
		for (String user : uID) {
			requestBody.put(user, contentList);
		}
		UUID uuid = UUID.randomUUID();
		Response accessResponseData = accessService.getContentUsersAccess(requestBody, rootOrg, uuid);
		Map<String, Object> userAccessReponse = (Map<String, Object>) accessResponseData.get("response");
		for (String user : userAccessReponse.keySet()) {
			Map<String, Object> data = new HashMap<>();
			data.put(user, userAccessReponse.get(user));

			allContentResponse.add(data);
		}

		for (Map<String, Object> eachUserData : allContentResponse) {
			String userId = (String) eachUserData.keySet().toArray()[0];
			Map<String, Object> accessData = (Map<String, Object>) eachUserData.get(userId);

			boolean hasAccess = true;
			for (String content : contentList) {
				if (accessData.containsKey(content)) {
					hasAccess = (boolean) ((Map<String, Object>) accessData.get(content)).get("hasAccess");
				} else {
					hasAccess = false;
				}
				if (!hasAccess) {
					break;
				}
			}
			statusData.put(userId, hasAccess);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getPlayListById_v1(String rootOrg, String userId, String playlistId) throws Exception {

		List<String> ret = new ArrayList<>();
		if (!userUtilService.validateUser(rootOrg, userId)) {
			throw new InvalidDataInputException("invalid.user");
		}
		Select select = QueryBuilder.select().all().from(userPlaylistDb.getKeySpace(), userPlaylistDb.getTableName())
				.limit(1);
		Where selectWhere = select.where(eq("root_org", rootOrg)).and(eq("user_id", userId))
				.and(eq("playlist_id", UUID.fromString(playlistId)));
		ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(selectWhere);

		Map<String, Object> finalMap = new HashMap<>();
		String sharedBy = "";
		for (Row row : results.all()) {
			ret = (List<String>) row.getObject("resource_ids");
			finalMap.put("isShared", row.getObject("isShared"));
			finalMap.put("visibility", row.getObject("visibility"));
			finalMap.put("created_on", row.getObject("created_on"));
			finalMap.put("playlist_title", row.getObject("playlist_title"));
			finalMap.put("source_playlist_id", row.getObject("source_playlist_id"));
			finalMap.put("playlist_id", row.getObject("playlist_id"));
			if (row.getObject("shared_by") != null && !row.getObject("shared_by").toString().isEmpty()) {
				sharedBy = row.getObject("shared_by").toString();
			} else {
				finalMap.put("shared_by", row.getObject("shared_by"));
			}
		}

		Map<String, Object> usersDataToFetch = new HashMap<>();
		List<String> users = new ArrayList<>();
		users.add(userId);
		usersDataToFetch.put("user_email", userId);
		usersDataToFetch.put("shared_by", sharedBy);
		if (sharedBy != "") {
			users.add(sharedBy);
			usersDataToFetch.put("shared_by", sharedBy);
		}

		Map<String, Object> emailMap = userUtilService.getUserEmailsFromUserIds(rootOrg, users);
		for (String key : usersDataToFetch.keySet()) {
			if (emailMap.containsKey(usersDataToFetch.get(key))) {
				if (emailMap.get(usersDataToFetch.get(key)) != null
						&& !emailMap.get(usersDataToFetch.get(key)).toString().isEmpty()) {
					finalMap.put(key, emailMap.get(usersDataToFetch.get(key)).toString());
				} else {
					finalMap.put(key, "");
				}
			} else {
				finalMap.put(key, "");
			}
		}
		Map<String, Object> statusData = new HashMap<>();
		this.checkForAccessAndRetiredStatus(rootOrg, userId, ret, statusData, true, true);

		List<Map<String, Object>> resultData = new ArrayList<>();
		String[] requiredFields = { "appIcon", "artifactUrl", "children", "complexityLevel", "contentType",
				"creatorContacts", "description", "downloadUrl", "duration", "identifier", "isExternal",
				"lastUpdatedOn", "learningMode", "learningObjective", "me_totalSessionsCount", "mimeType", "name",
				"resourceCategory", "resourceType", "sourceName", "status", "hasAccess", "averageRating",
				"totalRating" };
		if (statusData != null && !statusData.isEmpty()) {
			for (Map<String, Object> contentData : (List<Map<String, Object>>) statusData.get("meta")) {
				if (contentData.keySet().size() > 1) {
					Map<String, Object> requiredData = new HashMap<>();
					for (String field : requiredFields) {
						if (field.equalsIgnoreCase("averageRating")) {
							Float avgRating = 0f;
							Map<String, Object> rating = (Map<String, Object>) contentData.getOrDefault(field,
									new HashMap<>());
							if (!rating.isEmpty()) {
								if (rating.containsKey(rootOrg)) {
									avgRating = Float.parseFloat(rating.get(rootOrg).toString());
								}
							}
							requiredData.put("averageRating", avgRating);
						} else if (field.equalsIgnoreCase("totalRating")) {
							Integer totalRating = 0;
							Map<String, Object> rating = (Map<String, Object>) contentData.getOrDefault(field,
									new HashMap<>());
							if (!rating.isEmpty()) {
								if (rating.containsKey(rootOrg)) {
									totalRating = Integer.parseInt(rating.get(rootOrg).toString());
								}
							}
							requiredData.put("totalRating", totalRating);
						} else {
							requiredData.put(field, contentData.getOrDefault(field, ""));
						}
					}
					resultData.add(requiredData);
				}
			}
			finalMap.put("resource", resultData);
			finalMap.put("resource_ids", ret);
		}
		return finalMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getPlayListById(String rootOrg, String userId, String playlistId) throws Exception {

		List<String> ret = new ArrayList<>();
		if (!userUtilService.validateUser(rootOrg, userId)) {
			throw new InvalidDataInputException("invalid.user");
		}
		Select select = QueryBuilder.select().all().from(userPlaylistDb.getKeySpace(), userPlaylistDb.getTableName())
				.limit(1);
		Where selectWhere = select.where(eq("root_org", rootOrg)).and(eq("user_id", userId))
				.and(eq("playlist_id", UUID.fromString(playlistId)));
		ResultSet results = connectionManager.getSession(userPlaylistDb.getKeySpace()).execute(selectWhere);

		Map<String, Object> finalMap = new HashMap<>();
		String sharedBy = "";
		for (Row row : results.all()) {
			ret = (List<String>) row.getObject("resource_ids");
			finalMap.put("isShared", row.getObject("isShared"));
			finalMap.put("visibility", row.getObject("visibility"));
			finalMap.put("created_on", row.getObject("created_on"));
			finalMap.put("playlist_title", row.getObject("playlist_title"));
			finalMap.put("source_playlist_id", row.getObject("source_playlist_id"));
			finalMap.put("playlist_id", row.getObject("playlist_id"));
			if (row.getObject("shared_by") != null && !row.getObject("shared_by").toString().isEmpty()) {
				sharedBy = row.getObject("shared_by").toString();
			} else {
				finalMap.put("shared_by", row.getObject("shared_by"));
			}
		}
		Map<String, Object> usersDataToFetch = new HashMap<>();
		List<String> users = new ArrayList<>();

		users.add(userId);
		usersDataToFetch.put("user", userId);
		usersDataToFetch.put("shared_by", sharedBy);
		if (sharedBy != "") {
			users.add(sharedBy);
			usersDataToFetch.put("shared_by", sharedBy);
		}

		Map<String, Object> result = new HashMap<>();
		result = userUtilService.getUsersDataFromUserIds(rootOrg, users,
				Arrays.asList("first_name", "last_name", "email"));
		for (String key : usersDataToFetch.keySet()) {
			Map<String, Object> requiredData = new HashMap<>();
			if (result.containsKey(usersDataToFetch.get(key))) {
				Map<String, Object> userData = (Map<String, Object>) result.get(usersDataToFetch.get(key));
				if (userData != null && !userData.isEmpty()) {

					requiredData.put("user_id", usersDataToFetch.get(key));
					requiredData.put("name", this.getDisplayName(userData));
					requiredData.put("user_email", userData.get("email"));

				} else {
					requiredData.put("user_id", usersDataToFetch.get(key));
				}
				finalMap.put(key, requiredData);
			} else {
				finalMap.put(key, requiredData);
			}
		}

		Map<String, Object> statusData = new HashMap<>();
		this.checkForAccessAndRetiredStatus(rootOrg, userId, ret, statusData, true, false);

		List<Map<String, Object>> resultData = new ArrayList<>();
		String[] requiredFields = { "appIcon", "artifactUrl", "children", "complexityLevel", "contentType",
				"creatorContacts", "description", "downloadUrl", "duration", "identifier", "isExternal",
				"lastUpdatedOn", "learningMode", "learningObjective", "me_totalSessionsCount", "mimeType", "name",
				"resourceCategory", "resourceType", "sourceName", "status", "hasAccess", "averageRating",
				"totalRating" };
		if (statusData != null && !statusData.isEmpty()) {
			for (String content : ret) {
				if (statusData.containsKey(content)) {
					Map<String, Object> contentData = (Map<String, Object>) statusData.get(content);
					if (contentData.keySet().size() > 1) {
						Map<String, Object> requiredData = new HashMap<>();
						for (String field : requiredFields) {
							if (field.equalsIgnoreCase("averageRating")) {
								Float avgRating = 0f;
								Map<String, Object> rating = (Map<String, Object>) contentData.getOrDefault(field,
										new HashMap<>());
								if (!rating.isEmpty()) {
									if (rating.containsKey(rootOrg)) {
										avgRating = Float.parseFloat(rating.get(rootOrg).toString());
									}
								}
								requiredData.put("averageRating", avgRating);
							} else if (field.equalsIgnoreCase("totalRating")) {
								Integer totalRating = 0;
								Map<String, Object> rating = (Map<String, Object>) contentData.getOrDefault(field,
										new HashMap<>());
								if (!rating.isEmpty()) {
									if (rating.containsKey(rootOrg)) {
										totalRating = Integer.parseInt(rating.get(rootOrg).toString());
									}
								}
								requiredData.put("totalRating", totalRating);
							} else {
								requiredData.put(field, contentData.getOrDefault(field, ""));
							}
						}
						resultData.add(requiredData);
					}
				}
			}
			finalMap.put("resource", resultData);
			finalMap.put("resource_ids", ret);
		}
		return finalMap;
	}

	private String getDisplayName(Map<String, Object> userData) {
		String name = "";
		if (userData.get("first_name") != null && !(userData.get("first_name")).toString().isEmpty()
				&& userData.get("last_name") != null && !(userData.get("last_name")).toString().isEmpty()) {

			name = userData.get("first_name") + " " + userData.get("last_name");

		} else if (userData.get("first_name") != null && !(userData.get("first_name")).toString().isEmpty()
				&& (userData.get("last_name") == null || (userData.get("last_name")).toString().isEmpty())) {

			name = (userData.get("first_name")).toString();

		} else if ((userData.get("first_name") == null || (userData.get("first_name")).toString().isEmpty())
				&& userData.get("last_name") != null && !(userData.get("last_name")).toString().isEmpty()) {

			name = (userData.get("last_name")).toString();
		}
		return name;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getUserPlaylistWithoutMeta(String rootOrg, String userId) {

		userUtilService.validateUser(rootOrg, userId);

		Response contentResponse = getUserPlaylist(userPlaylistDb.getKeySpace(), userPlaylistDb.getTableName(), userId,
				rootOrg);

		return (List<Map<String, Object>>) contentResponse.getResult().get("response");

	}
}
