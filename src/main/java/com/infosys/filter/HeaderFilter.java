/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.annotation.Configuration;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

//import javax.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HeaderFilter implements Filter {

	public void destroy() {
		System.out.println("Destroying HeadFilter");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(req);
		String remote_addr = request.getRemoteAddr();
		requestWrapper.addHeader("remote_addr", remote_addr);

		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		httpServletResponse.addHeader("Cache-Control", "no-store");
		httpServletResponse.addHeader("pragma", "no-cache");
		filterChain.doFilter(requestWrapper, httpServletResponse);
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("Initiating HeaderFilter");
	}

}