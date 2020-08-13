/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Set;

@Table("user_personalization_roles")
public class UserPersonalizationRolesModel {

    @PrimaryKey
    private UserPersonalizationRolesPrimaryKeyModel primaryKey;

    @Column("roles")
    private Set<String> roles;

    public UserPersonalizationRolesModel(UserPersonalizationRolesPrimaryKeyModel primaryKey, Set<String> roles) {
        super();
        this.primaryKey = primaryKey;
        this.roles = roles;
    }

    public UserPersonalizationRolesModel() {
        super();
    }

    public UserPersonalizationRolesPrimaryKeyModel getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(UserPersonalizationRolesPrimaryKeyModel primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
