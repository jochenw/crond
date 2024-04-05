package com.github.jochenw.crond.backend.model.beans;

public class User extends Bean<User.Id> {
	public static class Id extends Bean.Id {
		private static final long serialVersionUID = 3375876823799195997L;

		public Id(String pId) {
			super(pId);
		}
	}

	public User(Id pId, String pName, String pEmail) {
		super(pId);
		name = pName;
		email = pEmail;
	}

	private String name, email;

	public String getName() { return name; }
	public void setName(String pName) { name = pName; }
	public String getEmail() { return email; }
	public void setEmail(String pEmail) { email = pEmail; }
}
