package com.github.jochenw.crond.ui.vaadin.model;

import java.time.ZonedDateTime;

import com.github.jochenw.crond.backend.model.beans.Execution.Id;

public class Execution {
	private final com.github.jochenw.crond.backend.model.beans.Execution execution;

	public Execution(com.github.jochenw.crond.backend.model.beans.Execution pExecution) {
		execution = pExecution;
	}

	public Id getId() { return execution.getId(); }
	public com.github.jochenw.crond.backend.model.beans.Job.Id getJobId() {	return execution.getJobId(); }
	public ZonedDateTime getStartTime() { return execution.getStartTime(); }
	public ZonedDateTime getEndTime() {	return execution.getEndTime(); }
}
