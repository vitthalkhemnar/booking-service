package com.ecom.book.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String SECRET_STRING = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
		String token = authHeader.substring(7);

		try {
			Claims claims = Jwts.parser()
					.verifyWith(Keys.hmacShaKeyFor(SECRET_STRING.getBytes()))
					.build()
					.parseSignedClaims(token)
					.getPayload();

			String username = claims.getSubject();
			List<String> roles = claims.get("role", List.class);

			var authorities = roles.stream()
					.map(SimpleGrantedAuthority::new)
					.toList();

			var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);

			Thread.currentThread().setName(username);
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			throw new RuntimeException(e);
		}
		
		filterChain.doFilter(request, response);
	}

}
