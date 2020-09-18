package com.infosys.service;

import org.sunbird.common.models.response.Response;

import java.util.Map;

public interface ContentShareService {

    Map<String, Object> createContentPublicUrl(Map<String, Object> request) throws Exception;


    Map<String, Object> verifyContentPublicToken(String publicToken, String rootOrg, String org, String pageType) throws Exception;

}
