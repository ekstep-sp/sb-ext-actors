/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.MasterValue;
import com.infosys.model.Topic;
import com.infosys.service.TopicService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class TopicServiceImpl implements TopicService {

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);
	private Util.DbInfo userTopicsDb = Util.dbInfoMap.get(LexJsonKey.USER_TOPICS_MAPPING_DB);
	private Util.DbInfo topicsDb = Util.dbInfoMap.get(LexJsonKey.TOPICS_DB);
	private Util.DbInfo masterValueInfoMap = Util.dbInfoMap.get(LexJsonKey.MASTER_VALUES);
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	public TopicServiceImpl() {
		Util.checkCassandraDbConnections(keyspace);
		Util.checkCassandraDbConnections(bodhiKeyspace);
	}

	@Override
	public boolean addTopic(Topic topic) {

		try {
			Date parsedTimeStamp = ProjectUtil.getDateFormatter().parse(topic.getDate_created());
			Timestamp timestamp = new Timestamp(parsedTimeStamp.getTime());

			Map<String, Object> request = topic.toMap();

			ProjectLogger.log("Topic Map for ES ", request, LoggerEnum.INFO.name());
			ElasticSearchUtil.createData(LexProjectUtil.EsIndex.lexTopic.getIndexName(),
					LexProjectUtil.EsType.content.getTypeName(), topic.getId(), request);

			request.put(LexJsonKey.DATE_CREATED, timestamp);
			// request.put(JsonKey.DATE_MODIFIED, timestamp);
			request.put(JsonKey.ID, UUID.fromString(topic.getId()));
			// As Elastic Search adds addition field to it
			request.remove(JsonKey.IDENTIFIER);
			ProjectLogger.log("Topic Map for Cassandra ", request, LoggerEnum.INFO.name());
			cassandraOperation.insertRecord(topicsDb.getKeySpace(), topicsDb.getTableName(), request);
			return true;
		} catch (Exception ex) {
			ProjectLogger.log("Exception id date format ", ex, LoggerEnum.DEBUG.name());
			ex.printStackTrace();
			return false;
		}
	}

//	@Override
//	public List<Map<String, Object>> getNewTopics() {
//
//		String aggregator = "concepts.name";
//		String fileName = "newTopics.txt";
//		List<Map<String, Object>> resp = new ArrayList<>();
//		try {
//			/*
//			 * File file = new ClassPathResource(fileName).getFile(); //Read File Content
//			 * List<String> content = Files.readAllLines(file.toPath());
//			 * Collections.sort(content);
//			 */
//
//			InputStream in = this.getClass().getResourceAsStream("/newTopics.txt");
//			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//			// Stream<String> lines = reader.readLine();
//			/*
//			 * for (String line : reader) { Map<String, Object> topic = new HashMap<String,
//			 * Object>() { { put(aggregator, line.trim()); put(JsonKey.COUNT, 0);
//			 * put(JsonKey.ID, ""); } }; resp.add(topic); }
//			 */
//			String line;
//			while ((line = reader.readLine()) != null) {
//				String tempLine = line;
//				// System.out.println(line);
//				Map<String, Object> topic = new HashMap<String, Object>() {
//					{
//						put(aggregator, tempLine.trim());
//						put(JsonKey.COUNT, 0);
//						put(JsonKey.ID, "");
//					}
//				};
//				resp.add(topic);
//			}
//			return resp;
//		} catch (Exception ex) {
//			return resp;
//		}
//	}

	@Override
	public List<Map<String, Object>> getNewTopics() {
		String aggregator = LexConstants.CONCEPTS_NAME;
		List<Map<String, Object>> recommendedInterests = new ArrayList<>();
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put(LexConstants.ENTITY, LexConstants.RECOMMENDED_INTERESTS);
		try {
			Response response = cassandraOperation.getRecordById(masterValueInfoMap.getKeySpace(),
					masterValueInfoMap.getTableName(), requestMap);
			if (response == null) {
				throw new ApplicationLogicError("Requested Resource could not be found");
			}
			List<Map<String, Object>> resultMaps = (List<Map<String, Object>>) response.getResult().get("response");
			Map<String, Object> resultMap = resultMaps.get(0);
			ObjectMapper mapper = new ObjectMapper();
			MasterValue masterValue = mapper.convertValue(resultMap, MasterValue.class);
			List<String> interests = masterValue.getValues();
			for (String interest : interests) {
				Map<String, Object> topic = new HashMap<>();
				topic.put(aggregator, interest);
				topic.put(JsonKey.COUNT, 0);
				topic.put(JsonKey.ID, "");
				recommendedInterests.add(topic);
			}
		} catch (NullPointerException nullPointerException) {
			throw new ApplicationLogicError("Something went wrong");
		} catch (IllegalArgumentException illegalArgumentException) {
			throw new ApplicationLogicError("Invalid Arguments");
		} catch (Exception exception) {
			throw new ApplicationLogicError("Something went wrong");
		}
		return recommendedInterests;

	}

	@Override
	public List<Map<String, Object>> getTopTopics() throws IOException {

		String aggregator = "concepts.name";

		Map<String, Object> filter = new HashMap<>();
		filter.put(JsonKey.STATUS, ProjectUtil.CourseMgmtStatus.LIVE.getValue());

		List<Map<String, Object>> resp = ElasticSearchUtil.searchMatchedData(
				LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), filter,
				aggregator);

		List<String> topics = new ArrayList<>();
		for (Map<String, Object> data : resp) {
			topics.add((String) data.get(aggregator));
		}

		List<Map<String, Object>> resp1 = ElasticSearchUtil.searchDataByValues(
				LexProjectUtil.EsIndex.lexTopic.getIndexName(), LexProjectUtil.EsType.content.getTypeName(),
				JsonKey.NAME, topics, topics.size());

		for (Map<String, Object> topic : resp) {
			for (Map<String, Object> topic1 : resp1) {
				if (topic.get(aggregator).equals(topic1.get(JsonKey.NAME))) {
					topic.put(JsonKey.ID, topic1.get(JsonKey.ID));
				}
			}
		}

		return resp;
	}

	@Override
	public List<Topic> getTopics(int size) {

		Map<String, Object> searchMap = new HashMap<>();
		List<Map<String, Object>> result = ElasticSearchUtil.searchData(LexProjectUtil.EsIndex.lexTopic.getIndexName(),
				LexProjectUtil.EsType.content.getTypeName(), searchMap, size);
		List<Topic> topics = new ArrayList<>();
		for (Map<String, Object> entry : result) {
			topics.add(Topic.fromMap(entry));
		}
		return topics;
	}

	@Override
	public List<Topic> getUserTopics(String userId) {

		Response response = cassandraOperation.getRecordsByProperty(userTopicsDb.getKeySpace(),
				userTopicsDb.getTableName(), LexJsonKey.TOPIC_USER_ID, userId);

		Map<String, Object> responseResult = response.getResult();
		List<Map<String, Object>> result = (List<Map<String, Object>>) responseResult.get(JsonKey.RESPONSE);

		// Sending blank list if user is not present in the table
		if (result.size() == 0) {
			return new ArrayList<>();
		}

		// Sending blank list if user is not subscribed to any topics
		Set<String> userTopics = (HashSet<String>) result.get(0).get(LexJsonKey.TOPIC_NAMES);
		if (userTopics.size() == 0) {
			return new ArrayList<>();
		}
		result = ElasticSearchUtil.searchDataByValues(LexProjectUtil.EsIndex.lexTopic.getIndexName(),
				LexProjectUtil.EsType.content.getTypeName(), JsonKey.NAME, new ArrayList<>(userTopics),
				userTopics.size());

		List<Topic> topics = new ArrayList<>();

		for (Map<String, Object> entry : result) {
			Topic topic = new ObjectMapper().convertValue(entry, Topic.class);
			topics.add(topic);
		}

		/*
		 * for(String userTopic: userTopics) {
		 * if(mapOfObjectsPresent.get(userTopic)==null) { Timestamp timestamp = new
		 * Timestamp(System.currentTimeMillis()); Map<String,Object> topicMap = new
		 * HashMap<String,Object>(); topicMap.put("id",UUID.randomUUID());
		 * topicMap.put("status", "unapproved"); topicMap.put("name", userTopic);
		 * topicMap.put("date_created", timestamp); topicMap.put("date_modified",
		 * timestamp); topicMap.put("source", ""); topicMap.put("source_source",
		 * "unapproved"); int count =
		 * cassandraOperation.getMaxRecord(topicsDb.getKeySpace(),
		 * topicsDb.getTableName(), "source_id", "source", ""); count+=1;
		 * topicMap.put("source_id", count);
		 * cassandraOperation.insertRecord(topicsDb.getKeySpace(),
		 * topicsDb.getTableName(), topicMap); Topic topic = new
		 * ObjectMapper().convertValue(topicMap, Topic.class); topics.add(topic); } }
		 */

		return topics;
	}

	@Override
	public boolean addUserTopics(String userId, List<String> topics) {
		try {
			// Validating the userId
			Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
					userId);
			List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse
					.get(Constants.RESPONSE);
			if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
				System.out.println(userId + "Invalid");
				ProjectLogger.log("Invalid userId " + userId);
				return false;
			}
			if (userDetails.get(0).get(LexJsonKey.EMAIL) == null) {
				return false;
			}
