/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.global.ParsedGlobal;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.model.FilterItem;
import com.infosys.model.cassandra.UserAccessPathsModel;
import com.infosys.repository.UserAccessPathsRepository;
import com.infosys.service.SearchTemplateService;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.SearchConstants;

@Service
public class SearchTemplateServiceImpl implements SearchTemplateService {

	@Autowired
	private UserAccessPathsRepository userAccessPathsRepository;

	private RestHighLevelClient elasticClient = ConnectionManager.getClient();

	@SuppressWarnings("unchecked")
	@Override
	public Response adminSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException {
		Map<String, Object> validatedRequest = null;
		try {
			validatedRequest = validateRequest(request, accessControlEnabled, 0);
		} catch (BadRequestException e) {
			throw new BadRequestException(e.getMessage());
		} catch (ApplicationLogicError e) {
			throw new ApplicationLogicError(e.getMessage());
		} catch (Exception e) {
			throw new ApplicationLogicError(e.getMessage());
		}

		Map<String, Object> paramsMap = new HashMap<>();
		List<String> orgs = (ArrayList<String>) validatedRequest.get("orgs");
		UUID userId = (UUID) validatedRequest.get("userId");

		if (accessControlEnabled) {
			addOrgs(validatedRequest, orgs, userId, rootOrg);
			validatedRequest.put("rootOrg", Arrays.asList(rootOrg));
		}
		validatedRequest.put("accessControlEnabled", accessControlEnabled);

		Response response = searchService(validatedRequest);

		return response;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Response generalSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException {
		Map<String, Object> validatedRequest = null;
		try {
			validatedRequest = validateRequest(request, accessControlEnabled, 1);
		} catch (BadRequestException e) {
			throw new BadRequestException(e.getMessage());
		} catch (ApplicationLogicError e) {
			throw new ApplicationLogicError(e.getMessage());
		} catch (Exception e) {
			throw new ApplicationLogicError(e.getMessage());
		}

		Boolean isCatalog = (Boolean) validatedRequest.get("isCatalog");
		List<String> orgs = (ArrayList<String>) validatedRequest.get("orgs");
		UUID userId = (UUID) validatedRequest.get("userId");
		Boolean isStandAlone = (Boolean) validatedRequest.get("isStandAlone");

		if (accessControlEnabled) {
			List<UserAccessPathsModel> userDataList = addOrgs(validatedRequest, orgs, userId, rootOrg);
			addAccessPathsToParamsMap(validatedRequest, orgs, userId, rootOrg, userDataList);
			validatedRequest.put("rootOrg", Arrays.asList(rootOrg));
		}
		validatedRequest.put("accessControlEnabled", accessControlEnabled);
		validatedRequest.put("filterStatusVal", Arrays.asList("Live","Deleted"));

		Response response = searchService(validatedRequest);

		if ((Long) ((Map<String, Object>) (response.get("response"))).get("totalHits") == 0) {
			if (null == isCatalog || !isCatalog) {
				if (null != isStandAlone && isStandAlone) {
					validatedRequest.put("isStandAlone", null);
					return searchService(validatedRequest);
				} else {
					((Map<String, Object>) response.get("response")).put("filters",
							new ArrayList<Map<String, Object>>());
				}
			}
		}
		return response;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Response authoringSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException {
		Map<String, Object> validatedRequest = null;
		try {
			validatedRequest = validateRequest(request, accessControlEnabled, 2);
		} catch (BadRequestException e) {
			throw new BadRequestException(e.getMessage());
		} catch (ApplicationLogicError e) {
			throw new ApplicationLogicError(e.getMessage());
		} catch (Exception e) {
			throw new ApplicationLogicError(e.getMessage());
		}

		Boolean isCatalog = (Boolean) validatedRequest.get("isCatalog");
		List<String> orgs = (ArrayList<String>) validatedRequest.get("orgs");
		UUID userId = (UUID) validatedRequest.get("userId");
		Boolean isStandAlone = (Boolean) validatedRequest.get("isStandAlone");

		if (accessControlEnabled) {
			List<UserAccessPathsModel> userDataList = addOrgs(validatedRequest, orgs, userId, rootOrg);
			addAccessPathsToParamsMap(validatedRequest, orgs, userId, rootOrg, userDataList);
			validatedRequest.put("rootOrg", Arrays.asList(rootOrg));
		}
		validatedRequest.put("accessControlEnabled", accessControlEnabled);

		Response response = searchService(validatedRequest);

		if ((Long) ((Map<String, Object>) (response.get("response"))).get("totalHits") == 0) {
			if (null == isCatalog || !isCatalog) {
				if (null != isStandAlone && isStandAlone) {
					validatedRequest.put("isStandAlone", null);
					return searchService(validatedRequest);
				} else {
					((Map<String, Object>) response.get("response")).put("filters",
							new ArrayList<Map<String, Object>>());
				}
			}
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	public Response searchService(Map<String, Object> validatedRequest) throws IOException {
		Response response = new Response();
		Map<String, Object> paramsMap = new HashMap<>();
		String query = (String) validatedRequest.get("query");
		Boolean isStandAlone = (Boolean) validatedRequest.get("isStandAlone");
		Integer pageNumber = (Integer) validatedRequest.get("pageNumber");
		Integer pageSize = (Integer) validatedRequest.get("pageSize");
		Map<String, List<String>> filtersUsed = (Map<String, List<String>>) validatedRequest.get("filtersUsed");
		List<Map<String, Object>> filtersNotUsed = (ArrayList<Map<String, Object>>) validatedRequest.get("filtersNotUsed");
		Boolean accessControlEnabled = (Boolean) validatedRequest.get("accessControlEnabled");
		String locale = (String) validatedRequest.get("locale");
		List<Map<String, String>> sort = (List<Map<String, String>>) validatedRequest.get("sort");
		List<String> status = (List<String>) validatedRequest.get("filterStatusVal");
		
		paramsMap.put("filterStatusVal", status);
		paramsMap.put("filterStatus", true);

		if (accessControlEnabled) {
			List<String> accessPaths = (List<String>) validatedRequest.get("accessPaths");
			List<String> rootOrg = (List<String>) validatedRequest.get("rootOrg");
			List<String> orgs = (List<String>) validatedRequest.get("org");
			paramsMap.put("accessPaths", accessPaths);
			paramsMap.put("rootOrg", rootOrg);
			paramsMap.put("org", orgs);
		}
		paramsMap.put("accessControlEnabled", accessControlEnabled);

		if (null == query || query.isEmpty() || query.toLowerCase().equals("all") || query.equals("*")) {
			if (null != sort && !sort.isEmpty()) {
				paramsMap.put("sort", true);
				paramsMap.put("sortVal", sort);
			}
		} else if (isPhraseQuery(query)) {
			query = query.substring(1, query.length() - 1);
			paramsMap.put("must", true);
			paramsMap.put("mustId", false);
			paramsMap.put("should", false);
			paramsMap.put("searchTerm", query);
		} else {
			String[] identifierQuery = query.split(",");
			if (identifierQuery.length > 1) {
				paramsMap.put("filterIdentifier", true);
				paramsMap.put("filterIdentifierVal", Arrays.asList(identifierQuery));
			}else {
				paramsMap.put("must", true);
				paramsMap.put("mustId", true);
				paramsMap.put("should", true);
				paramsMap.put("searchTerm", query);
			}
		}

		paramsMap.put("from", pageNumber * pageSize);
		paramsMap.put("size", pageSize);

		if (null != isStandAlone && isStandAlone) {
			paramsMap.put("isStandAlone", true);
			paramsMap.put("isStandAloneVal", true);
		} else if (null != isStandAlone && !isStandAlone) {
			paramsMap.put("isStandAlone", true);
			paramsMap.put("isStandAloneVal", false);
		}

		Map<String, List<String>> aggAndFilterNamesMap = getAggAndFilterNamesMap();

		for (String key : filtersUsed.keySet()) {
			if (key.equals(SearchConstants.FILTER_LAST_UPDATED_ON_FIELD_KEY)) {
				String val = filtersUsed.get(key).get(0);
				boolean flg = true;
				switch (val) {
				case "week":
					val = "now-1w";
					break;
				case "month":
					val = "now-1M";
					break;
				case "year":
					val = "now-1y";
					break;
				default:
					val = "now/d";
					flg = false;
					break;
				}
				if (flg) {
					paramsMap.put("filter" + WordUtils.capitalize(key) + "Gte", val);
					paramsMap.put("filter" + WordUtils.capitalize(key) + "Lte", "now/d");
					paramsMap.put("filter" + WordUtils.capitalize(key), true);
				}
			} else if (key.equals(SearchConstants.FILTER_DURATION_FIELD_KEY)) {
				String[] data = (String[]) filtersUsed.get(key).toArray(new String[filtersUsed.get(key).size()]);
				int size = data.length;
				List<Map<String, Object>> items = new ArrayList<>();
				for (int i = 0; i < size; i++) {
					Long[] range = getDurationRangeArray(data[i]);
					if (range == null) {
						throw new BadRequestException("DURATION RANGE INVALID");
					}
					Map<String, Object> tempMap = new HashMap<>();
					tempMap.put("filter" + WordUtils.capitalize(key) + "ItemGte", range[0]);
					tempMap.put("filter" + WordUtils.capitalize(key) + "ItemLte", range[1]);
					if (i != size - 1) {
						tempMap.put("filter" + WordUtils.capitalize(key) + "ItemComma", true);
					}
					items.add(tempMap);
				}
				paramsMap.put("filter" + WordUtils.capitalize(key) + "Item", items);
				paramsMap.put("filter" + WordUtils.capitalize(key), true);
			} else if (key.equals(SearchConstants.FILTER_RESOURCE_CATEGORY_FIELD_KEY)
					|| key.equals(SearchConstants.FILTER_RESOURCE_TYPE_FIELD_KEY)) {
				if (filtersUsed.keySet().contains(SearchConstants.FILTER_CONTENT_TYPE_FIELD_KEY)) {
					boolean flg = true;
					for (String contentType : filtersUsed.get(SearchConstants.FILTER_CONTENT_TYPE_FIELD_KEY)) {
						if (contentType.equals("Resource")) {
							flg = false;
							paramsMap.put("resourceTypeAggs", true);
							paramsMap.put("resourceCategoryAggs", true);
							paramsMap.put("filter" + WordUtils.capitalize(key) + "Val", filtersUsed.get(key));
							paramsMap.put("filter" + WordUtils.capitalize(key), true);
							aggAndFilterNamesMap.put(SearchConstants.RESOURCE_CATEGORY_AGGS_KEY,
									SearchConstants.FIELD_DISPLAYNAME_PAIR
											.get(SearchConstants.RESOURCE_CATEGORY_AGGS_KEY));
							aggAndFilterNamesMap.put(SearchConstants.RESOURCE_TYPE_AGGS_KEY,
									SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.RESOURCE_TYPE_AGGS_KEY));
							break;
						}
					}
					if (flg) {
						Map<String, Object> x = new HashMap<>();
						x.put(key, "CAN ONLY BE USED IF CONTENT TYPE IS RESOURCE");
						filtersNotUsed.add(x);
					}
				} else {
					Map<String, Object> x = new HashMap<>();
					x.put(key, "CAN ONLY BE USED IF CONTENT TYPE IS RESOURCE");
					filtersNotUsed.add(x);
				}
			} else if (key.equals(SearchConstants.FILTER_IS_EXTERNAL_FIELD_KEY)
					|| key.equals(SearchConstants.FILTER_LEARNING_MODE_FIELD_KEY)) {
				if (filtersUsed.keySet().contains(SearchConstants.FILTER_CONTENT_TYPE_FIELD_KEY)) {
					if (filtersUsed.get(SearchConstants.FILTER_CONTENT_TYPE_FIELD_KEY).contains("Course")) {
						if (key.equals(SearchConstants.FILTER_IS_EXTERNAL_FIELD_KEY)) {
							paramsMap.put("filter" + WordUtils.capitalize(key) + "Val", filtersUsed.get(key));
							paramsMap.put("filter" + WordUtils.capitalize(key), true);
						} else {
							paramsMap.put("filter" + WordUtils.capitalize(key) + "Val", filtersUsed.get(key));
							paramsMap.put("filter" + WordUtils.capitalize(key), true);
						}
						paramsMap.put("learningModeAggs", true);
						paramsMap.put("isExternalCourseAggs", true);
						aggAndFilterNamesMap.put(SearchConstants.LEARNING_MODE_AGGS_KEY,
								SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.LEARNING_MODE_AGGS_KEY));
						aggAndFilterNamesMap.put(SearchConstants.IS_EXTERNAL_AGGS_KEY,
								SearchConstants.FIELD_DISPLAYNAME_PAIR
										.get(SearchConstants.IS_EXTERNAL_AGGS_KEY));
					} else {
						Map<String, Object> x = new HashMap<>();
						x.put(key, "CAN ONLY BE USED IF CONTENT TYPE IS COURSE");
						filtersNotUsed.add(x);
					}
				} else {
					Map<String, Object> x = new HashMap<>();
					x.put(key, "CAN ONLY BE USED IF CONTENT TYPE IS COURSE");
					filtersNotUsed.add(x);
				}
			} else if (key.equals(SearchConstants.FILTER_CONTENT_TYPE_FIELD_KEY)) {
				paramsMap.put("filter" + WordUtils.capitalize(key) + "Val", filtersUsed.get(key));
				paramsMap.put("filter" + WordUtils.capitalize(key), true);

				if (filtersUsed.get(key).contains("Resource")) {
					paramsMap.put("resourceTypeAggs", true);
					paramsMap.put("resourceCategoryAggs", true);
					aggAndFilterNamesMap.put(SearchConstants.RESOURCE_CATEGORY_AGGS_KEY,
							SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.RESOURCE_CATEGORY_AGGS_KEY));
					aggAndFilterNamesMap.put(SearchConstants.RESOURCE_TYPE_AGGS_KEY,
							SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.RESOURCE_TYPE_AGGS_KEY));
				}
				if (filtersUsed.get(key).contains("Course")) {
					paramsMap.put("learningModeAggs", true);
					paramsMap.put("isExternalCourseAggs", true);
					aggAndFilterNamesMap.put(SearchConstants.IS_EXTERNAL_AGGS_KEY,
							SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.IS_EXTERNAL_AGGS_KEY));
					aggAndFilterNamesMap.put(SearchConstants.LEARNING_MODE_AGGS_KEY,
							SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.LEARNING_MODE_AGGS_KEY));
				}
			} else {
				paramsMap.put("filter" + WordUtils.capitalize(key) + "Val", filtersUsed.get(key));
				paramsMap.put("filter" + WordUtils.capitalize(key), true);
			}
		}

		if (null == locale || locale.isEmpty())
			locale = "_en";
		System.out.println(paramsMap);
		SearchRequest searchRequest = new SearchRequest().searchType(SearchType.QUERY_THEN_FETCH);
		searchRequest.indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
		searchRequest.types(LexProjectUtil.EsType.new_lex_search.getTypeName());

		SearchTemplateRequest templateRequest=new SearchTemplateRequest();
		templateRequest.setScript(SearchConstants.SEARCH_TEMPLATE);
		templateRequest.setScriptType(ScriptType.STORED);
		templateRequest.setScriptParams(paramsMap);
		templateRequest.setRequest(searchRequest);
		
		SearchResponse searchResponse = elasticClient.searchTemplate(templateRequest,RequestOptions.DEFAULT).getResponse();

		List<Map<String, Object>> filtersMap = new ArrayList<Map<String, Object>>();
		Iterator<Map.Entry<String, List<String>>> aggAndFilterNamesItr = aggAndFilterNamesMap.entrySet().iterator();

		while (aggAndFilterNamesItr.hasNext()) {
			Map.Entry<String, List<String>> pair = (Map.Entry<String, List<String>>) aggAndFilterNamesItr.next();
			parseFilters(filtersMap, searchResponse, pair.getKey().toString(), pair.getValue().get(0).toString(),
					pair.getValue().get(1).toString());
		}

		List<Object> resultMetaList = new ArrayList<Object>();

		if (searchResponse.getHits().totalHits > 0) {
			List<FilterItem> filterItemList = new ArrayList<FilterItem>();
			FilterItem filterItem = new FilterItem();
			filterItem.setType("week");
			filterItem.setDisplayName("Last Week");
			filterItemList.add(filterItem);
			filterItem = new FilterItem();
			filterItem.setType("month");
			filterItem.setDisplayName("Last Month");
			filterItemList.add(filterItem);
			filterItem = new FilterItem();
			filterItem.setType("year");
			filterItem.setDisplayName("Last Year");
			filterItemList.add(filterItem);
			filterItem = new FilterItem();
			filterItem.setType("older");
			filterItem.setDisplayName("Older");
			filterItemList.add(filterItem);

			Map<String, Object> topicAggMap = new HashMap<String, Object>();
			topicAggMap.put("type", SearchConstants.FIELD_DISPLAYNAME_PAIR.get(SearchConstants.LAST_UPDATED_ON_AGGS_KEY)
					.get(0).toString());
			topicAggMap.put("displayName", SearchConstants.FIELD_DISPLAYNAME_PAIR
					.get(SearchConstants.LAST_UPDATED_ON_AGGS_KEY).get(1).toString());
			topicAggMap.put("content", filterItemList);
			filtersMap.add(topicAggMap);

			for (SearchHit hit : searchResponse.getHits()) {
				resultMetaList.add(hit.getSourceAsMap());
			}
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("totalHits", searchResponse.getHits().totalHits);
		result.put("result", resultMetaList);
		result.put("filters", filtersMap);
		result.put("filtersNotUsed", filtersNotUsed);
		if (null == isStandAlone && searchResponse.getHits().totalHits == 0)
			throw new NoContentException("NO DATA FOUND FOR QUERY AND FILTERS COMBINATIONS");
		// result.put("reason", "NO DATA FOUND FOR QUERY AND FILTERS COMBINATIONS");
		// indexSearchTermToAutocompleteIndex(userId, query);
		response.put("response", result);
		return response;
	}

