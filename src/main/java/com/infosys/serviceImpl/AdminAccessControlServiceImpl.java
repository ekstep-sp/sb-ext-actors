/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.ConflictErrorException;
import com.infosys.exception.UnauthorizedException;
import com.infosys.model.cassandra.*;
import com.infosys.repository.*;
import com.infosys.repositoryImpl.UserRepositoryImpl;
import com.infosys.service.AdminAccessControlService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AdminAccessControlServiceImpl implements AdminAccessControlService {
    private static PropertiesCache properties = PropertiesCache.getInstance();
    private static String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
    private static String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;

    static {
        Util.checkCassandraDbConnections(keyspace);
        Util.checkCassandraDbConnections(bodhiKeyspace);
    }

    @Autowired
    UserRepositoryImpl userRepositoryImpl;
    @Autowired
    UserAccessPathsRepository userAccessPathsRepository;
    @Autowired
    UserAccessPathsAdminsRepository userAccessPathsAdminsRepository;
    @Autowired
    CassandraOperations cassandraOperations;
    @Autowired
    Environment environment;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationPropertiesRepository applicationPropertiesRepository;
    @Autowired
    EmailToGroupRepository emailToGroupRepo;
    @Autowired
    RestTemplate restTemplate;
    private RestHighLevelClient elasticClient = ConnectionManager.getClient();

    @SuppressWarnings("unchecked")
    @Override
    public Response getUserAccess(String uId, String rootOrg)
            throws BadRequestException, ApplicationLogicError, IOException {
        Response response = new Response();
        UUID userId = null;
        try {
            userId = UUID.fromString(uId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("userId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("userId");
        }

        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE userId");
        }
        if (null == rootOrg || rootOrg.isEmpty()) {
            rootOrg = environment.getProperty(LexConstants.ROOT_ORG);
            if (null == rootOrg || rootOrg.isEmpty()) {
                throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
            }
        }

        List<UserAccessPathsModel> result = userAccessPathsRepository
                .findByPrimaryKeyRootOrgAndPrimaryKeyUserId(rootOrg, userId);

        Set<String> combinedAccessPaths = new HashSet<>();

        result.forEach(item -> {
            combinedAccessPaths.addAll(item.getAccessPaths());
        });

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("userIds", userId.toString()));
        boolQuery.must(QueryBuilders.termQuery("rootOrg", rootOrg));
        String[] sources = {"rootOrg", "accessPaths", "org", "groupName", "identifier"};

        Map<String, Object> allContentData = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, sources, 1000);

        final String uid = userId.toString();
        List<Map<String, Object>> groups = new ArrayList<>();
        allContentData.forEach((contentId, contentData) -> {
            Map<String, Object> data = (Map<String, Object>) contentData;
            data.put("userIds", Arrays.asList(uid.toString()));
            combinedAccessPaths.addAll((ArrayList<String>) data.get("accessPaths"));
            groups.add(data);
        });

        response.put("special", result);
        response.put("groups", groups);
        response.put("totalHits", result.size() + allContentData.size());
        response.put("combinedAccessPaths", new ArrayList<String>(combinedAccessPaths));

        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response getContentUserAccess(String uId, List<String> contentIds, String rootOrg, UUID uuid)
            throws BadRequestException, ApplicationLogicError, IOException {
        Response response = new Response();

        UUID userId = null;
        try {
            userId = UUID.fromString(uId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("userId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("userId");
        }
        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE userId");
        }
        if (null == rootOrg || rootOrg.isEmpty()) {
            rootOrg = environment.getProperty(LexConstants.ROOT_ORG);
            if (null == rootOrg || rootOrg.isEmpty()) {
                throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
            }
        }

        Set<String> userPaths = new HashSet<String>();
        long start = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> Call started for cassandra for users " + start, LoggerEnum.INFO);
        Response userResponse = getUserAccess(userId.toString(), rootOrg);
        long end = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> Call ended for cassandra for users" + end +" Total time: "+(end-start), LoggerEnum.INFO);
        userPaths.addAll((ArrayList<String>) userResponse.get("combinedAccessPaths"));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("identifier", contentIds));
        boolQuery.filter(QueryBuilders.termQuery("rootOrg", rootOrg));
        boolQuery.filter(QueryBuilders.termsQuery("accessPaths", userPaths));
        String[] sources = {"identifier"};

        Map<String, Object> allContentData;
        start = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> Call started for elasticsearch for contents " + start, LoggerEnum.INFO);
        if (contentIds.size() <= 500) {

            allContentData = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                    LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sources, 500);

        } else {

            allContentData = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                    LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sources, 1000);

        }
        end = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> Call ended for elasticsearch for contents " + end +" Total time: "+(end-start), LoggerEnum.INFO);

        Map<String, Object> returnMap = new HashMap<>();
        contentIds.forEach(item->{
            Map<String, Object> tempMap = new HashMap<>();
            tempMap.put("hasAccess", false);

            if (null != allContentData.getOrDefault(item, null))
                tempMap.put("hasAccess", true);

            returnMap.put(item, tempMap);
        });

        response.put("response", returnMap);

        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response getCohortForUser(String uId, String rootOrg, String contentId, String groupId)
            throws BadRequestException, ApplicationLogicError, IOException {
        Response response = new Response();

        UUID userId = null;
        try {
            userId = UUID.fromString(uId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("userId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("userId");
        }
        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE userId");
        }
        if (null == rootOrg || rootOrg.isEmpty()) {
            rootOrg = environment.getProperty(LexConstants.ROOT_ORG);
            if (null == rootOrg || rootOrg.isEmpty()) {
                throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
            }
        }

        Response userAccessData = getUserAccess(uId, rootOrg);
        userAccessData.get("combinedAccessPaths");
        List<Map<String, Object>> userGroups = (List<Map<String, Object>>) userAccessData.get("groups");
        List<Map<String, Object>> returnList = new ArrayList<>();
        if (!StringUtils.isEmpty(contentId)) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.termsQuery("identifier", contentId));
            String[] sources = {"accessPaths"};
            Map<String, Object> esData = getDataFromElasticSearchWithNoScroll(
                    LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                    LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sources, 10);
            Map<String, Object> cd = (Map<String, Object>) esData.get(contentId);
            List<String> cap = (ArrayList<String>) cd.get("accessPaths");

            boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.termsQuery("accessPaths", cap));
            boolQuery.must(QueryBuilders.termsQuery("userIds", userId.toString()));
            Map<String, Object> esDataGroup = getDataFromElasticSearchWithNoScroll(
                    LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                    LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, null, 10);

            Set<String> grpuserIds = new HashSet<>();
            esDataGroup.forEach((id, val) -> {
                Map<String, Object> grp = (Map<String, Object>) val;
                ArrayList<String> grpuids = (ArrayList<String>) grp.get("userIds");
                grpuserIds.addAll(grpuids);
            });

            final String suid = userId.toString();
            grpuserIds.forEach(item -> {
                try {
                    Map<String, Object> data = userRepository.getUserDetails(item, "uuid");
                    if (!data.get("userid").equals(suid)) {
                        data.put("first_name", data.get("firstname"));
                        data.put("last_name", data.get("lastname"));
                        returnList.add(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } else if (!StringUtils.isEmpty(groupId)) {

            Set<String> filterPathsGeneric = new HashSet<>();
            Set<String> filterPathsStream = new HashSet<>();

            userGroups.forEach(item -> {
                if (item.get("groupName").toString().contains("generic")) {
                    filterPathsGeneric.addAll((ArrayList<String>) item.get("accessPaths"));
                } else {
                    filterPathsStream.addAll((ArrayList<String>) item.get("accessPaths"));
                }
            });

            if (groupId.equals("generic")) {

                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                boolQuery.must(QueryBuilders.termsQuery("accessPaths", filterPathsGeneric));
                Map<String, Object> esData = getDataFromElasticSearchWithNoScroll(
                        LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                        LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, null, 10);
                esData.forEach((id, val) -> {
                    returnList.add((Map<String, Object>) val);
                });
            } else {

                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                boolQuery.must(QueryBuilders.termsQuery("accessPaths", filterPathsStream));
                Map<String, Object> esData = getDataFromElasticSearchWithNoScroll(
                        LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                        LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, null, 10);
                esData.forEach((id, val) -> {
                    returnList.add((Map<String, Object>) val);
                });
            }
        } else {

        }
        response.put("response", returnList);
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response groupContentAccess(Request request) throws Exception {
        Map<String, Object> responseMap = new HashMap<>();
        Response response = new Response();
        if (request == null || request.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> reqMap = request.getRequest();
        String userId;
        Map<String, Object> featureMap = (Map<String, Object>) reqMap.get("features");
        System.out.println(featureMap);
        userId = (String) reqMap.get("userId");
        if (userId == null || featureMap == null || userId.isEmpty() || featureMap.isEmpty()) {
            throw new BadRequestException("Invalid Request Meta");
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("userIds", userId));
        String[] sourceUser = {"identifier"};
        Map<String, Object> allGroupsOfUser = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, sourceUser, 1000);
        System.out.println(allGroupsOfUser);
        for (String fid : featureMap.keySet()) {
            Map<String, Object> mapObj = (Map<String, Object>) featureMap.get(fid);
            System.out.println(mapObj);
            List<String> allGroupIds = (List<String>) mapObj.get("All");
            List<String> notGroupIds = (List<String>) mapObj.get("Not");
            List<String> someGroupIds = (List<String>) mapObj.get("Some");

            if (allGroupIds == null || notGroupIds == null || someGroupIds == null) {
                throw new BadRequestException("Invalid MapObjs");
            }
            Set<String> allUserGroups = getAllUserGroups(allGroupsOfUser);
            Set<String> copySet = new HashSet<>(allUserGroups);
            boolean allValue = userGroupsCheck(copySet, "all", allGroupIds);
            boolean notValue = userGroupsCheck(copySet, "not", notGroupIds);
            boolean someValue = userGroupsCheck(copySet, "some", someGroupIds);
            boolean result = allValue & notValue & someValue;
            responseMap.put(fid, result);
        }
        response.put("data", responseMap);
        return response;
    }

    private Set<String> getAllUserGroups(Map<String, Object> allGroups) {
        HashSet<String> groupsOfUser = new HashSet<>();
        for (String key : allGroups.keySet()) {
            groupsOfUser.add(key);
        }
        return groupsOfUser;
    }

    private boolean userGroupsCheck(Set<String> userGroups, String operation, List<String> groupIds) {
        Set<String> groupIdentifiers = new HashSet<>(groupIds);
        if (operation.equals("all")) {
            int allSize = groupIdentifiers.size();
            Set<String> copySet = new HashSet<>(userGroups);
            copySet.retainAll(groupIdentifiers);
            if (allSize == copySet.size()) {
                return true;
            } else {
                return false;
            }
        } else if (operation.equals("not")) {
            Set<String> copySet = new HashSet<>(userGroups);
            copySet.retainAll(groupIdentifiers);
            if (copySet.size() > 0) {
                return false;
            } else {
                return true;
            }
        } else if (operation.equals("some")) {
            Set<String> copySet = new HashSet<>(userGroups);
            copySet.retainAll(groupIdentifiers);
            if (copySet.size() > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new BadRequestException("Some invalid entry " + operation);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean userGroupCheck(Map<String, Object> map, String operation, String userId) {
        if (operation.equals("all")) {
            for (String mapKey : map.keySet()) {
                Map<String, Object> singleGroupData = (Map<String, Object>) map.get(mapKey);
                List<String> usersFromGroup = (List<String>) singleGroupData.get("userIds");
                System.out.println(usersFromGroup);
                if (!usersFromGroup.contains(userId)) {
                    return false;
                }
            }
            return true;
        } else if (operation.equals("not")) {
            for (String mapKey : map.keySet()) {
                Map<String, Object> singleGroupData = (Map<String, Object>) map.get(mapKey);
                List<String> usersFromGroup = (List<String>) singleGroupData.get("userIds");
                System.out.println(usersFromGroup);
                if (usersFromGroup.contains(userId)) {
                    return false;
                }
            }
            return true;
        } else if (operation.equals("some")) {
            for (String mapKey : map.keySet()) {
                Map<String, Object> singleGroupData = (Map<String, Object>) map.get(mapKey);
                List<String> usersFromGroup = (List<String>) singleGroupData.get("userIds");
                System.out.println(mapKey);
                System.out.println(usersFromGroup);
                if (usersFromGroup.contains(userId)) {
                    return true;
                }
            }
            return false;
        } else {
            throw new BadRequestException("Invalid operation passed");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response createGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        String groupName = req.get("groupName").toString();
        if (groupName == null || groupName.isEmpty()) {
            throw new BadRequestException("groupName is invalid");
        }
        groupName = groupName.replaceAll("_", "-");
        groupName = WordUtils.capitalize(groupName.toLowerCase().trim());
        List<String> accessPaths = (List<String>) req.get("accessPaths");
        if (accessPaths == null || accessPaths.isEmpty()) {
            throw new BadRequestException("accessPaths is invalid");
        }
        List<String> userIds = (List<String>) req.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("userIds is invalid");
        }
        Boolean isEmail = (Boolean) req.get("isEmail");
        if (isEmail == null) {
            throw new BadRequestException("isEmail is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        // new code
        String requestedBy = (String) req.get("requestedBy");
        if (requestedBy == null || requestedBy.isEmpty()) {
            throw new BadRequestException("requestedBy is invalid");
        }
        List<String> moderators = new ArrayList<>();
        try {
            moderators = (List<String>) req.get("moderators");
        } catch (NullPointerException e) {
            moderators = new ArrayList<>();
            ProjectLogger.log("moderator is null for " + groupName);
        }
        // List<String> moderators = (List<String>) req.get("moderators");
        // if (moderators == null || moderators.isEmpty()) {
        // moderators = new ArrayList<>();
        // }
        List<String> userEmails = new ArrayList<>();
        List<String> moderatorEmails = new ArrayList<>();
        List<String> missingEmailIds = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        List<String> modUuids = new ArrayList<>();
        if (isEmail) {
            try {
                List<ApplicationPropertiesModel> validDomain = applicationPropertiesRepository
                        .findByKeyIn(Arrays.asList("valid_domains"));
                if (!(validDomain == null || validDomain.isEmpty())) {
                    List<String> validDomains = Arrays.asList(validDomain.get(0).getValue().split(","));
                    userEmails = userIds.parallelStream().filter(userId -> userId.matches(
                            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                            .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                            .collect(Collectors.toList());
                    if (moderators != null && !(moderators.isEmpty())) {
                        moderatorEmails = moderators.parallelStream().filter(userId -> userId.matches(
                                "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                                .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                                .collect(Collectors.toList());
                    }
                    Map<String, Object> someMap = userRepositoryImpl.getUUIDsFromEmails(userEmails);
                    for (String uEmail : userEmails) {
                        if (someMap.containsKey(uEmail)) {
                            uuids.add((String) ((Map<String, Object>) someMap.get(uEmail)).get("id"));
                        } else {
                            missingEmailIds.add(uEmail);
                        }
                    }
                    userIds = uuids;
                    Map<String, Object> moderatorMap = userRepositoryImpl.getUUIDsFromEmails(moderatorEmails);
                    for (String modEmail : moderatorEmails) {
                        if (moderatorMap.containsKey(modEmail)) {
                            modUuids.add((String) ((Map<String, Object>) moderatorMap.get(modEmail)).get("id"));
                        }
                    }
                    moderators = modUuids;
                    try {
                        Map<String, Object> requestMap = userRepositoryImpl.getUUIDFromEmail(requestedBy);
                        if (requestMap.containsKey(requestedBy)) {
                            requestedBy = (String) ((Map<String, Object>) requestMap.get(requestedBy)).get("id");
                        } else {
                            requestedBy = "";
                        }
                    } catch (Exception e) {
                        throw new BadRequestException("Incorrect requestedBy");
                    }
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE valid emails");
            }
        } else {
            try {
                for (String i : userIds) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE A UUID");
            }
            try {
                for (String i : moderators) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("moderators MUST BE A UUID");
            }
            try {
                UUID.fromString(requestedBy);
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("requestedBy MUST BE A UUID");
            }
        }
        String rootOrg = req.get("rootOrg").toString();
        if (rootOrg == null || rootOrg.isEmpty()) {
            throw new BadRequestException("rootOrg is invalid");
        }
        rootOrg = rootOrg.replaceAll("_", "-");
        rootOrg = WordUtils.capitalize(rootOrg.toLowerCase().trim());
        String org = req.get("org").toString();
        if (org == null || org.isEmpty()) {
            throw new BadRequestException("org is invalid");
        }
        org = org.replaceAll("_", "-");
        org = WordUtils.capitalize(org.toLowerCase().trim());
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> modInsertObj = new ArrayList<>();
        for (String id : moderators) {
            Set<String> roles = new HashSet<>();
            roles.add("group_mod");
            UserAccessPathsAdminsPrimaryKeyModel obj = new UserAccessPathsAdminsPrimaryKeyModel();
            obj.setAdminId(UUID.fromString(id));
            obj.setOrg(org);
            obj.setRootOrg(rootOrg);
            obj.setRoles(roles);
            modInsertObj.add(obj);
        }
        try {
            userAccessPathsAdminsRepository.saveAll(modInsertObj);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new ApplicationLogicError("failed to insert into table");
        }
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        if (roles.contains("org_admin") == false) {
            throw new BadRequestException("adminId is not an admin");
        }
        List<String> validAccessPaths = new ArrayList<>();
        List<String> invalidAccessPaths = new ArrayList<>();
        String param = rootOrg + "/" + org;
        for (String ap : accessPaths) {
            ap = capitalizeAccessPath(ap);
            if (ap.startsWith(param)) {
                validAccessPaths.add(ap);
            } else {
                invalidAccessPaths.add(ap);
            }
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() > 0) {
            response.put("data", "Group name " + groupName + " already exists");
        } else {
            String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
            int pos = date.lastIndexOf(":");
            date = date.substring(0, pos) + "." + date.substring(pos + 1);
            pos = date.indexOf("+");
            date = date.substring(0, pos);
            Map<String, Object> resultMap = new HashMap<String, Object>();
            List<String> userId = new ArrayList<>(new HashSet<>(userIds));
            resultMap.put("groupName", WordUtils.capitalize(groupName.toLowerCase().trim()));
            resultMap.put("rootOrg", WordUtils.capitalize(rootOrg.toLowerCase().trim()));
            resultMap.put("org", WordUtils.capitalize(org.toLowerCase().trim()));
            resultMap.put("accessPaths", validAccessPaths);
            resultMap.put("userIds", userId);
            resultMap.put("moderators", moderators);
            resultMap.put("lastUpdatedBy", adminId);
            resultMap.put("requestedBy", requestedBy);
            resultMap.put("createdOn", date);
            resultMap.put("lastUpdatedOn", date);
            resultMap.put("createdBy", adminId);
            String x = ElasticSearchUtil.createData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                    LexProjectUtil.EsType.access_control_group.getTypeName(), id, resultMap);
            Map<String, Object> masterResultMap = new HashMap<>();
            masterResultMap.put("group_identifier", x);
            masterResultMap.put("invalid_access_paths", invalidAccessPaths);
            masterResultMap.put("missing_email_ids", missingEmailIds);
            response.put("data", masterResultMap);

            List<EmailToGroupModelPrimaryKeyModel> casTable = new ArrayList<>();
            for (String missingId : missingEmailIds) {
                EmailToGroupModelPrimaryKeyModel obj = new EmailToGroupModelPrimaryKeyModel();
                obj.setEmail(missingId);
                obj.setGroupIdentifier(id);
                casTable.add(obj);
            }
            emailToGroupRepo.saveAll(casTable);
        }
        return response;
    }

    private String capitalizeAccessPath(String ap) {
        String[] words = ap.split("(?<=/)");
        String afterCapitalize = "";
        for (String word : words) {
            word = WordUtils.capitalize(word.toLowerCase().trim());
            afterCapitalize = afterCapitalize + word;
        }
        return afterCapitalize;
    }

    @Override
    public Response deleteGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }

        String[] values = groupIdentifier.split("_");
        String groupName = WordUtils.capitalize(values[0].trim());
        String rootOrg = WordUtils.capitalize(values[1].trim());
        String org = WordUtils.capitalize(values[2].trim());
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        if (roles.contains("org_admin") == false) {
            throw new BadRequestException("adminId is not an admin");
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            response.put("data", "Group does not Exist");
        } else {
            String identifier = (String) searchMap.get("identifier");
            boolean res = ElasticSearchUtil.removeData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                    LexProjectUtil.EsType.access_control_group.getTypeName(), identifier);
            if (res) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("deleted_group_identifier", identifier);
                response.put("data", resultMap);
            } else {
                response.put("data", "Not deleted");
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response addUserToGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> userIds = (List<String>) req.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("userIds is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupId is invalid");
        }
        String groupName, rootOrg, org;
        try {
            String[] values = groupIdentifier.split("_");
            groupName = WordUtils.capitalize(values[0].trim());
            rootOrg = WordUtils.capitalize(values[1].trim());
            org = WordUtils.capitalize(values[2].trim());
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("invalid group identifier " + groupIdentifier);
        }
        Boolean isEmail;
        try {
            isEmail = (Boolean) req.get("isEmail");
            if (isEmail == null) {
                throw new BadRequestException("isEmail is invalid");
            }
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("Some junk value for isEmail");
        }

        List<String> userEmails = new ArrayList<>();
        List<String> missingEmailIds = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        if (isEmail) {
            try {
                List<ApplicationPropertiesModel> validDomain = applicationPropertiesRepository
                        .findByKeyIn(Arrays.asList("valid_domains"));
                if (!(validDomain == null || validDomain.isEmpty())) {
                    List<String> validDomains = Arrays.asList(validDomain.get(0).getValue().split(","));
                    userEmails = userIds.parallelStream().filter(userId -> userId.matches(
                            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                            .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                            .collect(Collectors.toList());
                    Map<String, Object> someMap = userRepositoryImpl.getUUIDsFromEmails(userEmails);
                    for (String uEmail : userEmails) {
                        if (someMap.containsKey(uEmail)) {
                            uuids.add((String) ((Map<String, Object>) someMap.get(uEmail)).get("id"));
                        } else {
                            missingEmailIds.add(uEmail);
                        }
                    }
                }
                userIds = uuids;
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE valid emails");
            }
        } else {
            try {
                for (String i : userIds) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE A UUID");
            }
        }

        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }

        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            response.put("data", "Group name" + groupName + " does not Exist");
        } else {
            if (roles.contains("org_admin") == false) {
                if (roles.contains("group_mod") == false) {
                    throw new BadRequestException("adminId is not an admin/moderator");
                } else {
                    List<String> moderators = (List<String>) searchMap.get("moderators");
                    if (moderators.contains(adminId) == false) {
                        throw new BadRequestException("moderator not authorized for this group");
                    }
                }
            }

            String identifier = (String) searchMap.get("identifier");
            List<String> userIdsFromES = (List<String>) searchMap.get("userIds");
            List<String> temp = new ArrayList<>(userIdsFromES);
            for (String user : userIds) {
                if (!userIdsFromES.contains(user)) {
                    userIdsFromES.add(user);
                }
            }
            if (userIdsFromES.equals(temp)) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("nothing_to_update", identifier);
                resultMap.put("missing_email_ids", missingEmailIds);
                response.put("data", resultMap);
                List<EmailToGroupModelPrimaryKeyModel> casTable = new ArrayList<>();
                for (String missingId : missingEmailIds) {
                    EmailToGroupModelPrimaryKeyModel obj = new EmailToGroupModelPrimaryKeyModel();
                    obj.setEmail(missingId);
                    obj.setGroupIdentifier(id);
                    casTable.add(obj);
                }
                emailToGroupRepo.saveAll(casTable);
            } else {
                Map<String, Object> resultMap = new HashMap<String, Object>();
                String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
                int pos = date.lastIndexOf(":");
                date = date.substring(0, pos) + "." + date.substring(pos + 1);
                pos = date.indexOf("+");
                date = date.substring(0, pos);
                resultMap.put("userIds", userIdsFromES);
                resultMap.put("lastUpdatedOn", date);
                resultMap.put("lastUpdatedBy", adminId);
                boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                        LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
                if (x) {
                    Map<String, Object> masterResultMap = new HashMap<>();
                    masterResultMap.put("updated_group_id", identifier);
                    masterResultMap.put("missing_email_ids", missingEmailIds);
                    response.put("data", masterResultMap);
                }
                List<EmailToGroupModelPrimaryKeyModel> casTable = new ArrayList<>();
                for (String missingId : missingEmailIds) {
                    EmailToGroupModelPrimaryKeyModel obj = new EmailToGroupModelPrimaryKeyModel();
                    obj.setEmail(missingId);
                    obj.setGroupIdentifier(id);
                    casTable.add(obj);
                }
                emailToGroupRepo.saveAll(casTable);
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response removeUserFromGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> userIds = (List<String>) req.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("userIds is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        Boolean isEmail = (Boolean) req.get("isEmail");
        if (isEmail == null) {
            throw new BadRequestException("isEmail is invalid");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        String groupName, rootOrg, org;
        try {
            String[] values = groupIdentifier.split("_");
            groupName = WordUtils.capitalize(values[0].trim());
            rootOrg = WordUtils.capitalize(values[1].trim());
            org = WordUtils.capitalize(values[2].trim());
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("invalid group identifier");
        }
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        List<String> userEmails = new ArrayList<>();
        List<String> missingEmailIds = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        if (isEmail) {
            try {
                List<ApplicationPropertiesModel> validDomain = applicationPropertiesRepository
                        .findByKeyIn(Arrays.asList("valid_domains"));
                if (!(validDomain == null || validDomain.isEmpty())) {
                    List<String> validDomains = Arrays.asList(validDomain.get(0).getValue().split(","));
                    userEmails = userIds.parallelStream().filter(userId -> userId.matches(
                            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                            .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                            .collect(Collectors.toList());
                    Map<String, Object> someMap = userRepositoryImpl.getUUIDsFromEmails(userEmails);
                    for (String uEmail : userEmails) {
                        if (someMap.containsKey(uEmail)) {
                            uuids.add((String) ((Map<String, Object>) someMap.get(uEmail)).get("id"));
                        } else {
                            missingEmailIds.add(uEmail);
                        }
                    }
                }
                userIds = uuids;
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE valid emails");
            }
        } else {
            try {
                for (String i : userIds) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE A UUID");
            }
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            response.put("data", "Group does not Exist");
        } else {
            if (roles.contains("org_admin") == false) {
                if (roles.contains("group_mod") == false) {
                    throw new BadRequestException("adminId is not an admin/moderator");
                } else {
                    List<String> moderators = (List<String>) searchMap.get("moderators");
                    if (moderators.contains(adminId) == false) {
                        throw new BadRequestException("moderator not authorized for this group");
                    }
                }
            }
            String identifier = (String) searchMap.get("identifier");
            List<String> userIdsFromES = (List<String>) searchMap.get("userIds");
            List<String> temp = new ArrayList<>(userIdsFromES);
            for (String user : userIds) {
                if (userIdsFromES.contains(user)) {
                    userIdsFromES.remove(user);
                }
            }
            if (userIdsFromES.equals(temp)) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("nothing_to_update", identifier);
                resultMap.put("missing_email_ids", missingEmailIds);
                response.put("data", resultMap);
                List<EmailToGroupModelPrimaryKeyModel> casTable = new ArrayList<>();
                for (String missingId : missingEmailIds) {
                    EmailToGroupModelPrimaryKeyModel obj = new EmailToGroupModelPrimaryKeyModel();
                    obj.setEmail(missingId);
                    obj.setGroupIdentifier(id);
                    casTable.add(obj);
                }
                emailToGroupRepo.saveAll(casTable);
            } else {
                Map<String, Object> resultMap = new HashMap<String, Object>();
                String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
                int pos = date.lastIndexOf(":");
                date = date.substring(0, pos) + "." + date.substring(pos + 1);
                pos = date.indexOf("+");
                date = date.substring(0, pos);
                resultMap.put("userIds", userIdsFromES);
                resultMap.put("lastUpdatedBy", adminId);
                resultMap.put("lastUpdatedOn", date);
                boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                        LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
                if (x) {
                    Map<String, Object> masterResultMap = new HashMap<>();
                    masterResultMap.put("updated_group_id", identifier);
                    masterResultMap.put("missing_email_ids", missingEmailIds);
                    response.put("data", masterResultMap);
                }
                List<EmailToGroupModelPrimaryKeyModel> casTable = new ArrayList<>();
                for (String missingId : missingEmailIds) {
                    EmailToGroupModelPrimaryKeyModel obj = new EmailToGroupModelPrimaryKeyModel();
                    obj.setEmail(missingId);
                    obj.setGroupIdentifier(id);
                    casTable.add(obj);
                }
                emailToGroupRepo.saveAll(casTable);
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response addAccessPathToGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> accessPaths = (List<String>) req.get("accessPaths");
        if (accessPaths == null || accessPaths.isEmpty()) {
            throw new BadRequestException("accessPaths is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        String[] values = groupIdentifier.split("_");
        String groupName = WordUtils.capitalize(values[0].trim());
        String rootOrg = WordUtils.capitalize(values[1].trim());
        String org = WordUtils.capitalize(values[2].trim());
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        if (roles.contains("org_admin") == false) {
            throw new BadRequestException("adminId is not an admin");
        }
        List<String> validAccessPaths = new ArrayList<>();
        List<String> invalidAccessPaths = new ArrayList<>();
        String param = rootOrg + "/" + org;

        for (String ap : accessPaths) {
            ap = capitalizeAccessPath(ap);
            if (ap.startsWith(param)) {
                validAccessPaths.add(ap);
            } else {
                invalidAccessPaths.add(ap);
            }
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            response.put("data", "Group name" + groupName + " does not Exist");
        } else {
            String identifier = (String) searchMap.get("identifier");
            List<String> accessPathsFromES = (List<String>) searchMap.get("accessPaths");
            for (String ap : validAccessPaths) {
                if (!accessPathsFromES.contains(ap)) {
                    accessPathsFromES.add(ap);
                }
            }
            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put("accessPaths", accessPathsFromES);
            boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                    LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
            if (x) {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("updated_group_id", identifier);
                masterResultMap.put("invalid_access_paths", invalidAccessPaths);
                response.put("data", masterResultMap);
            } else {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("nothing_to_update", identifier);
                masterResultMap.put("invalid_access_paths", invalidAccessPaths);
                response.put("data", masterResultMap);
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response removeAccessPathFromGroup(Request requestBody) throws Exception {
        Response response = new Response();

        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> accessPaths = (List<String>) req.get("accessPaths");
        if (accessPaths == null || accessPaths.isEmpty()) {
            throw new BadRequestException("accessPaths is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        String[] values = groupIdentifier.split("_");
        String groupName = WordUtils.capitalize(values[0].trim());
        String rootOrg = WordUtils.capitalize(values[1].trim());
        String org = WordUtils.capitalize(values[2].trim());
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        if (roles.contains("org_admin") == false) {
            throw new BadRequestException("adminId is not an admin");
        }
        List<String> validAccessPaths = new ArrayList<>();
        List<String> invalidAccessPaths = new ArrayList<>();
        String param = rootOrg + "/" + org;
        for (String ap : accessPaths) {
            ap = capitalizeAccessPath(ap);
            if (ap.startsWith(param)) {
                validAccessPaths.add(ap);
            } else {
                invalidAccessPaths.add(ap);
            }
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            throw new BadRequestException("Group name" + groupName + " does not Exist");
        } else {
            String identifier = (String) searchMap.get("identifier");
            List<String> accessPathsFromES = (List<String>) searchMap.get("accessPaths");
            for (String ap : validAccessPaths) {
                if (accessPathsFromES.contains(ap)) {
                    accessPathsFromES.remove(ap);
                }
            }
            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put("accessPaths", accessPathsFromES);
            boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                    LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
            if (x) {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("updated_group_id", identifier);
                masterResultMap.put("invalid_access_paths", invalidAccessPaths);
                response.put("data", masterResultMap);
            } else {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("nothing_to_update", identifier);
                masterResultMap.put("invalid_access_paths", invalidAccessPaths);
                response.put("data", masterResultMap);
            }
        }
        return response;
    }

//    private Map<String, Object> getDataFromElasticSearchWithNoScroll(String index, String type,
//                                                                      BoolQueryBuilder boolQuery, String[] sourcesList, int size, int timeMillis) throws IOException {
//        Map<String, Object> allIdsContentMeta = new HashMap<>();
//        SearchResponse masterContentMetaSearchHit = elasticClient
//                .search(new SearchRequest().indices(index).types(type).searchType(SearchType.QUERY_THEN_FETCH)
//                        .source(new SearchSourceBuilder().query(boolQuery).fetchSource(sourcesList, null).size(size))
//                        .scroll(new TimeValue(timeMillis)), RequestOptions.DEFAULT);
//        for (SearchHit hit : masterContentMetaSearchHit.getHits()) {
//            Map<String, Object> temp = hit.getSourceAsMap();
//            allIdsContentMeta.put(hit.getId(), temp);
//        }
//        return allIdsContentMeta;
//    }

//    private Map<String, Object> getDataFromElasticSearchWithNoScroll(String index, String type,
//                                                                   BoolQueryBuilder boolQuery, String[] sourcesList, int size, int timeMillis) throws IOException {
//
//        SearchResponse masterContentMetaSearchHit = elasticClient
//                .search(new SearchRequest().indices(index).types(type).searchType(SearchType.QUERY_THEN_FETCH)
//                        .source(new SearchSourceBuilder().query(boolQuery).fetchSource(sourcesList, null).size(size))
//                        .scroll(new TimeValue(timeMillis)), RequestOptions.DEFAULT);
//        Map<String, Object> allIdsContentMeta = new HashMap<>();
//        do {
//            for (SearchHit hit : masterContentMetaSearchHit.getHits()) {
//                Map<String, Object> temp = hit.getSourceAsMap();
//                allIdsContentMeta.put(hit.getId(), temp);
//            }
//            masterContentMetaSearchHit = elasticClient.scroll(
//                    new SearchScrollRequest(masterContentMetaSearchHit.getScrollId()).scroll(new TimeValue(timeMillis)),
//                    RequestOptions.DEFAULT);
//        } while (masterContentMetaSearchHit.getHits().getHits().length != 0);
//
//        return allIdsContentMeta;
//    }

    private Map<String, Object> getDataFromElasticSearchWithNoScroll(String index, String type, BoolQueryBuilder boolQuery, String[] sourcesList, int size) throws IOException {

        SearchResponse masterContentMetaSearchHit = elasticClient
                .search(new SearchRequest().indices(index).types(type).searchType(SearchType.QUERY_THEN_FETCH)
                        .source(new SearchSourceBuilder().query(boolQuery).fetchSource(sourcesList, null).from(0).size(size)), RequestOptions.DEFAULT);
        Map<String, Object> allIdsContentMeta = new HashMap<>();
        int from = 0;
        do {
            ++from;
            for (SearchHit hit : masterContentMetaSearchHit.getHits()) {
                Map<String, Object> temp = hit.getSourceAsMap();
                allIdsContentMeta.put(hit.getId(), temp);
            }
            masterContentMetaSearchHit = elasticClient
                    .search(new SearchRequest().indices(index).types(type).searchType(SearchType.QUERY_THEN_FETCH)
                            .source(new SearchSourceBuilder().query(boolQuery).fetchSource(sourcesList, null).from(from * size).size(size)), RequestOptions.DEFAULT);;
        } while (masterContentMetaSearchHit.getHits().getHits().length != 0);

        return allIdsContentMeta;
    }

    @Override
    public Response getAllAccessPaths(String adminId) throws BadRequestException, ApplicationLogicError, IOException {
        Response response = new Response();

        UUID userId = null;
        try {
            userId = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }

        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE adminId");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> data = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(userId);

        if (null == data || data.size() == 0) {
            throw new BadRequestException("INVALID ADMIN USER");
        }

        List<String> adminOrgs = new ArrayList<>();
        List<String> adminRootOrgs = new ArrayList<>();

        data.forEach(item -> {
            adminOrgs.add(item.getOrg());
            adminRootOrgs.add(item.getRootOrg());
        });

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termsQuery("rootOrg", adminRootOrgs));
        boolQueryBuilder
                .must(QueryBuilders.nestedQuery("org", QueryBuilders.termsQuery("org.org", adminOrgs), ScoreMode.Avg));

        SearchResponse searchResponse = elasticClient.search(
                new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
                        .types(LexProjectUtil.EsType.new_lex_search.getTypeName())
                        .searchType(SearchType.QUERY_THEN_FETCH)
                        .source(new SearchSourceBuilder().query(boolQueryBuilder).size(0).aggregation(
                                AggregationBuilders.terms("accessPaths_aggs").field("accessPaths").size(5000))),
                RequestOptions.DEFAULT);

        List<String> filterItemList = new ArrayList<>();
        Terms aggregation = searchResponse.getAggregations().get("accessPaths_aggs");
        for (Terms.Bucket bucket : aggregation.getBuckets()) {
            if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {
            } else if (bucket.getDocCount() > 0) {
                filterItemList.add(bucket.getKeyAsString());
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("totalHits", searchResponse.getHits().totalHits);
        result.put("accessPaths", filterItemList);

        response.put("response", result);

        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response addUserAccess(Request request) throws BadRequestException, ApplicationLogicError {
        Response response = new Response();

        Map<String, Object> requestMap;
        try {
            requestMap = request.getRequest();
        } catch (Exception e) {
            throw new BadRequestException("REQUEST MUST BE AN OBJECT");
        }

        UUID adminId = null;
        try {
            if (!requestMap.containsKey("adminId"))
                throw new ClassCastException();
            adminId = UUID.fromString((String) requestMap.get("adminId"));
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }

        if (null == adminId || adminId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE adminId");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> adminData = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminId);

        if (null == adminData || adminData.size() == 0) {
            throw new BadRequestException("INVALID ADMIN USER");
        }

        List<String> adminPermissions = new ArrayList<>();
        adminData.forEach(item -> {
            adminPermissions.add(item.getRootOrg() + "/" + item.getOrg());
        });

        UUID userId = null;
        try {
            if (!requestMap.containsKey("userId"))
                throw new ClassCastException();
            userId = UUID.fromString((String) requestMap.get("userId"));
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("userId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("userId");
        }

        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE userId");
        }

        List<UserAccessPathsModel> result = userAccessPathsRepository.findByPrimaryKeyUserId(userId);

        if (null == result || result.size() == 0) {
            throw new BadRequestException("INVALID USER");
        }

        Set<String> combinedUserExsistingAccessPaths = new HashSet<>();
        Map<Integer, UserAccessPathsModel> ttlToUAPMMap = new HashMap<>();

        result.forEach(item -> {
            ttlToUAPMMap.put(item.getTtl(), item);
            combinedUserExsistingAccessPaths.addAll(item.getAccessPaths());
        });

        List<Map<String, Object>> accessPathsList = null;
        try {
            accessPathsList = (ArrayList<Map<String, Object>>) requestMap.get("accessPathsList");
        } catch (ClassCastException e) {
            throw new BadRequestException("accessPathsList MUST BE A LIST OF OBJECTS");
        } catch (Exception e) {
            throw new ApplicationLogicError("accessPathsList");
        }

        if (null == accessPathsList || accessPathsList.size() == 0) {
            throw new BadRequestException("PLEASE PROVIDE accessPathsList");
        }

        List<Map<String, Object>> failed = new ArrayList<>();
        List<Map<String, Object>> succeded = new ArrayList<>();

        for (Map<String, Object> accessPathsItem : accessPathsList) {
            String accessPath = null;
            Integer ttl = null;
            try {
                accessPath = (String) accessPathsItem.get("accessPath");
                accessPath = capitalizeAccessPath(accessPath);
                ttl = (Integer) accessPathsItem.get("ttl");
                if (null == accessPath || null == ttl)
                    throw new Exception();
            } catch (Exception e) {
                accessPathsItem.put("reason", "accesPath MUST BE STRING and ttl MUST BE INTEGER");
                failed.add(accessPathsItem);
                continue;
            }
            if (combinedUserExsistingAccessPaths.contains(accessPath)) {
                accessPathsItem.put("reason", "Path already exists");
                failed.add(accessPathsItem);
                continue;
            }
            String[] accessPathsSplit = accessPath.split("/");
            if (accessPathsSplit.length < 2) {
                accessPathsItem.put("reason", "Invalid access path");
                failed.add(accessPathsItem);
                continue;
            }
            String rootOrg = accessPathsSplit[0];
            String org = accessPathsSplit[1];

            if (rootOrg.equals(org)) {
                accessPathsItem.put("reason", "RootOrg and Org can not be same");
                failed.add(accessPathsItem);
                continue;
            }

            if (!adminPermissions.contains(rootOrg + "/" + org)) {
                accessPathsItem.put("reason", "Insufficient admin rights");
                failed.add(accessPathsItem);
                continue;
            }

            Set<String> accessPaths = new HashSet<>();
            accessPaths.add(accessPath);
            String temp = accessPath;
            while (true) {
                int li = temp.lastIndexOf("/");
                if (li == -1)
                    break;
                String subs = temp.substring(0, li);
                accessPaths.add(subs);
                temp = subs;
            }

            UserAccessPathsModel rowExsists = ttlToUAPMMap.get(ttl);
            if (null != rowExsists) {
                Set<String> x = rowExsists.getAccessPaths();
                x.addAll(accessPaths);
                rowExsists.setAccessPaths(x);
                userAccessPathsRepository.save(rowExsists);
                ttlToUAPMMap.put(ttl, rowExsists);
                succeded.add(accessPathsItem);
                continue;
            }

            UserAccessPathsPrimaryKeyModel newRowPK = new UserAccessPathsPrimaryKeyModel(rootOrg, org, userId,
                    UUID.randomUUID());
            UserAccessPathsModel newRow = null;
            if (ttl == 0) {
                newRow = new UserAccessPathsModel(newRowPK, accessPaths, false, 0);
                userAccessPathsRepository.insert(newRow);
                ttlToUAPMMap.put(ttl, newRow);
                succeded.add(accessPathsItem);
            } else {
                Instant i = Instant.now();
                long timeStamp = i.getEpochSecond();
                int expiry = (int) (ttl - timeStamp);
                if (expiry < 0) {
                    // throw new BadRequestException("ttl CAN NOT BE IN THE
                    // PAST");
                    accessPathsItem.put("reason", "ttl CAN NOT BE IN THE PAST");
                    failed.add(accessPathsItem);
                    continue;
                }
                newRow = new UserAccessPathsModel(newRowPK, accessPaths, true, ttl);
                InsertOptions writeOptions = InsertOptions.builder().ttl(expiry).build();
                cassandraOperations.insert(newRow, writeOptions);
                ttlToUAPMMap.put(ttl, newRow);
                succeded.add(accessPathsItem);
            }
        }
        response.put("succeded", succeded);
        response.put("failed", failed);
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response deleteUserAccess(Request request) throws BadRequestException, ApplicationLogicError {
        Response response = new Response();

        Map<String, Object> requestMap;
        try {
            requestMap = request.getRequest();
        } catch (Exception e) {
            throw new BadRequestException("REQUEST MUST BE AN OBJECT");
        }

        UUID adminId = null;
        try {
            if (!requestMap.containsKey("adminId"))
                throw new ClassCastException();
            adminId = UUID.fromString((String) requestMap.get("adminId"));
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }

        if (null == adminId || adminId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE adminId");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> adminData = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminId);

        if (null == adminData || adminData.size() == 0) {
            throw new BadRequestException("INVALID ADMIN USER");
        }

        Set<String> adminPermissions = new HashSet<>();
        adminData.forEach(item -> {
            adminPermissions.add(capitalizeAccessPath(item.getOrg()));
            adminPermissions.add(capitalizeAccessPath(item.getRootOrg()));
            adminPermissions.add(capitalizeAccessPath(item.getRootOrg() + "/" + item.getOrg()));
        });

        UUID userId = null;
        try {
            if (!requestMap.containsKey("userId"))
                throw new ClassCastException();
            userId = UUID.fromString((String) requestMap.get("userId"));
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("userId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("userId");
        }

        if (null == userId || userId.toString().isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE userId");
        }

        List<UserAccessPathsModel> result = userAccessPathsRepository.findByPrimaryKeyUserId(userId);

        if (null == result || result.size() == 0) {
            throw new BadRequestException("INVALID USER");
        }

        Set<String> combinedUserExsistingAccessPaths = new HashSet<>();

        result.forEach(item -> {
            combinedUserExsistingAccessPaths.addAll(item.getAccessPaths());
        });

        List<Map<String, Object>> accessPathsList = null;
        try {
            accessPathsList = (ArrayList<Map<String, Object>>) requestMap.get("accessPathsList");
        } catch (ClassCastException e) {
            throw new BadRequestException("accessPathsList MUST BE A LIST OF OBJECTS");
        } catch (Exception e) {
            throw new ApplicationLogicError("accessPathsList");
        }

        if (null == accessPathsList || accessPathsList.size() == 0) {
            throw new BadRequestException("PLEASE PROVIDE accessPathsList");
        }

        List<Map<String, Object>> failed = new ArrayList<>();
        List<Map<String, Object>> succeded = new ArrayList<>();

        for (Map<String, Object> accessPathsItem : accessPathsList) {
            final String accessPath;
            try {
                accessPath = capitalizeAccessPath((String) accessPathsItem.get("accessPath"));
                if (null == accessPath)
                    throw new Exception();
            } catch (Exception e) {
                accessPathsItem.put("reason", "accessPath MUST BE String");
                failed.add(accessPathsItem);
                continue;
            }

            if (!combinedUserExsistingAccessPaths.contains(accessPath)) {
                accessPathsItem.put("reason", "Path does not exists");
                failed.add(accessPathsItem);
                continue;
            }

            if (!adminPermissions.stream().anyMatch(item -> accessPath.startsWith(item))) {
                accessPathsItem.put("reason", "Insufficient admin rights");
                failed.add(accessPathsItem);
                continue;
            }

            if (accessPathsItem.containsKey("casId")) {
                final UUID casId;
                try {
                    casId = UUID.fromString((String) accessPathsItem.get("casId"));
                } catch (Exception e) {
                    accessPathsItem.put("reason", "casId MUST BE UUID");
                    failed.add(accessPathsItem);
                    continue;
                }
                Boolean deleteRow = null;
                try {
                    deleteRow = (Boolean) accessPathsItem.get("deleteRow");
                } catch (Exception e) {
                    accessPathsItem.put("reason", "deleteRow MUST BE BOOLEAN");
                    failed.add(accessPathsItem);
                    continue;
                }
                if (null == deleteRow || !deleteRow) {
                    result.forEach(item -> {
                        if (item.getPrimaryKey().getCasId() == casId) {
                            Set<String> x = item.getAccessPaths();
                            x.remove(accessPath);
                            if (x.size() == 0) {
                                userAccessPathsRepository.delete(item);
                            } else {
                                item.setAccessPaths(x);
                                userAccessPathsRepository.save(item);
                                succeded.add(accessPathsItem);
                                return;
                            }
                        }
                    });
                } else {
                    result.forEach(item -> {
                        if (item.getPrimaryKey().getCasId() == casId) {
                            userAccessPathsRepository.delete(item);
                            succeded.add(accessPathsItem);
                            return;
                        }
                    });
                }
            } else {
                result.forEach(item -> {
                    Set<String> x = item.getAccessPaths();
                    if (x.contains(accessPath)) {
                        x.remove(accessPath);
                        if (x.size() == 0) {
                            userAccessPathsRepository.delete(item);
                        } else {
                            item.setAccessPaths(x);
                            userAccessPathsRepository.save(item);
                        }
                    }
                });
                succeded.add(accessPathsItem);
            }
        }

        response.put("succeded", succeded);
        response.put("failed", failed);
        return response;

    }

    @SuppressWarnings("unchecked")
    @Override
    public Response getContentAccess(String contentId, String aId)
            throws BadRequestException, ApplicationLogicError, IOException {
        Response response = new Response();

        UUID adminId = null;
        try {
            adminId = UUID.fromString(aId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }

        if (null == adminId) {
            throw new BadRequestException("PLEASE PROVIDE ADMIN ID");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> data = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminId);

        if (null == data || data.size() == 0) {
            throw new BadRequestException("INVALID ADMIN USER");
        }

        Set<String> rootOrgs = new HashSet<>();
        Set<String> orgs = new HashSet<>();
        String rootOrgRegex = "^(";
        String orgRegex = "(";
        for (int i = 0; i < data.size(); i++) {
            rootOrgs.add(data.get(i).getRootOrg());
            orgs.add(data.get(i).getOrg());
            rootOrgRegex += data.get(i).getRootOrg() + "|";
            orgRegex += data.get(i).getOrg() + "|";
        }
        rootOrgRegex = rootOrgRegex.substring(0, rootOrgRegex.lastIndexOf("|")) + ")/";
        orgRegex = orgRegex.substring(0, orgRegex.lastIndexOf("|")) + ").*$";
        Pattern r = Pattern.compile(rootOrgRegex + orgRegex);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchPhraseQuery("identifier", contentId));
        boolQuery.must(QueryBuilders.termsQuery("rootOrg", rootOrgs));
        boolQuery.must(QueryBuilders.nestedQuery("org", QueryBuilders.termsQuery("org.org", orgs), ScoreMode.Avg));

        SearchResponse searchResponse = elasticClient.search(
                new SearchRequest().indices(LexProjectUtil.EsIndex.new_lex_search.getIndexName())
                        .types(LexProjectUtil.EsType.new_lex_search.getTypeName())
                        .searchType(SearchType.QUERY_THEN_FETCH).source(new SearchSourceBuilder().query(boolQuery)),
                RequestOptions.DEFAULT);

        if (searchResponse.getHits().getTotalHits() == 1) {
            Map<String, Object> searchResult = searchResponse.getHits().getAt(0).getSourceAsMap();
            List<String> allPaths = (ArrayList<String>) searchResult.get("accessPaths");
            List<String> visiblePaths = new ArrayList<>();
            allPaths.forEach(path -> {
                Matcher m = r.matcher(path);
                if (m.find())
                    visiblePaths.add(path);
            });
            String rootOrg = (String) searchResult.get("rootOrg");
            List<Map<String, String>> allOrgs = (ArrayList<Map<String, String>>) searchResult.get("org");
            List<Map<String, String>> visibleOrgs = new ArrayList<>();
            allOrgs.forEach(item -> {
                if (orgs.contains(item.get("org")))
                    visibleOrgs.add(item);
            });
            rootOrgs.retainAll(Arrays.asList(rootOrg));
            searchResult.put("rootOrg", rootOrgs);
            searchResult.put("org", visibleOrgs);
            searchResult.put("accessPaths", visiblePaths);
            response.put("response", searchResult);
        } else {
            throw new BadRequestException("NO CONTENT FOUND FOR " + contentId);
        }

        ProjectLogger.log("getContentAccess->AdminId:" + adminId + " contentId" + contentId);

        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response updateContentAccess(Request request) throws Exception {
        Map<String, Object> requestMap;
        String contentId = null;
        Request addReq = new Request();
        Request removeReq = new Request();
        Response response = new Response();
        Response removeResponse = new Response();
        Response addResponse = new Response();
        UUID adminId = null;
        try {
            requestMap = request.getRequest();
        } catch (Exception e) {
            throw new BadRequestException("REQUEST MUST BE AN OBJECT");
        }
        try {
            contentId = (String) requestMap.get("contentId");
        } catch (ClassCastException e) {
            throw new BadRequestException("contentId MUST BE A STRING");
        } catch (Exception e) {
            throw new ApplicationLogicError("contentId");
        }
        try {
            adminId = UUID.fromString((String) requestMap.get("adminId"));
        } catch (ClassCastException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }
        List<String> accessPathToBeAdded = new ArrayList<>();
        ;
        try {
            accessPathToBeAdded = (List<String>) requestMap.get("accessPathsToBeAdded");
        } catch (ClassCastException e) {
            throw new BadRequestException("accessPath MUST BE A List of Strings");
        } catch (Exception e) {
            throw new ApplicationLogicError("accessPath To Be added");
        }
        List<String> accessPathToBeRemoved = new ArrayList<>();
        ;
        try {
            accessPathToBeRemoved = (List<String>) requestMap.get("accessPathToBeRemoved");
        } catch (ClassCastException e) {
            throw new BadRequestException("accessPath MUST BE A List of Strings");
        } catch (Exception e) {
            throw new ApplicationLogicError("accessPath To Be removed");
        }
        addReq.put("contentId", contentId);
        addReq.put("adminId", adminId.toString());
        addReq.put("accessPath", accessPathToBeAdded);

        removeReq.put("contentId", contentId);
        removeReq.put("adminId", adminId.toString());
        removeReq.put("accessPath", accessPathToBeRemoved);
        if (accessPathToBeRemoved.size() > 0) {
            removeResponse = deleteContentAccess(removeReq);
        }
        if (((List<String>) removeResponse.getResult().get("failedIds")).size() > 0) {
            return removeResponse;
        }
        if (accessPathToBeAdded.size() > 0) {
            addResponse = addContentAccess(addReq);
        }
        if (((List<String>) addResponse.getResult().get("invalid access paths: ")).size() > 0
                || ((Map<String, Object>) addResponse.getResult().get("failedIdsReasonMap")).size() > 0) {
            return removeResponse;
        }
        response.put("Message", "Success");
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response addContentAccess(Request request)
            throws BadRequestException, ApplicationLogicError, JsonParseException, JsonMappingException, IOException {
        try {
            Map<String, Object> requestMap;
            String contentId = null;
            Response response = new Response();
            UUID adminId = null;
            try {
                requestMap = request.getRequest();
            } catch (Exception e) {
                throw new BadRequestException("REQUEST MUST BE AN OBJECT");
            }
            try {
                contentId = (String) requestMap.get("contentId");
            } catch (ClassCastException e) {
                throw new BadRequestException("contentId MUST BE A STRING");
            } catch (Exception e) {
                throw new ApplicationLogicError("contentId");
            }
            List<String> pathToBeAddedReq = new ArrayList<>();
            try {
                pathToBeAddedReq = (List<String>) requestMap.get("accessPath");
            } catch (ClassCastException e) {
                throw new BadRequestException("accessPath MUST BE A List of Strings");
            } catch (Exception e) {
                throw new ApplicationLogicError("accessPath");
            }
            if (null == contentId || null == pathToBeAddedReq || contentId.isEmpty() || pathToBeAddedReq.isEmpty()) {
                throw new BadRequestException("contentId, orgPaths, contentType CANNOT BE EMPTY");
            }
            List<String> uniquePaths = new ArrayList<>(new HashSet<>(pathToBeAddedReq));
            // System.out.println(uniquePaths);
            List<String> pathToBeAdded = new ArrayList<>();
            for (String path : uniquePaths) {
                pathToBeAdded.add(capitalizeAccessPath(path));
            }

            try {
                adminId = UUID.fromString((String) requestMap.get("adminId"));
            } catch (ClassCastException e) {
                throw new BadRequestException("adminId MUST BE A UUID");
            } catch (Exception e) {
                throw new ApplicationLogicError("adminId");
            }

            List<UserAccessPathsAdminsPrimaryKeyModel> adminData = userAccessPathsAdminsRepository
                    .findByPrimaryKeyAdminId(adminId);
            if (null == adminData || adminData.size() == 0) {
                throw new BadRequestException("INVALID ADMIN USER");
            }

            Set<String> adminRootOrg = new HashSet<>();
            Set<String> adminOrg = new HashSet<>();
            Set<String> roles = new HashSet<>();
            adminData.forEach(item -> {
                adminRootOrg.add(item.getRootOrg());
                adminOrg.add(item.getOrg());
                roles.addAll(item.getRoles());
            });
            if (roles.contains("org_admin") == false) {
                throw new BadRequestException("adminId is not an admin");
            }
            BoolQueryBuilder boolQueryFetch = QueryBuilders.boolQuery();
            Map<String, Object> contentMap = new HashMap<>();
            boolQueryFetch.must(QueryBuilders.termsQuery("identifier", contentId));
            String[] sourcesListFetch = {"rootOrg", "org"};
            contentMap = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                    LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQueryFetch, sourcesListFetch, 500);
            contentMap = (Map<String, Object>) contentMap.get(contentId);
            List<Map<String, Object>> orgFromESMap = new ArrayList<>();
            List<String> orgFromES = new ArrayList<>();
            List<String> rootOrgFromES = new ArrayList<>();
            try {
                rootOrgFromES = (List<String>) contentMap.get("rootOrg");
            } catch (Exception e) {
                e.printStackTrace();
                throw new BadRequestException("rootOrg meta is corrupt for id: " + contentId);
            }
            try {
                orgFromESMap = (List<Map<String, Object>>) contentMap.get("org");
                for (Map<String, Object> orgMeta : orgFromESMap) {
                    orgFromES.add((String) orgMeta.get("org"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new BadRequestException("org meta is corrupt for id: " + contentId);
            }
            List<String> validAccessPaths = new ArrayList<>();
            List<String> invalidAccessPaths = new ArrayList<>();
            for (String ap : pathToBeAdded) {
                String[] values = ap.split("/");
                String tempRootOrg = WordUtils.capitalize(values[0].trim());
                String tempOrg = WordUtils.capitalize(values[1].trim());
                // first compare ap rootOrg and org with admin rootOrg and org
                if (adminRootOrg.contains(tempRootOrg) && adminOrg.contains(tempOrg)) {
                    // compare ap rootOrg and org with cotent rootOrg and org
                    if (rootOrgFromES.contains(tempRootOrg) && orgFromES.contains(tempOrg)) {
                        // if both are true then ap is valid
                        validAccessPaths.add(ap);
                    } else {
                        invalidAccessPaths.add(ap);
                    }
                } else {
                    invalidAccessPaths.add(ap);
                }
            }
            String URL = Util.getIpAndPortFromEnv("content.service.host", "bodhi_content_port",
                    "all-ids/" + contentId + "?index=" + LexProjectUtil.EsIndex.new_lex_search.getIndexName());
            System.out.println(URL);
            URL url = null;
            try {
                url = new URL(URL);
                System.out.println(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new BadRequestException("Impropper Url");
            }
            ObjectMapper mapper = new ObjectMapper();
            List<String> childIds = null;
            try {
                childIds = mapper.readValue(url, List.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            Map<String, String> failedReasonMap = new HashMap<>();
            Map<String, Object> masterContentMeta = new HashMap<>();
            boolQuery.must(QueryBuilders.termsQuery("identifier", childIds));
            String[] sourcesList = {"identifier", "collections", "children", "accessPaths",
                    "accessPathsLockedStatus"};
            masterContentMeta = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                    LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);
            Map<String, Object> map = (Map<String, Object>) masterContentMeta.get(contentId);
            List<String> failedIds = new ArrayList<>();
            if ((boolean) map.get("accessPathsLockedStatus")) {
                failedReasonMap.put((String) map.get("identifier"), "CONTENT IS LOCKED CANNOT EDIT");
            } else {
                BulkRequest builder = new BulkRequest();
                for (String path : validAccessPaths) {
                    addContentAccessES((Map<String, Object>) masterContentMeta.get(contentId), false, path,
                            masterContentMeta, builder, failedReasonMap);
                }
                if (builder.numberOfActions() > 0) {
                    BulkResponse resObj = elasticClient.bulk(builder, RequestOptions.DEFAULT);
                    if (resObj.hasFailures()) {
                        resObj.forEach(item -> {
                            failedIds.add(item.getFailure().getId());
                        });
                    }
                } else {

                }
            }
            response.put("failedIdsReasonMap", failedReasonMap);
            response.put("invalid access paths: ", invalidAccessPaths);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void addContentAccessES(Map<String, Object> contentMeta, boolean checkParent, String pathToBeAdded,
                                    Map<String, Object> masterContentMeta, BulkRequest builder, Map<String, String> failedReasonMap) {
        UpdateRequest updateObj = new UpdateRequest();
        updateObj.index(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        updateObj.type(LexProjectUtil.EsType.new_lex_search.getTypeName());
        updateObj.id((String) contentMeta.get("identifier"));
        List<String> accessPathsForId = new ArrayList<>();
        accessPathsForId = (List<String>) contentMeta.get("accessPaths");
        if (checkParent) {
            boolean masterAddCondition = true;
            boolean continueRecursion = true;
            for (String path : accessPathsForId) {
                if (path.startsWith(pathToBeAdded) && (!(path.equals(pathToBeAdded)))) {
                    failedReasonMap.put((String) contentMeta.get("identifier"),
                            "CANNOT ADD, THE PATH ALREADY PRESENT IS : " + path);
                    masterAddCondition = false;
                    continueRecursion = false;
                    break;
                } else if (pathToBeAdded.startsWith(path) && (!(pathToBeAdded.equals(path)))) {
                    failedReasonMap.put((String) contentMeta.get("identifier"),
                            "CANNOT ADD, THE PATH ALREADY PRESENT IS : " + path);
                    masterAddCondition = false;
                    continueRecursion = false;
                    break;
                } else if (path.equals(pathToBeAdded)) {
                    masterAddCondition = false;
                    continueRecursion = true;
                    break;
                }
            }
            if (continueRecursion == true) {
                Map<String, Object> params = new HashMap<>();
                // if(uniqueAps.contains(pathToBeAdded)==false){}
                params.put("x", pathToBeAdded);
                if (masterAddCondition == true) {
                    updateObj.script(new Script(ScriptType.INLINE, "painless",
                            "ctx._source['accessPaths'].add(params.x)", params));
                    builder.add(updateObj);
                }
                List<Map<String, Object>> children = (ArrayList<Map<String, Object>>) contentMeta.get("children");
                for (Map<String, Object> child : children) {
                    addContentAccessES((Map<String, Object>) masterContentMeta.get(child.get("identifier")), true,
                            pathToBeAdded, masterContentMeta, builder, failedReasonMap);
                }
            }
        } else {
            boolean masterAddConditionParent = true;
            boolean continueRecursion = true;
            for (String path : accessPathsForId) {
                System.out.println(path + " path from ES");
                System.out.println(pathToBeAdded + " path TBA");
                System.out.println(path.equals(pathToBeAdded) + " equality condition");
                System.out.println(path.startsWith(pathToBeAdded) + " starts with condition");
                boolean b = path.startsWith(pathToBeAdded) && (!(path.equals(pathToBeAdded)));
                System.out.println(b + " 1st IF CONDITION");
                boolean b1 = pathToBeAdded.startsWith(path) && (!(pathToBeAdded.equals(path)));
                System.out.println(pathToBeAdded.equals(path) + " possible break place, path TBA:  " + pathToBeAdded
                        + " equals " + path);
                System.out.println(b1 + " else if condition");
                if (path.startsWith(pathToBeAdded) && (!(path.equals(pathToBeAdded)))) {
                    failedReasonMap.put((String) contentMeta.get("identifier"),
                            "CANNOT ADD, THE PATH ALREADY PRESENT IS : " + path);
                    masterAddConditionParent = false;
                    continueRecursion = false;
                    break;
                } else if (pathToBeAdded.startsWith(path) && (!(pathToBeAdded.equals(path)))) {
                    failedReasonMap.put((String) contentMeta.get("identifier"),
                            "CANNOT ADD, THE PATH ALREADY PRESENT IS : " + path);
                    masterAddConditionParent = false;
                    continueRecursion = false;
                    break;
                } else if (pathToBeAdded.equals(path)) {
                    masterAddConditionParent = false;
                    continueRecursion = true;
                    break;
                }
            }
            if (continueRecursion == true) {
                // means ap is valid
                Map<String, Object> params = new HashMap<>();
                params.put("x", pathToBeAdded);
                // uniqueAps.add(pathToBeAdded);
                if (masterAddConditionParent == true) {
                    updateObj.script(new Script(ScriptType.INLINE, "painless",
                            "ctx._source['accessPaths'].add(params.x)", params));
                    builder.add(updateObj);
                }
                List<Map<String, Object>> children = (ArrayList<Map<String, Object>>) contentMeta.get("children");
                for (Map<String, Object> child : children) {
                    addContentAccessES((Map<String, Object>) masterContentMeta.get(child.get("identifier")), true,
                            pathToBeAdded, masterContentMeta, builder, failedReasonMap);
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response deleteContentAccess(Request request)
            throws BadRequestException, ApplicationLogicError, ConflictErrorException, IOException {
        Response response = new Response();

        Map<String, Object> requestMap;
        try {
            requestMap = request.getRequest();
        } catch (Exception e) {
            throw new BadRequestException("REQUEST MUST BE AN OBJECT");
        }
        String contentId = null;
        List<String> pathToBeDeletedReq = new ArrayList<>();
        try {
            contentId = (String) requestMap.get("contentId");
        } catch (Exception e) {
            throw new BadRequestException("contentId MUST BE A STRING");
        }
        try {
            pathToBeDeletedReq = (List<String>) requestMap.get("accessPath");
        } catch (Exception e) {
            throw new BadRequestException("accessPath MUST BE A STRING");
        }
        List<String> pathToBeDeleted = new ArrayList<>();
        for (String path : pathToBeDeletedReq) {
            pathToBeDeleted.add(capitalizeAccessPath(path));
        }
        UUID adminId;
        try {
            adminId = UUID.fromString((String) requestMap.get("adminId"));
        } catch (ClassCastException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        } catch (Exception e) {
            throw new ApplicationLogicError("adminId");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> adminData = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminId);
        if (null == adminData || adminData.size() == 0) {
            throw new BadRequestException("INVALID ADMIN USER");
        }

        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        adminData.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if (roles.contains("org_admin") == false) {
            throw new BadRequestException("adminId is not an admin");
        }
        BoolQueryBuilder boolQueryFetch = QueryBuilders.boolQuery();
        Map<String, Object> contentMap = new HashMap<>();
        boolQueryFetch.must(QueryBuilders.termsQuery("identifier", contentId));
        String[] sourcesListFetch = {"rootOrg", "org"};
        contentMap = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQueryFetch, sourcesListFetch, 500);
        contentMap = (Map<String, Object>) contentMap.get(contentId);
        List<Map<String, Object>> orgFromESMap = new ArrayList<>();
        List<String> orgFromES = new ArrayList<>();
        List<String> rootOrgFromES = new ArrayList<>();
        try {
            rootOrgFromES = (List<String>) contentMap.get("rootOrg");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("rootOrg meta is corrupt for id: " + contentId);
        }
        try {
            orgFromESMap = (List<Map<String, Object>>) contentMap.get("org");
            for (Map<String, Object> orgMeta : orgFromESMap) {
                orgFromES.add((String) orgMeta.get("org"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("org meta is corrupt for id: " + contentId);
        }
        List<String> validAccessPaths = new ArrayList<>();
        List<String> invalidAccessPaths = new ArrayList<>();
        for (String ap : pathToBeDeleted) {
            String[] values = ap.split("/");
            String tempRootOrg = WordUtils.capitalize(values[0].trim());
            String tempOrg = WordUtils.capitalize(values[1].trim());
            // first compare ap rootOrg and org with admin rootOrg and org
            if (adminRootOrg.contains(tempRootOrg) && adminOrg.contains(tempOrg)) {
                // compare ap rootOrg and org with cotent rootOrg and org
                if (rootOrgFromES.contains(tempRootOrg) && orgFromES.contains(tempOrg)) {
                    // if both are true then ap is valid
                    validAccessPaths.add(ap);
                } else {
                    invalidAccessPaths.add(ap);
                }
            } else {
                invalidAccessPaths.add(ap);
            }
        }
        String URL = Util.getIpAndPortFromEnv("content.service.host", "bodhi_content_port",
                "all-ids/" + contentId + "?index=" + LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        URL url = null;
        try {
            url = new URL(URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new BadRequestException("Impropper Url");
        }
        System.out.println("----------------------------------------------------");
        System.out.println(url);
        System.out.println("----------------------------------------------------");
        ObjectMapper mapper = new ObjectMapper();
        List<String> allIds = null;
        try {
            allIds = mapper.readValue(url, List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("----------------------------------------------------");
        System.out.println(allIds);
        System.out.println("-----------------------------------------------------");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("identifier", allIds));
        String[] sourcesList = {"identifier", "collections", "children", "accessPaths", "accessPathsLockedStatus"};

        Map<String, Object> allIdsContentMeta = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);

        Map<String, Object> toBeDeletedMeta = (Map<String, Object>) allIdsContentMeta.get(contentId);
        List<String> accessPaths = (List<String>) toBeDeletedMeta.get("accessPaths");
        Map<String, String> ignoredIds = new HashMap<>();

        if (toBeDeletedMeta.get("accessPathsLockedStatus") == null) {
            throw new BadRequestException("Meta is corrupt accessPathsLockedStatus does not exist");
        }

        if ((boolean) toBeDeletedMeta.get("accessPathsLockedStatus")) {
            throw new ConflictErrorException("CONTENT IS LOCKED, NO PERCOLATION DONE");
        }

        List<String> bulkFailed = new ArrayList<>();
        for (String path : validAccessPaths) {
            if (null != accessPaths && accessPaths.contains(path)) {
                BulkRequest bulkUpdateBuilder = new BulkRequest();
                deletePathFromContent(contentId, path, allIdsContentMeta, false, ignoredIds, null, bulkUpdateBuilder);

                if (bulkUpdateBuilder.numberOfActions() > 0) {
                    bulkUpdateBuilder.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
                    BulkResponse bulkResponse = elasticClient.bulk(bulkUpdateBuilder, RequestOptions.DEFAULT);
                    if (bulkResponse.hasFailures()) {
                        bulkResponse.forEach(item -> {
                            bulkFailed.add(item.getFailure().getId());
                        });
                    }
                }
            } else {
                throw new ConflictErrorException("PATH TO BE DELETED NOT FOUND, NO PERCOLATION DONE");
            }
        }
        response.put("ignoredIds", ignoredIds);
        response.put("failedIds", bulkFailed);
        response.put("invalid access paths : ", invalidAccessPaths);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void deletePathFromContent(String contentId, String pathToBeDeleted, Map<String, Object> allIdsContentMeta,
                                       boolean checkParent, Map<String, String> ignoredIds, String parentId, BulkRequest bulkUpdateBuilder)
            throws BadRequestException, ApplicationLogicError, IOException {
        Map<String, Object> contentMeta = (Map<String, Object>) allIdsContentMeta.get(contentId);
        if ((boolean) contentMeta.get("accessPathsLockedStatus")) {
            ignoredIds.put(contentId, "PATH CAN NOT BE DELETED BECAUSE CONTENT IS LOCKED");
            return;
            // IGNORED
        }
        if (checkParent) {
            List<String> accessPaths = (List<String>) contentMeta.get("accessPaths");
            if (null != accessPaths && accessPaths.contains(pathToBeDeleted)) {
                List<Map<String, Object>> parents = (List<Map<String, Object>>) contentMeta.get("collections");
                List<String> parentIds = new ArrayList<String>();
                for (Map<String, Object> parent : parents) {
                    if (!((String) parent.get("identifier")).equals(parentId))
                        parentIds.add((String) parent.get("identifier"));
                }

                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                boolQuery.must(QueryBuilders.termsQuery("identifier", parentIds));
                String[] sourcesList = {"identifier", "accessPaths"};
                Map<String, Object> parentsContentMeta = getDataFromElasticSearchWithNoScroll(
                        LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                        LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);

                for (String key : parentsContentMeta.keySet()) {
                    List<String> paths = (ArrayList<String>) ((Map<String, Object>) parentsContentMeta.get(key))
                            .get("accessPaths");

                    if (paths.contains(pathToBeDeleted)) {
                        ignoredIds.put(contentId, "PATH CAN NOT BE DELETED BECAUSE ITS PRESENT IN " + key);
                        return;
                    }
                }
            } else {
                ignoredIds.put(contentId, "PATH NOT PRESENT");
                return;
            }
        }

        UpdateRequest updateObj = new UpdateRequest();
        updateObj.index(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        updateObj.type(LexProjectUtil.EsType.new_lex_search.getTypeName());
        updateObj.id(contentId);
        Map<String, Object> params = new HashMap<>();
        params.put("data", pathToBeDeleted);
        updateObj.script(new Script(ScriptType.INLINE, "painless",
                "ctx._source['accessPaths'].remove(ctx._source['accessPaths'].indexOf(params.data))", params));
        bulkUpdateBuilder.add(updateObj);

        List<Map<String, Object>> children = (List<Map<String, Object>>) contentMeta.get("children");
        if (children.size() > 0) {
            for (Map<String, Object> child : children) {
                deletePathFromContent((String) child.get("identifier"), pathToBeDeleted, allIdsContentMeta, true,
                        ignoredIds, contentId, bulkUpdateBuilder);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response lockContents(String contentId, String aId)
            throws BadRequestException, ApplicationLogicError, ConflictErrorException, IOException {
        Response response = new Response();

        String URL = Util.getIpAndPortFromEnv("content.service.host", "bodhi_content_port",
                "all-ids/" + contentId + "?index=" + LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        URL url = null;
        try {
            url = new URL(URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new BadRequestException("Impropper Url");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<String> allIds = null;
        try {
            allIds = mapper.readValue(url, List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("identifier", allIds));
        String[] sourcesList = {"identifier", "collections", "children", "accessPathsLockedStatus"};

        Map<String, Object> allIdsContentMeta = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);
        Map<String, Object> toBeLockedMeta = (Map<String, Object>) allIdsContentMeta.get(contentId);
        Boolean loockedStatus = (Boolean) toBeLockedMeta.get("accessPathsLockedStatus");

        if (null != loockedStatus && loockedStatus) {
            throw new BadRequestException("CONTENT ALREADY LOCKED, NO PERCOLATION DONE");
        }

        Map<String, String> ignoredIds = new HashMap<>();
        List<String> bulkFailed = new ArrayList<>();
        BulkRequest request = new BulkRequest();

        lockContent(contentId, allIdsContentMeta, ignoredIds, request);

        if (request.numberOfActions() > 0) {
            request.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
            BulkResponse bulkResponse = elasticClient.bulk(request, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                bulkResponse.forEach(item -> {
                    bulkFailed.add(item.getFailure().getId());
                });
            }
        }
        response.put("ignoredIds", ignoredIds);
        response.put("failedIds", bulkFailed);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void lockContent(String contentId, Map<String, Object> allIdsContentMeta, Map<String, String> ignoredIds,
                             BulkRequest bulkUpdateBuilder) throws BadRequestException, ApplicationLogicError {
        Map<String, Object> toBeLockedMeta = (Map<String, Object>) allIdsContentMeta.get(contentId);
        Boolean loockedStatus = (Boolean) toBeLockedMeta.get("accessPathsLockedStatus");

        if (null != loockedStatus && loockedStatus) {
            ignoredIds.put(contentId, "ALREADY LOCKED");
            return;
        }

        UpdateRequest updateObj = new UpdateRequest();
        updateObj.index(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        updateObj.type(LexProjectUtil.EsType.new_lex_search.getTypeName());
        updateObj.id(contentId);
        Map<String, Object> data = new HashMap<>();
        data.put("accessPathsLockedStatus", true);
        updateObj.doc(data);
        bulkUpdateBuilder.add(updateObj);

        List<Map<String, Object>> children = (List<Map<String, Object>>) toBeLockedMeta.get("children");
        if (children.size() > 0) {
            for (Map<String, Object> child : children) {
                lockContent((String) child.get("identifier"), allIdsContentMeta, ignoredIds, bulkUpdateBuilder);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response unlockContents(String contentId, String aId)
            throws JsonParseException, JsonMappingException, IOException {
        Response response = new Response();
        String URL = Util.getIpAndPortFromEnv("content.service.host", "bodhi_content_port",
                "all-ids/" + contentId + "?index=" + LexProjectUtil.EsIndex.new_lex_search.getIndexName());
        URL url = null;
        try {
            url = new URL(URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new BadRequestException("Impropper Url");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<String> childIds = null;
        try {
            childIds = mapper.readValue(url, List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        Map<String, String> failedReasonMap = new HashMap<>();
        List<String> failedIds = new ArrayList<>();
        Map<String, Object> masterContentMeta = new HashMap<>();
        boolQuery.must(QueryBuilders.termsQuery("identifier", childIds));
        String[] sourcesList = {"identifier", "collections", "children", "accessPaths", "accessPathsLockedStatus"};
        masterContentMeta = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);
        BulkRequest request = new BulkRequest();
        unlockContent((Map<String, Object>) masterContentMeta.get(contentId), false, masterContentMeta, request,
                failedReasonMap, null);
        if (request.numberOfActions() > 0) {
            BulkResponse resObj = elasticClient.bulk(request, RequestOptions.DEFAULT);

            if (resObj.hasFailures()) {
                resObj.forEach(item -> {
                    failedIds.add(item.getFailure().getId());
                });
            }
        } else {
        }

        response.put("failedIdsReasonMap", failedReasonMap);
        return response;

    }

    @SuppressWarnings("unchecked")
    private void unlockContent(Map<String, Object> contentMeta, boolean checkParent,
                               Map<String, Object> masterContentMeta, BulkRequest builder, Map<String, String> failedReasonMap,
                               String parentId) throws IOException {
        UpdateRequest updateObj = new UpdateRequest();
        if (checkParent) {
            List<Map<String, Object>> collections = new ArrayList<>();
            if (collections.size() > 1) {

                List<String> parentIds = new ArrayList<>();
                for (Map<String, Object> parent : collections) {
                    if (!((String) parent.get("identifier")).equals(parentId)) {
                        parentIds.add((String) parent.get("identifier"));
                    }
                }
                String[] sourcesList = {"identifier", "collections", "children", "accessPaths",
                        "accessPathsLockedStatus"};
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                boolQuery.must(QueryBuilders.termsQuery("identifier", parentIds));
                Map<String, Object> parentData = getDataFromElasticSearchWithNoScroll(
                        LexProjectUtil.EsIndex.new_lex_search.getIndexName(),
                        LexProjectUtil.EsType.new_lex_search.getTypeName(), boolQuery, sourcesList, 500);

                for (String key : parentData.keySet()) {
                    if (((Boolean) ((Map<String, Object>) parentData.get(key)).get("accessPathsLockedStatus"))) {
                        failedReasonMap.put((String) contentMeta.get("identifier"), ": ID has a parent - "
                                + parentData.get("identifer") + "which is locked so, cannot unlock");
                        return;
                    }
                }

                updateObj.index(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
                updateObj.type(LexProjectUtil.EsType.new_lex_search.getTypeName());
                updateObj.id(contentMeta.get("identifier").toString());
                Map<String, Object> pushObj = new HashMap<>();
                pushObj.put("accessPathsLockedStatus", false);
                updateObj.doc(pushObj);
                builder.add(updateObj);
            }
        } else {
            updateObj.index(LexProjectUtil.EsIndex.new_lex_search.getIndexName());
            updateObj.type(LexProjectUtil.EsType.new_lex_search.getTypeName());
            updateObj.id(contentMeta.get("identifier").toString());
            Map<String, Object> pushObj = new HashMap<>();
            pushObj.put("accessPathsLockedStatus", false);
            updateObj.doc(pushObj);
            builder.add(updateObj);
            List<Map<String, Object>> children = (List<Map<String, Object>>) contentMeta.get("children");
            for (Map<String, Object> child : children) {
                unlockContent((Map<String, Object>) masterContentMeta.get(child.get("identifier")), true,
                        masterContentMeta, builder, failedReasonMap, (String) contentMeta.get("identifier"));
            }
        }

    }

    private Map<String, Object> fetchGroupsV2(List<String> groupIds) throws Exception {
        System.out.println(groupIds);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("identifier", groupIds));
        System.out.println(boolQuery);
        String[] sources = {"rootOrg", "org", "userIds", "identifier"};
        Map<String, Object> allGroupData = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, sources, 500);
        System.out.println(allGroupData);
        return allGroupData;
    }

    private Map<String, Object> fetchGroups(List<String> groupIds) throws Exception {

        System.out.println(groupIds);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("identifier", groupIds));
        System.out.println(boolQuery);
        String[] sources = {"rootOrg", "org", "userIds", "identifier"};
        Map<String, Object> allGroupData = getDataFromElasticSearchWithNoScroll(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, sources, 10);
        return allGroupData;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response fetchGroup(String groupIdentifier, String adminId) throws Exception {
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }

        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        String[] values = groupIdentifier.split("_");
        String groupName = WordUtils.capitalize(values[0].trim());
        String rootOrg = WordUtils.capitalize(values[1].trim());
        String org = WordUtils.capitalize(values[2].trim());
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }

        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), groupIdentifier);

        Response response = new Response();
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            throw new BadRequestException("Group : " + groupName + " does not Exist");
        } else {
            List<String> allMods = (List<String>) searchMap.get("moderators");
            if (allMods.contains(adminId) || roles.contains("org_admin")) {
                Map<String, String> emails = userRepository.getEmailsFromUUIDS((List<String>) searchMap.get("userIds"));
                Map<String, String> modemails = userRepository
                        .getEmailsFromUUIDS((List<String>) searchMap.get("moderators"));
                searchMap.put("userIds", emails);
                searchMap.put("moderators", modemails);
                response.put("data", searchMap);
            } else {
                throw new UnauthorizedException("Not enough access to view");
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response fetchAllGroups(String adminId, int pageNo) throws Exception {
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        List<Map<String, Object>> groupResults = new ArrayList<>();
        if (roles.contains("org_admin")) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.termsQuery("rootOrg", adminRootOrg));
            boolQuery.must(QueryBuilders.termsQuery("org", adminOrg));
            final Map<String, Object> groupSearchResults = new HashMap<>();
            SearchResponse searchResponse = elasticClient.search(
                    new SearchRequest().indices(LexProjectUtil.EsIndex.access_control_groups.getIndexName())
                            .types(LexProjectUtil.EsType.access_control_group.getTypeName())
                            .searchType(SearchType.QUERY_THEN_FETCH)
                            .source(new SearchSourceBuilder().query(boolQuery).size(10).from(pageNo * 10)),
                    RequestOptions.DEFAULT);

            searchResponse.getHits().forEach(item -> {
                groupSearchResults.put((String) item.getSourceAsMap().get("identifier"), item.getSourceAsMap());
            });

            for (Object obj : groupSearchResults.values()) {
                Map<String, Object> mapVal = (Map<String, Object>) obj;
                Map<String, String> emails = userRepository.getEmailsFromUUIDS((List<String>) mapVal.get("userIds"));
                Map<String, String> modemails = userRepository
                        .getEmailsFromUUIDS((List<String>) mapVal.get("moderators"));
                mapVal.put("userIds", emails);
                mapVal.put("moderators", modemails);
                groupResults.add(mapVal);
            }

        } else if (roles.contains("group_mod")) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.termsQuery("rootOrg", adminRootOrg));
            boolQuery.must(QueryBuilders.termsQuery("org", adminOrg));
            boolQuery.must(QueryBuilders.termQuery("moderators.keyword", adminId));
            final Map<String, Object> groupSearchResults = new HashMap<>();
            SearchResponse searchResponse = elasticClient.search(
                    new SearchRequest().indices(LexProjectUtil.EsIndex.access_control_groups.getIndexName())
                            .types(LexProjectUtil.EsType.access_control_group.getTypeName())
                            .searchType(SearchType.QUERY_THEN_FETCH)
                            .source(new SearchSourceBuilder().query(boolQuery).size(10).from(pageNo * 10)),
                    RequestOptions.DEFAULT);

            searchResponse.getHits().forEach(item -> {
                groupSearchResults.put((String) item.getSourceAsMap().get("identifier"), item.getSourceAsMap());
            });

            for (Object obj : groupSearchResults.values()) {
                Map<String, Object> mapVal = (Map<String, Object>) obj;
                Map<String, String> emails = userRepository.getEmailsFromUUIDS((List<String>) mapVal.get("userIds"));
                Map<String, String> modemails = userRepository
                        .getEmailsFromUUIDS((List<String>) mapVal.get("moderators"));
                mapVal.put("userIds", emails);
                mapVal.put("moderators", modemails);
                groupResults.add(mapVal);
            }

        }
        Response response = new Response();
        response.put("data", groupResults);
        return response;
    }

    @Override
    public Response fetchAdminDetails(String adminId) {
        Response resp = new Response();
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId not found");
        }

        Set<String> rootOrgs = new HashSet<>();
        Set<String> orgs = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            rootOrgs.add(item.getRootOrg());
            orgs.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        resp.put("rootOrgs", rootOrgs);
        resp.put("orgs", orgs);
        resp.put("roles", roles);
        return resp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response addModeratorToGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> moderators = (List<String>) req.get("moderators");
        if (moderators == null || moderators.isEmpty()) {
            throw new BadRequestException("userIds is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupId is invalid");
        }
        String groupName, rootOrg, org;
        try {
            String[] values = groupIdentifier.split("_");
            groupName = WordUtils.capitalize(values[0].trim());
            rootOrg = WordUtils.capitalize(values[1].trim());
            org = WordUtils.capitalize(values[2].trim());
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException(
                    "invalid group identifier/Old data not supported through apis  " + groupIdentifier);
        }
        Boolean isEmail;
        try {
            isEmail = (Boolean) req.get("isEmail");
            if (isEmail == null) {
                throw new BadRequestException("isEmail is invalid");
            }
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("Some junk value for isEmail");
        }

        List<String> userEmails = new ArrayList<>();
        List<String> missingEmailIds = new ArrayList<>();
        List<String> uuids = new ArrayList<>();

        if (isEmail) {
            try {
                List<ApplicationPropertiesModel> validDomain = applicationPropertiesRepository
                        .findByKeyIn(Arrays.asList("valid_domains"));
                if (!(validDomain == null || validDomain.isEmpty())) {
                    List<String> validDomains = Arrays.asList(validDomain.get(0).getValue().split(","));
                    userEmails = moderators.parallelStream().filter(userId -> userId.matches(
                            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                            .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                            .collect(Collectors.toList());
                    Map<String, Object> someMap = userRepositoryImpl.getUUIDsFromEmails(userEmails);
                    for (String uEmail : userEmails) {
                        if (someMap.containsKey(uEmail)) {
                            uuids.add((String) ((Map<String, Object>) someMap.get(uEmail)).get("id"));
                        } else {
                            missingEmailIds.add(uEmail);
                        }
                    }
                }
                moderators = uuids;
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE valid emails");
            }
        } else {
            try {
                for (String i : moderators) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE A UUID");
            }
        }

        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }

        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            throw new BadRequestException("Group name" + groupName + " does not Exist");
        } else {
            if (roles.contains("org_admin") == false) {
                throw new BadRequestException("adminId does not have the required role");
            }
            String identifier = (String) searchMap.get("identifier");
            List<String> userIdsFromES = (List<String>) searchMap.get("moderators");
            List<String> temp = new ArrayList<>(userIdsFromES);
            for (String user : moderators) {
                if (!userIdsFromES.contains(user)) {
                    userIdsFromES.add(user);
                }
            }
            if (userIdsFromES.equals(temp)) {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("nothing_to_update", identifier);
                masterResultMap.put("missing_email_ids", missingEmailIds);
                response.put("data", masterResultMap);
            } else {
                Map<String, Object> resultMap = new HashMap<String, Object>();
                String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
                int pos = date.lastIndexOf(":");
                date = date.substring(0, pos) + "." + date.substring(pos + 1);
                pos = date.indexOf("+");
                date = date.substring(0, pos);
                resultMap.put("moderators", userIdsFromES);
                resultMap.put("lastUpdatedOn", date);
                resultMap.put("lastUpdatedBy", adminId);
                boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                        LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
                if (x) {
                    Map<String, Object> masterResultMap = new HashMap<>();
                    masterResultMap.put("updated_group_id", identifier);
                    masterResultMap.put("missing_email_ids", missingEmailIds);
                    response.put("data", masterResultMap);
                }
                List<UserAccessPathsAdminsPrimaryKeyModel> modInsertObj = new ArrayList<>();
                for (String modId : moderators) {
                    Set<String> rolesCas = new HashSet<>();
                    rolesCas.add("group_mod");
                    UserAccessPathsAdminsPrimaryKeyModel obj = new UserAccessPathsAdminsPrimaryKeyModel();
                    obj.setAdminId(UUID.fromString(modId));
                    obj.setOrg(org);
                    obj.setRootOrg(rootOrg);
                    obj.setRoles(rolesCas);
                    modInsertObj.add(obj);
                }
                try {
                    userAccessPathsAdminsRepository.saveAll(modInsertObj);
                } catch (ClassCastException | IllegalArgumentException e) {
                    throw new ApplicationLogicError("failed to insert into table");
                }
            }

        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response removeModeratorFromGroup(Request requestBody) throws Exception {
        Response response = new Response();
        if (requestBody == null || requestBody.toString().isEmpty()) {
            throw new BadRequestException("Request is invalid");
        }
        Map<String, Object> req = requestBody.getRequest();
        List<String> moderators = (List<String>) req.get("moderators");
        if (moderators == null || moderators.isEmpty()) {
            throw new BadRequestException("userIds is invalid");
        }
        String adminId = (String) req.get("adminId");
        if (adminId == null || adminId.isEmpty()) {
            throw new BadRequestException("adminId is invalid");
        }
        UUID adminUUID;
        try {
            adminUUID = UUID.fromString(adminId);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("adminId MUST BE A UUID");
        }
        List<UserAccessPathsAdminsPrimaryKeyModel> casRes = userAccessPathsAdminsRepository
                .findByPrimaryKeyAdminId(adminUUID);
        if (casRes.size() == 0 || casRes.isEmpty()) {
            throw new BadRequestException("adminId is not authorised");
        }
        Boolean isEmail = (Boolean) req.get("isEmail");
        if (isEmail == null) {
            throw new BadRequestException("isEmail is invalid");
        }
        String groupIdentifier = req.get("groupId").toString();
        if (groupIdentifier == null || groupIdentifier.isEmpty()) {
            throw new BadRequestException("groupIdentifier is invalid");
        }
        String groupName, rootOrg, org;
        try {
            String[] values = groupIdentifier.split("_");
            groupName = WordUtils.capitalize(values[0].trim());
            rootOrg = WordUtils.capitalize(values[1].trim());
            org = WordUtils.capitalize(values[2].trim());
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new BadRequestException("invalid group identifier");
        }
        Set<String> adminRootOrg = new HashSet<>();
        Set<String> adminOrg = new HashSet<>();
        Set<String> roles = new HashSet<>();
        casRes.forEach(item -> {
            adminRootOrg.add(item.getRootOrg());
            adminOrg.add(item.getOrg());
            roles.addAll(item.getRoles());
        });
        if ((adminOrg.contains(org) == false) || (adminRootOrg.contains(rootOrg) == false)) {
            throw new BadRequestException("adminId is not authorised for rootOrg/Org");
        }
        List<String> userEmails = new ArrayList<>();
        List<String> missingEmailIds = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        if (isEmail) {
            try {
                List<ApplicationPropertiesModel> validDomain = applicationPropertiesRepository
                        .findByKeyIn(Arrays.asList("valid_domains"));
                if (!(validDomain == null || validDomain.isEmpty())) {
                    List<String> validDomains = Arrays.asList(validDomain.get(0).getValue().split(","));
                    userEmails = moderators.parallelStream().filter(userId -> userId.matches(
                            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
                            .filter(userId -> validDomains.contains(userId.substring(userId.indexOf("@"))))
                            .collect(Collectors.toList());
                    Map<String, Object> someMap = userRepositoryImpl.getUUIDsFromEmails(userEmails);
                    for (String uEmail : userEmails) {
                        if (someMap.containsKey(uEmail)) {
                            uuids.add((String) ((Map<String, Object>) someMap.get(uEmail)).get("id"));
                        } else {
                            missingEmailIds.add(uEmail);
                        }
                    }
                }
                moderators = uuids;
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE valid emails");
            }
        } else {
            try {
                for (String i : moderators) {
                    UUID.fromString(i);
                }
            } catch (ClassCastException | IllegalArgumentException e) {
                throw new BadRequestException("userIds MUST BE A UUID");
            }
        }
        String id = groupName.toLowerCase().trim() + "_" + rootOrg.toLowerCase().trim() + "_"
                + org.toLowerCase().trim();
        Map<String, Object> searchMap = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                LexProjectUtil.EsType.access_control_group.getTypeName(), id);
        if (searchMap.size() == 0 || searchMap.isEmpty()) {
            throw new BadRequestException("Group : " + groupName + " does not exist");
        } else {
            if (roles.contains("org_admin") == false) {
                throw new BadRequestException("adminId does not have required roles");
            }
            String identifier = (String) searchMap.get("identifier");
            List<String> userIdsFromES = (List<String>) searchMap.get("moderators");
            List<String> temp = new ArrayList<>(userIdsFromES);
            for (String user : moderators) {
                if (userIdsFromES.contains(user)) {
                    userIdsFromES.remove(user);
                }
            }
            if (userIdsFromES.equals(temp)) {
                Map<String, Object> masterResultMap = new HashMap<>();
                masterResultMap.put("nothing_to_update", identifier);
                masterResultMap.put("missing_email_ids", missingEmailIds);
                response.put("data", masterResultMap);
            } else {
                Map<String, Object> resultMap = new HashMap<String, Object>();
                String date = ProjectUtil.getFormattedDate().replaceFirst(" ", "T");
                int pos = date.lastIndexOf(":");
                date = date.substring(0, pos) + "." + date.substring(pos + 1);
                pos = date.indexOf("+");
                date = date.substring(0, pos);
                resultMap.put("moderators", userIdsFromES);
                resultMap.put("lastUpdatedBy", adminId);
                resultMap.put("lastUpdatedOn", date);
                boolean x = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.access_control_groups.getIndexName(),
                        LexProjectUtil.EsType.access_control_group.getTypeName(), identifier, resultMap);
                if (x) {
                    Map<String, Object> masterResultMap = new HashMap<>();
                    masterResultMap.put("updated_group_id", identifier);
                    masterResultMap.put("missing_email_ids", missingEmailIds);
                    response.put("data", masterResultMap);
                }
                List<UserAccessPathsAdminsPrimaryKeyModel> modInsertObj = new ArrayList<>();
                for (String modId : moderators) {
                    Set<String> rolesCas = new HashSet<>();
                    rolesCas.add("group_mod");
                    UserAccessPathsAdminsPrimaryKeyModel obj = new UserAccessPathsAdminsPrimaryKeyModel();
                    obj.setAdminId(UUID.fromString(modId));
                    obj.setOrg(org);
                    obj.setRootOrg(rootOrg);
                    obj.setRoles(rolesCas);
                    modInsertObj.add(obj);
                }
                try {
                    userAccessPathsAdminsRepository.deleteAll(modInsertObj);
                } catch (ClassCastException | IllegalArgumentException e) {
                    throw new ApplicationLogicError("failed to insert into table");
                }
            }
        }
        return response;

    }

    @Override
    public List<String> getAllGroupsForUser(String userId) throws Exception {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termsQuery("userIds", userId));
        String[] sourceUser = {"identifier"};
        Map<String, Object> allGroupsOfUser = getDataFromElasticSearchWithNoScroll(LexProjectUtil.EsIndex.access_control_groups.getIndexName(), LexProjectUtil.EsType.access_control_group.getTypeName(), boolQuery, sourceUser, 100);
        HashSet<String> groupSetIds = (HashSet<String>) getAllUserGroups(allGroupsOfUser);
        List<String> groupIds = new ArrayList<>(groupSetIds);
        return groupIds;
    }

    @Override
    public Response getContentUsersAccess(Map<String, Object> req, String rootOrg, UUID uuid) throws Exception {
        Response response = new Response();
        Map<String, Object> finalMap = new HashMap<>();
        long start = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> getContentUsersAccess for " + req, LoggerEnum.INFO);
        ProjectLogger.log(uuid + " --> Fetching content Access started at " + start, LoggerEnum.INFO);
        for (String s : req.keySet()) {
            ArrayList<String> currVal;
            try {
                currVal = (ArrayList<String>) req.get(s);
            } catch (Exception e) {
                throw new BadRequestException("contentIds should be List<String");
            }

            Response tempResponse = getContentUserAccess(s, currVal, rootOrg, uuid);
            finalMap.put(s, tempResponse.getResult().get("response"));
        }
        long end = System.currentTimeMillis();
        ProjectLogger.log(uuid + " --> Fetching content Access ended at " + new Date() + " Total time taken : " + (end-start), LoggerEnum.INFO);
        response.put("response", finalMap);
        return response;
    }

}