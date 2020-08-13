/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.model.CourseRecommendation;
import com.infosys.service.TrendingService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class TrendingServicesImpl implements TrendingService {

	@Autowired
	private AdminAccessControlServiceImpl accessControlService;

	private boolean accessControlEnabled;
	@Autowired
	private Environment environment;

	@PostConstruct
	private void initialize() {
		accessControlEnabled = environment.getProperty(LexConstants.ENABLE_ACCESS_CONTROL, Boolean.class);
	}

	public List<CourseRecommendation> getTrendingCourses(String userId, String pageNumberStr, String pageSizeStr) {
		
		int pageNumber;
		int pageSize;
		
		try {
			pageNumber = Integer.valueOf(pageNumberStr);
			pageSize = Integer.valueOf(pageSizeStr);
		} catch(NumberFormatException ex) {
			throw new BadRequestException("Page Size and Page Number should be numbers");
		}
		
		if (pageNumber < 0 || pageSize < 0) {
			throw new BadRequestException("Page Size and Page Number should be non negative");
		}
		
		try {
			RestHighLevelClient client = ConnectionManager.getClient();
			SearchResponse response = client.search(new SearchRequest().indices(Util.TRENDING_SCORE_INDEX)
					.types(Util.TRENDING_SCORE_INDEX_TYPE)
					.source(new SearchSourceBuilder().query(QueryBuilders.rangeQuery("score").gt(Integer.valueOf(0)))
							.sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)).size(pageSize)
							.from(pageNumber * pageSize)),
					RequestOptions.DEFAULT);

			ArrayList<CourseRecommendation> trendingCourses = new ArrayList<>();
			ArrayList<String> itemIds = new ArrayList<>();
			HashMap<String, Double> scores = new HashMap<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				scores.put((String) map.get("course_id"), (Double) map.get("score"));
				itemIds.add((String) map.get("course_id"));
			}

			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			if (accessControlEnabled && userId != null && !userId.equals("")) {
				Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
				List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
				boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
			}

			for (String id : itemIds) {
				boolQuery.should(QueryBuilders.termQuery("identifier", id));
			}

			SearchResponse responseMeta = client.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
							.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
							.source(new SearchSourceBuilder().fetchSource(new String[] { "name", "identifier",
									"description", "resourceType", "contentType", "isExternalCourse", "learningMode",
									"appIcon", "artifactUrl", "children", "mimeType", "creatorContacts", "downloadUrl",
									"duration", "me_totalSessionsCount", "size", "complexityLevel", "lastUpdatedOn",
									"resourceCategory","status","isInIntranet", "isStandAlone" }, null).query(boolQuery).size(pageSize)),
					RequestOptions.DEFAULT);

			for (SearchHit hit : responseMeta.getHits()) {
				Map<String, Object> courseMeta = hit.getSourceAsMap();
				if (courseMeta != null && !courseMeta.isEmpty() && courseMeta.get("identifier") != null
						&& (scores.get(courseMeta.get("identifier")) != null)) {
					double score = (double) scores.get(courseMeta.get("identifier"));
					if (score > 0.01) {
						CourseRecommendation recommendation = new CourseRecommendation(score, courseMeta, null);
						trendingCourses.add(recommendation);
					}
				}
			}
			return trendingCourses.stream().sorted().filter(recommendation -> recommendation.getCourse() != null)
					.collect(Collectors.toList());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public List<CourseRecommendation> getTrendingCourses(String userId, String pageNumberStr, String pageSizeStr, List<String> langCodes) {

		System.out.println("--------------1-------------------");
		
		int pageNumber;
		int pageSize;

		try {
			pageNumber = Integer.valueOf(pageNumberStr);
			pageSize = Integer.valueOf(pageSizeStr);
		} catch(NumberFormatException ex) {
			throw new BadRequestException("Page Size and Page Number should be numbers");
		}

		if (pageNumber < 0 || pageSize < 0) {
			throw new BadRequestException("Page Size and Page Number should be non negative");
		}

		try {
			RestHighLevelClient client = ConnectionManager.getClient();
			SearchResponse response = client.search(new SearchRequest().indices(Util.TRENDING_SCORE_INDEX)
							.types(Util.TRENDING_SCORE_INDEX_TYPE)
							.source(new SearchSourceBuilder().query(QueryBuilders.termsQuery("locale",langCodes))
									.sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)).size(pageSize)
									.from(pageNumber * pageSize)),
					RequestOptions.DEFAULT);

			ArrayList<CourseRecommendation> trendingCourses = new ArrayList<>();
			ArrayList<String> itemIds = new ArrayList<>();
			HashMap<String, Double> scores = new HashMap<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				scores.put((String) map.get("course_id"), (Double) map.get("score"));
				itemIds.add((String) map.get("course_id"));
			}

			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			if (accessControlEnabled && userId != null && !userId.equals("")) {
				Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
				List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
				boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
			}

			for (String id : itemIds) {
				boolQuery.should(QueryBuilders.termQuery("identifier", id));
			}

			//boolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));
			
			boolQuery.filter(QueryBuilders.termQuery("status", "Live"));


			SearchResponse responseMeta = client.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
							.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
							.source(new SearchSourceBuilder().fetchSource(new String[] { "name", "identifier",
									"description", "resourceType", "contentType", "isExternalCourse", "learningMode",
									"appIcon", "artifactUrl", "children", "mimeType", "creatorContacts", "downloadUrl",
									"duration", "me_totalSessionsCount", "size", "complexityLevel", "lastUpdatedOn",
									"resourceCategory", "averageRating", "status","isInIntranet", "isStandAlone" }, null).query(boolQuery).size(pageSize)),
					RequestOptions.DEFAULT);

			for (SearchHit hit : responseMeta.getHits()) {
				Map<String, Object> courseMeta = hit.getSourceAsMap();
				if (courseMeta != null && !courseMeta.isEmpty() && courseMeta.get("identifier") != null
						&& (scores.get(courseMeta.get("identifier")) != null)) {
					double score = (double) scores.get(courseMeta.get("identifier"));
					if (score > 0.01) {
						CourseRecommendation recommendation = new CourseRecommendation(score, courseMeta, null);
						trendingCourses.add(recommendation);
					}
				}
			}
			return trendingCourses.stream().sorted().filter(recommendation -> recommendation.getCourse() != null)
					.collect(Collectors.toList());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	
	public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr, String pageSizeStr, List<String> langCodes) {

        System.out.println("--------------2-------------------");

		int pageNumber;
		int pageSize;

		try {
			pageNumber = Integer.valueOf(pageNumberStr);
			pageSize = Integer.valueOf(pageSizeStr);
		} catch(NumberFormatException ex) {
			throw new BadRequestException("Page Size and Page Number should be numbers");
		}

		if (pageNumber < 0 || pageSize < 0) {
			throw new BadRequestException("Page Size and Page Number should be non negative");
		}

		try {
			RestHighLevelClient client = ConnectionManager.getClient();
			BoolQueryBuilder trendingBoolQuery = QueryBuilders.boolQuery();
			trendingBoolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));
			trendingBoolQuery.filter(QueryBuilders.termQuery("root_org.keyword", rootOrg));
			SearchResponse response = client.search(new SearchRequest().indices(Util.TRENDING_SCORE_INDEX)
							.types(Util.TRENDING_SCORE_INDEX_TYPE)
							.source(new SearchSourceBuilder().query(trendingBoolQuery)
									.sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)).size(pageSize)
									.from(pageNumber * pageSize)),
					RequestOptions.DEFAULT);

			ArrayList<CourseRecommendation> trendingCourses = new ArrayList<>();
			ArrayList<String> itemIds = new ArrayList<>();
			HashMap<String, Double> scores = new HashMap<>();
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				scores.put((String) map.get("course_id"), (Double) map.get("score"));
				itemIds.add((String) map.get("course_id"));
			}

			BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
			if (accessControlEnabled && userId != null && !userId.equals("")) {
				Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
				List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
				boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
			}

			for (String id : itemIds) {
				boolQuery.should(QueryBuilders.termQuery("identifier", id));
			}

			//boolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));

            boolQuery.filter(QueryBuilders.termQuery("status", "Live"));

			SearchResponse responseMeta = client.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
							.types(LexProjectUtil.EsType.new_lex_search.getTypeName())
							.source(new SearchSourceBuilder().fetchSource(new String[] { "name", "identifier",
									"description", "resourceType", "contentType", "isExternalCourse", "learningMode",
									"appIcon", "artifactUrl", "children", "mimeType", "creatorContacts", "downloadUrl",
									"duration", "me_totalSessionsCount", "size", "complexityLevel", "lastUpdatedOn",
									"resourceCategory", "averageRating", "status", "isInIntranet", "isStandAlone" }, null).query(boolQuery).size(pageSize)),
					RequestOptions.DEFAULT);

			for (SearchHit hit : responseMeta.getHits()) {
				Map<String, Object> courseMeta = hit.getSourceAsMap();
				if (courseMeta != null && !courseMeta.isEmpty() && courseMeta.get("identifier") != null
						&& (scores.get(courseMeta.get("identifier")) != null)) {
					double score = (double) scores.get(courseMeta.get("identifier"));
					if (score > 0.01) {
						CourseRecommendation recommendation = new CourseRecommendation(score, courseMeta, null);
						trendingCourses.add(recommendation);
					}
				}
			}
			return trendingCourses.stream().sorted().filter(recommendation -> recommendation.getCourse() != null)
					.collect(Collectors.toList());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	
