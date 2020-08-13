/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.model.BatchExecutionData;
import com.infosys.repository.BatchExecutionRepository;
import com.infosys.repository.TimeSpentRepository;
import com.infosys.service.TimeSpentService;
import com.infosys.util.LexJsonKey;

@Service
public class TimeSpentServiceImpl implements TimeSpentService {

	@Autowired
	TimeSpentRepository tsRepositiry;
	
	@Autowired
	BatchExecutionRepository beRepository;

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getUserDashboard(String emailId, String startDate, String endDate) throws Exception {
		
		//Validating Parameters
		this.validateParams(emailId);
		
		Map<String, Object> ret = new HashMap<>();
		try {
			Map<String, Date> dates = this.getDates(startDate, endDate);
			Map<String, Object> userGraph = tsRepositiry.getUserDurationStats(emailId, dates.get("sDate"),
					dates.get("eDate"));
			Map<String, Object> orgGraph = tsRepositiry.getAvgDurationStats(dates.get("sDate"), dates.get("eDate"));
			ret.put("user", this.generateResultsForRange(userGraph, dates.get("sDate"), dates.get("eDate")));
			ret.put("org", this.generateResultsForRange(orgGraph, dates.get("sDate"), dates.get("eDate")));
			ret.put("userAvg", this.getTimeSpentAverage((List<Map<String,Object>>)ret.get("user")));
			
			List<BatchExecutionData> data=beRepository.findByBatchName("daily_time_spent_rollup_batch", PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "batch_started_on")));
			String lastUpdatedDate=new SimpleDateFormat("dd MMM yyyy 00:00 z").format(new Date(0));
			if(data.size()>0)
				lastUpdatedDate=new SimpleDateFormat("dd MMM yyyy 00:00 z").format(data.get(0).getBatchStartedOn());

			ret.put("lastUpdatedDate", lastUpdatedDate);
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	private Map<String, Date> getDates(String startDate, String endDate) throws Exception {
		Map<String, Date> ret = new HashMap<String, Date>();
		Date eDate = new Date();
		Date sDate = new Date();
		if (!startDate.equals("0") && !endDate.equals("0")) {
			if (LexJsonKey.formatter.parse(endDate).after(LexJsonKey.formatter.parse(startDate))) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(LexJsonKey.formatter.parse(endDate));
				eDate = cal.getTime();
				if (eDate.compareTo((Calendar.getInstance().getTime())) > 0) {
					throw new Exception("End Date cannot be greater than todays's Date.");
				}
				cal.setTime(LexJsonKey.formatter.parse(startDate));
				sDate = cal.getTime();

			} else {
				throw new Exception("The start date should be smaller than the end date.");
			}
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(LexJsonKey.formatter.parse(LexJsonKey.formatter.format(new Date())));
			eDate = cal.getTime();
			cal.add(Calendar.DAY_OF_YEAR, -6);
			sDate = cal.getTime();
		}
		ret.put("sDate", sDate);
		ret.put("eDate", eDate);
		return ret;
	}

	private List<Map<String, Object>> generateResultsForRange(Map<String, Object> data, Date startDate, Date endDate) {
		List<String> dateList = this.getDatesInRange(startDate, endDate);
		List<Map<String, Object>> ret = new ArrayList<>();
		for (String date : dateList) {
			Map<String, Object> temp = new HashMap<>();
			temp.put("day", date);
			if (data.containsKey(date))
				temp.put("duration", data.get(date));
			else
				temp.put("duration", 0);
			ret.add(temp);
		}
		return ret;
	}

	private List<String> getDatesInRange(Date startDate, Date endDate) {
		List<String> ret = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		while (cal.getTime().before(endDate)) {
			ret.add(LexJsonKey.formatter.format(cal.getTime()));
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		ret.add(LexJsonKey.formatter.format(cal.getTime()));
		return ret;
	}

	private Double getTimeSpentAverage(List<Map<String, Object>> data) {
		double total = 0;
		for (Map<String, Object> map : data) {
			total += Double.parseDouble(map.get("duration").toString());
		}
		return total == 0 ? 0 : total / data.size();
	}
	
	
	private void validateParams(String emailId) {
		
		if (!emailId.matches("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$")) {
			throw new InvalidDataInputException("Bad Request");
		}
	}
}
