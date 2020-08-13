/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.infosys.model.CourseRecommendation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface UsageRecommendationService {

    public List<CourseRecommendation> getUsageRecommendations(String emailId, String pageSize, String pageNumber);

    public Map<String, List<CourseRecommendation>> getUsageRecommendationsForCourses(String[] contentIds) throws IOException;
}
