package com.github.jochenw.crond.ui.vaadin.model;

import com.github.jochenw.afw.core.util.Objects;

public class Job {
	private final com.github.jochenw.crond.backend.model.beans.Job job;

	public Job(com.github.jochenw.crond.backend.model.beans.Job pJob) {
		this.job = Objects.requireNonNull(pJob, "Job");
	}

	public com.github.jochenw.crond.backend.model.beans.Job.Id getId() { return job.getId(); }
	public String getName() { return job.getName(); }
	public String getOwner() { return job.getOwnerId().getId(); }
}
