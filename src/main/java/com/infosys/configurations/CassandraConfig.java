/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.configurations;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation;

@Configuration
@EnableCassandraRepositories(basePackages = { "com.infosys.repository" })
public class CassandraConfig {

    @Bean
    public <S> CassandraEntityInformation<S, UUID> entityInfo() throws Exception {
        return new CassandraEntityInformation<S, UUID>() {

			@Override
			public CqlIdentifier getTableName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UUID getId(S arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isNew(S arg0) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Class<UUID> getIdType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Class<S> getJavaType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getIdAttribute() {
				// TODO Auto-generated method stub
				return null;
			}
		};
    }
}
