/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.BatchExecutionData;
import com.infosys.repository.BatchExecutionRepository;
import com.infosys.repository.ProgressRepository;
import com.infosys.service.BadgeService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Service
public class BadgeServiceImpl implements BadgeService {

	@Autowired
	ProgressRepository prRepository;

	@Autowired
	BatchExecutionRepository beRepository;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private Comparator<Map<String, Object>> intCompare = new Comparator<Map<String, Object>>() {
		public int compare(Map<String, Object> m1, Map<String, Object> m2) {
			return ((Double) (Double.parseDouble(m2.get("progress").toString())
					- Double.parseDouble((m1.get("progress").toString())))).intValue();
		}
	};

	private Comparator<Map<String, Object>> dateCompare = new Comparator<Map<String, Object>>() {
		public int compare(Map<String, Object> m1, Map<String, Object> m2) {
			try {
				int c = (formatter.parse((m2.get("first_received_date")).toString()))
						.compareTo(formatter.parse((m1.get("first_received_date")).toString()));
				if (c == 0)
					c = (m1.get("badge_order").toString()).compareTo((m2.get("badge_order").toString()));
				if (c == 0)
					c = (m1.get("badge_id").toString()).compareTo((m2.get("badge_id").toString()));
				return c;
			} catch (ParseException e) {
				return 0;
			}
		}
	};

	private Comparator<Map<String, Object>> stringCompare = new Comparator<Map<String, Object>>() {
		public int compare(Map<String, Object> m1, Map<String, Object> m2) {
			return (m1.get("badge_order").toString()).compareTo((m2.get("badge_order").toString()));
		}
	};

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String allEmptyBadges = properties.getProperty(LexJsonKey.AllBadges);
	private String userBadges = properties.getProperty(LexJsonKey.UserBadges);
	private String totalPointsTable = properties.getProperty(LexJsonKey.totalPoints);
	private CassandraConnectionManager connectionManager;

