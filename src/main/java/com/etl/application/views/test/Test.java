package com.etl.application.views.test;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.PermitAll;

@PermitAll
@PageTitle("Test")
@Route("")
@Menu(order = 0, icon = "line-awesome/svg/user.svg")

public class Test extends Composite<VerticalLayout> {

    public Test() {
        //do dodania
    }
}
