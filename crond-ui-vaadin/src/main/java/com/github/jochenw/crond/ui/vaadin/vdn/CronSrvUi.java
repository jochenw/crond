package com.github.jochenw.crond.ui.vaadin.vdn;

import java.util.ArrayList;
import java.util.List;

import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.ui.vaadin.srvlt.CronSrvInitializer;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class CronSrvUi extends UI {
	private static final long serialVersionUID = -4808808490650644076L;

	private IComponentFactory cf;
	private TabSheet tabSheet;
	private Grid<Job> jobsTable;
	private ILog log;

	@Override
	protected void init(VaadinRequest pRequest) {
		cf = CronSrvInitializer.getComponentFactory(VaadinServlet.getCurrent().getServletContext());
		log.entering("init");
		log = cf.requireInstance(ILogFactory.class).getLog(getClass());
		setContent(newContent());
		log.exiting("init");
	}


	protected Component newContent() {
		final VerticalLayout vl = new VerticalLayout();
		vl.addComponent(new Label("<h1>The Cron server</h1>", ContentMode.HTML));
		tabSheet = new TabSheet();
		tabSheet.addTab(newJobsTab());
		tabSheet.addTab(newExecutionsTab());
		vl.addComponent(tabSheet);
		return vl;
	}

	public static class Job {
		private final String name, owner;

		public Job(String pName, String pOwner) {
			super();
			name = pName;
			owner = pOwner;
		}
		public String getName() { return name; }
		public String getOwner() { return owner; }
	}

	protected Component newJobsTab() {
		final VerticalLayout vl = new VerticalLayout();
		final List<Job> list = new ArrayList<>();
		jobsTable = new Grid<>();
		jobsTable.addColumn((j) -> j.getName()).setCaption("Name");
		jobsTable.addColumn((j) -> j.getOwner()).setCaption("Owner");
		jobsTable.setItems(list);
		vl.addComponent(jobsTable);
		return vl;
	}

	protected Component newExecutionsTab() {
		final VerticalLayout vl = new VerticalLayout();
		final List<Job> list = new ArrayList<>();
		jobsTable = new Grid<>();
		jobsTable.addColumn((j) -> j.getName()).setCaption("Name");
		jobsTable.addColumn((j) -> j.getOwner()).setCaption("Owner");
		jobsTable.setItems(list);
		vl.addComponent(jobsTable);
		return vl;
	}
}