//    @Async
//    private void indexSearchTermToAutocompleteIndex(UUID uid, String query) {
//    	if(null!=uid && null!=query && !query.isEmpty()) {
//		    String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
//		    int pos = date.lastIndexOf(":");
//		    date = date.substring(0, pos) + "." + date.substring(pos + 1);
//		    pos = date.indexOf("+");
//		    date = date.substring(0, pos);
//		
//		    Map<String, Object> data = new HashMap<>();
//		    data.put("id", uid.toString() + ":" + query.toLowerCase().replaceAll(" ", "_"));
//		    data.put("uid", uid.toString());
//		    data.put("searchword", query);
//		    data.put("dtSearched", new Date().getTime());
//		    data.put("dateInserted", date);
//		    elasticClient.prepareIndex("historyautocomplete", "autocomplete", uid.toString() + ":" + query.toLowerCase().replaceAll(" ", "_")).setSource(data).get();
//    	}
//    }

	private List<UserAccessPathsModel> addOrgs(Map<String, Object> paramsMap, List<String> orgs, UUID userId,
			String rootOrg) {
		List<UserAccessPathsModel> userDataList;
		if (null != orgs) {
			userDataList = userAccessPathsRepository
					.findByPrimaryKeyRootOrgAndPrimaryKeyOrgInAndPrimaryKeyUserId(rootOrg, orgs, userId);
		} else {
			userDataList = userAccessPathsRepository.findByPrimaryKeyRootOrgAndPrimaryKeyUserId(rootOrg, userId);
		}

		if (null == orgs) {
			orgs = new ArrayList<>();
			for (UserAccessPathsModel userData : userDataList) {
				orgs.add(userData.getPrimaryKey().getOrg());
			}
			orgs = new ArrayList<String>(new HashSet<String>(orgs));
		}

		paramsMap.put("org", orgs);
		return userDataList;
	}

	private void addAccessPathsToParamsMap(Map<String, Object> paramsMap, List<String> orgs, UUID userId,
			String rootOrg, List<UserAccessPathsModel> userDataList) throws IOException {
		Set<String> accessPathsArrayList = new HashSet<>();

		for (UserAccessPathsModel userData : userDataList) {
			accessPathsArrayList.addAll(userData.getAccessPaths());
		}

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.must(QueryBuilders.termQuery("userIds", userId.toString()));
		boolQuery.filter(QueryBuilders.termQuery("rootOrg", rootOrg));
		if (null != orgs) {
			boolQuery.filter(QueryBuilders.termsQuery("org", orgs));
		}

		SearchResponse elasticData = elasticClient.search(new SearchRequest()
				.indices(LexProjectUtil.EsIndex.access_control_groups.getIndexName())
				.types(LexProjectUtil.EsType.access_control_group.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
				.source(new SearchSourceBuilder().query(boolQuery)
						.aggregation(AggregationBuilders.terms("accessPathsAggs").field("accessPaths").size(10000))
						.size(0)),
				RequestOptions.DEFAULT);

		Terms aggs = elasticData.getAggregations().get("accessPathsAggs");
		for (Bucket bucket : aggs.getBuckets()) {
			accessPathsArrayList.add(bucket.getKeyAsString());
		}

		if (accessPathsArrayList.size() == 0)
			throw new NoContentException("YOU DO NOT HAVE ACCESS TO ANY CONTENT");

		paramsMap.put("accessPaths", new ArrayList<String>(accessPathsArrayList));
	}

	private Map<String, List<String>> getAggAndFilterNamesMap() {
		Map<String, List<String>> x = new HashMap<String, List<String>>(SearchConstants.aggAndFilterNamesMap);
		return x;
	}

	private void parseFilters(List<Map<String, Object>> completeMap, SearchResponse searchResponse,
			String aggregationName, String filterName, String displayName) {

		ParsedGlobal internalGlobalAggs = searchResponse.getAggregations().get("TotalAggs");
		boolean isTagsAgg = false;
		List<Object> filterItemList = new ArrayList<>();
		List<Map<String, Object>> allRootNodes = new ArrayList<>();
		if (aggregationName.equals(SearchConstants.DURATION_AGGS_KEY)) {
			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			Range aggregation = internalFilter.getAggregations().get(aggregationName);
			for (Range.Bucket bucket : aggregation.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					if (bucket.getKeyAsString().equals(SearchConstants.DURATION_SHORT_DISPLAY_NAME))
						filterItem.setType(SearchConstants.DURATION_SHORT);
					else if (bucket.getKeyAsString().equals(SearchConstants.DURATION_MEDIUM_DISPLAY_NAME))
						filterItem.setType(SearchConstants.DURATION_MEDIUM);
					else if (bucket.getKeyAsString().equals(SearchConstants.DURATION_LONG_DISPLAY_NAME))
						filterItem.setType(SearchConstants.DURATION_LONG);
					else
						filterItem.setType(SearchConstants.DURATION_DETAILED);
					filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}
			}
		} else if (aggregationName.equals(SearchConstants.CATALOG_PATHS_AGGS_KEY)) {
			isTagsAgg = true;
			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			Terms aggregation = internalFilter.getAggregations().get(aggregationName);
			TreeNode root = new TreeNode("ROOT", "ROOT", -1l);

			for (Bucket bucket : aggregation.getBuckets()) {
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					TreeNode cur = root;
					for (String key : bucket.getKeyAsString().split("/")) {
						cur = cur.addChild(new TreeNode(key, bucket.getKeyAsString(), bucket.getDocCount()));
					}
				}
			}

			for (TreeNode child : root.children) {
				allRootNodes.add(parseTree(child));
			}
		} else if (aggregationName.equals(SearchConstants.CONCEPTS_AGGS_KEY)) {

			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			ParsedNested internalNested = internalFilter.getAggregations().get(aggregationName);
			Map<String, Aggregation> subAggregation = internalNested.getAggregations().asMap();
			Terms termsAgg = (Terms) subAggregation.get("concepts_identifier_aggs");
			for (Bucket bucket : termsAgg.getBuckets()) {
				Map<String, Aggregation> subSubAggregation = bucket.getAggregations().asMap();
				Terms subTermsAgg = (Terms) subSubAggregation.get("concepts_name_aggs");
				Bucket subBucket = subTermsAgg.getBuckets().get(0);
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					filterItem.setType(subBucket.getKeyAsString());
					filterItem.setDisplayName(subBucket.getKeyAsString());
					filterItem.setId(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}
			}
		} else if (aggregationName.equals(SearchConstants.IS_EXTERNAL_AGGS_KEY)) {
			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			ParsedLongTerms stringTerms = internalFilter.getAggregations().get(aggregationName);
			for (Bucket bucket : stringTerms.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					filterItem.setType(bucket.getKeyAsString());
					if (bucket.getKeyAsString().equals("true"))
						filterItem.setDisplayName("Yes");
					else
						filterItem.setDisplayName("No");
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}
			}
		} else if(aggregationName.equals(SearchConstants.CONTENT_TYPE_AGGS_KEY)){
			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			ParsedStringTerms stringTerms = internalFilter.getAggregations().get(aggregationName);
			for (Bucket bucket : stringTerms.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					filterItem.setType(bucket.getKeyAsString());
					if(bucket.getKeyAsString().equals("Collection"))
						filterItem.setDisplayName("Module");
					else
						filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}
			}
		} else {
			ParsedFilter internalFilter = internalGlobalAggs.getAggregations().get(aggregationName);
			ParsedStringTerms stringTerms = internalFilter.getAggregations().get(aggregationName);
			for (Bucket bucket : stringTerms.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
				} else if (bucket.getDocCount() > 0) {
					filterItem.setType(bucket.getKeyAsString());
					filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}
			}
		}

		if (!allRootNodes.isEmpty() || !filterItemList.isEmpty()) {
			Map<String, Object> topicAggMap = new HashMap<String, Object>();
			topicAggMap.put("type", filterName);
			topicAggMap.put("displayName", displayName);
			if (isTagsAgg) {
				topicAggMap.put("content", allRootNodes);
			} else {
				topicAggMap.put("content", filterItemList);
			}
			completeMap.add(topicAggMap);
		}
	}

	private Long[] getDurationRangeArray(String string) {
		Long[] rangeArray = new Long[2];
		switch (string) {
		case SearchConstants.DURATION_SHORT:
			rangeArray[0] = Long.MIN_VALUE;
			rangeArray[1] = SearchConstants.DURATION_SHORT_MAX;
			break;
		case SearchConstants.DURATION_MEDIUM:
			rangeArray[0] = SearchConstants.DURATION_MEDIUM_MIN;
			rangeArray[1] = SearchConstants.DURATION_MEDIUM_MAX;
			break;
		case SearchConstants.DURATION_LONG:
			rangeArray[0] = SearchConstants.DURATION_LONG_MIN;
			rangeArray[1] = SearchConstants.DURATION_LONG_MAX;
			break;
		case SearchConstants.DURATION_DETAILED:
			rangeArray[0] = SearchConstants.DURATION_DETAILED_MIN;
			rangeArray[1] = Long.MAX_VALUE;
			break;

		default:
			return null;
		}
		return rangeArray;
	}

	private boolean isPhraseQuery(String query) {
		if (!query.equals("\"") && !query.equals("\"\"") && query.charAt(0) == '"'
				&& query.charAt(query.length() - 1) == '"'
				|| (!query.equals("'") && !query.equals("''") && query.charAt(0) == '\''
						&& query.charAt(query.length() - 1) == '\''))
			return true;
		else
			return false;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> validateRequest(Request request, boolean accessControlEnabled, int requestType) {
		Map<String, Object> returnMap = new HashMap<>();
		Map<String, Object> requestMap;
		UUID userId = null;
		Boolean isCatalog = null;
		String query = null;
		Boolean isStandAlone = null;
		Map<String, Object> filterMap = null;
		Integer pageNumber = null;
		Integer pageSize = null;
		List<String> orgs = null;
		List<Map<String, String>> sort = null;

		try {
			requestMap = request.getRequest();
		} catch (Exception e) {
			throw new BadRequestException("REQUEST MUST BE AN OBJECT");
		}

		try {
			userId = UUID.fromString((String) requestMap.get(SearchConstants.USER_ID));
		} catch (ClassCastException | IllegalArgumentException e) {
			if (accessControlEnabled) {
				throw new BadRequestException("userId MUST BE A UUID");
			}
		} catch (Exception e) {
			if (accessControlEnabled) {
				throw new ApplicationLogicError("userId");
			}
		}

		if (accessControlEnabled && null == userId || userId.toString().isEmpty()) {
			throw new BadRequestException("PLEASE PROVIDE userId");
		}

		try {
			orgs = (ArrayList<String>) requestMap.get(SearchConstants.ORG);
			if (null != orgs) {
				if (orgs.isEmpty())
					orgs = null;
			}
		} catch (ClassCastException e) {
			if (accessControlEnabled) {
				throw new BadRequestException("orgs MUST BE A LIST OF STRING");
			}
		} catch (Exception e) {
			if (accessControlEnabled) {
				throw new ApplicationLogicError("orgs");
			}
		}

		try {
			isCatalog = (Boolean) requestMap.get(SearchConstants.IS_CATALOG);
			if (null == isCatalog)
				isCatalog = false;
		} catch (ClassCastException e) {
			throw new BadRequestException("catalog MUST BE A BOOLEAN");
		} catch (Exception e) {
			throw new ApplicationLogicError("isCatalog");
		}

		try {
			query = (String) requestMap.get(SearchConstants.QUERY);
		} catch (ClassCastException e) {
			throw new BadRequestException("query MUST BE A STRING");
		} catch (Exception e) {
			throw new ApplicationLogicError("query");
		}

		try {
			isStandAlone = (Boolean) requestMap.get(SearchConstants.IS_STAND_ALONE);
		} catch (ClassCastException e) {
			throw new BadRequestException("isStandAlone MUST BE A BOOLEAN");
		} catch (Exception e) {
			throw new ApplicationLogicError("isStandAlone");
		}

		try {
			filterMap = (Map<String, Object>) requestMap.get(SearchConstants.FILTERS);
		} catch (ClassCastException e) {
			throw new BadRequestException("FILTERS SHOULD BE AN OBJECT OF <STRING,ARRAY OF STRINGS>");
		} catch (Exception e) {
			throw new ApplicationLogicError("filterMap");
		}

		Map<String, List<String>> filtersUsed = new HashMap<>();
		List<Map<String, String>> filtersNotUsed = new ArrayList<>();
		boolean invalidFlag = true;
		if (query == null || query.isEmpty()) {
			try {
				sort = (List<Map<String, String>>) requestMap.get(SearchConstants.SORT);
				if (null != sort)
					sort = sort.stream()
							.filter(item -> null != item && !item.isEmpty() && item.size() == 1
									&& SearchConstants.SORT_FIELDS.containsAll(item.keySet())
									&& (item.values().contains("asc") || item.values().contains("desc")))
							.collect(Collectors.toList());
			} catch (Exception e) {
				e.printStackTrace();
				throw new BadRequestException("SORT SHOULD BE A LIST OF {FIELD,ORDER}");
			}
			if (filterMap == null || filterMap.isEmpty()) {
				throw new BadRequestException("EITHER QUERY OR ATLEAST ONE FILTER MUST BE SPECIFIED");
			}
			invalidFlag = false;
		} else if (!query.isEmpty()) {
			invalidFlag = false;
		}
		if (filterMap != null && !filterMap.isEmpty()) {
			for (String key : filterMap.keySet()) {
				if (null != filterMap.get(key)) {
					if (!SearchConstants.LIST_FILTER_FIELD_KEYS.contains(key)) {
						Map<String, String> x = new HashMap<>();
						x.put(key, "FILTER NOT SUPPORTED");
						filtersNotUsed.add(x);
						continue;
					}
					
					if(requestType == 1) {
						if(key.equals("status")) {
							Map<String, String> x = new HashMap<>();
							x.put(key, "FILTER NOT SUPPORTED");
							filtersNotUsed.add(x);
							continue;
						}
					}

					List<String> x = null;
					try {
						x = (ArrayList<String>) filterMap.get(key);
					} catch (ClassCastException e) {
						throw new BadRequestException("FILTER-> " + key + " MUST BE LIST OF STRINGS");
					} catch (Exception e) {
						Map<String, String> y = new HashMap<>();
						y.put(key, "UNKNOW EXCEPTION");
						filtersNotUsed.add(y);
					}
					for (int i = 0; i < x.size(); i++) {
						if (StringUtils.isBlank(x.get(i))) {
							x.remove(i);
							i = i - 1;
						}
					}

					if (x.size() == 0) {
						Map<String, String> y = new HashMap<>();
						y.put(key, "FILTER IS EMPTY");
						filtersNotUsed.add(y);
					} else {
						filtersUsed.put(key, x);
					}
				} else {
					Map<String, String> x = new HashMap<>();
					x.put(key, "FILTER IS NULL");
					filtersNotUsed.add(x);
				}
			}
		}

		if (null == query && filtersUsed.size() == 0) {
			throw new BadRequestException("EITHER QUERY OR ATLEAST ONE FILTER MUST BE SPECIFIED");
		}

		if (invalidFlag) {
			throw new BadRequestException("EITHER QUERY OR ATLEAST ONE FILTER MUST BE SPECIFIED");
		}

		try {
			pageNumber = (Integer) requestMap.get("pageNo");
			if (pageNumber == null)
				pageNumber = 0;
		} catch (ClassCastException e) {
			throw new BadRequestException("pageNo SHOULD BE AN INTEGER");
		} catch (Exception e) {
			throw new ApplicationLogicError("pageNo");
		}

		try {
			pageSize = (Integer) requestMap.get("pageSize");
			if (pageSize == null)
				pageSize = 10;
		} catch (ClassCastException e) {
			throw new BadRequestException("pageSize SHOULD BE AN INTEGER");
		} catch (Exception e) {
			throw new ApplicationLogicError("pageNo");
		}

		returnMap.put("orgs", orgs);
		returnMap.put("filtersUsed", filtersUsed);
		returnMap.put("filtersNotUsed", filtersNotUsed);
		returnMap.put("userId", userId);
		returnMap.put("isCatalog", isCatalog);
		returnMap.put("query", query);
		returnMap.put("isStandAlone", isStandAlone);
		returnMap.put("filterMap", filterMap);
		returnMap.put("pageNumber", pageNumber);
		returnMap.put("pageSize", pageSize);
		returnMap.put("sort", sort);

		return returnMap;
	}

	private class TreeNode {
		private final Set<TreeNode> children = new LinkedHashSet<>();
		private final String type;
		private final Long count;
		private final String key;

		TreeNode(String key, String type, Long count) {
			this.type = type;
			this.count = count;
			this.key = key;
		}

		TreeNode addChild(TreeNode newChild) {
			for (TreeNode child : children) {
				if (child.key.equals(newChild.key)) {
					return child;
				}
			}
			children.add(new TreeNode(newChild.key, newChild.type, newChild.count));
			return newChild;
		}
	}

	private Map<String, Object> parseTree(TreeNode root) {
		Map<String, Object> map = new HashMap<>();
		List<Map<String, Object>> childrenList = new ArrayList<>();
		map.put("type", root.type);
		map.put("displayName", root.key);
		map.put("count", root.count);
		if (root.children.isEmpty()) {

		} else {
			for (TreeNode child : root.children) {
				childrenList.add(parseTree(child));
			}
		}
		map.put("children", childrenList);
		return map;
	}
}
