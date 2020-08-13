/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import com.infosys.exception.PropertiesNotFoundException;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    public final static String[] getIPAndPort(String ip, String port, String propertiesFile) throws PropertiesNotFoundException {
        String ips = System.getenv(ip);
        String envPort = System.getenv(port);
        //System.out.println("coming env file "+ips+" "+envPort);
        Properties properties = null;

        if (ProjectUtil.isStringNullOREmpty(ips) || ProjectUtil.isStringNullOREmpty(envPort)) {
            PropertiesUtil util = new PropertiesUtil();
            properties = util.loadFromPropertiesFile(propertiesFile);
            ips = properties.getProperty(ip);
            envPort = properties.getProperty(port);
            //System.out.println("coming from properties file");
        }

        if (ips == null || envPort == null)
            throw new PropertiesNotFoundException("ip or port not found");

        String ipAndPasswords[] = new String[2];
        ipAndPasswords[0] = ips;
        ipAndPasswords[1] = envPort;

        return ipAndPasswords;
    }

    private Properties loadFromPropertiesFile(String propertiesFile) {

        InputStream input = null;
        Properties properties = new Properties();

        try {
            input = this.getClass().getResourceAsStream("/" + LexJsonKey.IP_FILE);
            // load a properties file
            properties.load(input);
        } catch (IOException ex) {
            ProjectLogger.log(ex.getMessage(), ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    ProjectLogger.log(e.getMessage(), e);
                }
            }
        }
        return properties;
    }

}