	public BadgeServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		System.out.println(properties.getProperty(JsonKey.DB_IP));
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String, Object> getAllBadges(String email_id) {
		System.out.println("port cassandra : "+properties.getProperty(JsonKey.DB_IP));
		HashMap<String, Object> returnMap = new HashMap<String, Object>();
		try {
			List<Map<String, Object>> latest = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> earned = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> closeToEarn = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> canEarn = new ArrayList<Map<String, Object>>();
			Boolean elfFlag = false;
			DateTime lastWeek = new DateTime((formatter.parse(formatter.format(new Date())))).minusWeeks(1);

			List<Map<String, Object>> allBadges = (List<Map<String, Object>>) cassandraOperation
					.getAllRecords(bodhiKeyspace, allEmptyBadges).getResult().get("response");// empty badges

			Map<String, Object> userBadgeDataMap = new HashMap<>();

			{
				Map<String, Object> certificateMap = new HashMap<>();
				Set<String> contentIds = new HashSet<>();
				{
					List<Document> contentDoc = prRepository.getCourseProgress(email_id);

					if (contentDoc != null)
						for (Document doc : contentDoc) {
							Map<String, Object> temp = new HashMap<>();
							temp.put("progress", (Double.parseDouble(doc.get("progress").toString()) * 100));
							temp.put("badge_type", doc.get("badgeType"));
							temp.put("first_received_date", "");
							temp.put("received_count", 0);
							temp.put("last_received_date", "");
							temp.put("is_new", 0);
							temp.put("threshold", 1);
							certificateMap.put(doc.get("cid").toString(), temp);
							contentIds.add(doc.get("cid").toString());
						}
				}

				{
					List<Map<String, Object>> badgeData = (List<Map<String, Object>>) cassandraOperation
							.getRecordsByProperty(bodhiKeyspace, userBadges, "email_id", email_id).getResult()
							.get("response");
					for (Map<String, Object> userBadgeData : badgeData) {
						if (userBadgeData.get("badge_type").toString().toUpperCase().matches("M|C")) {
							Map<String, Object> temp = new HashMap<>();
							temp.put("progress", userBadgeData.get("progress"));
							temp.put("badge_type", userBadgeData.get("badge_type"));
							temp.put("first_received_date",
									formatter.format(((Date) userBadgeData.get("first_received_date"))));
							temp.put("received_count", userBadgeData.get("received_count"));
							temp.put("last_received_date",
									formatter.format(((Date) userBadgeData.get("last_received_date"))));
							if (lastWeek.compareTo(new DateTime(formatter
									.parse(formatter.format(((Date) userBadgeData.get("first_received_date")))))) < 0)
								temp.put("is_new", 1);
							else {
								temp.put("is_new", 0);
							}
							temp.put("threshold", 1);
							certificateMap.put(userBadgeData.get("badge_id").toString(), temp);
							contentIds.add(userBadgeData.get("badge_id").toString());
						} else {
							userBadgeDataMap.put(userBadgeData.get("badge_id").toString().toLowerCase(), userBadgeData);
						}
					}
				}

				RestHighLevelClient client = ConnectionManager.getClient();

				SearchResponse response = client.search(new SearchRequest().indices("mlsearch_*").types("searchresources")
						.source(new SearchSourceBuilder()
								.fetchSource(new String[] { "identifier", "name" }, new String[0])
								.query(QueryBuilders.boolQuery().must(QueryBuilders.termsQuery("_id", contentIds)))
								.size(contentIds.size())),
						RequestOptions.DEFAULT);
				for (SearchHit hit : response.getHits()) {
					Map<String, Object> source = hit.getSourceAsMap();
					Map<String, Object> temp = (Map<String, Object>) certificateMap
							.get(source.get("identifier").toString());
					if (temp != null) {
						temp.put("badge_id", source.get("identifier").toString());
						temp.put("badge_group", temp.get("badge_type"));
						temp.put("badge_name", source.get("name").toString());
						if (temp.get("badge_type").toString().toUpperCase().equals("C")) {
							temp.put("message",
									"Congratulations on completing the course '" + source.get("name") + "'.");
							temp.put("hover_text", "For completing '" + source.get("name") + "' course.");
							temp.put("how_to_earn", "Complete the course");
							temp.put("image", "/content/Achievements/Badges/Certificate.png?type=assets");// get image
						} else {
							temp.put("message", "Hats off to you, elite learner! Congrats.");
							temp.put("hover_text", "For completing '" + source.get("name") + "' learning path.");
							temp.put("how_to_earn", "Complete the learning path");
							temp.put("image", "/content/Achievements/Badges/Eilte.png?type=assets");// get image
						}
						temp.put("badge_order", temp.get("badge_type"));
//						System.out.println(temp.get("first_received_date").toString().equals(""));
						if (Double.parseDouble(temp.get("progress").toString()) == 100
								&& !temp.get("first_received_date").toString().equals("")) {
							temp.put("how_to_earn",
									temp.get("how_to_earn").toString().replace("Complete", "Completed"));
							earned.add(temp);
						} else {
							temp.remove("message");
							temp.put("hover_text", temp.get("how_to_earn"));
							if (temp.get("badge_type").equals("M") & Calendar.getInstance().get(Calendar.MONTH) == 11)
								elfFlag = true;
							closeToEarn.add(temp);
						}
					}
				}
			}

			DateTime yesterday = new DateTime((formatter.parse(formatter.format(new Date())))).minusDays(1);

			for (Iterator<Map<String, Object>> badgeIter = allBadges.iterator(); badgeIter.hasNext();) {
				Map<String, Object> badge = badgeIter.next();
				Map<String, Object> userBadgeData = (Map<String, Object>) userBadgeDataMap
						.get(badge.get("badge_id").toString().toLowerCase());
				if (userBadgeData == null) {
					continue;
				}

				badge.remove("created_date");

				if (badge.get("badge_type").toString().toUpperCase().equals("R") && yesterday.compareTo(new DateTime(
						formatter.parse(formatter.format(((Date) userBadgeData.get("progress_date")))))) < 0)
					badge.put("progress", 0);
				else
					badge.put("progress", userBadgeData.get("progress"));

				badge.put("first_received_date", formatter.format(((Date) userBadgeData.get("first_received_date"))));
				badge.put("received_count", userBadgeData.get("received_count"));
				badge.put("last_received_date", formatter.format(((Date) userBadgeData.get("last_received_date"))));

				if (lastWeek
						.compareTo(new DateTime(formatter.parse(badge.get("first_received_date").toString()))) < 0) {
					badge.put("is_new", 1);
				} else {
					badge.put("is_new", 0);
				}

				badge.put("how_to_earn", badge.get("description"));
				badge.remove("description");

				Double progress = Double.parseDouble(badge.get("progress").toString());
				badge.put("hover_text", this.getHoverMessage(badge.get("badge_id").toString(), progress));
				if (progress == 100 || Integer.parseInt(badge.get("received_count").toString()) > 0) {
					if (badge.get("badge_type").toString().toUpperCase().equals("R")) {
						if (badge.get("received_count").toString() == "1")
							badge.put("hover_text", "Earned 1 time");
						else
							badge.put("hover_text", "Earned " + badge.get("received_count").toString() + " times");
					}
					badge.put("how_to_earn", badge.get("past_description"));
					badge.remove("past_description");
					earned.add(badge);
				} else if (progress >= 85) {
					badge.remove("message");
					badge.remove("past_description");
					closeToEarn.add(badge);
				} else {
//					badge.remove("hover_text");
					badge.remove("message");
					badge.remove("last_received_date");
					badge.remove("first_received_date");
					badge.remove("past_description");
					canEarn.add(badge);
				}

				badgeIter.remove();
			}
			for (Map<String, Object> badge : allBadges) {
				badge.remove("created_date");
				badge.put("progress", 0);
				badge.put("received_count", 0);
				badge.put("is_new", 0);
				badge.remove("message");
				badge.put("how_to_earn", badge.get("description"));
				badge.remove("description");
				badge.remove("past_description");
				badge.put("hover_text", this.getHoverMessage(badge.get("badge_id").toString(), 0d));
				if (badge.get("badge_id").equals("Elf") & elfFlag) {
					badge.put("hover_text", badge.get("how_to_earn"));
					closeToEarn.add(badge);
				} else {
					canEarn.add(badge);
				}

			}

			Collections.sort(earned, dateCompare);
			Collections.sort(closeToEarn, intCompare);
			Collections.sort(canEarn, stringCompare);
			if (earned.size() > 0)
				latest.add(earned.remove(0));

			if (canEarn.size() > 0)
				if (canEarn.get(0).get("badge_id").equals("NewUser"))
					canEarn.remove(0);

			returnMap.put("recent", latest);
			returnMap.put("earned", earned);
			returnMap.put("closeToEarning", closeToEarn);
			returnMap.put("canEarn", canEarn);
			returnMap.put("totalPoints", this.getTotalPoints(email_id));

			List<BatchExecutionData> data = beRepository.findByBatchName("badge_batch3",
					PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "batch_started_on")));
			String lastUpdatedDate = new SimpleDateFormat("dd MMM yyyy 00:00 z").format(new Date(0));
			if (data.size() > 0)
				lastUpdatedDate = new SimpleDateFormat("dd MMM yyyy 00:00 z").format(data.get(0).getBatchStartedOn());

