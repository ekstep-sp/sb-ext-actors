/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.infosys.model.CourseRecommendation;

import java.util.List;

public interface TrendingService {

	List<CourseRecommendation> getTrendingCourses(String userId, String pageNumber, String pageSize);

    public List<CourseRecommendation> getTrendingCourses(String userId, String pageNumberStr, String pageSizeStr, List<String> langCodes);
	
    public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr, String pageSizeStr, List<String> langCodes);
    
    public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr,
            String pageSizeStr, List<String> langCodes, List<String> fieldsRequired);
            
    public List<CourseRecommendation> getTrendingCourses(String rootOrg, String userId, String pageNumberStr,
            String pageSizeStr, Boolean isInIntranet, List<String> langCodes, List<String> fieldsRequired);


	
}
