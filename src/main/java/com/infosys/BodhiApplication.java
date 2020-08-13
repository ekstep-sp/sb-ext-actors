/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableCaching
@SpringBootApplication
public class BodhiApplication {

    public static void main(String[] args) {
        Properties props = System.getProperties();
		props.setProperty("es.set.netty.runtime.available.processors", "false");
        SpringApplication.run(BodhiApplication.class, args);
    }

    @Bean
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
}