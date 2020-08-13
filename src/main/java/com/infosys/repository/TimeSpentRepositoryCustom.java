/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.Date;
import java.util.Map;

public interface TimeSpentRepositoryCustom {

	public Map<String, Object> getUserDurationStats(String emailId, Date startDate, Date endDate);

	public Map<String, Object> getAvgDurationStats(Date startDate, Date endDate);

}
