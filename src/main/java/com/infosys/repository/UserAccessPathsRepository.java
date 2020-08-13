/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.infosys.model.cassandra.UserAccessPathsModel;
import com.infosys.model.cassandra.UserAccessPathsPrimaryKeyModel;


@Repository
public interface UserAccessPathsRepository extends CassandraRepository<UserAccessPathsModel,UserAccessPathsPrimaryKeyModel> {
	
	List<UserAccessPathsModel> findByPrimaryKeyRootOrgAndPrimaryKeyOrgAndPrimaryKeyUserId(String rootOrg, String org, UUID userId);
	
	UserAccessPathsModel findByPrimaryKeyRootOrgAndPrimaryKeyOrgAndPrimaryKeyUserIdAndTemporary(String rootOrg, String org, UUID userId, Boolean temporary);
	
//	@Query("SELECT * from user_access_paths WHERE user_id= :userId ALLOW FILTERING;")
//	List<UserAccessPathsModel> findByPrimaryKeyUserId(@Param("userId") UUID userId);
	
	@Query("SELECT * from user_access_paths WHERE user_id= :userId;")
	List<UserAccessPathsModel> findByPrimaryKeyUserId(@Param("userId") UUID userId);

    @Query("SELECT * FROM user_access_paths WHERE root_org= :rootOrg AND user_id= :userId ALLOW FILTERING;")
    List<UserAccessPathsModel> findByPrimaryKeyRootOrgAndPrimaryKeyUserId(@Param("rootOrg") String rootOrg, @Param("userId") UUID userId);

    @Query("SELECT * from user_access_paths WHERE root_org= :rootOrg AND org IN :org AND user_id= :userId;")
    List<UserAccessPathsModel> findByPrimaryKeyRootOrgAndPrimaryKeyOrgInAndPrimaryKeyUserId(@Param("rootOrg") String rootOrg, @Param("org") List<String> org, @Param("userId") UUID userId);
}


