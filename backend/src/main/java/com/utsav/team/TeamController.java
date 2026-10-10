package com.utsav.team;

import com.utsav.security.JwtService;
import com.utsav.vendor.VendorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Team builder: assemble vendor teams for events. */
@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams")
public class TeamController {

  private final TeamService teamService;
  private final JwtService jwtService;

  public TeamController(TeamService teamService, JwtService jwtService) {
    this.teamService = teamService;
    this.jwtService = jwtService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Create an event team (CUSTOMER only)")
  public ResponseEntity<TeamView> create(
      @RequestHeader("Authorization") String auth, @RequestBody CreateTeamRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    EventTeam team =
        teamService.createTeam(claims.userId(), req.name(), req.occasionSlug(), req.eventDate());
    return ResponseEntity.status(HttpStatus.CREATED).body(TeamView.of(team));
  }

  @GetMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "My event teams (CUSTOMER only)")
  public List<TeamView> myTeams(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return teamService.myTeams(claims.userId()).stream().map(TeamView::of).toList();
  }

  @PostMapping("/{teamId}/members")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Add a vendor to my team (CUSTOMER only, own team)")
  public TeamView addMember(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID teamId,
      @RequestBody AddMemberRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return TeamView.of(
        teamService.addMember(claims.userId(), teamId, req.vendorId(), req.roleLabel()));
  }

  @DeleteMapping("/{teamId}/members/{vendorId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Remove a vendor from my team (CUSTOMER only, own team)")
  public ResponseEntity<Void> removeMember(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID teamId,
      @PathVariable UUID vendorId) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    teamService.removeMember(claims.userId(), teamId, vendorId);
    return ResponseEntity.noContent().build();
  }

  public record CreateTeamRequest(
      @NotBlank String name,
      String occasionSlug,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {}

  public record AddMemberRequest(@NotNull UUID vendorId, @NotBlank String roleLabel) {}

  public record TeamView(
      String id, String name, String occasionSlug, LocalDate eventDate, List<MemberView> members) {
    static TeamView of(EventTeam t) {
      return new TeamView(
          t.getId().toString(),
          t.getName(),
          t.getOccasionSlug(),
          t.getEventDate(),
          t.getMembers().stream()
              .map(m -> new MemberView(VendorDto.View.of(m.getVendor()), m.getRoleLabel()))
              .toList());
    }
  }

  public record MemberView(VendorDto.View vendor, String roleLabel) {}
}
