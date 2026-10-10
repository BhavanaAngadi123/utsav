package com.utsav.ui;

import com.utsav.team.TeamService;
import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

/** Team builder: assemble vendor teams for events. */
@Route(value = "teams", layout = MainLayout.class)
@PageTitle("Utsav — Team Builder")
public class TeamBuilderView extends VerticalLayout {

  private final TeamService teamService;
  private final VendorService vendorService;
  private final SessionContext session;

  public TeamBuilderView(
      TeamService teamService, VendorService vendorService, SessionContext session) {
    this.teamService = teamService;
    this.vendorService = vendorService;
    this.session = session;
    addClassName("utsav-view");
    setSpacing(true);

    H2 heading = new H2("Team Builder");
    heading.addClassName("utsav-h2");
    Paragraph sub = new Paragraph("Assemble your dream vendor team for the event.");
    sub.addClassName("utsav-sub");

    add(heading, sub);

    if (!session.isLoggedIn()) {
      add(new Paragraph("Please sign in to build teams."));
      return;
    }

    // Create team
    TextField name = new TextField("Team name");
    name.setPlaceholder("e.g. Priya's Mehendi");
    TextField occasion = new TextField("Occasion");
    occasion.setPlaceholder("e.g. mehendi");
    Button create = new Button("Create team", e -> {
      try {
        teamService.createTeam(session.requireUserId(), name.getValue(),
            occasion.getValue(), null);
        Notification.show("Team created!");
        refreshTeams();
      } catch (Exception ex) {
        Notification.show("Couldn't create: " + ex.getMessage());
      }
    });
    create.addClassName("utsav-btn-primary");
    add(new HorizontalLayout(name, occasion, create));

    Div teamsDiv = new Div();
    teamsDiv.addClassName("utsav-teams");
    add(teamsDiv);
    refreshTeams();
  }

  private void refreshTeams() {
    Div teamsDiv = (Div) getComponentAt(getComponentCount() - 1);
    teamsDiv.removeAll();
    for (var team : teamService.myTeams(session.requireUserId())) {
      Div card = new Div();
      card.addClassName("utsav-team-card");
      card.add(new H2(team.getName()));

      // Members
      Grid<TeamMemberRow> grid = new Grid<>(TeamMemberRow.class, false);
      grid.addColumn(TeamMemberRow::vendorName).setHeader("Vendor");
      grid.addColumn(TeamMemberRow::role).setHeader("Role");
      grid.setItems(team.getMembers().stream()
          .map(m -> new TeamMemberRow(m.getVendor().getBusinessName(), m.getRoleLabel()))
          .toList());
      card.add(grid);

      // Add member
      ComboBox<VendorDto.View> vendorPick = new ComboBox<>("Add vendor");
      vendorPick.setItems(
          vendorService.search(null, null, null, false, PageRequest.of(0, 50))
              .map(VendorDto.View::of).getContent());
      vendorPick.setItemLabelGenerator(v -> v.businessName() + " (" + v.city() + ")");
      TextField roleLabel = new TextField("Role");
      roleLabel.setPlaceholder("e.g. Photographer");
      Button add = new Button("Add", e -> {
        if (vendorPick.getValue() == null) return;
        try {
          teamService.addMember(session.requireUserId(), team.getId(),
              UUID.fromString(vendorPick.getValue().id()), roleLabel.getValue());
          Notification.show("Added!");
          refreshTeams();
        } catch (Exception ex) {
          Notification.show("Couldn't add: " + ex.getMessage());
        }
      });
      card.add(new HorizontalLayout(vendorPick, roleLabel, add));
      teamsDiv.add(card);
    }
  }

  record TeamMemberRow(String vendorName, String role) {}
}
