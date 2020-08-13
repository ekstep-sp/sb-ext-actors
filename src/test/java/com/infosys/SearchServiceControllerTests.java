/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys;

/*(@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchServiceControllerTests {
	
	MockMvc mockMvc;
	
	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
	
	private final String SEARCH_REQUEST = "{\"request\" : { \"query\" : \"\" , \"contentType\" : \"\", \"resourceType\" : \"\", \"track\" : [], \"topics\" : [] }}";
	
	@Autowired
	NewSearchServiceContoller newSearchServiceController;
	
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(newSearchServiceController)
				.build();
	}
	
	@SuppressWarnings("unchecked")
	@Test
    public void test_for_search_Default() throws Exception {
		String contentString = this.mockMvc
		.perform(
				post("/search").content(SEARCH_REQUEST)
						.contentType(APPLICATION_JSON_UTF8))
		.andReturn().getResponse().getContentAsString();
		contentString = contentString.substring(2,contentString.length()-2);
		System.out.println(contentString);
		
		Map<String,String> map = new HashMap<String,String>();
		
		    String[] entry = contentString.split(":");                 
		    map.put(entry[0].trim(), entry[1].trim());         
		System.out.println(map.get(entry[0].trim()));
		
		/*try {
		List<Map<String,Object>> listOfResults =  (List<Map<String,Object>>) map.get("response");
		
		Assert.assertNotNull(listOfResults);
		
		System.out.println("printing");
		
		}catch(Exception e){
			ProjectLogger.log("response from the api could not be explicitly mapped to the list");
		}
		
    }
}*/
