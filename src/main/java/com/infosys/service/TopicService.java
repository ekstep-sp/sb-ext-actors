/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.infosys.model.Topic;

public interface TopicService {

	public List<Topic> getTopics(int size);

	public boolean addTopic(Topic topic);

	public List<Topic> getUserTopics(String userId);

	public boolean addUserTopics(String userId, List<String> topics);

	List<Map<String, Object>> getNewTopics();

	List<Map<String, Object>> getTopTopics() throws IOException;

}
