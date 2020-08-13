/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import org.springframework.stereotype.Component;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

@Component
public class ConfigurationsUtil {

	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private CassandraConnectionManager connectionManager;

	public ConfigurationsUtil() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	public String getUserDataSourceConfiguration(String rootOrg) {

		String dataSource = "";
		Select.Where select = QueryBuilder.select().all().from(bodhiKeyspace, "app_config")
				.where(eq("root_org", rootOrg)).and(eq("key", "user_data_source"));
		ResultSet results = connectionManager.getSession(bodhiKeyspace).execute(select);
		dataSource = results.one().getString("value");

		return dataSource;
	}

	public String getSharePlaylistTargetURL(String rootOrg) {

		String url = "";
		Select.Where select = QueryBuilder.select().all().from(bodhiKeyspace, "app_config")
				.where(eq("root_org", rootOrg)).and(eq("key", "playlist_share_target_url"));
		ResultSet results = connectionManager.getSession(bodhiKeyspace).execute(select);
		url = results.one().getString("value");

		return url;
	}
}
