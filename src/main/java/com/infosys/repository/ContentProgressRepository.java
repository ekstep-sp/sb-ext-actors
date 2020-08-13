/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.infosys.model.cassandra.ContentProgressModel;
import com.infosys.model.cassandra.ContentProgressPrimaryKeyModel;

@Repository
public interface ContentProgressRepository extends CassandraRepository<ContentProgressModel,ContentProgressPrimaryKeyModel>,ContentProgressRepositoryCustom {

    @Query("select content_id,progress from user_content_progress where root_org = ?0 and user_id=?1 and content_type in (?2) and content_id in (?3)")
    public List<ContentProgressModel> getProgress(String rootOrg,String userId, List<String> contentTypes, List<String> contentIds);
    
    @Query("select content_id,progress from user_content_progress where root_org=?0 and user_id=?1 and content_type=?2 and content_id in ?3")
    public List<ContentProgressModel> findProgressByUserIdAndContentId(String rootOrg,String userId, String contentType, List<String> contentIds);
}
