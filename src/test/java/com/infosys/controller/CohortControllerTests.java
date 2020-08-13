/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import org.sunbird.common.models.response.Response;
//import org.sunbird.common.models.util.ProjectLogger;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class CohortControllerTests {
//	private static String testUser = null;
//    private static String[] contentId= null;
//    private static String goalId=null ;
//    @Autowired
//    private TestRestTemplate restTemplate;
// 
//    @BeforeClass
//	public static void setUp() {
//    	contentId=new String[]{"lex_21537507400378147000","lex_16738322739763497000"};
//	}
//    
//    @SuppressWarnings("unchecked")
//    @Test
//    public void upsertUserLearningGoals(){
//        String user = testUser;
//        String goalType="user";
//        String goalTitle="testing goal add or update";
//        String goalDesc="This method is used to test feature of user goal addition or updation";
//        List<String>contentIds= new ArrayList<String>();
//        contentIds.add(contentId[0]);
//        List<Map<String,Object>>goalDataCollection=new ArrayList<Map<String,Object>>();
//        Map<String,Object>goalData=new HashMap<String,Object>();
//        goalData.put("goal_content_id",contentIds);
//        goalData.put("goal_type", goalType);
//        goalData.put("goal_title",goalTitle);
//        goalData.put("goal_desc",goalDesc);
//        goalDataCollection.add(goalData);
//        Map<String,Object>userGoalData=new HashMap<String,Object>();
//        userGoalData.put("goal_data",goalDataCollection);
//        Map<String,Object>reqMap=new HashMap<String,Object>();
//        reqMap.put("request", userGoalData);
//              	
//        try {
//        	//testing for adding a goal
//        ResponseEntity<Response> responseEntity = restTemplate.postForEntity("/v1/users/"+user+"/goals",reqMap,Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//		List<Map<String,Object>> goalListMap=(List<Map<String,Object>>)resultMap.get("response");
//        //using the goal id coming to test updating a goal
//        for (Map<String,Object>map:goalListMap) {
//        	assertThat(map.get("goal_id").toString()).isNotNull();
//        		goalId=map.get("goal_id").toString();
//        }
//        //testing for updating the goal just created
//        //adding one course in goal
//        contentIds.add(contentId[1]);
//        goalData.put("goal_content_id",contentIds);
//        goalData.put("goal_type", goalType);
//        goalTitle="My goal has been updated";
//        goalData.put("goal_title",goalTitle);
//        goalDesc="Updating my goal desc to new goal desc";
//        goalData.put("goal_desc",goalDesc);
//        goalData.put("goal_id",goalId);
//        goalDataCollection.clear();
//        goalDataCollection.add(goalData);    
//        reqMap.put("request", userGoalData);
//        responseEntity = restTemplate.postForEntity("/v1/users/"+user+"/goals",reqMap,Response.class);
//        response = responseEntity.getBody();
//        resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        for (Map<String,Object>map:goalListMap) {
//        	assertThat(map.get("goal_id").toString()).isNotNull();
//    }
//        }
//        catch(Exception e) {
//        	ProjectLogger.log("Exception occured while testing service for upserting user goals : "
//					+ e.getMessage(), e);
//        }
//        
//    }
//    
//    @Test
//    public void getUserGoalsWithoutCommonGoals(){
//        String user = testUser;
//      	
//        try {
//        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/users/"+user+"/goals&type=custom",Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        }
//        catch(Exception e) {
//        	ProjectLogger.log("Exception occured while testing service for fetching custom user goals : "
//					+ e.getMessage(), e);
//        }
//        
//    }
//    @Test
//    public void  getAllCommonLearningGoals(){
//        String user = testUser;
//      	
//        try {
//        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/users/"+user+"/goals&type=common&signup=false",Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        }
//        catch(Exception e) {
//        	ProjectLogger.log("Exception occured while testing service for fetching common goals for a user: "
//					+ e.getMessage(), e);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//	@Test
//    public void  getUserLearningGoalsProgress(){
//        String user = testUser;
//      	
//        try {
//        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/users/"+user+"/goals",Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        Map<String,Object> resultData=(Map<String,Object>)resultMap.get("response");
//        assertThat(resultData.get("goals_in_progress")).isNotNull();
//        assertThat(resultData.get("completed_goals")).isNotNull(); 
//        for(Map<String,Object> map:(List<Map<String,Object>>)resultData.get("goals_in_progress"))
//        {
//        	assertThat(map.get("goal_id")).isNotNull();
//			assertThat(map.get("goal_content_id")).isNotNull();
//			assertThat(map.get("resource_progress")).isNotNull();
//			assertThat(map.get("goal_type")).isNotNull();
//			assertThat(map.get("goal_title")).isNotNull();
//			assertThat(map.get("goalProgress")).isNotNull();
//			assertThat(map.get("resource_progress")).isNotNull();
//			assertThat((List<Map<String,Object>>)map.get("resource_progress")).isNotEmpty();
//        }
//        }
//        catch(Exception e) {
//        	ProjectLogger.log("Exception occured while testing service for fetching user goals progress : "
//					+ e.getMessage(), e);
//        }
//        
//    }
//    @Test
//    public void removeUserLearningGoals(){
//        String user = testUser;
//        String goalType="user";
//        goalId="abc";
//        // calling upsertUserLearningGoals to create a new goal and use that goalId to remove that goal
//       upsertUserLearningGoals();
//        try {
//          restTemplate.delete("v1/users/"+user+"/goals/"+goalId+"?goalType="+goalType);
//        }
//        catch(Exception e) {
//           assertThat(1).isEqualTo(0);
//        	
//        }
//        
//    }
//    
//    @SuppressWarnings("unchecked")
//	@Test
//    public void  getActiveUsers(){
//        String user = testUser;
//      	String resourceId=contentId[0];
//        try {
//        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/resources/"+resourceId+"/user/"+user+"&type=activeusers",Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        Map<String,Object> resultData=(Map<String,Object>)resultMap.get("response");
//        assertThat(resultData.get("active_users")).isNotNull();
//        assertThat(resultData.get("similar_goals_users")).isNotNull();           
//        }
//        catch(Exception e) {
//        	ProjectLogger.log("Exception occured while testing service for fetching active users for cohorts: "
//					+ e.getMessage(), e);
//        }
//        
//    }
//    
//    @SuppressWarnings("unchecked")
//	@Test
//    public void  getSMEs(){
//        String user = testUser;
//        String resourceId=contentId[0];
//        try {
//        ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/resources/"+resourceId+"/user/"+user+"&type=sme",Response.class);
//        Response response = responseEntity.getBody();
//        Map<String, Object> resultMap = response.getResult();
//        assertThat(resultMap).isNotNull();
//        Map<String,Object> resultData=(Map<String,Object>)resultMap.get("response");
//        assertThat(resultData.get("highPerfomers")).isNotNull();
//        assertThat(resultData.get("educators")).isNotNull();
//        assertThat(resultData.get("authors")).isNotNull();
//        }
//        catch(Exception e) {
//        assertThat(0).isEqualTo(1);
//        	ProjectLogger.log("Exception occured while testing service for fetching sme for cohorts: "
//					+ e.getMessage(), e);
//        }
//        
//    }
//   
//
//}
