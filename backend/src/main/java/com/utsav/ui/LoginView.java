package com.utsav.ui;

import com.utsav.common.Role;
import com.utsav.user.AuthController;
import com.utsav.user.AuthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Sign in / register. */
@Route(value = "login", layout = MainLayout.class)
@PageTitle("Utsav — Sign in")
public class LoginView extends VerticalLayout {

  public LoginView(AuthService authService, SessionContext session) {
    addClassName("utsav-view");
    addClassName("utsav-narrow");
    setSpacing(true);

    H2 heading = new H2("Welcome to Utsav");
    heading.addClassName("utsav-h2");

    // Login form
    EmailField email = new EmailField("Email");
    email.setWidthFull();
    PasswordField password = new PasswordField("Password");
    password.setWidthFull();
    Button login = new Button("Sign in", e -> {
      try {
        var tokens = authService.login(email.getValue(), password.getValue());
        session.login(tokens.accessToken(), tokens.refreshToken());
        Notification.show("Welcome back!");
        UI.getCurrent().navigate("");
      } catch (Exception ex) {
        Notification.show("Sign in failed: " + ex.getMessage());
      }
    });
    login.addClassName("utsav-btn-primary");
    login.setWidthFull();

    Paragraph divider = new Paragraph("— or create an account —");
    divider.addClassName("utsav-divider");

    // Register form
    TextField name = new TextField("Display name");
    name.setWidthFull();
    EmailField regEmail = new EmailField("Email");
    regEmail.setWidthFull();
    PasswordField regPassword = new PasswordField("Password");
    regPassword.setWidthFull();
    regPassword.setHelperText("10+ chars, upper, lower, digit, symbol");
    ComboBox<Role> role = new ComboBox<>("I am a");
    role.setItems(Role.CUSTOMER, Role.VENDOR);
    role.setValue(Role.CUSTOMER);
    role.setWidthFull();

    Button register = new Button("Create account", e -> {
      try {
        authService.register(
            regEmail.getValue(), regPassword.getValue(), name.getValue(), role.getValue());
        var tokens = authService.login(regEmail.getValue(), regPassword.getValue());
        session.login(tokens.accessToken(), tokens.refreshToken());
        Notification.show("Account created — welcome!");
        UI.getCurrent().navigate("");
      } catch (Exception ex) {
        Notification.show("Registration failed: " + ex.getMessage());
      }
    });
    register.addClassName("utsav-btn-secondary");
    register.setWidthFull();

    add(heading, email, password, login, divider, name, regEmail, regPassword, role, register);
  }
}
