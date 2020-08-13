/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.helper.ServiceFactory;
import com.infosys.service.PreferencesService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PreferencesServiceImpl implements PreferencesService {
	private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private Util.DbInfo userPreferencesDb = Util.dbInfoMap.get(LexJsonKey.USER_PREFERENCES);
	private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);

	@Override
	@SuppressWarnings("unchecked")
    public Map<String, Object> getPreferences(String idType, String identifier)
            throws JsonParseException, JsonMappingException, IOException {
		Map<String, Object> reqMap = new HashMap<>();
		reqMap.put("user_id", getUserId(idType, identifier));
		Response response = cassandraOperation.getRecordById(userPreferencesDb.getKeySpace(),
				userPreferencesDb.getTableName(), reqMap);
		List<Map<String, Object>> responseMaps = (List<Map<String, Object>>) response.getResult().get("response");

		if (responseMaps.size() == 0) {
            throw new NoContentException("No preferences for given user found.");
		}
		Map<String, Object> responseMap = responseMaps.get(0);

        String preferencesData = String.valueOf(responseMap.get("preference_data"));

        responseMap = new ObjectMapper().readValue(preferencesData, new TypeReference<Map<String, String>>() {
        });

        return responseMap;
	}

	@Override
	public void updatePreferences(String idType, String identifier, String preferencesData) {

        Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("user_id", getUserId(idType, identifier));
		requestMap.put("date_updated", Calendar.getInstance().getTime());
		requestMap.put("preference_data", preferencesData.trim());
		cassandraOperation.insertRecord(userPreferencesDb.getKeySpace(), userPreferencesDb.getTableName(), requestMap);

    }

	@SuppressWarnings("unchecked")
	public String getUserId(String idType, String identifier) {
		if (!idType.equalsIgnoreCase("uuid") && !idType.equalsIgnoreCase("email")) {
			throw new BadRequestException("invalid identifier type specified");
		}
		if (idType.equalsIgnoreCase("uuid")) {
			return identifier;
		}
		Response response = cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(),
				"email", identifier);
		List<Map<String, Object>> responseMaps = (List<Map<String, Object>>) response.getResult().get("response");
		if (responseMaps.size() == 0) {
			throw new ResourceNotFoundException("no user found");
		}
		Map<String, Object> responseMap = responseMaps.get(0);
		return responseMap.get("id").toString();
	}

}
