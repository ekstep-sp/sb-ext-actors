/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

@RestController
@CrossOrigin(origins = "*")
public class DiscussionForumController {
	
	@PutMapping("/v1/discuss/post/{emailId}/{groupId}")
	public Response createPost(@RequestBody Request requestBody, @PathVariable("emailId") String emailId, @PathVariable("groupId") String groupId) {
		Response response = new Response();
		return response;
	}
	
	@PutMapping("/v1/discuss/comment/{emailId}/{groupId}/{parentId")
	public Response createComment(@RequestBody Request requestBody, @PathVariable("emailId") String emailId, @PathVariable("groupId") String groupId, @PathVariable("parentId") String parentId) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/post/{postId}")
	public Response getPost(
			@PathVariable("postId") String postId) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/posts/{groupId}")
	public Response getPosts(
			@PathVariable("groupId") String groupId,
			@RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(value = "sort", defaultValue = "POST_DATE") String sort,
			@RequestParam(value = "sortType", defaultValue = "DESC") String sortType) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/comments/{parentId}")
	public Response getComments(
			@PathVariable("parentId") String parentId,
			@RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(value = "sort", defaultValue = "POST_DATE") String sort,
			@RequestParam(value = "sortType", defaultValue = "DESC") String sortType) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/hierarchy/{groupId}")
	public Response getHierarchy(
			@PathVariable("groupId") String groupId,
			@RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(value = "sort", defaultValue = "POST_DATE") String sort,
			@RequestParam(value = "sortType", defaultValue = "DESC") String sortType) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/post/history/{postId}")
	public Response getHistory(
			@PathVariable("postId") String postId,
			@RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
		Response response = new Response();
		return response;
	}
	
	@GetMapping("/v1/discuss/posts/flagged/{groupId}")
	public Response getFlaggedPosts(
			@PathVariable("postId") String postId,
			@RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
			@RequestParam(value = "sort", defaultValue = "POST_DATE") String sort,
			@RequestParam(value = "sortType", defaultValue = "DESC") String sortType) {
		Response response = new Response();
		return response;
	}
}