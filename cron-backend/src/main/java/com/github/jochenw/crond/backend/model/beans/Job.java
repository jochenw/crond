package com.github.jochenw.crond.backend.model.beans;

public class Job extends Bean<Job.Id> {
	public static class Id extends Bean.Id {
		private static final long serialVersionUID = 3375876823799195997L;

		public Id(String pId) {
			super(pId);
		}
	}
	private static final long serialVersionUID = -7823432901120412809L;
	private String name;
	private User.Id ownerId;

	public Job(Id pId, String pName, User.Id pOwnerId) {
		super(pId);
		name = pName;
		ownerId = pOwnerId;
	}

	public String getName() { return name; }
	public void setName(String pName) { name = pName; }
	public User.Id getOwnerId() { return ownerId; }
	public void setOwnerId(User.Id pOwnerId) { ownerId = pOwnerId; }
}
