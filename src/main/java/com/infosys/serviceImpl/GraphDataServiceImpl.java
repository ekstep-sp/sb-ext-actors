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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infosys.repository.InfyMeDownloadDataByDateRepository;
import com.infosys.service.GraphDataService;

@Service
public class GraphDataServiceImpl implements GraphDataService {

	@Autowired
	InfyMeDownloadDataByDateRepository infyMeRepo;

	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy");

	@Override
	public Map<String, Object> getInfymeData(String startDateStr, String endDateStr) throws Exception {

		Map<String, Object> graphDataMap = new HashMap<String, Object>();

		List<Map<String, Object>> downloadDataByDate = null;
		graphDataMap.put("totalDownloads", infyMeRepo.count());

		List<Map<String, Object>> downloads = new ArrayList<Map<String, Object>>();

		if (startDateStr == null || endDateStr == null) {
			downloadDataByDate = infyMeRepo.getDownloadsByDate();

		} else {
			Date startDate = simpleDateFormat.parse(startDateStr);
			Date endDate = simpleDateFormat.parse(endDateStr);
			downloadDataByDate = infyMeRepo.getDownloadsByDateBetween(startDate, endDate);
		}
		int totalCount = 0;
		for (Map<String, Object> downloadData : downloadDataByDate) {
			Map<String, Object> downloadDataMap = new HashMap<String, Object>();
			downloadDataMap.put("date", ((Date) downloadData.get("download_date")).getTime());
			downloadDataMap.put("count", Integer.valueOf(downloadData.get("downloads").toString()));
			downloads.add( downloadDataMap);
		}
		Collections.reverse(downloads);
		for (Map<String, Object> download : downloads) {
			totalCount += Integer.valueOf(download.get("count").toString());
			download.put("count", totalCount);
		}
		graphDataMap.put("downloads", downloads);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		Date yesterday = sdf.parse(sdf.format(new DateTime(new Date()).minusDays(1).toDate()));
		Map<String, Object> downloadDataYesterday = infyMeRepo.getDownloadsOn(yesterday);

		graphDataMap.put("yesterdaysDownloads", Integer.valueOf(downloadDataYesterday.get("downloads").toString()));

		return graphDataMap;
	}

}
