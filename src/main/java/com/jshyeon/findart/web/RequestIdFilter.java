package com.jshyeon.findart.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = request.getHeader(HEADER);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}
		request.setAttribute(ATTRIBUTE, requestId);
		response.setHeader(HEADER, requestId);
		filterChain.doFilter(request, response);
	}

	public static String getRequestId(HttpServletRequest request) {
		return (String) request.getAttribute(ATTRIBUTE);
	}
}
