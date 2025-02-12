package com.github.jochenw.crond.core.beans;

import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.crond.core.api.IModel.User;

public class UserImpl implements User {
	private final Long id;
	private final String email, name;

	private UserImpl(Long pUserId, String pEmail, String pName) {
		id = Objects.requireNonNull(pUserId);
		email = Objects.requireNonNull(pEmail, "Email");
		name = pName;
	}

	public static UserImpl of(Long pUserId, String pEmail, String pName) {
		return new UserImpl(pUserId, pEmail, pName);
	}

	@Override
	public String getEmail() { return email; }
	@Override
	public Long getId() { return id; }
	@Override
	public String getName() { return name; }
}
