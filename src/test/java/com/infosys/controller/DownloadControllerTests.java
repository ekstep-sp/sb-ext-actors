/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*package com.infosys.controller;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;import com.infosys.util.LexJsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = { "classpath:dbconfig.properties", "classpath:elasticsearch.config.properties" })
public class DownloadControllerTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeClass
	public static void setUp() {

	}

	@Test
	public void addUserContentDownload() throws Exception {
		String userId = "TestUser";
		String contentId = "do_2123972413746216961280";
		Map<String, Object> reqMap = new HashMap<>();
		Map<String, Object> resMap = new HashMap<>();
		resMap.put(JsonKey.USER_ID, userId);
		resMap.put(JsonKey.CONTENT_ID, contentId);
		reqMap.put(JsonKey.REQUEST, resMap);

		ResponseEntity<Response> responseEntity = restTemplate.postForEntity("/v1/user/content/download", reqMap,
				Response.class);
		Response response = responseEntity.getBody();
		Map<String, Object> resultMap = response.getResult();
		Assert.assertNotNull(resultMap.get("response"));

		try {
			Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
			Map<String, Object> resultObject = (Map<String, Object>) mapObject.get("result");
			String resp = (String) resultObject.get("response");
			System.out.println(resp);
			Assert.assertEquals(resp, "SUCCESS");
		} catch (Exception e) {
			ProjectLogger.log("exception caught while Testing for addUserContentDownload");
		}
	}

	@Test
	public void getDownloadedCourses() throws Exception {
		String userId = "TestUser";

		ResponseEntity<Response> responseEntity = restTemplate
				.getForEntity("/v1/user/" + userId + "/content/downloaded", Response.class);
		Response response = responseEntity.getBody();
		Map<String, Object> resultMap = response.getResult();
		Assert.assertNotNull(resultMap.get("response"));

		try {
			Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
			Map<String, Object> resultObject = (Map<String, Object>) mapObject.get("result");
			List<Object> resp = (List<Object>) resultObject.get("resources");
			int numberOfRecords = resp.size();
			Assert.assertEquals(1, numberOfRecords);
		} catch (Exception e) {
			ProjectLogger.log("exception caught while Testing for getDownloadedCourses");
		}
	}

}*/