//		String email = (String) userDetails.get(0).get(LexJsonKey.EMAIL);

			cassandraResponse = cassandraOperation.getRecordsByProperty(userTopicsDb.getKeySpace(),
					userTopicsDb.getTableName(), LexJsonKey.TOPIC_USER_ID, userId);
			userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);

			Timestamp ts = new Timestamp(new Date().getTime());
			Map<String, Object> userTopics = new HashMap<>();
			userTopics.put(LexJsonKey.TOPIC_USER_ID, userId);
			Set<String> topicsSet = new HashSet<>(topics);
			this.validateTopics(topicsSet);
			userTopics.put(LexJsonKey.TOPIC_NAMES, topicsSet);
//		userTopics.put(LexJsonKey.EMAIL,email);

			if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
				userTopics.put(LexJsonKey.DATE_CREATED, ts);
				userTopics.put(LexJsonKey.DATE_MODIFIED, null);
			} else {
				userTopics.put(LexJsonKey.DATE_MODIFIED, ts);
			}

			cassandraOperation.upsertRecord(userTopicsDb.getKeySpace(), userTopicsDb.getTableName(), userTopics);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * This method validates the topics as against the ES index and adds the
	 * non-existing topics
	 *
	 * @param topicsSet
	 */
	private void validateTopics(Set<String> topicsSet) {
		for (String name : topicsSet) {
			Map<String, Object> topicMap = new HashMap<>();
			topicMap.put(JsonKey.NAME, name);
			List<Map<String, Object>> resp = ElasticSearchUtil.searchMatchedData(
					LexProjectUtil.EsIndex.lexTopic.getIndexName(), LexProjectUtil.EsType.content.getTypeName(),
					topicMap);
			ProjectLogger.log("Validating topic " + name + ". ES Result ==> ", resp, LoggerEnum.INFO.name());
			if (resp.size() == 0) {
				Topic topic = new Topic(ProjectUtil.generateUniqueId(), ProjectUtil.getFormattedDate(), null, name,
						"", 0, "_USER", "unapproved", "unapproved");
				this.addTopic(topic);
			}
		}
	}

}
