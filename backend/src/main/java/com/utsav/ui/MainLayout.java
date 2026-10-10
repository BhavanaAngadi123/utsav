package com.utsav.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

/** Main app layout: branded header + side navigation. */
@CssImport("./utsav.css")
public class MainLayout extends AppLayout {

  private final SessionContext session;

  public MainLayout(SessionContext session) {
    this.session = session;

    DrawerToggle toggle = new DrawerToggle();

    H1 title = new H1("Utsav");
    title.addClassName("utsav-logo");
    title.addClickListener(e -> UI.getCurrent().navigate(""));

    Span tagline = new Span("event vendors, curated");
    tagline.addClassName("utsav-tagline");

    HorizontalLayout header = new HorizontalLayout(toggle, title, tagline);
    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    header.addClassName("utsav-header");
    header.setWidthFull();

    // Right side: login/logout
    HorizontalLayout right = new HorizontalLayout();
    right.setWidthFull();
    right.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    updateAuthArea(right);
    header.add(right);
    header.setFlexGrow(1, right);

    addToNavbar(header);

    SideNav nav = new SideNav();
    nav.addItem(new SideNavItem("Discover", ""));
    nav.addItem(new SideNavItem("AI Concierge", "concierge"));
    nav.addItem(new SideNavItem("Budget Freeze", "budget"));
    nav.addItem(new SideNavItem("Team Builder", "teams"));
    nav.addItem(new SideNavItem("My Bookings", "bookings"));
    nav.addItem(new SideNavItem("Vendor Dashboard", "vendor"));
    nav.addItem(new SideNavItem("Admin", "admin"));
    addToDrawer(nav);
  }

  private void updateAuthArea(HorizontalLayout right) {
    right.removeAll();
    if (session.isLoggedIn()) {
      Span hello = new Span("Hi, " + session.getDisplayName());
      hello.addClassName("utsav-hello");
      Button logout = new Button("Sign out", e -> {
        session.logout();
        UI.getCurrent().navigate("");
        UI.getCurrent().getPage().reload();
      });
      logout.addClassName("utsav-btn-ghost");
      right.add(hello, logout);
    } else {
      RouterLink login = new RouterLink("Sign in", LoginView.class);
      login.addClassName("utsav-btn-primary");
      right.add(login);
    }
  }
}
