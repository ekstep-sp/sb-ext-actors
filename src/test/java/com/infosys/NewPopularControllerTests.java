/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*package com.infosys;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;
import com.infosys.controller.GenericServiceImplController;
import com.infosys.util.BodhiElasticSearchUtil;

import org.junit.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NewPopularControllerTests {

	@Autowired
    private TestRestTemplate restTemplate;
	
    @SuppressWarnings({ "unchecked", "unused" })
	@Test
    public void testForNewCourses_With_Default_Parameters() {
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/new", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
    	
    	InputStream inputStream = this.getClass().getResourceAsStream("/config.properties");
    	Properties properties = new Properties();
    	properties.load(inputStream);
    	
    	String newDefaultCourses = (String)properties.get("new.course.default.size");
    	Assert.assertEquals(String.valueOf(listObject.size()),newDefaultCourses.trim());
    	
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explixitly converting the response into a list object");
    	}
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testForNewCourses_By_Specifying_Size_Parameters_Within_Range() {
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/new?resourceCount=8", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	
		List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
    	Assert.assertEquals(listObject.size(),8);
    	
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explicitly converting the response into a list object");
    	}
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testForNewCourses_By_Specifying_Size_Parameters_Outside_Range() {
        int courseCountBeingAsked = 100;
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/new?resourceCount=100", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	
		List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
		int responseCourseCount = listObject.size();
        int totalCourseCount = BodhiElasticSearchUtil.getAllRecords().size();
        if(totalCourseCount < courseCountBeingAsked)
        	Assert.assertEquals(responseCourseCount,totalCourseCount);
        else
        	Assert.assertEquals(responseCourseCount,courseCountBeingAsked);
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explixitly converting the response into a list object");
    	}
    }
    
    @SuppressWarnings("unchecked")
	@Test
    public void testForPopularCourses_With_Default_Parameters() {
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/popular", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
    	
    	InputStream inputStream = this.getClass().getResourceAsStream("/config.properties");
    	Properties properties = new Properties();
    	properties.load(inputStream);
    	
    	//System.out.println(listObject.size()+" "+properties.get("popular.course.default.size"));
    	String properties_course_default_size = (String) properties.get("popular.course.default.size");
    	Assert.assertEquals(String.valueOf(listObject.size()),properties_course_default_size.trim());
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explixitly converting the response into a list object");
    	}
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testForPopularCourses_By_Specifying_Size_Parameters_Within_Range() {
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/popular?resourceCount=8", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	
		List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
    	Assert.assertEquals(listObject.size(),8);
    	
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explicitly converting the response into a list object");
    	}
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testForPopularCourses_By_Specifying_Size_Parameters_Outside_Range() {
        int courseCountBeingAsked = 100;
        
    	ResponseEntity<Response> responseEntity = restTemplate.getForEntity("/v1/course/popular?resourceCount=100", Response.class);
    	Response response = responseEntity.getBody();
    	Map<String,Object> resultMap = response.getResult();
    	
    	Assert.assertNotNull(resultMap.get("response"));
    	
    	try {
    	
		List<Map<String,Object>> listObject = (List<Map<String,Object>>) resultMap.get("response");
		int responseCourseCount = listObject.size();
        int totalCourseCount = BodhiElasticSearchUtil.getAllRecords().size();
        System.out.println("responseCount : " + responseCourseCount + "totalCourseCount : " + totalCourseCount + " " + "courseCountBeingAsked : " + courseCountBeingAsked);
        if(totalCourseCount < courseCountBeingAsked)
        	Assert.assertEquals(responseCourseCount,totalCourseCount);
        else
        	Assert.assertEquals(responseCourseCount,courseCountBeingAsked);
    	}
    	catch(Exception e)
    	{
    		ProjectLogger.log("exception caught while explixitly converting the response into a list object");
    	}
    } 

}
*/