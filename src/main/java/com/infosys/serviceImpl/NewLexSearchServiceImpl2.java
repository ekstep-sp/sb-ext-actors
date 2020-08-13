/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
// package com.infosys.serviceImpl;
//
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Map;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
//
// import org.elasticsearch.action.search.SearchRequestBuilder;
// import org.elasticsearch.action.search.SearchResponse;
// import org.elasticsearch.action.search.SearchType;
// import org.elasticsearch.client.Client;
// import org.elasticsearch.index.query.BoolQueryBuilder;
// import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
// import org.elasticsearch.index.query.QueryBuilders;
// import org.elasticsearch.search.SearchHit;
// import org.elasticsearch.search.aggregations.Aggregation;
// import org.elasticsearch.search.aggregations.AggregationBuilders;
// import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
// import
// org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
// import org.elasticsearch.search.aggregations.bucket.global.InternalGlobal;
// import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
// import org.elasticsearch.search.aggregations.bucket.range.Range;
// import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
// import org.elasticsearch.search.aggregations.bucket.terms.Terms;
// import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
// import org.springframework.stereotype.Service;
// import org.sunbird.common.models.response.Response;
// import org.sunbird.common.request.Request;
// import org.sunbird.common.responsecode.ResponseCode;
// import org.sunbird.helper.ConnectionManager;
//
// import com.infosys.model.FilterClass;
// import com.infosys.model.FilterItem;
// import com.infosys.service.NewLexSearchService;
// import com.infosys.util.Constants;
// import com.infosys.util.LexProjectUtil;
//
// @Service
// public class NewLexSearchServiceImpl2 implements NewLexSearchService {
//
// public static void addCategoriesNode(Terms.Bucket bucket, List<Map<String,
// Object>> allRootNodes) {
// Map<String, Object> rootMap = new HashMap<>();
// rootMap.put("type", bucket.getKeyAsString());
// rootMap.put("displayName", bucket.getKeyAsString());
// List<Map<String, Object>> childrenList = new ArrayList<>();
// rootMap.put("children", childrenList);
// rootMap.put("count", bucket.getDocCount());
// allRootNodes.add(rootMap);
// }
//
// public static void addTracksNode(Terms.Bucket bucket, List<Map<String,
// Object>> allRootNodes) {
// String rootKey = bucket.getKeyAsString().split("/")[0];
// for (Map<String, Object> rootMap : allRootNodes) {
// if (rootMap.get("type").equals(rootKey)) {
// allRootNodes.remove(rootMap);
//
// Map<String, Object> subNode = new HashMap<>();
// subNode.put("type", bucket.getKeyAsString());
// subNode.put("displayName", bucket.getKeyAsString().split("/")[1]);
// List<Map<String, Object>> childrenList = new ArrayList<>();
// subNode.put("children", childrenList);
// subNode.put("count", bucket.getDocCount());
//
// List<Map<String, Object>> rootNodeChildren = (List<Map<String, Object>>)
// rootMap.get("children");
// rootNodeChildren.add(subNode);
// rootMap.put("children", rootNodeChildren);
// allRootNodes.add(rootMap);
// break;
// }
// }
// }
//
// public static void addSubTracksNode(Terms.Bucket bucket, List<Map<String,
// Object>> allRootNodes) {
// String[] keyArray = bucket.getKeyAsString().split("/");
// String rootKey = keyArray[0];
// String subRootKey = keyArray[1];
// for (Map<String, Object> rootMap : allRootNodes) {
// if (rootMap.get("type").equals(rootKey)) {
// for (Map<String, Object> subRootMap : (List<Map<String, Object>>)
// rootMap.get("children")) {
// if (subRootMap != null && subRootMap.get("type").equals(rootKey + "/" +
// subRootKey)) {
// List<Map<String, Object>> rootNodeChildren = (List<Map<String, Object>>)
// rootMap
// .get("children");
// rootNodeChildren.remove(subRootMap);
// allRootNodes.remove(rootMap);
// Map<String, Object> subSubNode = new HashMap<>();
// subSubNode.put("type", bucket.getKeyAsString());
// subSubNode.put("displayName", bucket.getKeyAsString().split("/")[2]);
// List<Map<String, Object>> childrenList = new ArrayList<>();
// subSubNode.put("children", childrenList);
// subSubNode.put("count", bucket.getDocCount());
// List<Map<String, Object>> subRootNodeChildren = (List<Map<String, Object>>)
// subRootMap
// .get("children");
// subRootNodeChildren.add(subSubNode);
// subRootMap.put("children", subRootNodeChildren);
// rootNodeChildren.add(subRootMap);
// rootMap.put("children", rootNodeChildren);
// allRootNodes.add(rootMap);
// break;
// }
// }
// break;
// }
// }
// }
//
// public static void addSubSubTracksNode(Terms.Bucket bucket, List<Map<String,
// Object>> allRootNodes) {
// String[] keyArray = bucket.getKeyAsString().split("/");
// String rootKey = keyArray[0];
// String subRootKey = keyArray[1];
// String subSubRootKey = keyArray[2];
// for (Map<String, Object> rootMap : allRootNodes) {
// if (rootMap.get("type").equals(rootKey)) {
// for (Map<String, Object> subRootMap : (List<Map<String, Object>>)
// rootMap.get("children")) {
// if (subRootMap != null && subRootMap.get("type").equals(rootKey + "/" +
// subRootKey)) {
// for (Map<String, Object> subSubRootMap : (List<Map<String, Object>>)
// subRootMap
// .get("children")) {
// if (subSubRootMap != null && subSubRootMap.get("type")
// .equals(rootKey + "/" + subRootKey + "/" + subSubRootKey)) {
// List<Map<String, Object>> subRootNodeChildren = (List<Map<String, Object>>)
// subRootMap
// .get("children");
// subRootNodeChildren.remove(subSubRootMap);
// List<Map<String, Object>> rootNodeChildren = (List<Map<String, Object>>)
// rootMap
// .get("children");
// rootNodeChildren.remove(subRootMap);
// allRootNodes.remove(rootMap);
//
// Map<String, Object> subSubSubNode = new HashMap<>();
// subSubSubNode.put("type", bucket.getKeyAsString());
// subSubSubNode.put("displayName", bucket.getKeyAsString().split("/")[3]);
// List<Map<String, Object>> childrenList = new ArrayList<>();
// subSubSubNode.put("children", childrenList);
// subSubSubNode.put("count", bucket.getDocCount());
//
// List<Map<String, Object>> subSubRootNodeChildren = (List<Map<String,
// Object>>) subSubRootMap
// .get("children");
// subSubRootNodeChildren.add(subSubSubNode);
// subSubRootMap.put("children", subSubRootNodeChildren);
//
// subRootNodeChildren.add(subSubRootMap);
// subRootMap.put("children", subRootNodeChildren);
//
// rootNodeChildren.add(subRootMap);
// rootMap.put("children", rootNodeChildren);
// allRootNodes.add(rootMap);
// break;
// }
// }
// break;
// }
// }
// break;
// }
// }
// }
//
// public static void addFiltersNew(List<Map<String, Object>> completeMap,
// SearchResponse searchResponse,
// String aggregationName, String filterName, String displayName) {
//
// InternalGlobal internalGlobalAggs =
// searchResponse.getAggregations().get("TotalAggs");
// boolean isTagsAgg = false;
// List<Object> filterItemList = new ArrayList<>();
// List<Map<String, Object>> allRootNodes = new ArrayList<>();
// if (aggregationName.equals(Constants.DURATION_AGGS_KEY)) {
// InternalFilter internalFilter =
// internalGlobalAggs.getAggregations().get(aggregationName);
// Range aggregation = internalFilter.getAggregations().get(aggregationName);
// for (Range.Bucket bucket : aggregation.getBuckets()) {
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// if (bucket.getKeyAsString().equals(Constants.DURATION_SHORT_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_SHORT);
// else if
// (bucket.getKeyAsString().equals(Constants.DURATION_MEDIUM_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_MEDIUM);
// else if
// (bucket.getKeyAsString().equals(Constants.DURATION_LONG_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_LONG);
// else
// filterItem.setType(Constants.DURATION_DETAILED);
// filterItem.setDisplayName(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// } else if (aggregationName.equals(Constants.TAGS_AGGS_KEY)) {
// isTagsAgg = true;
// Pattern pattern = Pattern.compile("/");
// InternalFilter internalFilter =
// internalGlobalAggs.getAggregations().get(aggregationName);
// Terms aggregation = internalFilter.getAggregations().get(aggregationName);
//
// List<Terms.Bucket> categoriesKeys = new ArrayList<>();
// List<Terms.Bucket> tracksKeys = new ArrayList<>();
// List<Terms.Bucket> subTracksKeys = new ArrayList<>();
// List<Terms.Bucket> subSubTracksKeys = new ArrayList<>();
//
// for (Terms.Bucket bucket : aggregation.getBuckets()) {
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// String key = bucket.getKeyAsString();
// Matcher matcher = pattern.matcher(key);
// int countOfSlash = 0;
// while (matcher.find()) {
// countOfSlash++;
// }
//
// if (countOfSlash == 0) {
// categoriesKeys.add(bucket);
// } else if (countOfSlash == 1) {
// tracksKeys.add(bucket);
// } else if (countOfSlash == 2) {
// subTracksKeys.add(bucket);
// } else if (countOfSlash == 3) {
// subSubTracksKeys.add(bucket);
// }
// }
// }
//
// for (Terms.Bucket bucket : categoriesKeys) {
// addCategoriesNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : tracksKeys) {
// addTracksNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : subTracksKeys) {
// addSubTracksNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : subSubTracksKeys) {
// addSubSubTracksNode(bucket, allRootNodes);
// }
// } else if (aggregationName.equals(Constants.CONCEPTS_AGGS_KEY)) {
// InternalFilter internalFilter =
// internalGlobalAggs.getAggregations().get(aggregationName);
// InternalNested internalNested =
// internalFilter.getAggregations().get(aggregationName);
// Map<String, Aggregation> subAggregation =
// internalNested.getAggregations().asMap();
// Terms termsAgg = (Terms) subAggregation.get("concepts_identifier_aggs");
// for (Terms.Bucket bucket : termsAgg.getBuckets()) {
// Map<String, Aggregation> subSubAggregation =
// bucket.getAggregations().asMap();
// Terms subTermsAgg = (Terms) subSubAggregation.get("concepts_name_aggs");
// Terms.Bucket subBucket = subTermsAgg.getBuckets().get(0);
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// filterItem.setType(subBucket.getKeyAsString());
// filterItem.setDisplayName(subBucket.getKeyAsString());
// filterItem.setId(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// } else {
// InternalFilter internalFilter =
// internalGlobalAggs.getAggregations().get(aggregationName);
// StringTerms stringTerms =
// internalFilter.getAggregations().get(aggregationName);
// for (Bucket bucket : stringTerms.getBuckets()) {
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// filterItem.setType(bucket.getKeyAsString());
// filterItem.setDisplayName(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// }
//
// Map<String, Object> topicAggMap = new HashMap<String, Object>();
// topicAggMap.put("type", filterName);
// topicAggMap.put("displayName", displayName);
// if (isTagsAgg) {
// topicAggMap.put("content", allRootNodes);
// } else {
// topicAggMap.put("content", filterItemList);
// }
// completeMap.add(topicAggMap);
// }
//
// public static void addFilters(List<Map<String, Object>> completeMap,
// SearchResponse searchResponse,
// String aggregationName, String filterName, String displayName) {
// boolean isTagsAgg = false;
// List<Object> filterItemList = new ArrayList<>();
// List<Map<String, Object>> allRootNodes = new ArrayList<>();
// if (aggregationName.equals(Constants.DURATION_AGGS_KEY)) {
// Range aggregation = searchResponse.getAggregations().get(aggregationName);
// for (Range.Bucket bucket : aggregation.getBuckets()) {
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// if (bucket.getKeyAsString().equals(Constants.DURATION_SHORT_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_SHORT);
// else if
// (bucket.getKeyAsString().equals(Constants.DURATION_MEDIUM_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_MEDIUM);
// else if
// (bucket.getKeyAsString().equals(Constants.DURATION_LONG_DISPLAY_NAME))
// filterItem.setType(Constants.DURATION_LONG);
// else
// filterItem.setType(Constants.DURATION_DETAILED);
// filterItem.setDisplayName(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// } else if (aggregationName.equals(Constants.TAGS_AGGS_KEY)) {
// isTagsAgg = true;
// Pattern pattern = Pattern.compile("/");
// Terms aggregation = searchResponse.getAggregations().get(aggregationName);
//
// List<Terms.Bucket> categoriesKeys = new ArrayList<>();
// List<Terms.Bucket> tracksKeys = new ArrayList<>();
// List<Terms.Bucket> subTracksKeys = new ArrayList<>();
// List<Terms.Bucket> subSubTracksKeys = new ArrayList<>();
//
// for (Terms.Bucket bucket : aggregation.getBuckets()) {
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// String key = bucket.getKeyAsString();
// Matcher matcher = pattern.matcher(key);
// int countOfSlash = 0;
// while (matcher.find()) {
// countOfSlash++;
// }
//
// if (countOfSlash == 0) {
// categoriesKeys.add(bucket);
// } else if (countOfSlash == 1) {
// tracksKeys.add(bucket);
// } else if (countOfSlash == 2) {
// subTracksKeys.add(bucket);
// } else if (countOfSlash == 3) {
// subSubTracksKeys.add(bucket);
// }
// }
// }
//
// for (Terms.Bucket bucket : categoriesKeys) {
// addCategoriesNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : tracksKeys) {
// addTracksNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : subTracksKeys) {
// addSubTracksNode(bucket, allRootNodes);
// }
// for (Terms.Bucket bucket : subSubTracksKeys) {
// addSubSubTracksNode(bucket, allRootNodes);
// }
// } else if (aggregationName.equals(Constants.CONCEPTS_AGGS_KEY)) {
// InternalNested aggregation =
// searchResponse.getAggregations().get(aggregationName);
// Map<String, Aggregation> subAggregation =
// aggregation.getAggregations().asMap();
// Terms termsAgg = (Terms) subAggregation.get("concepts_identifier_aggs");
// for (Terms.Bucket bucket : termsAgg.getBuckets()) {
// Map<String, Aggregation> subSubAggregation =
// bucket.getAggregations().asMap();
// Terms subTermsAgg = (Terms) subSubAggregation.get("concepts_name_aggs");
// Terms.Bucket subBucket = subTermsAgg.getBuckets().get(0);
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// filterItem.setType(subBucket.getKeyAsString());
// filterItem.setDisplayName(subBucket.getKeyAsString());
// filterItem.setId(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// } else {
// Terms aggregation = searchResponse.getAggregations().get(aggregationName);
// for (Terms.Bucket bucket : aggregation.getBuckets()) {
// FilterItem filterItem = new FilterItem();
// if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
// } else if (bucket.getDocCount() > 0) {
// filterItem.setType(bucket.getKeyAsString());
// filterItem.setDisplayName(bucket.getKeyAsString());
// filterItem.setCount(bucket.getDocCount());
// filterItemList.add(filterItem);
// }
// }
// }
//
// Map<String, Object> topicAggMap = new HashMap<String, Object>();
// topicAggMap.put("type", filterName);
// topicAggMap.put("displayName", displayName);
// if (isTagsAgg) {
// topicAggMap.put("content", allRootNodes);
// } else {
// topicAggMap.put("content", filterItemList);
// }
// completeMap.add(topicAggMap);
// }
//
// @Override
// public Response searchSearvice(Request request) {
// Map<String, Object> requestMap;
// Response response = new Response();
//
// try {
// requestMap = request.getRequest();
// } catch (Exception e) {
// ResponseCode responseCode =
// ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
// response.setResponseCode(responseCode);
// e.printStackTrace();
// return response;
// }
//
// String query = null;
// Boolean isStandAlone = null;
//
// boolean invalidFlag = true;
// query = (String) requestMap.get(Constants.QUERY);
// isStandAlone = (Boolean) requestMap.get(Constants.IS_STAND_ALONE);
//
// Map<String, Object> filterMap = (Map<String, Object>)
// requestMap.get(Constants.FILTERS);
// if (query == null) {
// if (filterMap != null || !filterMap.isEmpty()) {
// invalidFlag = false;
// }
// } else {
// if (filterMap != null) {
// if (!filterMap.isEmpty()) {
// for (String key : filterMap.keySet()) {
// if (filterMap.get(key) == null) {
// invalidFlag = true;
// break;
// } else {
// invalidFlag = false;
// }
// }
// }
// } else {
// invalidFlag = false;
// }
// }
//
// if (invalidFlag) {
// ResponseCode responseCode =
// ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
// response.setResponseCode(responseCode);
// return response;
// }
//
// FilterClass filters = null;
// try {
// filters = FilterClass.fromMap(filterMap);
// } catch (IllegalArgumentException e) {
// ResponseCode responseCode =
// ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
// response.setResponseCode(responseCode);
// return response;
// }
//
// Integer pageNumber = (Integer) requestMap.get("pageNo");
// Integer pageSize = (Integer) requestMap.get("pageSize");
//
// if (pageNumber == null)
// pageNumber = 0;
// if (pageSize == null)
// pageSize = 10;
//
// Client requestBuilder = ConnectionManager.getClient();
//
// SearchRequestBuilder searchRequestBuilder = requestBuilder
// .prepareSearch(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
// .setTypes(LexProjectUtil.EsType.new_lex_search.getTypeName())
// .setSearchType(SearchType.QUERY_THEN_FETCH);
// BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
// BoolQueryBuilder conceptsBoolQueryFilterForAggs = QueryBuilders.boolQuery();
// BoolQueryBuilder sourceShortNameBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder resourceCategoryBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder resourceTypeBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder contentTypeBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder fileTypeBoolQueryFilterForAggs = QueryBuilders.boolQuery();
// BoolQueryBuilder lastUpdatedOnBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder durationBoolQueryFilterForAggs = QueryBuilders.boolQuery();
// BoolQueryBuilder complexityLevelBoolQueryFilterForAggs =
// QueryBuilders.boolQuery();
// BoolQueryBuilder tagsBoolQueryFilterForAggs = QueryBuilders.boolQuery();
//
// if (query != null && !query.isEmpty()) {
// if (query.toLowerCase().equals("all") || query.equals("*")) {
//
// } else {
// if ((!query.equals("\"") && !query.equals("\"\"") && query.charAt(0) == '"'
// && query.charAt(query.length() - 1) == '"')
// || (!query.equals("'") && !query.equals("''") && query.charAt(0) == '\''
// && query.charAt(query.length() - 1) == '\'')) {
// query = query.substring(1, query.length() - 1);
// queryBuilder.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
//
// conceptsBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// sourceShortNameBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// resourceCategoryBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// resourceTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// contentTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// fileTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// lastUpdatedOnBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// durationBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// complexityLevelBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// tagsBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "title", "descriptionSearch", "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.PHRASE));
// } else {
// queryBuilder.should(QueryBuilders.matchPhraseQuery("title", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("descriptionSearch",
// query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("concepts.name", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("keywords", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("tracks", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("subTracks", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("subSubTracks", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("childrenTitle", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("childrenDescription",
// query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.should(QueryBuilders.matchPhraseQuery("sourceShortName", query)
// .slop(Constants.NEW_LEX_SEARCH_SLOP).analyzer(Constants.NEW_LEX_SEARCH_ANALYZER));
// queryBuilder.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
//
// conceptsBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// sourceShortNameBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// resourceCategoryBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// resourceTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// contentTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// fileTypeBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// lastUpdatedOnBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// durationBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// complexityLevelBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// tagsBoolQueryFilterForAggs.must(QueryBuilders
// .multiMatchQuery(query, "identifier", "title", "descriptionSearch",
// "childrenTitle",
// "childrenDescription", "keywords", "concepts.name", "tracks", "subTracks",
// "subSubTracks", "sourceShortName")
// .analyzer(Constants.NEW_LEX_SEARCH_ANALYZER).type(Type.BEST_FIELDS));
// }
// }
// }
//
// if (isStandAlone != null && (isStandAlone instanceof Boolean)) {
// queryBuilder.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
//
// conceptsBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// sourceShortNameBoolQueryFilterForAggs
// .filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE, isStandAlone));
// resourceCategoryBoolQueryFilterForAggs
// .filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE, isStandAlone));
// resourceTypeBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// contentTypeBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// fileTypeBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// lastUpdatedOnBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// durationBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// complexityLevelBoolQueryFilterForAggs
// .filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE, isStandAlone));
// tagsBoolQueryFilterForAggs.filter(QueryBuilders.termQuery(Constants.IS_STAND_ALONE,
// isStandAlone));
// }
//
// if (filters != null) {
//
// if (filters.getConcepts() != null && filters.getConcepts().length > 0) {
// addFilterToQuery(queryBuilder, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
//
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getConcepts(),
// Constants.FILTER_CONCEPTS_NAME_FIELD_KEY);
// }
//
// if (filters.getSourceShortName() != null &&
// filters.getSourceShortName().length > 0) {
// addFilterToQuery(queryBuilder, filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getSourceShortName(),
// Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY);
// }
//
// if (filters.getResourceCategory() != null &&
// filters.getResourceCategory().length > 0) {
// addFilterToQuery(queryBuilder, filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getResourceCategory(),
// Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY);
// }
//
// if (filters.getResourceType() != null && filters.getResourceType().length >
// 0) {
// addFilterToQuery(queryBuilder, filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs, filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs,
// filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs, filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs,
// filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs, filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getResourceType(),
// Constants.FILTER_RESOURCE_TYPE_FIELD_KEY);
// }
//
// if (filters.getContentType() != null && filters.getContentType().length > 0)
// {
// addFilterToQuery(queryBuilder, filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs, filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs,
// filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs, filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs,
// filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs, filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getContentType(),
// Constants.FILTER_CONTENT_TYPE_FIELD_KEY);
// }
//
// if (filters.getFileType() != null && filters.getFileType().length > 0) {
// addFilterToQuery(queryBuilder, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs,
// filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getFileType(),
// Constants.FILTER_FILE_TYPE_FIELD_KEY);
// }
//
// if (filters.getLastUpdatedOn() != null && filters.getLastUpdatedOn().length >
// 0) {
// String val = filters.getLastUpdatedOn()[filters.getLastUpdatedOn().length -
// 1];
// boolean flg = false;
// switch (val) {
// case "week":
// val = "now-1w";
// break;
// case "month":
// val = "now-1M";
// break;
// case "year":
// val = "now-1y";
// break;
// default:
// val = "now/d";
// flg = true;
// break;
// }
// if (flg)
// queryBuilder.filter(QueryBuilders.rangeQuery(Constants.FILTER_LAST_UPDATED_ON_FIELD_KEY).lte(val));
// else
// queryBuilder.filter(QueryBuilders.rangeQuery(Constants.FILTER_LAST_UPDATED_ON_FIELD_KEY).gte(val));
// }
//
// if (filters.getDuration() != null && filters.getDuration().length > 0) {
// int size = filters.getDuration().length;
// BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
// for (int i = 0; i < size; i++) {
// Long[] range = getDurationRangeArray(filters.getDuration()[i]);
// if (range == null) {
// ResponseCode responseCode = ResponseCode
// .getResponse(ResponseCode.invalidRequestData.getErrorCode());
// response.setResponseCode(responseCode);
// return response;
// }
// nestedBoolQuery.should(
// QueryBuilders.rangeQuery(Constants.FILTER_DURATION_FIELD_KEY).gte(range[0]).lt(range[1]));
// }
// queryBuilder.filter(nestedBoolQuery);
// conceptsBoolQueryFilterForAggs.filter(nestedBoolQuery);
// sourceShortNameBoolQueryFilterForAggs.filter(nestedBoolQuery);
// resourceCategoryBoolQueryFilterForAggs.filter(nestedBoolQuery);
// resourceTypeBoolQueryFilterForAggs.filter(nestedBoolQuery);
// contentTypeBoolQueryFilterForAggs.filter(nestedBoolQuery);
// fileTypeBoolQueryFilterForAggs.filter(nestedBoolQuery);
// lastUpdatedOnBoolQueryFilterForAggs.filter(nestedBoolQuery);
// complexityLevelBoolQueryFilterForAggs.filter(nestedBoolQuery);
// tagsBoolQueryFilterForAggs.filter(nestedBoolQuery);
// }
//
// if (filters.getComplexityLevel() != null &&
// filters.getComplexityLevel().length > 0) {
// addFilterToQuery(queryBuilder, filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs,
// filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// addFilterToQuery(tagsBoolQueryFilterForAggs, filters.getComplexityLevel(),
// Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY);
// }
//
// if (filters.getTags() != null && filters.getTags().length > 0) {
// addFilterToQuery(queryBuilder, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
//
// addFilterToQuery(conceptsBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(sourceShortNameBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(resourceCategoryBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(resourceTypeBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(contentTypeBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(fileTypeBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(lastUpdatedOnBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(durationBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// addFilterToQuery(complexityLevelBoolQueryFilterForAggs, filters.getTags(),
// Constants.FILTER_TAGS_FIELD_KEY);
// }
//
// // if (filters.getCollections() != null && filters.getCollections().length >
// 0)
// // {
// // addFilterToQuery(queryBuilder, filters.getCollections(),
// // Constants.FILTER_COLLECTIONS_IDENTIFIER_FIELD_KEY);
// // }
//
// }
//
// if (filters == null) {
// searchRequestBuilder.addAggregation(
// AggregationBuilders.nested(Constants.CONCEPTS_AGGS_KEY,
// Constants.FILTER_CONCEPTS_FIELD_KEY)
// .subAggregation(AggregationBuilders.terms("concepts_identifier_aggs")
// .field(Constants.FILTER_CONCEPTS_IDENTIFIER_FIELD_KEY).size(25)
// .subAggregation(AggregationBuilders.terms("concepts_name_aggs")
// .field(Constants.FILTER_CONCEPTS_NAME_FIELD_KEY).size(25))));
// searchRequestBuilder.addAggregation(AggregationBuilders.terms(Constants.CONTENT_TYPE_AGGS_KEY)
// .field(Constants.FILTER_CONTENT_TYPE_FIELD_KEY).size(1000));
// searchRequestBuilder.addAggregation(AggregationBuilders.terms(Constants.FILE_TYPE_AGGS_KEY)
// .field(Constants.FILTER_FILE_TYPE_FIELD_KEY).size(1000));
// searchRequestBuilder.addAggregation(AggregationBuilders.terms(Constants.COMPLEXITY_LEVEL_AGGS_KEY)
// .field(Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY).size(1000));
// searchRequestBuilder.addAggregation(AggregationBuilders.terms(Constants.SOURCE_SHORT_NAME_AGGS_KEY)
// .field(Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY).size(1000));
// searchRequestBuilder.addAggregation(AggregationBuilders.terms(Constants.TAGS_AGGS_KEY)
// .field(Constants.FILTER_TAGS_FIELD_KEY).size(1000));
// searchRequestBuilder.addAggregation(
// AggregationBuilders.range(Constants.DURATION_AGGS_KEY).field(Constants.FILTER_DURATION_FIELD_KEY)
// .addUnboundedTo(Constants.DURATION_SHORT_DISPLAY_NAME,
// Constants.DURATION_SHORT_MAX)
// .addRange(Constants.DURATION_MEDIUM_DISPLAY_NAME,
// Constants.DURATION_MEDIUM_MIN,
// Constants.DURATION_MEDIUM_MAX)
// .addRange(Constants.DURATION_LONG_DISPLAY_NAME, Constants.DURATION_LONG_MIN,
// Constants.DURATION_LONG_MAX)
// .addUnboundedFrom(Constants.DURATION_DETAILED_DISPLAY_NAME,
// Constants.DURATION_DETAILED_MIN));
// } else {
// GlobalAggregationBuilder totalAggs = AggregationBuilders.global("TotalAggs");
//
// totalAggs.subAggregation(
// AggregationBuilders.filter(Constants.CONCEPTS_AGGS_KEY,
// conceptsBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders
// .nested(Constants.CONCEPTS_AGGS_KEY, Constants.FILTER_CONCEPTS_FIELD_KEY)
// .subAggregation(AggregationBuilders.terms("concepts_identifier_aggs")
// .field(Constants.FILTER_CONCEPTS_IDENTIFIER_FIELD_KEY).size(25)
// .subAggregation(AggregationBuilders.terms("concepts_name_aggs")
// .field(Constants.FILTER_CONCEPTS_NAME_FIELD_KEY).size(25)))));
// totalAggs.subAggregation(
// AggregationBuilders.filter(Constants.CONTENT_TYPE_AGGS_KEY,
// contentTypeBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.CONTENT_TYPE_AGGS_KEY)
// .field(Constants.FILTER_CONTENT_TYPE_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(
// AggregationBuilders.filter(Constants.FILE_TYPE_AGGS_KEY,
// fileTypeBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.FILE_TYPE_AGGS_KEY)
// .field(Constants.FILTER_FILE_TYPE_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(AggregationBuilders
// .filter(Constants.COMPLEXITY_LEVEL_AGGS_KEY,
// complexityLevelBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.COMPLEXITY_LEVEL_AGGS_KEY)
// .field(Constants.FILTER_COMPLEXITY_LEVEL_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(AggregationBuilders
// .filter(Constants.SOURCE_SHORT_NAME_AGGS_KEY,
// sourceShortNameBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.SOURCE_SHORT_NAME_AGGS_KEY)
// .field(Constants.FILTER_SOURCE_SHORT_NAME_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(AggregationBuilders.filter(Constants.TAGS_AGGS_KEY,
// tagsBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.TAGS_AGGS_KEY)
// .field(Constants.FILTER_TAGS_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(
// AggregationBuilders.filter(Constants.DURATION_AGGS_KEY,
// durationBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.range(Constants.DURATION_AGGS_KEY)
// .field(Constants.FILTER_DURATION_FIELD_KEY)
// .addUnboundedTo(Constants.DURATION_SHORT_DISPLAY_NAME,
// Constants.DURATION_SHORT_MAX)
// .addRange(Constants.DURATION_MEDIUM_DISPLAY_NAME,
// Constants.DURATION_MEDIUM_MIN,
// Constants.DURATION_MEDIUM_MAX)
// .addRange(Constants.DURATION_LONG_DISPLAY_NAME, Constants.DURATION_LONG_MIN,
// Constants.DURATION_LONG_MAX)
// .addUnboundedFrom(Constants.DURATION_DETAILED_DISPLAY_NAME,
// Constants.DURATION_DETAILED_MIN)));
//
// if (filters.getContentType() != null && filters.getContentType().length > 0)
// {
// for (String contentType : filters.getContentType()) {
// if (contentType.equals("Resource")) {
// totalAggs.subAggregation(AggregationBuilders
// .filter(Constants.RESOURCE_TYPE_AGGS_KEY, resourceTypeBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.RESOURCE_TYPE_AGGS_KEY)
// .field(Constants.FILTER_RESOURCE_TYPE_FIELD_KEY).size(1000)));
// totalAggs.subAggregation(AggregationBuilders
// .filter(Constants.RESOURCE_CATEGORY_AGGS_KEY,
// resourceCategoryBoolQueryFilterForAggs)
// .subAggregation(AggregationBuilders.terms(Constants.RESOURCE_CATEGORY_AGGS_KEY)
// .field(Constants.FILTER_RESOURCE_CATEGORY_FIELD_KEY).size(1000)));
// break;
// }
// }
// }
//
// searchRequestBuilder.addAggregation(totalAggs);
// }
//
// // System.out.println(queryBuilder);
// // System.out.println(searchRequestBuilder);
// searchRequestBuilder =
// searchRequestBuilder.setQuery(queryBuilder).setSize(pageSize)
// .setFrom(pageNumber * pageSize);
// // System.out.println(searchRequestBuilder);
// SearchResponse searchResponse = searchRequestBuilder.get();
//
// Map<String, List<String>> aggAndFilterNamesMap = new HashMap<String,
// List<String>>();
//
// // aggAndFilterNamesMap.put(Constants.CONCEPTS_AGGS_KEY,
// // Constants.CONCEPTS_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.CONTENT_TYPE_AGGS_KEY,
// // Constants.CONTENT_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.FILE_TYPE_AGGS_KEY,
// // Constants.FILE_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.COMPLEXITY_LEVEL_AGGS_KEY,
// // Constants.COMPLEXITY_LEVEL_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.SOURCE_SHORT_NAME_AGGS_KEY,
// // Constants.SOURCE_SHORT_NAME_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.TAGS_AGGS_KEY,
// // Constants.TAGS_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.DURATION_AGGS_KEY,
// // Constants.DURATION_AGGS_FIELD_DISPLAYNAME_PAIR);
//
// // if (filters != null) {
// // if (filters.getContentType() != null && filters.getContentType().length >
// 0)
// // {
// // for (String contentType : filters.getContentType()) {
// // if (contentType.equals("Resource")) {
// // aggAndFilterNamesMap.put(Constants.RESOURCE_CATEGORY_AGGS_KEY,
// // Constants.RESOURCE_CATEGORY_AGGS_FIELD_DISPLAYNAME_PAIR);
// // aggAndFilterNamesMap.put(Constants.RESOURCE_TYPE_AGGS_KEY,
// // Constants.RESOURCE_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR);
// // break;
// // }
// // }
// // }
// // }
//
// List<Map<String, Object>> completeMap = new ArrayList<Map<String, Object>>();
// Iterator<Map.Entry<String, List<String>>> aggAndFilterNamesItr =
// aggAndFilterNamesMap.entrySet().iterator();
//
// if (filters != null) {
// while (aggAndFilterNamesItr.hasNext()) {
// Map.Entry<String, List<String>> pair = (Map.Entry<String, List<String>>)
// aggAndFilterNamesItr.next();
// addFiltersNew(completeMap, searchResponse, pair.getKey().toString(),
// pair.getValue().get(0).toString(),
// pair.getValue().get(1).toString());
// }
// } else {
// while (aggAndFilterNamesItr.hasNext()) {
// Map.Entry<String, List<String>> pair = (Map.Entry<String, List<String>>)
// aggAndFilterNamesItr.next();
// addFilters(completeMap, searchResponse, pair.getKey().toString(),
// pair.getValue().get(0).toString(),
// pair.getValue().get(1).toString());
// }
// }
//
// List<Object> intersectionMetaList = new ArrayList<Object>();
//
// if (searchResponse.getHits().totalHits > 0) {
// List<FilterItem> filterItemList = new ArrayList<FilterItem>();
// FilterItem filterItem = new FilterItem();
// filterItem.setType("week");
// filterItem.setDisplayName("Last Week");
// filterItemList.add(filterItem);
// filterItem = new FilterItem();
// filterItem.setType("month");
// filterItem.setDisplayName("Last Month");
// filterItemList.add(filterItem);
// filterItem = new FilterItem();
// filterItem.setType("year");
// filterItem.setDisplayName("Last Year");
// filterItemList.add(filterItem);
// filterItem = new FilterItem();
// filterItem.setType("older");
// filterItem.setDisplayName("Older");
// filterItemList.add(filterItem);
//
// Map<String, Object> topicAggMap = new HashMap<String, Object>();
// topicAggMap.put("type",
// Constants.LAST_UPDATED_ON_AGGS_FIELD_DISPLAYNAME_PAIR.get(0).toString());
// topicAggMap.put("displayName",
// Constants.LAST_UPDATED_ON_AGGS_FIELD_DISPLAYNAME_PAIR.get(1).toString());
// topicAggMap.put("content", filterItemList);
// completeMap.add(topicAggMap);
//
// for (SearchHit hit : searchResponse.getHits()) {
// intersectionMetaList.add(hit.getSourceAsMap());
// }
// } else {
// if (isStandAlone != null && isStandAlone) {
// Map<String, Object> requestMapRedirect;
//
// try {
// requestMapRedirect = request.getRequest();
// } catch (Exception e) {
// ResponseCode responseCode = ResponseCode
// .getResponse(ResponseCode.invalidRequestData.getErrorCode());
// response.setResponseCode(responseCode);
// e.printStackTrace();
// return response;
// }
// requestMapRedirect.put(Constants.IS_STAND_ALONE, null);
// request.setRequest(requestMapRedirect);
// return searchSearvice(request);
// }
// }
//
// Map<String, Object> result = new HashMap<String, Object>();
// result.put("totalHits", searchResponse.getHits().totalHits);
// result.put("result", intersectionMetaList);
// result.put("filters", completeMap);
//
// response.put("response", result);
// return response;
//
// }
//
// private Long[] getDurationRangeArray(String string) {
// Long[] rangeArray = new Long[2];
// switch (string) {
// case Constants.DURATION_SHORT:
// rangeArray[0] = Long.MIN_VALUE;
// rangeArray[1] = Constants.DURATION_SHORT_MAX;
// break;
// case Constants.DURATION_MEDIUM:
// rangeArray[0] = Constants.DURATION_MEDIUM_MIN;
// rangeArray[1] = Constants.DURATION_MEDIUM_MAX;
// break;
// case Constants.DURATION_LONG:
// rangeArray[0] = Constants.DURATION_LONG_MIN;
// rangeArray[1] = Constants.DURATION_LONG_MAX;
// break;
// case Constants.DURATION_DETAILED:
// rangeArray[0] = Constants.DURATION_DETAILED_MIN;
// rangeArray[1] = Long.MAX_VALUE;
// break;
//
// default:
// return null;
// }
// return rangeArray;
// }
//
// public void addFilterToQuery(BoolQueryBuilder queryBuilder, String[]
// fieldsArray, String fieldName) {
// queryBuilder.filter(QueryBuilders.termsQuery(fieldName, fieldsArray));
// }
// }
