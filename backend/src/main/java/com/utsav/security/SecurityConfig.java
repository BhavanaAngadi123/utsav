package com.utsav.security;

import com.utsav.config.UtsavProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security: JWT stateless auth, RBAC via @PreAuthorize, strict CORS,
 * security headers, rate limiting on auth/payment endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final UtsavProperties props;
  private final RateLimitFilter rateLimitFilter;

  public SecurityConfig(
      JwtAuthFilter jwtAuthFilter, UtsavProperties props, RateLimitFilter rateLimitFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.props = props;
    this.rateLimitFilter = rateLimitFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // stateless JWT API; Vaadin uses its own CSRF
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Public: auth, docs, health, Vaadin UI, browse endpoints
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/webhooks/**")
                    .permitAll() // signature-verified
                    .requestMatchers("/api/concierge/status")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/vendors", "/api/vendors/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/**", "/api/catalog/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/info")
                    .permitAll()
                    // Vaadin frontend routes
                    .requestMatchers("/", "/login", "/concierge", "/budget", "/teams", "/bookings",
                        "/vendor/**", "/admin/**", "/VAADIN/**", "/frontend/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                    .accessDeniedHandler(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                                    + "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:"))
                    .referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()")))
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    // Optional Google OAuth2.
    if (props.getOauth().isGoogleEnabled()) {
      http.oauth2Login(oauth -> oauth.defaultSuccessUrl("/", true));
    }

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> origins =
        Arrays.stream(props.getCorsAllowedOrigins().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Stripe-Signature"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
