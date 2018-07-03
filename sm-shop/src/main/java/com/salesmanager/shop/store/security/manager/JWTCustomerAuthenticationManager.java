package com.salesmanager.shop.store.security.manager;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import com.salesmanager.shop.store.security.JWTTokenUtil;

import io.jsonwebtoken.ExpiredJwtException;

@Component("jwtCustomCustomerAuthenticationManager")
public class JWTCustomerAuthenticationManager extends CustomAuthenticationManager {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
    @Inject
    private JWTTokenUtil jwtTokenUtil;
    
    @Inject
    private UserDetailsService jwtCustomerDetailsService;

	@Override
	Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {

        final String requestHeader = request.getHeader(super.getTokenHeader());
        String username = null;
        String authToken = null;
        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {
            authToken = requestHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(authToken);
            } catch (IllegalArgumentException e) {
                logger.error("an error occured during getting username from token", e);
            } catch (ExpiredJwtException e) {
                logger.warn("the token is expired and not valid anymore", e);
            }
        } else {
        	throw new CustomAuthenticationException("No Bearer token found in the request");
        }
        
        UsernamePasswordAuthenticationToken authentication = null;
		
        
        logger.info("checking authentication for user " + username);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.jwtCustomerDetailsService.loadUserByUsername(username);
            if (userDetails != null && jwtTokenUtil.validateToken(authToken, userDetails)) {
                authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                logger.info("authenticated user " + username + ", setting security context");
            }
        }
		
		return authentication;
	}

	@Override
	void successfullAuthentication(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws AuthenticationException {

    }

	@Override
	void unSuccessfullAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {

    }

}
