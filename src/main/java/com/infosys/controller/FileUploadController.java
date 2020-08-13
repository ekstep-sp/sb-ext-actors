/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

@RestController
public class FileUploadController {
	

	    @RequestMapping(value="/upload", method=RequestMethod.POST)
	    public @ResponseBody String handleFileUpload(@RequestParam("name") String name,
	                                                 @RequestParam("file") MultipartFile file,
	                                                 @RequestParam("format") String format,
	                                                 @RequestParam("id") String resourceId){
	        if (!file.isEmpty()) {
	            try {
	                byte[] bytes = file.getBytes();
	               // String path = "http:///:5903content//ETA//"+resourceId+"//"+name+"."+format+"?type=assets";
	                String path = "C:/"+resourceId;
	                boolean createDir = new File(path).mkdir();
	                path+="/assets";
	                boolean createAssetsDir = new File(path).mkdir();
	                
	                System.out.println(createDir+" "+createAssetsDir);
	                
	                String fileName = path+"/"+name+"."+format;
	                BufferedOutputStream stream =
	                        new BufferedOutputStream(new FileOutputStream(new File(fileName)));
	                stream.write(bytes);
	                stream.close();
	                return "You successfully uploaded " + name + " into " + name + "-uploaded !";
	            } catch (Exception e) {
	                return "You failed to upload " + name + " => " + e.getMessage();
	            }
	        } else {
	            return "You failed to upload " + name + " because the file was empty.";
	        }
	    }
	}