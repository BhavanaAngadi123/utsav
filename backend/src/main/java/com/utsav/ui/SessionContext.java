package com.utsav.ui;

import com.utsav.common.Role;
import com.utsav.security.JwtService;
import com.vaadin.flow.server.VaadinSession;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Vaadin session-scoped auth context. Stores the JWT after login;
 * views use it to call the backend services.
 */
@Component
@SessionScope
public class SessionContext {

  private String accessToken;
  private String refreshToken;
  private UUID userId;
  private String email;
  private String displayName;
  private Role role;

  private final JwtService jwtService;

  public SessionContext(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public void login(String accessToken, String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    try {
      var claims = jwtService.parseAccessToken(accessToken);
      this.userId = claims.userId();
      this.email = claims.email();
      this.role = claims.role();
      this.displayName = email != null && email.contains("@")
          ? email.substring(0, email.indexOf('@'))
          : email;
    } catch (Exception e) {
      logout();
      throw new IllegalArgumentException("invalid token");
    }
  }

  public void logout() {
    accessToken = null;
    refreshToken = null;
    userId = null;
    email = null;
    displayName = null;
    role = null;
    VaadinSession.getCurrent().getSession().invalidate();
  }

  public boolean isLoggedIn() {
    return accessToken != null;
  }

  public boolean hasRole(Role r) {
    return role == r;
  }

  public UUID requireUserId() {
    if (userId == null) throw new IllegalStateException("not logged in");
    return userId;
  }

  public String authHeader() {
    return "Bearer " + accessToken;
  }

  public String getAccessToken() { return accessToken; }
  public String getRefreshToken() { return refreshToken; }
  public UUID getUserId() { return userId; }
  public String getEmail() { return email; }
  public String getDisplayName() { return displayName; }
  public Role getRole() { return role; }
}
