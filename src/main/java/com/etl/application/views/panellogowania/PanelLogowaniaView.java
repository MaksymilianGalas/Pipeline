package com.etl.application.views.panellogowania;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("Panel Logowania")
@Route("")
@Menu(order = 0, icon = "line-awesome/svg/user.svg")
@AnonymousAllowed
public class PanelLogowaniaView extends Composite<VerticalLayout> {

    public PanelLogowaniaView() {
        Hr hr = new Hr();
        LoginForm loginForm = new LoginForm();
        Hr hr2 = new Hr();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().setJustifyContentMode(JustifyContentMode.START);
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setAlignSelf(FlexComponent.Alignment.CENTER, loginForm);
        loginForm.setWidth("100%");
        loginForm.setMinWidth("240px");
        loginForm.setMaxWidth("350px");
        getContent().add(hr);
        getContent().add(loginForm);
        getContent().add(hr2);
    }
}
