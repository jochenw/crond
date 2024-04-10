package com.github.jochenw.crond.ui.vaadin.vdn;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.backend.model.Criteria;
import com.github.jochenw.crond.backend.model.Criteria.Operation;
import com.github.jochenw.crond.backend.model.IModel;
import com.github.jochenw.crond.ui.vaadin.model.Execution;
import com.github.jochenw.crond.ui.vaadin.model.Job;
import com.github.jochenw.crond.ui.vaadin.model.User;
import com.github.jochenw.crond.ui.vaadin.srvlt.CronSrvInitializer;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class CronSrvUi extends UI {
	private static final long serialVersionUID = -4808808490650644076L;

	private IComponentFactory cf;
	private IModel model;
	private TabSheet tabSheet;
	private Grid<Job> jobsTable, jobsAdminTable;
	private Grid<Execution> executionsTable;
	private Grid<User> usersAdminTable;
	private ILog log;

	@Override
	protected void init(VaadinRequest pRequest) {
		cf = CronSrvInitializer.getComponentFactory(VaadinServlet.getCurrent().getServletContext());
		model = cf.requireInstance(IModel.class);
		log = cf.requireInstance(ILogFactory.class).getLog(getClass());
		log.entering("init");
		setContent(newContent());
		log.exiting("init");
	}


	protected Component newContent() {
		final VerticalLayout vl = new VerticalLayout();
		vl.addComponent(new Label("<h1>The Cron server</h1>", ContentMode.HTML));
		tabSheet = new TabSheet();
		tabSheet.addTab(newJobsTab()).setCaption("Jobs");
		tabSheet.addTab(newExecutionsTab()).setCaption("Executions");
		tabSheet.addTab(newAdminTab()).setCaption("Administration");
		vl.addComponent(tabSheet);
		return vl;
	}

	protected Component newJobsTab() {
		final VerticalLayout vl = new VerticalLayout();
		final List<Job> list = new ArrayList<>();
		jobsTable = new Grid<>();
		jobsTable.addColumn((j) -> j.getId().getId()).setCaption("Id");
		jobsTable.addColumn((j) -> j.getName()).setCaption("Name");
		jobsTable.addColumn((j) -> j.getOwner()).setCaption("Owner");
		jobsTable.setItems(list);
		vl.addComponent(jobsTable);
		return vl;
	}

	protected Component newExecutionsTab() {
		final VerticalLayout vl = new VerticalLayout();
		final List<Execution> list = new ArrayList<>();
		executionsTable = new Grid<>();
		executionsTable.addColumn((e) -> e.getId().getId()).setCaption("Id");
		executionsTable.addColumn((e) -> DateTimeFormatter.ISO_DATE_TIME.format(e.getStartTime())).setCaption("Start Time");
		executionsTable.addColumn((e) -> DateTimeFormatter.ISO_DATE_TIME.format(e.getEndTime())).setCaption("End Time");
		executionsTable.setItems(list);
		vl.addComponent(executionsTable);
		return vl;
	}

	protected Component newAdminTab() {
		final VerticalLayout vl = new VerticalLayout();
		final TabSheet tabSheet = new TabSheet();
		tabSheet.addTab(newUsersAdminTab()).setCaption("Users");
		tabSheet.addTab(newJobsAdminTab()).setCaption("Jobs");
		vl.addComponent(tabSheet);
		return vl;
	}

	protected Component newUsersAdminTab() {
		final VerticalLayout vl = new VerticalLayout();
		final HorizontalLayout hl = new HorizontalLayout();
		final Button button = new Button();
		button.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 5858913290608313960L;

			@Override
			public void buttonClick(ClickEvent event) {
				newUser();
			}
		});
		hl.addComponent(button);
		final List<User> list = new ArrayList<>();
		usersAdminTable = new Grid<>();
		usersAdminTable.addColumn((u) -> u.getId().getId()).setCaption("Id");
		usersAdminTable.addColumn((u) -> u.getName()).setCaption("Name");
		usersAdminTable.addColumn((u) -> u.getEmail()).setCaption("Email");
		usersAdminTable.setItems(list);
		hl.addComponent(usersAdminTable);
		vl.addComponent(hl);
		return vl;
	}

	protected Component newJobsAdminTab() {
		final VerticalLayout vl = new VerticalLayout();
		final List<Job> list = new ArrayList<>();
		jobsAdminTable = new Grid<>();
		jobsAdminTable.addColumn((j) -> j.getId().getId()).setCaption("Id");
		jobsAdminTable.addColumn((j) -> j.getName()).setCaption("Name");
		jobsAdminTable.addColumn((j) -> j.getOwner()).setCaption("Owner");
		jobsAdminTable.setItems(list);
		vl.addComponent(jobsAdminTable);
		return vl;
	}

	protected void newUser() {
		final Window window = new Window("Create User");
		final VerticalLayout vl = new VerticalLayout();
		final FormLayout fl = new FormLayout();
		final TextField tfName = new TextField("Name");
		tfName.setIcon(VaadinIcons.USER);
		tfName.setRequiredIndicatorVisible(true);
		fl.addComponent(tfName);
		final TextField tfEmail = new TextField("Email");
		tfEmail.setIcon(VaadinIcons.ENVELOPE);
		tfEmail.setRequiredIndicatorVisible(true);
		fl.addComponent(tfEmail);
		vl.addComponent(fl);
		final HorizontalLayout hl = new HorizontalLayout();
		final ClickListener validator = new ClickListener() {
			private static final long serialVersionUID = -5099365285514988666L;

			@Override
			public void buttonClick(ClickEvent event) {
				final String name = tfName.getValue();
				if (name == null  ||  name.length() == 0) {
					tfName.setComponentError(new UserError("Name is required"));
				}
				final String email = tfEmail.getValue();
				if (email == null  ||  email.length() == 0) {
					tfEmail.setComponentError(new UserError("Name is required"));
				}
				final com.github.jochenw.crond.backend.model.beans.User existingUser = model.findUser(Criteria.of("email", Operation.EQ, tfEmail.getValue()));
				if (existingUser != null) {
					tfEmail.setComponentError(new UserError("Email address already in use."));
				}
			}
		};
		final Button createButton = new Button("Create");
		createButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 214608892834931103L;

			@Override
			public void buttonClick(ClickEvent pEvent) {
				validator.buttonClick(pEvent);
				if (tfName.getComponentError() == null  &&  tfEmail.getComponentError() == null) {
					model.createUser(new com.github.jochenw.crond.backend.model.beans.User(null, tfName.getValue(), tfEmail.getValue()));
					window.close();
				}
			}
		});
		final Button cancelButton = new Button("Cancel");
		cancelButton.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent pEvent) {
				window.close();
			}
		});
		hl.addComponent(createButton);
		hl.addComponent(cancelButton);
		vl.addComponent(hl);
		window.setContent(vl);
		UI.getCurrent().addWindow(window);
	}
}
