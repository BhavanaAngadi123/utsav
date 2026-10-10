package com.utsav.security;

import com.utsav.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates the Bearer JWT and populates the SecurityContext. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository users;

  public JwtAuthFilter(JwtService jwtService, UserRepository users) {
    this.jwtService = jwtService;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        var claims = jwtService.parseAccessToken(token);
        var user = users.findById(claims.userId());
        if (user.isPresent() && user.get().isEnabled() && !user.get().isLocked()) {
          var auth =
              new UsernamePasswordAuthenticationToken(
                  claims.userId().toString(),
                  null,
                  List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name())));
          auth.setDetails(claims);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (JwtException | IllegalArgumentException e) {
        // Invalid token: leave unauthenticated; security rules will reject.
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
