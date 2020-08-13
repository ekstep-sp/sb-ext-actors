/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.sunbird.common.models.response.Response;

public interface PlaylistService {

	Response deletePlaylist(Map<String, Object> playlistData);

	Response fetchRecentPlaylist(Map<String, Object> playlistData) throws Exception;

	Response fetchUserPlaylist(Map<String, Object> playlistData) throws Exception;

	Response upsertPlaylist(Map<String, Object> playlistData) throws Exception;

	Response sharePlaylist(Map<String, Object> playlistData) throws ParseException, Exception;

	Response fetchResourceListForSyncing(Map<String, Object> playlistData) throws Exception;

	Response deleteSharedPlaylist(Map<String, Object> playlistData);

	List<String> getContentIdsForPlayList(String userId, String playlistId) throws Exception;

	Map<String, Object> getPlayListById_v1(String rootOrg, String userId, String playlistId) throws Exception;

	Map<String, Object> getPlayListById(String rootOrg, String userId, String playlistId) throws Exception;

	List<Map<String, Object>> getUserPlaylistWithoutMeta(String rootOrg, String userId);
}
