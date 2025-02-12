package com.github.jochenw.crond.core.beans;

import com.github.jochenw.crond.core.api.IModel.Job;

public class JobImpl implements Job {
	private final Long id, userId;
	private final String name;

	private JobImpl(Long pId, Long pUserId, String pName) {
		id = pId;
		userId = pUserId;
		name = pName;
	}

	@Override public Long getId() { return id; }
	@Override public Long getUserId() { return userId; }
	@Override public String getName() { return name; }

	public static JobImpl of(Long pId, Long pUserId, String pName) {
		return new JobImpl(pId, pUserId, pName);
	}

}
