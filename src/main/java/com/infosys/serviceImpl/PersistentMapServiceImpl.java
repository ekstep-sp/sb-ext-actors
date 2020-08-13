/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

import com.infosys.model.cassandra.PersistentMapModel;
import com.infosys.repository.PersistentMapRepository;
import com.infosys.service.PersistentMapService;

@Service
public class PersistentMapServiceImpl implements PersistentMapService{
	
	@Autowired
	private PersistentMapRepository persistentMapRepository;

	@Override
	public Response getKeyValuePairs(ArrayList<String> keys) {
		Response resp = new Response();
		
		Iterable<PersistentMapModel> data = persistentMapRepository.findAllById(keys);
		data.forEach(item->{
			resp.put(item.getKey(), item.getValue());
		});
		
		return resp;
	}

	@Override
	public Response saveKeyValuePairs(Request request) {
		Response resp = new Response();
		
		Map<String, Object> req = request.getRequest();
		List<PersistentMapModel> entities = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		
		req.forEach((key,val)->{
			PersistentMapModel model = new PersistentMapModel();
			try {
				model.setKey(key);
				model.setValue((String) val);
				entities.add(model);
			}catch (Exception e) {
				failed.add(key);
			}
		});
		
		Iterable<PersistentMapModel> data = persistentMapRepository.saveAll(entities);
		
		data.forEach(item->{
			if(!req.containsKey(item.getKey()))
				failed.add(item.getKey());
		});
		
		resp.put("failed", failed);
		
		return resp;
	}

	@Override
	public Response deleteKeyValuePairs(ArrayList<String> keys) {
		Response resp = new Response();
		
		List<PersistentMapModel> entities = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		
		keys.forEach(key->{
			PersistentMapModel model = new PersistentMapModel();
			try {
				model.setKey(key);
				entities.add(model);
			}catch (Exception e) {
				failed.add(key);
			}
		});
		
		persistentMapRepository.deleteAll(entities);
		
		resp.put("failed", failed);
		
		return resp;
	}
	
}
