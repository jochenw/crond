package com.github.jochenw.crond.backend.model.beans;

import java.time.ZonedDateTime;

public class Execution extends Bean<Execution.Id> {
	private static final long serialVersionUID = -8636507623665329444L;

	public static class Id extends Bean.Id {
		private static final long serialVersionUID = -1478710420686813351L;

		public Id(String pId) {
			super(pId);
		}
	}

	private Job.Id jobId;
	private ZonedDateTime startTime, endTime;
	
	public Execution(Id pId, Job.Id pJobId, ZonedDateTime pStartTime, ZonedDateTime pEndTime) {
		super(pId);
		jobId = pJobId;
		startTime = pStartTime;
		endTime = pEndTime;
	}

	public Job.Id getJobId() { return jobId; }
	public void setJobId(Job.Id pJobId) { jobId = pJobId; }
	public ZonedDateTime getStartTime() { return startTime; }
	public void setStartTime(ZonedDateTime pStartTime) { startTime = pStartTime; }
	public ZonedDateTime getEndTime() { return endTime; }
	public void setEndTime(ZonedDateTime pEndTime) { endTime = pEndTime; }
}
