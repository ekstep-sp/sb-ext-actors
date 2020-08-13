/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.exception.NoContentException;
import com.infosys.model.BatchExecutionData;
import com.infosys.repository.BatchExecutionRepository;
import com.infosys.service.LeaderBoardService;
import com.infosys.service.ProfileService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Service
public class LeaderBoardServiceImpl implements LeaderBoardService {

	@Autowired
	BatchExecutionRepository beRepository;

	@Autowired
	UserUtilityService userUtilService;

	@Autowired
	ProfileService profileService;

//    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String leaderboard = properties.getProperty(LexJsonKey.LeaderboardTable);
	private String leaderboardRank = properties.getProperty(LexJsonKey.LeaderboardRankTable);

	public LeaderBoardServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getLeaderBoard(String duration_type, String leaderboard_type, String email_id,
			String year, String duration_value) throws Exception {

		// Parameter Validations
		this.validateParameters(duration_type, leaderboard_type, year, duration_value, email_id);

		Map<String, Object> returnValue = new HashMap<String, Object>();
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		Integer leaderboard_year = 0;
		Integer duration_number = 0;
		Integer leaderboard_year_prev = 0;
		Integer duration_number_prev = 0;
		Integer leaderboard_year_next = 0;
		Integer duration_number_next = 0;
		String start_date = "";
		String end_date = "";
		LocalDate currentDate = LocalDate.now();
		LocalDate calDate = LocalDate.now();

		if (duration_type.toUpperCase().equals("M")) {
			calDate = LocalDate.of(!year.equals("0") ? Integer.parseInt(year) : calDate.get(ChronoField.YEAR),
					!duration_value.equals("0") ? Integer.parseInt(duration_value)
							: calDate.get(ChronoField.MONTH_OF_YEAR),
					calDate.get(ChronoField.DAY_OF_MONTH));

			if (currentDate.get(ChronoField.DAY_OF_MONTH) == 1
					&& calDate.get(ChronoField.MONTH_OF_YEAR) == currentDate.get(ChronoField.MONTH_OF_YEAR)) {
				calDate = calDate.minus(1, ChronoUnit.MONTHS);
			}

			duration_number = calDate.get(ChronoField.MONTH_OF_YEAR);
			leaderboard_year = calDate.get(ChronoField.YEAR);

			calDate = calDate.withDayOfMonth(1);

			start_date = dateTimeFormatter.format(calDate);

			calDate = calDate.plus(1, ChronoUnit.MONTHS);

			duration_number_next = calDate.get(ChronoField.MONTH_OF_YEAR);
			leaderboard_year_next = calDate.get(ChronoField.YEAR);

			calDate = calDate.minus(1, ChronoUnit.DAYS);

			end_date = dateTimeFormatter.format(calDate);

			calDate = calDate.minus(1, ChronoUnit.MONTHS);

			duration_number_prev = calDate.get(ChronoField.MONTH_OF_YEAR);
			leaderboard_year_prev = calDate.get(ChronoField.YEAR);
		} else {
			calDate = calDate.with(IsoFields.WEEK_BASED_YEAR,
					!year.equals("0") ? Integer.parseInt(year) : calDate.get(IsoFields.WEEK_BASED_YEAR));
			calDate = calDate.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR,
					!duration_value.equals("0") ? Integer.parseInt(duration_value)
							: calDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			if (currentDate.get(ChronoField.DAY_OF_WEEK) == 1 && currentDate
					.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == calDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) {
				calDate = calDate.minusWeeks(1);
			}
			duration_number = calDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			leaderboard_year = calDate.get(IsoFields.WEEK_BASED_YEAR);

			calDate = calDate.with(ChronoField.DAY_OF_WEEK, 1);

			start_date = dateTimeFormatter.format(calDate);

			calDate = calDate.plusWeeks(1);

			duration_number_next = calDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			leaderboard_year_next = calDate.get(IsoFields.WEEK_BASED_YEAR);

			calDate = calDate.minus(1, ChronoUnit.DAYS);

			end_date = dateTimeFormatter.format(calDate);

			calDate = calDate.minusWeeks(1);

			duration_number_prev = calDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			leaderboard_year_prev = calDate.get(IsoFields.WEEK_BASED_YEAR);
		}

		propertyMap.put("leaderboard_year", leaderboard_year);
		propertyMap.put("duration_type", duration_type.toUpperCase());
		propertyMap.put("duration_value", duration_number);
		propertyMap.put("leaderboard_type", leaderboard_type.toUpperCase());

		returnValue.putAll(propertyMap);// adds to return
		returnValue.put("start_date", start_date);
		returnValue.put("end_date", end_date);

		propertyMap.put("email_id", email_id);

		try {

			List<Map<String, Object>> result = (List<Map<String, Object>>) userUtilService
					.getRecordsByProperties(bodhiKeyspace, leaderboard, propertyMap).getResult().get("response");
			propertyMap.remove("email_id");

			propertyMap.put("leaderboard_year",
					Arrays.asList(leaderboard_year_prev, leaderboard_year, leaderboard_year_next));
			propertyMap.put("duration_value",
					Arrays.asList(duration_number_prev, duration_number, duration_number_next));
			if (result.size() != 0) {
				Integer rank = Integer.parseInt(((HashMap<String, Object>) result.get(0)).get("rank").toString());
				propertyMap.put("rank", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, rank));
			} else {
				propertyMap.put("rank", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
			}
			result = (List<Map<String, Object>>) userUtilService
					.getRecordsByProperties(bodhiKeyspace, leaderboardRank, propertyMap).getResult().get("response");

			if (result.size() != 0) {
				for (Iterator<Map<String, Object>> i = result.iterator(); i.hasNext();) {
					Map<String, Object> map = i.next();
					Integer y = Integer.parseInt(map.get("leaderboard_year").toString());
					Integer v = Integer.parseInt(map.get("duration_value").toString());
					if (y.equals(leaderboard_year_prev) && v.equals(duration_number_prev)
							&& !returnValue.containsKey("prev")) {
						Map<String, Object> prev = new HashMap<String, Object>();
						prev.put("leaderboard_year", y);
						prev.put("duration_value", v);
						returnValue.put("prev", prev);
					} else if (y.equals(leaderboard_year_next) && v.equals(duration_number_next)
							&& !returnValue.containsKey("next")) {
						Map<String, Object> next = new HashMap<String, Object>();
						next.put("leaderboard_year", y);
						next.put("duration_value", v);
						returnValue.put("next", next);
					} else if (y.equals(leaderboard_year) && v.equals(duration_number))
						continue;
					i.remove();
				}
				if (result.size() != 0) {
					Float maxPoints = Float.parseFloat(result.get(0).get("points").toString());

					List<String> users = new ArrayList<String>();
					for (Map<String, Object> map : result) {
						if (map.get("email_id").toString() != "") {
							String email = map.get("email_id") == null ? "" : map.get("email_id").toString();
							users.add(email);
						}
					}

					List<Map<String, Object>> userData = profileService.getMultipleUserData(users);
					Map<String, Object> userDataMap = new HashMap<String, Object>();
					for (Map<String, Object> user : userData) {
						userDataMap.put(user.get("onPremisesUserPrincipalName").toString().toLowerCase(), user);
					}
					// Map<String, String> userPhotos =
					// userDataUtil.getMultipleUserPhotoFromActiveDirectory(users);
					users = new ArrayList<String>();

					for (Map<String, Object> map : result) {
						Boolean userNameFlag = true;
						Map<String, Object> user = null;

						if (userDataMap.containsKey(map.get("email_id").toString().toLowerCase()))
							user = (Map<String, Object>) userDataMap.get(map.get("email_id").toString().toLowerCase());

						if (user != null) {
							String temp = user.get("department") == null ? "" : user.get("department").toString();
							if (!temp.equals(""))
								map.put("unit", temp);

							temp = user.get("jobTitle") == null ? "" : user.get("jobTitle").toString();
							if (!temp.equals(""))
								map.put("designation", temp);

							temp = user.get("surname") == null ? "" : user.get("surname").toString();
							if (!temp.equals(""))
								map.put("last_name", temp);

							temp = user.get("givenName") == null ? "" : user.get("givenName").toString();
							if (!temp.equals("")) {
								userNameFlag = false;
								map.put("first_name", temp);
							}

						}

						map.put("percentile",
								maxPoints == 0 ? 0 : (Float.parseFloat(map.get("points").toString())) / maxPoints);
						map.remove("leaderboard_year");
						map.remove("leaderboard_type");
						map.remove("duration_type");
						map.remove("duration_value");
						map.remove("start_date");
						map.remove("end_date");
						// map.put("photo", userPhotos.get(email));
						if (userNameFlag) {
							users.add(map.get("email_id").toString());
						}
					}

					if (users.size() > 0) {
						List<Map<String, Object>> userNames = userUtilService.getUserDataFromElastic(users);
						userDataMap = new HashMap<String, Object>();
						for (Map<String, Object> user : userNames) {
							userDataMap.put(user.get("loginId").toString().toLowerCase(), user);
						}
						for (Map<String, Object> map : result) {
							Map<String, Object> user = null;
							if (userDataMap.containsKey(map.get("email_id").toString().toLowerCase()))
								user = (Map<String, Object>) userDataMap.get(map.get("email_id").toString());
							if (user != null) {
								map.put("first_name", user.get("firstName"));
								map.put("last_name", user.get("lastName"));
							}

						}
					}
				}
			}
			returnValue.put("items", result);
			List<BatchExecutionData> data = beRepository.findByBatchName("leader_batch4",
					PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "batch_started_on")));
			String lastUpdatedDate = new SimpleDateFormat("dd MMM yyyy 00:00 z").format(new Date(0));
			if (data.size() > 0)
				lastUpdatedDate = new SimpleDateFormat("dd MMM yyyy 00:00 z").format(data.get(0).getBatchStartedOn());
			returnValue.put("lastUpdatedDate", lastUpdatedDate);
		} catch (Exception e) {
			returnValue = new HashMap<String, Object>();
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return returnValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> pastTopLearners(String duration_type, String leaderboard_type) throws Exception {
		
		//Validating Parameters
		this.validateParameters(duration_type, leaderboard_type);
		
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		propertyMap.put("duration_type", duration_type.toUpperCase());
		propertyMap.put("leaderboard_type", leaderboard_type.toUpperCase());
		propertyMap.put("rank", 1);
		try {
			result = (List<Map<String, Object>>) userUtilService
					.getRecordsByProperties(bodhiKeyspace, leaderboardRank, propertyMap).getResult().get("response");
			List<String> users = new ArrayList<String>();
			if (result.size() > 0)
				if (Integer
						.parseInt(result.get(0).get("duration_value")
								.toString()) == (Calendar.getInstance().get(Calendar.MONTH) + 1)
						&& Integer.parseInt(result.get(0).get("leaderboard_year").toString()) == (Calendar.getInstance()
								.get(Calendar.YEAR)))
					result.remove(0);

			for (Map<String, Object> map : result) {
				if (map.get("email_id").toString() != "") {
					String email = map.get("email_id") == null ? "" : map.get("email_id").toString();
					if (email.contains("@"))
						email = email.substring(0, email.indexOf("@"));
					users.add(email);
				}
			}

			List<Map<String, Object>> userData = userUtilService.getUsersFromActiveDirectory(users);

			Map<String, Object> userDataMap = new HashMap<String, Object>();
			for (Map<String, Object> user : userData) {
				userDataMap.put(user.get("onPremisesUserPrincipalName").toString().toLowerCase(), user);
			}
			// Map<String, String> userPhotos =
			// userDataUtil.getMultipleUserPhotoFromActiveDirectory(users);
			users = new ArrayList<String>();

			for (Map<String, Object> map : result) {
				Boolean userNameFlag = true;
				Map<String, Object> user = null;

				if (userDataMap.containsKey(map.get("email_id").toString().toLowerCase()))
					user = (Map<String, Object>) userDataMap.get(map.get("email_id").toString().toLowerCase());

				if (user != null) {
					String temp = user.get("department") == null ? "" : user.get("department").toString();
					if (!temp.equals(""))
						map.put("unit", temp);

					temp = user.get("jobTitle") == null ? "" : user.get("jobTitle").toString();
					if (!temp.equals(""))
						map.put("designation", temp);

					temp = user.get("surname") == null ? "" : user.get("surname").toString();
					if (!temp.equals(""))
						map.put("last_name", temp);

					temp = user.get("givenName") == null ? "" : user.get("givenName").toString();
					if (!temp.equals("")) {
						userNameFlag = false;
						map.put("first_name", temp);
					}

				}
				map.remove("start_date");
				map.remove("end_date");
				// map.put("photo", userPhotos.get(email));
				if (userNameFlag) {
					users.add(map.get("email_id").toString());
				}
			}

			if (users.size() > 0) {
				List<Map<String, Object>> userNames = userUtilService.getUserDataFromElastic(users);
				userDataMap = new HashMap<String, Object>();
				for (Map<String, Object> user : userNames) {
					userDataMap.put(user.get("loginId").toString().toLowerCase(), user);
				}
				for (Map<String, Object> map : result) {
					Map<String, Object> user = null;
					if (userDataMap.containsKey(map.get("email_id").toString().toLowerCase()))
						user = (Map<String, Object>) userDataMap.get(map.get("email_id").toString());
					if (user != null) {
						map.put("first_name", user.get("firstName"));
						map.put("last_name", user.get("lastName"));
					}

				}
			}
		} catch (Exception e) {
			result = new ArrayList<Map<String, Object>>();
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return result;
	}

	private void validateParameters(String duration_type, String leaderboard_type, String year, String duration_value, String email_id) {

		if (!email_id.matches("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$")) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		if (!Arrays.asList("L","C").contains(leaderboard_type)) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		if (!Arrays.asList("M","W").contains(duration_type)) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		int yearAsInt, duration;
		try {
			yearAsInt = Integer.parseInt(year);
			duration = Integer.parseInt(duration_value);
		} catch (NumberFormatException e) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		if (yearAsInt < 2017 || yearAsInt > Year.now().getValue()) {
			throw new NoContentException("Bad Request");
		}
		if (duration_type.equals("M") && (duration < 0 || duration > 12)) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		if (duration_type.equals("W") && (duration < 0 || duration > 53)) {
			throw new InvalidDataInputException("Bad Request");
		}
	}

	
	private void validateParameters(String duration_type, String leaderboard_type) {
		
		if (!Arrays.asList("L","C").contains(leaderboard_type)) {
			throw new InvalidDataInputException("Bad Request");
		}
		
		if (!Arrays.asList("M","W").contains(duration_type)) {
			throw new InvalidDataInputException("Bad Request");
		}
	}
}
