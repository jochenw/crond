package com.github.jochenw.crond.ui.vaadin.model;

import com.github.jochenw.crond.backend.model.beans.User.Id;

public class User {
	private final com.github.jochenw.crond.backend.model.beans.User user;

	public User(com.github.jochenw.crond.backend.model.beans.User pUser) {
		user = pUser;
	}

	public String getName() { return user.getName(); }
	public void setName(String pName) {	user.setName(pName); }
	public String getEmail() { return user.getEmail(); }
	public void setEmail(String pEmail) { user.setEmail(pEmail); }
	public Id getId() {	return user.getId(); }
}
