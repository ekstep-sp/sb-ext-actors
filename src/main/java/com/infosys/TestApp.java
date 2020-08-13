/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys;

import com.infosys.model.ContentMeta;
import com.infosys.serviceImpl.ParentsServiceImpl;

import java.util.List;
import java.util.Map;

public class TestApp {
    public static void main(String[] args) {
        ParentsServiceImpl p = new ParentsServiceImpl();
        Map<String, Object> data = p.getCourseParents("lex_20605780076019790000");
        System.out.println("Courses");
        for (ContentMeta c : (List<ContentMeta>) data.get("courses")) {
            System.out.println(c.getIdentifier());
            System.out.println(c.getName());
            System.out.println(c.getContentType());
            System.out.println();
        }
        System.out.println("\nModules");
        for (ContentMeta c : (List<ContentMeta>) data.get("modules")) {
            System.out.println(c.getIdentifier());
            System.out.println(c.getName());
            System.out.println(c.getContentType());
            System.out.println();
        }
    }
}