public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr,
            String pageSizeStr, List<String> langCodes, List<String> fieldsRequired) {

        //System.out.println("--------------3-------------------");
        
        //System.out.println(rootOrg+" "+userId+" "+pageNumberStr+" "+pageSizeStr+" "+langCodes.toString()+" "+fieldsRequired.toString());
 

        int pageNumber;
        int pageSize;

 
        if(langCodes==null || langCodes.isEmpty()){
            langCodes = new ArrayList<String>();
            langCodes.add("en");
        }
        

        try {
            pageNumber = Integer.valueOf(pageNumberStr);
            pageSize = Integer.valueOf(pageSizeStr);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Page Size and Page Number should be numbers");
        }

 

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Page Size and Page Number should be non negative");
        }

 

        try {
            RestHighLevelClient client = ConnectionManager.getClient();
            BoolQueryBuilder trendingBoolQuery = QueryBuilders.boolQuery();
            trendingBoolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));
            trendingBoolQuery.filter(QueryBuilders.termQuery("root_org.keyword", rootOrg));
            SearchResponse response = client
                    .search(new SearchRequest().indices(Util.TRENDING_SCORE_INDEX).types(Util.TRENDING_SCORE_INDEX_TYPE)
                            .source(new SearchSourceBuilder().query(trendingBoolQuery)
                                    .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)).size(pageSize)
                                    .from(pageNumber * pageSize)),
                            RequestOptions.DEFAULT);

 

            ArrayList<CourseRecommendation> trendingCourses = new ArrayList<>();
            ArrayList<String> itemIds = new ArrayList<>();
            HashMap<String, Double> scores = new HashMap<>();
            for (SearchHit hit : response.getHits()) {
                Map<String, Object> map = hit.getSourceAsMap();
                scores.put((String) map.get("course_id"), (Double) map.get("score"));
                itemIds.add((String) map.get("course_id"));
               // System.out.println((String)map.get("course_id"));
            }

 
           // System.out.println(response.getHits().totalHits);

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (accessControlEnabled && userId != null && !userId.equals("")) {
                Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
                List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
                boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
            }

 

            for (String id : itemIds) {
                boolQuery.should(QueryBuilders.termQuery("identifier", id));
            }

 

            // boolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));

 

            List<String> statusAllowed = new ArrayList<String>();
            statusAllowed.add("Live");
            statusAllowed.add("MarkedForDeletion");

 

            boolQuery.filter(QueryBuilders.termsQuery("status", statusAllowed));

 

            String[] fieldsReq = null;

 

            if (fieldsRequired == null || fieldsRequired.isEmpty()) {
                fieldsReq = new String[] { "name", "identifier", "description", "resourceType", "contentType",
                        "isExternalCourse", "learningMode", "appIcon", "artifactUrl", "children", "mimeType",
                        "creatorContacts", "downloadUrl", "duration", "me_totalSessionsCount", "size",
                        "complexityLevel", "lastUpdatedOn", "resourceCategory", "averageRating", "status", "isInIntranet", "isStandAlone" };
            } else {
                fieldsReq = new String[fieldsRequired.size()];
                for(int i=0;i<fieldsReq.length;i++){
                    fieldsReq[i] = fieldsRequired.get(i);
                }
            }

 

            SearchResponse responseMeta = client.search(
                    new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
                            .types(LexProjectUtil.EsType.new_lex_search.getTypeName()).source(new SearchSourceBuilder()
                                    .fetchSource(fieldsReq, null).query(boolQuery).size(pageSize)),
                    RequestOptions.DEFAULT);

         // System.out.println(responseMeta.getHits().totalHits);

            for (SearchHit hit : responseMeta.getHits()) {
                Map<String, Object> courseMeta = hit.getSourceAsMap();
                if (courseMeta != null && !courseMeta.isEmpty() && courseMeta.get("identifier") != null
                        && (scores.get(courseMeta.get("identifier")) != null)) {
                    double score = scores.get(courseMeta.get("identifier"));
                    if (score > 0.01) {
                        CourseRecommendation recommendation = new CourseRecommendation(score, courseMeta, null);
                        trendingCourses.add(recommendation);
                    }
                }
            }
            return trendingCourses.stream().sorted().filter(recommendation -> recommendation.getCourse() != null)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    
    
    public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr,
            String pageSizeStr, Boolean isInIntranet, List<String> langCodes, List<String> fieldsRequired) {

         System.out.println("-----------"+rootOrg+" "+userId+" "+pageNumberStr+" "+pageSizeStr+" "+isInIntranet+" "+langCodes.toString()+" "+fieldsRequired.toString()+"------------");
 

        int pageNumber;
        int pageSize;
        
        
       

        try {
            pageNumber = Integer.valueOf(pageNumberStr);
            pageSize = Integer.valueOf(pageSizeStr);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Page Size and Page Number should be numbers");
        }

 

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Page Size and Page Number should be non negative");
        }

 

        try {
            RestHighLevelClient client = ConnectionManager.getClient();
            BoolQueryBuilder trendingBoolQuery = QueryBuilders.boolQuery();
            trendingBoolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));
            trendingBoolQuery.filter(QueryBuilders.termQuery("root_org.keyword", rootOrg));
            
            if(rootOrg!=null && !rootOrg.equals("Infosys") && isInIntranet!=null && isInIntranet==false){
                 trendingBoolQuery.filter(QueryBuilders.termQuery("isInIntranet", isInIntranet));
                 System.out.println("---coming here----");
            }
            
            
            SearchResponse response = client
                    .search(new SearchRequest().indices(Util.TRENDING_SCORE_INDEX).types(Util.TRENDING_SCORE_INDEX_TYPE)
                            .source(new SearchSourceBuilder().query(trendingBoolQuery)
                                    .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)).size(pageSize)
                                    .from(pageNumber * pageSize)),
                            RequestOptions.DEFAULT);

 

            ArrayList<CourseRecommendation> trendingCourses = new ArrayList<>();
            ArrayList<String> itemIds = new ArrayList<>();
            HashMap<String, Double> scores = new HashMap<>();
            
            
            System.out.println("------"+response.getHits().totalHits+"-------");
            
            
            for (SearchHit hit : response.getHits()) {
                Map<String, Object> map = hit.getSourceAsMap();
                scores.put((String) map.get("course_id"), (Double) map.get("score"));
                itemIds.add((String) map.get("course_id"));
            }

 

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (accessControlEnabled && userId != null && !userId.equals("")) {
                Response accessControlResponse = accessControlService.getUserAccess(userId.toString(), null);
                List<String> accessPaths = (ArrayList<String>) accessControlResponse.get("combinedAccessPaths");
                boolQuery.filter(QueryBuilders.termsQuery("accessPaths", accessPaths));
            }

 

            for (String id : itemIds) {
                boolQuery.should(QueryBuilders.termQuery("identifier", id));
            }

 

            // boolQuery.filter(QueryBuilders.termsQuery("locale", langCodes));

 

            List<String> statusAllowed = new ArrayList<String>();
            statusAllowed.add("Live");
            statusAllowed.add("MarkedForDeletion");

 

            boolQuery.filter(QueryBuilders.termsQuery("status", statusAllowed));

 

            String[] fieldsReq = null;

            System.out.println("--------------printing before fieldsReq---------------");

            if (fieldsRequired == null || fieldsRequired.isEmpty()) {
                fieldsReq = new String[] { "name", "identifier", "description", "resourceType", "contentType",
                        "isExternalCourse", "learningMode", "appIcon", "artifactUrl", "children", "mimeType",
                        "creatorContacts", "downloadUrl", "duration", "me_totalSessionsCount", "size",
                        "complexityLevel", "lastUpdatedOn", "resourceCategory", "averageRating", "status", "isInIntranet", "isStandAlone" };
            } else {
                fieldsReq = fieldsRequired.toArray(fieldsReq);
            }

            System.out.println("----------printing after fieldsReq---------------");
            
            SearchResponse responseMeta = client.search(
                    new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
                            .types(LexProjectUtil.EsType.new_lex_search.getTypeName()).source(new SearchSourceBuilder()
                                    .fetchSource(fieldsReq, null).query(boolQuery).size(pageSize)),
                    RequestOptions.DEFAULT);

            System.out.println("---------------"+response.getHits().totalHits+"---------------");
 

            for (SearchHit hit : responseMeta.getHits()) {
                Map<String, Object> courseMeta = hit.getSourceAsMap();
                if (courseMeta != null && !courseMeta.isEmpty() && courseMeta.get("identifier") != null
                        && (scores.get(courseMeta.get("identifier")) != null)) {
                    double score = scores.get(courseMeta.get("identifier"));
                    if (score > 0.01) {
                        CourseRecommendation recommendation = new CourseRecommendation(score, courseMeta, null);
                        trendingCourses.add(recommendation);
                        System.out.println(courseMeta.get("identifier"));
                    }
                }
            }
            return trendingCourses.stream().sorted().filter(recommendation -> recommendation.getCourse() != null)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }










}