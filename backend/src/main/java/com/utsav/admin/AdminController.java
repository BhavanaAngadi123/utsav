package com.utsav.admin;

import com.utsav.common.Role;
import com.utsav.security.JwtService;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Admin: role management, audit log. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final UserRepository users;
  private final AuditService audit;
  private final JwtService jwtService;

  public AdminController(UserRepository users, AuditService audit, JwtService jwtService) {
    this.users = users;
    this.audit = audit;
    this.jwtService = jwtService;
  }

  @PostMapping("/users/{id}/role")
  @Operation(summary = "Change a user's role (ADMIN only)")
  public UserView setRole(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID id,
      @RequestBody SetRoleRequest req) {
    var adminClaims = jwtService.parseAccessToken(auth.substring(7));
    User admin = users.findById(adminClaims.userId()).orElseThrow();
    User user =
        users
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    Role old = user.getRole();
    user.setRole(req.role());
    users.save(user);
    audit.log(
        admin, "ROLE_CHANGED", "User", id.toString(), old + " -> " + req.role());
    return UserView.of(user);
  }

  @GetMapping("/audit")
  @Operation(summary = "Audit log, newest first (ADMIN only)")
  public Page<AuditView> audit(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return audit.recent(PageRequest.of(page, size)).map(AuditView::of);
  }

  public record SetRoleRequest(Role role) {}

  public record UserView(String id, String email, String displayName, String role) {
    static UserView of(User u) {
      return new UserView(
          u.getId().toString(), u.getEmail(), u.getDisplayName(), u.getRole().name());
    }
  }

  public record AuditView(
      String id, String actorEmail, String action, String entityType, String entityId, String detail, String createdAt) {
    static AuditView of(AuditLog log) {
      return new AuditView(
          log.getId().toString(),
          log.getActor() != null ? log.getActor().getEmail() : null,
          log.getAction(),
          log.getEntityType(),
          log.getEntityId(),
          log.getDetail(),
          log.getCreatedAt().toString());
    }
  }
}