			returnMap.put("lastUpdatedDate", lastUpdatedDate);
		} catch (Exception e) {
			returnMap = new HashMap<String, Object>();
			ProjectLogger.log("Error : " + e.getMessage(), e);
			e.printStackTrace();
		}
		return returnMap;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getTotalPoints(String email_id) {
		List<Map<String, Object>> totalPoints = (List<Map<String, Object>>) cassandraOperation
				.getRecordsByProperty(bodhiKeyspace, totalPointsTable, "email_id", email_id).getResult()
				.get("response");// total user points
		System.out.println("Badge Cassandra Success");
		ProjectLogger.log("Badge Cassandra Success");
		// total points
		for (Map<String, Object> map : totalPoints) {
			map.remove("email_id");
			break;
		}

		if (totalPoints.isEmpty()) {
			Map<String, Object> temp = new HashMap<String, Object>();
			temp.put("learning_points", 0);
			temp.put("collaborative_points", 0);
			totalPoints.add(temp);
		}

		return totalPoints;
	}

	private String getHoverMessage(String badge_id, Double progress) {
		String message = "";
		Integer value = 0;
		if (badge_id.equals("Quiz1")) {
			message = "Complete a quiz and get this!";
		} else if (badge_id.equals("Quiz25")) {
			value = 25 - ((Double) (progress * 0.25)).intValue();
			message = value + " more quizzes, to go!";
		} else if (badge_id.equals("Quiz100")) {
			value = 100 - progress.intValue();
			message = value + " more quizzes, to go!";
		} else if (badge_id.equals("Quiz250")) {
			value = 250 - ((Double) (progress * 2.5)).intValue();
			message = value + " more quizzes, to go!";
		} else if (badge_id.equals("Quiz1000")) {
			value = 1000 - ((Double) (progress * 10)).intValue();
			message = value + " more quizzes, to go!";
		} else if (badge_id.equals("4Day")) {
			value = 240 - ((Double) (progress * 2.4)).intValue();
			message = value + " minutes to go!";
		} else if (badge_id.equals("20Week")) {
			value = 1200 - ((Double) (progress * 12)).intValue();
			message = value + " minutes to go!";
		} else if (badge_id.equals("30MWeek")) {
			value = 5 - ((Double) (progress * 0.05)).intValue();
			if (value == 1)
				message = "One day to go!";
			else
				message = value + " more days to go!";
		} else if (badge_id.equals("30MMonth")) {
			value = 25 - ((Double) (progress * 0.25)).intValue();
			if (value == 1)
				message = "One day to go!";
			else
				message = value + " more days to go!";
		} else if (badge_id.equals("Course1")) {
			message = "Complete a course and get this!";
		} else if (badge_id.equals("Course10")) {
			value = 10 - ((Double) ((progress * 10) / 100)).intValue();
			message = value + " more courses to go!";
		} else if (badge_id.equals("Course25")) {
			value = 25 - ((Double) ((progress * 25) / 100)).intValue();
			message = value + " more courses to go!";
		} else if (badge_id.equals("Course100")) {
			value = 100 - progress.intValue();
			message = value + " more courses to go!";
		}

		return message;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getRecentBadge(String email_id) {
		Map<String, Object> returnVal = new HashMap<String, Object>();
		try {
			DateTime lastWeek = new DateTime((formatter.parse(formatter.format(new Date())))).minusWeeks(1);
			Map<String, Object> latest = new HashMap<String, Object>();
			List<Map<String, Object>> earned = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> allBadges = (List<Map<String, Object>>) cassandraOperation
					.getAllRecords(bodhiKeyspace, allEmptyBadges).getResult().get("response");// empty badges

			Map<String, Object> userBadgeDataMap = new HashMap<>();

			{
				Map<String, Object> certificateMap = new HashMap<>();
				Set<String> contentIds = new HashSet<>();
				{
					List<Map<String, Object>> badgeData = (List<Map<String, Object>>) this
							.getEarnedBadge("email_id", email_id).getResult().get("response");
					for (Map<String, Object> userBadgeData : badgeData) {
						if (userBadgeData.get("badge_type").toString().toUpperCase().matches("M|C")) {
							Map<String, Object> temp = new HashMap<>();
							temp.put("progress", userBadgeData.get("progress"));
							temp.put("badge_type", userBadgeData.get("badge_type"));
							temp.put("first_received_date",
									formatter.format(((Date) userBadgeData.get("first_received_date"))));
							temp.put("received_count", userBadgeData.get("received_count"));
							temp.put("last_received_date",
									formatter.format(((Date) userBadgeData.get("last_received_date"))));
							if (lastWeek.compareTo(new DateTime(formatter
									.parse(formatter.format(((Date) userBadgeData.get("first_received_date")))))) < 0)
								temp.put("is_new", 1);
							else {
								temp.put("is_new", 0);
							}
							temp.put("threshold", 1);
							certificateMap.put(userBadgeData.get("badge_id").toString(), temp);
							contentIds.add(userBadgeData.get("badge_id").toString());
						} else {
							userBadgeDataMap.put(userBadgeData.get("badge_id").toString().toLowerCase(), userBadgeData);
						}

					}
				}

				for (String id : contentIds) {
					Map<String, Object> temp = (Map<String, Object>) certificateMap.get(id);
					if (temp != null) {
						temp.put("badge_id", id);
						temp.put("badge_group", temp.get("badge_type"));
						temp.put("badge_order", temp.get("badge_type"));
						earned.add(temp);
					}
				}
			}

			DateTime yesterday = new DateTime((formatter.parse(formatter.format(new Date())))).minusDays(1);

			for (Iterator<Map<String, Object>> badgeIter = allBadges.iterator(); badgeIter.hasNext();) {
				Map<String, Object> badge = badgeIter.next();
				Map<String, Object> userBadgeData = (Map<String, Object>) userBadgeDataMap
						.get(badge.get("badge_id").toString().toLowerCase());
				if (userBadgeData == null) {
					continue;
				}

				badge.remove("created_date");

				if (badge.get("badge_type").toString().toUpperCase().equals("R") && yesterday.compareTo(new DateTime(
						formatter.parse(formatter.format(((Date) userBadgeData.get("progress_date")))))) < 0)
					badge.put("progress", 0);
				else
					badge.put("progress", userBadgeData.get("progress"));

				badge.put("first_received_date", formatter.format(((Date) userBadgeData.get("first_received_date"))));
				badge.put("received_count", userBadgeData.get("received_count"));
				badge.put("last_received_date", formatter.format(((Date) userBadgeData.get("last_received_date"))));
				badge.put("how_to_earn", badge.get("past_description"));
				badge.remove("description");
				badge.remove("past_description");
				if (lastWeek
						.compareTo(new DateTime(formatter.parse(badge.get("first_received_date").toString()))) < 0) {
					badge.put("is_new", 1);
				} else {
					badge.put("is_new", 0);
				}
				Double progress = Double.parseDouble(badge.get("progress").toString());
				badge.put("hover_text", this.getHoverMessage(badge.get("badge_id").toString(), progress));
				if (progress == 100 || Integer.parseInt(badge.get("received_count").toString()) > 0) {
					if (badge.get("badge_type").toString().toUpperCase().equals("R")) {
						if (badge.get("received_count").toString() == "1")
							badge.put("hover_text", "Earned 1 time");
						else
							badge.put("hover_text", "Earned " + badge.get("received_count").toString() + " times");
					}
					earned.add(badge);
				}

				badgeIter.remove();
			}

			Collections.sort(earned, dateCompare);
			if (earned.size() > 0) {
				latest = earned.remove(0);
				if (latest.get("badge_type").toString().toUpperCase().matches("M|C")) {

					SearchResponse response = ConnectionManager
							.getClient().search(
									new SearchRequest().indices("mlsearch_*").types("searchresources")
											.source(new SearchSourceBuilder()
													.fetchSource(new String[] { "identifier", "name" }, new String[0])
													.query(QueryBuilders.boolQuery()
															.must(QueryBuilders.termsQuery("_id",
																	latest.get("badge_id").toString())))
													.size(1)),
									RequestOptions.DEFAULT);

					for (SearchHit hit : response.getHits()) {
						Map<String, Object> source = hit.getSourceAsMap();
						latest.put("badge_name", source.get("name").toString());
						if (latest.get("badge_type").toString().toUpperCase().equals("C")) {
							latest.put("message",
									"Congratulations on completing the course '" + source.get("name") + "'.");
							latest.put("hover_text", "For completing '" + source.get("name") + "' course.");
							latest.put("how_to_earn", "Complete the course");
							latest.put("image", "/content/Achievements/Badges/Certificate.png?type=assets");// get
																											// image
						} else {
							latest.put("message", "Hats off to you, elite learner! Congrats.");
							latest.put("hover_text", "For completing '" + source.get("name") + "' learning path.");
							latest.put("how_to_earn", "Complete the learning path");
							latest.put("image", "/content/Achievements/Badges/Eilte.png?type=assets");// get image
						}
					}
				}
			}

			returnVal.put("recent_badge", latest);
			returnVal.put("totalPoints", this.getTotalPoints(email_id));

		} catch (Exception e) {
			returnVal = new HashMap<String, Object>();
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return returnVal;
	}

	private Response getEarnedBadge(String key, String value) {
		Response response = new Response();
		try {
			Select selectQuery = QueryBuilder.select().all().from(bodhiKeyspace, userBadges);
			Where selectWhere = selectQuery.where();
			Clause clause = eq(key, value);
			selectWhere.and(clause);
			clause = gt("received_count", 0);
			selectWhere.and(clause);
			ResultSet results = connectionManager.getSession(bodhiKeyspace).execute(selectQuery.allowFiltering());
			response = CassandraUtil.createResponse(results);
		} catch (Exception e) {
			ProjectLogger.log("Error : Cassandra fetch failed :" + e.getMessage(), e);
		}
		return response;
	}
}
