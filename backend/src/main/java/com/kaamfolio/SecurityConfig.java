package com.kaamfolio;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
	@Bean
	UserDetailsService users(@Value("${app.username}") String username, @Value("${app.password}") String password) {
		if (password.length() < 12)
			throw new IllegalArgumentException("APP_PASSWORD must have at least 12 characters");
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("OWNER").build());
	}

	@Bean
	SecurityFilterChain security(HttpSecurity http) throws Exception {
		return http.cors(c -> {
		}).csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(
						a -> a.requestMatchers("/api/health", "/media/**").permitAll().anyRequest().authenticated())
				.httpBasic(b -> b.authenticationEntryPoint((req, res, e) -> {
					res.setStatus(401);
					res.setContentType("application/json");
					res.getWriter().write("{\"message\":\"Sign in with your owner username and password.\"}");
				})).build();
	}

	@Bean
	CorsConfigurationSource cors(@Value("${app.allowed-origins}") String origins) {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
