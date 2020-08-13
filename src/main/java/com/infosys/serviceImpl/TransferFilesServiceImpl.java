/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.model.ContentMeta;
import com.infosys.model.ObjectMeta;
import com.infosys.util.LexProjectUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

//import java.io.File;
//import java.util.Base64;

@RestController
public class TransferFilesServiceImpl {

	
	public int createContent(ContentMeta contentObj) {


        String ret = ElasticSearchUtil.createData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                LexProjectUtil.EsType.resource.getTypeName(), contentObj.getIdentifier(), contentObj.toMap(false));

		if (ret.equals(contentObj.getIdentifier()))
			return 0;
		else
			return -1;
	}
	
	
	
	
	
	@GetMapping("/v1/{resourceId}")
	public void publishToLexContentIndex(@PathVariable("resourceId") String resourceId) throws IOException {
		//System.out.println(resourceId);
		
		Map<String, Object> contentMeta = ElasticSearchUtil.getDataByIdentifier(
                LexProjectUtil.EsIndex.authoring_tool.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(),
                resourceId);
        if (contentMeta == null || contentMeta.isEmpty()) {
            contentMeta = ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                    LexProjectUtil.EsType.resource.getTypeName(), resourceId);
            if (contentMeta == null || contentMeta.isEmpty()) {
                throw new ResourceNotFoundException("No resource with identifier found");
			}
			else
				return;
		}

		contentMeta.remove("msArtifactDetails");

		ContentMeta contentMetaObject = ContentMeta.fromMap(contentMeta);
		
		System.out.println(contentMetaObject.getIdentifier());
		
		if (contentMetaObject == null)
			throw new ApplicationLogicError("Backend problem");

		if (createContent(contentMetaObject) == -1)
			throw new ApplicationLogicError("Internal Server Error, Something wrong");

		ObjectMeta[] children = contentMetaObject.getChildren();

		for (int i = 0; i < children.length; i++) {
			String identifier = children[i].getIdentifier();
			//System.out.println(identifier);
			if (identifier == null)
				throw new ApplicationLogicError("Internal Server Error, Something wrong");
			publishToLexContentIndex(identifier);
		}

	}
	
	
}