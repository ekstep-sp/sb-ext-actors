/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*
package com.infosys.controller;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;import com.infosys.util.LexJsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import com.infosys.model.Topic;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TopicsControllerTests {

    public static Topic mockedTopic;
    private static Topic topic1;

    private static String testUserId = "f657b9f7-0e14-4ab9-930c-e92e1c26ec0f";
    List<String> topicList = Arrays.asList("API Economy", "Big Data Testing");

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setUp() {
        mockedTopic = mock(Topic.class);
        topic1 = new Topic("1", "1", "1", "testTopic", "1", 1, "1", "1", "1");

    }


    @Test
    public void getAllTopics() {

        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/topics/read", Response.class);
        Response response = responseEntity.getBody();
        Map<String, Object> resultMap = response.getResult();

        Assert.assertNotNull(resultMap.get("response"));

        try {
            Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
            List<Object> listObject = (List<Object>) mapObject.get("topics");
            int numberOfRecords = listObject.size();
            System.out.println(numberOfRecords);
            Assert.assertEquals(10, numberOfRecords);
        } catch (Exception e) {
            ProjectLogger.log("exception caught while Testing for getAllTopics");
        }

        responseEntity = restTemplate.getForEntity("/v1/topics/read?size=5", Response.class);
        response = responseEntity.getBody();
        resultMap = response.getResult();
        Assert.assertNotNull(resultMap.get("response"));
        try {
            Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
            List<Object> listObject = (List<Object>) mapObject.get("topics");
            int numberOfRecords = listObject.size();
            System.out.println(numberOfRecords);
            Assert.assertEquals(5, numberOfRecords);
        } catch (Exception e) {
            ProjectLogger.log("exception caught while Testing for getAllTopics");
        }
    }

    @Test
    public void getRecommendedTopics() {
        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/topics/recommended", Response.class);
        Response response = responseEntity.getBody();
        Map<String, Object> resultMap = response.getResult();
        Assert.assertNotNull(resultMap.get("response"));
        try {
            Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
            List<Object> listObject = (List<Object>) mapObject.get("topics");
            int numberOfRecords = listObject.size();
            System.out.println(numberOfRecords);
            Assert.assertEquals(10, numberOfRecords);
        } catch (Exception e) {
            ProjectLogger.log("exception caught while Testing for getAllTopics");
        }

        responseEntity = restTemplate.getForEntity("/v1/topics/recommended?q=new", Response.class);
        response = responseEntity.getBody();
        resultMap = response.getResult();
        Assert.assertNotNull(resultMap.get("response"));
        try {
            Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
            List<Object> listObject = (List<Object>) mapObject.get("topics");
            int numberOfRecords = listObject.size();
            System.out.println(numberOfRecords);
            //Assert.assertEquals(10, numberOfRecords);
        } catch (Exception e) {
            ProjectLogger.log("exception caught while Testing for getAllTopics");
        }
    }


    @Test
    public void addUserTopic() {

        Map<String, Object> reqMap = new HashMap<>();
        Map<String, Object> resMap = new HashMap<>();
        resMap.put(JsonKey.USER_ID, testUserId);
        resMap.put(JsonKey.TOPICS, topicList);
        reqMap.put(JsonKey.REQUEST, resMap);

        ResponseEntity<Response> responseEntity = restTemplate.postForEntity("/v1/user/topic/add", reqMap, Response.class);
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
            ProjectLogger.log("exception caught while Testing for addUserTopic");
        }
    }


    @Test
    public void getUserTopic() {
        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/user/topic/read/" + testUserId, Response.class);
        Response response = responseEntity.getBody();
        Map<String, Object> resultMap = response.getResult();
        Assert.assertNotNull(resultMap.get("response"));

        try {
            Map<String, Object> mapObject = (Map<String, Object>) resultMap.get("response");
            List<Object> listObject = (List<Object>) mapObject.get("topics");
            int numberOfRecords = listObject.size();
            System.out.println(numberOfRecords);
            //Assert.assertEquals(1, numberOfRecords);
        } catch (Exception e) {
            ProjectLogger.log("exception caught while Testing for getUserTopic");
        }

    }

//    @Test
//    public void addUserTopic_Success() throws Exception {
//        String userId = "TestUser";
//        List<String> topicList = Arrays.asList("testTopic1", "testTopic2", "testTopic3");
//        Map<String, Object> reqMap = new HashMap<>();
//        Map<String, Object> resMap = new HashMap<>();
//        resMap.put(JsonKey.USER_ID, userId);
//        resMap.put(JsonKey.TOPICS, topicList);
//        reqMap.put(JsonKey.REQUEST, resMap);
//        mockMvc = MockMvcBuilders.standaloneSetup(topicsController).build();
//        when(mockTopicService.addUserTopics(userId, topicList)).thenReturn(true);
//
//        mockMvc.perform(post("/v1/user/topic/add", reqMap)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(new ObjectMapper().writeValueAsString(reqMap)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.response", is("SUCCESS")));
//
//        verify(mockTopicService, times(1)).addUserTopics(userId, topicList);
//        verifyZeroInteractions(mockTopicService);
//    }
//
//    @Test
//    public void addUserTopic_Failure() throws Exception {
//        String userId = "TestUser";
//        List<String> topicList = Arrays.asList("testTopic1", "testTopic2", "testTopic3");
//        Map<String, Object> reqMap = new HashMap<>();
//        Map<String, Object> resMap = new HashMap<>();
//        resMap.put(JsonKey.USER_ID, userId);
//        resMap.put(JsonKey.TOPICS, topicList);
//        reqMap.put(JsonKey.REQUEST, resMap);
//        mockMvc = MockMvcBuilders.standaloneSetup(topicsController).build();
//        when(mockTopicService.addUserTopics(userId, topicList)).thenReturn(false);
//
//        mockMvc.perform(post("/v1/user/topic/add", reqMap)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(new ObjectMapper().writeValueAsString(reqMap)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.response", is("FAILED")));
//
//        verify(mockTopicService, times(1)).addUserTopics(userId, topicList);
//        verifyZeroInteractions(mockTopicService);
//    }
//
//    @Test
//    public void getUserTopic_mocked() throws Exception {
//        String userId = "TestUser";
//        List<Topic> retList = Arrays.asList(topic1);
//        mockMvc = MockMvcBuilders.standaloneSetup(topicsController).build();
//        when(mockTopicService.getUserTopics(userId)).thenReturn(retList);
//
//        mockMvc.perform(get("/v1/user/topic/read/{uid}", userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.result.response.topics", hasSize(1)))
//                .andExpect(jsonPath("$.result.response.topics[0].id", is("1")))
//                .andExpect(jsonPath("$.result.response.topics[0].name", is("testTopic")));
//
//        verify(mockTopicService, times(1)).getUserTopics(userId);
//        verifyZeroInteractions(mockTopicService);
//    }
}

*/