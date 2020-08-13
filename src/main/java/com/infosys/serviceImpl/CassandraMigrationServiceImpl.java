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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.infosys.model.cassandra.InfyMeDownloadDataModel;
import com.infosys.model.cassandra.InfyMeDownloadDataPrimaryKeyModel;
import com.infosys.repository.InfyMeDownloadDataRepository;
import com.infosys.service.CassandraMigrationService;

@Service
public class CassandraMigrationServiceImpl implements CassandraMigrationService {

	@Autowired
	InfyMeDownloadDataRepository infyMeData;

	@Override
	public Boolean migrateFile(MultipartFile file) throws Exception {

//		truncate disabled
//		infyMeData.deleteAll();
		List<InfyMeDownloadDataModel> uploadList = new ArrayList<>();
		InputStream is = file.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			
			String[] data = line.contains(",")?line.split(","):line.split("\\s");
			String email=data[0].contains("@")?data[0].replaceAll("@infosys", "@ad.infosys"):data[0]+"@ad.infosys.com";
			uploadList.add(new InfyMeDownloadDataModel(
					new InfyMeDownloadDataPrimaryKeyModel("common_bucket",email.toLowerCase(), new SimpleDateFormat("MM/dd/yyyy").parse(data[1]))));
			if (uploadList.size() == 100) {
				infyMeData.insert(uploadList);
				uploadList.clear();
			}
		}
		infyMeData.insert(uploadList);
		uploadList.clear();
		return true;
	}
}
