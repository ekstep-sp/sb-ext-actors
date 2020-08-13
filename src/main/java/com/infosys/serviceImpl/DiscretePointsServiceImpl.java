/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.model.cassandra.DiscretePointsModel;
import com.infosys.model.cassandra.DiscretePointsPrimaryKeyModel;
import com.infosys.repository.DiscretePointsRepository;
import com.infosys.service.DiscretePointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class DiscretePointsServiceImpl implements DiscretePointsService {

    @Autowired
    DiscretePointsRepository discretePointsRepository;

    Map<String, Integer> pointsMap = new HashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;

        {
            put("resource", 1);
            put("quiz", 2);
            put("assessment_course", 5);
            put("assessment_collection", 3);
            put("exercise", 3);
        }
    };

    @Async
    @Override
    public void PutPoints(String userUUID, String contentId, String resourecType, String parent) {
        int point = 0;
        String points_for = "";
        if (resourecType.toLowerCase().equals("assessment")) {
            if (parent.toLowerCase().equals("course")) {
                points_for = "assessment_course";
            } else {
                points_for = "assessment_collection";
            }
        } else if (resourecType.toLowerCase().equals("quiz")) {
            points_for = "quiz";
        } else if (resourecType.toLowerCase().equals("exercise")) {
            points_for = "exercise";
        } else {
            points_for = "resource";
        }
        point = pointsMap.get(points_for);
        discretePointsRepository.insert(new DiscretePointsModel(new DiscretePointsPrimaryKeyModel(), point, new Date(), points_for));

    }

}