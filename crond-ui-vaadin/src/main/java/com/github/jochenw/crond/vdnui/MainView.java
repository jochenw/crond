package com.github.jochenw.crond.vdnui;

import org.apache.commons.lang3.NotImplementedException;

import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.vdnui.vdn.JobsView;
import com.github.jochenw.crond.vdnui.vdn.UsersView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;

@Route
public class MainView extends VerticalLayout {
	private static final long serialVersionUID = -5897297878136878485L;
	private final IComponentFactory componentFactory; 

    public MainView() {
    	componentFactory = findComponentFactory();
    	final TabSheet tabSheet = new TabSheet();
    	add(tabSheet);
    	if (isCurrentUserAdministrator()) {
    		tabSheet.add("Users", newUsersComponent());
    	}
    	tabSheet.add("Jobs", newJobsComponent());
    }
   
    protected boolean isCurrentUserAdministrator() { return true; }
   
    protected Component newUsersComponent() {
    	return new UsersView(this);
    }
   
    protected Component newJobsComponent() {
    	return new JobsView(this);
    }

    public IComponentFactory getComponentFactory() {
    	return componentFactory;
    }

    protected IComponentFactory findComponentFactory() {
    	throw new NotImplementedException();
    }
}
