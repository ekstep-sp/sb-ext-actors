/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.Lists;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.repository.UserRepository;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Repository
public class UserRepositoryImpl implements UserRepository {
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String sunbirdKeyspace = JsonKey.SUNBIRD;
	private String userByEmail = LexJsonKey.MV_USER;
	private String userByUUID = JsonKey.USER;
	private CassandraConnectionManager connectionManager;
	private PreparedStatement getEmail = null;
	private PreparedStatement getUUID = null;
	private PreparedStatement getUUIDs = null;
	private PreparedStatement autoComplete = null;
	private PreparedStatement getEmails = null;

	public UserRepositoryImpl() {
		Util.checkCassandraDbConnections(sunbirdKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	private PreparedStatement getStatement(String name, String keyspaceName, String tableName) {
		PreparedStatement ret = null;
		if (name.equals("selectEmail")) {
			if (getEmail == null)
				getEmail = connectionManager.getSession(keyspaceName)
						.prepare("select email,firstname,lastname from " + tableName + " where id=?;");
			ret = getEmail;
		} else if (name.equals("selectUUID")) {
			if (getUUID == null)
				getUUID = connectionManager.getSession(keyspaceName)
						.prepare("select id,firstname,lastname from " + tableName + " where email=?;");
			ret = getUUID;
		} else if (name.equals("selectUUIDs")) {
			if (getUUIDs == null)
				getUUIDs = connectionManager.getSession(keyspaceName)
						.prepare("select id,firstname,lastname,email from " + tableName + " where email in ?;");
			ret = getUUIDs;
		} else if (name.equals("autocomplete")) {
			if (autoComplete == null)
				autoComplete = connectionManager.getSession(keyspaceName)
						.prepare("select firstname,lastname,email from " + tableName + " where email like ? limit 5;");
			ret = autoComplete;
		} else if (name.equals("getEmails")) {
			if (getEmails == null) {
				getEmails = connectionManager.getSession(keyspaceName)
						.prepare("select id,email from " + tableName + " where id in ?;");
			}
			ret = getEmails;
		}
		return ret;
	}

	@Override
	public Map<String, Object> getUUIDFromEmail(String email) throws Exception {
		Map<String, Object> ret = new HashMap<>();
		try {
			PreparedStatement selectStatement = this.getStatement("selectUUID", sunbirdKeyspace, userByEmail);
			ResultSet result = connectionManager.getSession(sunbirdKeyspace).execute(selectStatement.bind(email));
			for (Row row : result.all()) {
				ret.put("id", row.getString("id"));
				ret.put("firstname", row.getString("firstname"));
				ret.put("lastname", row.getString("lastname"));
			}
			if (ret.isEmpty())
				throw new InvalidDataInputException("Invalid user email");
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public Map<String, Object> getUUIDsFromEmails(List<String> emails) throws Exception {
		Map<String, Object> ret = new HashMap<>();
		try {
			PreparedStatement selectStatement = this.getStatement("selectUUIDs", sunbirdKeyspace, userByEmail);
			ResultSet result = connectionManager.getSession(sunbirdKeyspace).execute(selectStatement.bind(emails));
			for (Row row : result.all()) {
				Map<String, String> temp = new HashMap<>();
				temp.put("id", row.getString("id"));
				temp.put("firstname", row.getString("firstname"));
				temp.put("lastname", row.getString("lastname"));
				ret.put(row.getString("email"), temp);
			}
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public Map<String, Object> getEmailFromUUID(String uuid) throws Exception {
		Map<String, Object> ret = new HashMap<>();
		try {
			PreparedStatement selectStatement = this.getStatement("selectEmail", sunbirdKeyspace, userByUUID);
			ResultSet result = connectionManager.getSession(sunbirdKeyspace).execute(selectStatement.bind(uuid));
			for (Row row : result.all()) {
				ret.put("email", row.getString("email"));
				ret.put("firstname", row.getString("firstname"));
				ret.put("lastname", row.getString("lastname"));
			}
		} catch (Exception e) {
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getAutoCompleteEmails(String queyString) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<>();
		try {
			PreparedStatement selectStatement = this.getStatement("autocomplete", sunbirdKeyspace, userByUUID);
			ResultSet result = connectionManager.getSession(sunbirdKeyspace)
					.execute(selectStatement.bind(queyString + "%"));
			for (Row row : result.all()) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("mail", row.getString("email"));
				temp.put("displayName", row.getString("firstname")
						+ (row.getString("lastname").isEmpty() ? "" : " " + row.getString("lastname")));
				ret.add(temp);
			}
		} catch (Exception e) {
			throw new Exception("Cassandra Selection failed.");
		}
		return ret;
	}

	@Override
	public Map<String, Object> getUserDetails(String idValue, String idType) throws Exception {
		Map<String, Object> output = new HashMap<>();
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			if (idType.equals("email")) {
				stmt = connectionManager.getSession(sunbirdKeyspace)
						.prepare("select * from " + JsonKey.USER + " where email=?;");
				result = connectionManager.getSession(sunbirdKeyspace).execute(stmt.bind(idValue));
			} else {
				stmt = connectionManager.getSession(sunbirdKeyspace)
						.prepare("select * from " + JsonKey.USER + " where id=?;");
				result = connectionManager.getSession(sunbirdKeyspace).execute(stmt.bind(idValue));
			}
			for (Row row : result.all()) {
				output.put("userid", row.getString("userid"));
				output.put("email", row.getString("email").toLowerCase());
				output.put("firstname", row.getString("firstname"));
				output.put("lastname", row.getString("lastname"));
				output.put("countrycode", row.getString("lastname"));
				output.put("dob", row.getString("dob"));
				output.put("emailverified", row.getBool("emailverified"));
				output.put("gender", row.getString("gender"));
				output.put("location", row.getString("location"));
				output.put("phone", row.getString("phone"));
				output.put("thumbnail", row.getString("thumbnail"));
				output.put("username", row.getString("username"));
				output.put("profilesummary", row.getString("profilesummary"));
				output.put("rootorgid", row.getString("rootorgid"));
				output.put("lastlogintime", row.getString("lastlogintime"));
				output.put("regorgid", row.getString("regorgid"));
				output.put("roles", row.getList("roles", String.class));
				output.put("subject", row.getList("subject", String.class));
				output.put("language", row.getList("language", String.class));
				output.put("profilevisibility", row.getMap("profilevisibility", String.class, String.class));
			}
		} catch (Exception e) {
			ProjectLogger.log("Error occurred in getUserDetails " + e.getLocalizedMessage());
			throw new Exception(e.getMessage());
		}
		return output;
	}

	@Override
	public Map<String, String> getEmailsFromUUIDS(List<String> uuids) throws Exception {
		Map<String, String> email = new HashMap<>();
		try {
			List<List<String>> allSubsets = Lists.partition(Lists.newArrayList(uuids), 500);
			for (List<String> subList : allSubsets) {
				PreparedStatement selectStatement = this.getStatement("getEmails", sunbirdKeyspace, userByUUID);
				ResultSet result = connectionManager.getSession(sunbirdKeyspace).execute(selectStatement.bind(subList));

				for (Row row : result.all()) {
					email.put(row.getString("email"), row.getString("id"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
//			throw new Exception("Cassandra Selection failed.");
		}
		return email;
	}

	@Override
	public List<String> findUUIDs(List<String> uuids) throws Exception {
		List<String> resultList = new ArrayList<>();
		try {
			PreparedStatement selectStatement = this.getStatement("selectUUIDs", sunbirdKeyspace, userByUUID);
			ResultSet result = connectionManager.getSession(sunbirdKeyspace).execute(selectStatement.bind(uuids));
			for (Row row : result.all()) {
				resultList.add(row.getString("id"));
			}
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Cassandra Selection failed.");
		}
		return resultList;
	}

}
