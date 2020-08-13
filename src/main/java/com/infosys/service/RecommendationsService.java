/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import org.sunbird.common.models.response.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RecommendationsService {
    Response getLatestOrg(UUID userId, String learningMode, String rootOrg, String org, int pageNumber, int pageSize, List<String> contentTypefiltersArray, List<String> sourceShortName, Boolean externalContent, List<String> locale, Boolean isInIntranet, Boolean isStandAlone) throws Exception;

    Response getLatestForRoles(UUID userId, String learningMode, String rootOrg, String org, int pageNumber, int pageSize, List<String> contentTypefiltersArray, List<String> sourceShortName, Boolean externalContent, List<String> locale, Boolean isInIntranet, Boolean isStandAlone)throws Exception;

    Response getRecommendationsForJL(UUID userId, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getRecommendationsForUnit(UUID userId, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getRecommendationsForAccount(UUID userId, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getRecommendationsForOrg(UUID userId, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getTrendingForUnit(UUID userId, String rootOrg, String org, int pageNumber, int pageSize, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getTrendingForAccount(UUID userId, String rootOrg, String org, int pageNumber, int pageSize, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getTrendingForJL(UUID userId, String rootOrg, String org, int pageNumber, int pageSize, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    List<Map<String, Object>> getTrendingOrg(UUID userId, String rootOrg, String org, int pageNumber, int pageSize, List<String> filtersArray, List<String> locales, List<String> sourceFields, Boolean isInIntranet)throws Exception;

    Response getInterests(UUID userId, List<String> learningMode, int pageNumber, int pageSize, List<String> excludeContentTypes, Boolean externalContentFilter, List<String> locale, String rootOrg, List<String> sourceFields, Boolean isInIntranet, Boolean isStandAlone) throws Exception;
}
