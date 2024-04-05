package com.github.jochenw.crond.backend.model.beans;

public class Job extends Bean<Job.Id> {
	public static class Id extends Bean.Id {
		private static final long serialVersionUID = 3375876823799195997L;

		public Id(String pId) {
			super(pId);
		}
	}
	private static final long serialVersionUID = -7823432901120412809L;
	private String name, owner;

	public Job(Id pId, String pName, String pOwner) {
		super(pId);
		name = pName;
		owner = pOwner;
	}

	public String getName() { return name; }
	public void setName(String pName) { name = pName; }
	public String getOwner() { return owner; }
	public void setOwner(String pOwner) { owner = pOwner; }
}
