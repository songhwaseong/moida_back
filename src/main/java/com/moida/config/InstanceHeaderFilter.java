package com.moida.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class InstanceHeaderFilter extends OncePerRequestFilter {

    private static final String INSTANCE_HEADER = "X-Instance-Name";

    @Value("${INSTANCE_NAME:${HOSTNAME:unknown}}")
    private String instanceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(INSTANCE_HEADER, instanceName);
        filterChain.doFilter(request, response);
    }
}
