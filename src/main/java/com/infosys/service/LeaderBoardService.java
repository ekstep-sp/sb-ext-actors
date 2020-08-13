/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.util.List;
import java.util.Map;


public interface LeaderBoardService {

    public Map<String, Object> getLeaderBoard(String duration_type, String leaderboard_type, String email_id, String year, String duration_value) throws Exception;

    public List<Map<String, Object>> pastTopLearners(String duration_type, String leaderboard_type) throws Exception;

}
