package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;

import com.utsav.booking.Booking;
import com.utsav.booking.BookingService;
import com.utsav.review.ReviewService;
import com.utsav.team.TeamService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Reviews update vendor rating; team builder creates teams and adds members. */
class ReviewAndTeamTest extends IntegrationTestBase {

  @Autowired private ReviewService reviewService;
  @Autowired private BookingService bookingService;
  @Autowired private TeamService teamService;

  @Test
  void reviewUpdatesVendorRating() {
    Booking booking =
        runAs(
            customer,
            () ->
                bookingService.book(
                    customer.getId(), vendor.getId(), LocalDate.now().plusDays(50), "Evening", null, null));
    BigDecimal before = vendor.getRatingAvg();
    runAs(customer, () -> reviewService.addReview(customer.getId(), booking.getId(), 5, "Amazing!"));
    var updated = vendors.findById(vendor.getId()).orElseThrow();
    assertThat(updated.getReviewCount()).isEqualTo(11);
    assertThat(updated.getRatingAvg()).isGreaterThan(before);
  }

  @Test
  void teamBuilderCreatesTeamAndAddsMember() {
    var team =
        runAs(
            customer, () -> teamService.createTeam(customer.getId(), "Test Team", "mehendi", null));
    assertThat(team.getName()).isEqualTo("Test Team");
    var withMember =
        runAs(
            customer,
            () -> teamService.addMember(customer.getId(), team.getId(), vendor.getId(), "Decorator"));
    assertThat(withMember.getMembers()).hasSize(1);
    assertThat(withMember.getMembers().get(0).getVendor().getId()).isEqualTo(vendor.getId());
  }

  @Test
  void myTeamsListsCreatedTeams() {
    runAs(customer, () -> teamService.createTeam(customer.getId(), "Team A", null, null));
    runAs(customer, () -> teamService.createTeam(customer.getId(), "Team B", null, null));
    var teams = teamService.myTeams(customer.getId());
    assertThat(teams).hasSize(2);
  }
}
