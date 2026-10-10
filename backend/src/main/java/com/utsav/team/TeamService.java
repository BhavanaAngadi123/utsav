package com.utsav.team;

import com.utsav.admin.AuditService;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Team builder: customers assemble vendor teams for events. */
@Service
public class TeamService {

  private final EventTeamRepository teams;
  private final UserRepository users;
  private final VendorRepository vendors;
  private final AuditService audit;

  public TeamService(
      EventTeamRepository teams,
      UserRepository users,
      VendorRepository vendors,
      AuditService audit) {
    this.teams = teams;
    this.users = users;
    this.vendors = vendors;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public EventTeam createTeam(
      UUID customerId, String name, String occasionSlug, LocalDate eventDate) {
    User customer =
        users
            .findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    EventTeam team = new EventTeam(customer, name, occasionSlug, eventDate);
    teams.save(team);
    audit.log(customer, "TEAM_CREATED", "EventTeam", team.getId().toString(), name);
    return team;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public EventTeam addMember(UUID customerId, UUID teamId, UUID vendorId, String roleLabel) {
    EventTeam team = getOwnedTeam(customerId, teamId);
    Vendor vendor =
        vendors
            .findById(vendorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendor not found"));
    boolean already =
        team.getMembers().stream().anyMatch(m -> m.getVendor().getId().equals(vendorId));
    if (already) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "vendor already on team");
    }
    EventTeamMember member = new EventTeamMember(team, vendor, roleLabel);
    team.getMembers().add(member);
    audit.log(
        team.getCustomer(),
        "TEAM_MEMBER_ADDED",
        "EventTeam",
        teamId.toString(),
        vendor.getBusinessName());
    return teams.save(team);
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public void removeMember(UUID customerId, UUID teamId, UUID vendorId) {
    EventTeam team = getOwnedTeam(customerId, teamId);
    team.getMembers().removeIf(m -> m.getVendor().getId().equals(vendorId));
    teams.save(team);
  }

  public List<EventTeam> myTeams(UUID customerId) {
    return teams.findByCustomerIdOrderByCreatedAtDesc(customerId);
  }

  public EventTeam getOwnedTeam(UUID customerId, UUID teamId) {
    EventTeam team =
        teams
            .findById(teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "team not found"));
    if (!team.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your team");
    }
    return team;
  }
}
