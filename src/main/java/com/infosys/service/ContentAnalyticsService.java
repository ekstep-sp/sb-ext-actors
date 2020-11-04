package com.infosys.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public interface ContentAnalyticsService {
	Map<String, Object> getContentCountByStatus(String rootOrg, String org, String langCode, String contentStatus, String startDate, String endDate) throws IOException;
	ArrayList<String> getExternalResourcesList(String rootOrg, String org, String langCode, String startDate, String endDate , int searchSize , int offSet) throws IOException;
}
