package com.github.jochenw.crond.backend.model.beans;

import java.io.Serializable;

public class Bean<I extends Bean.Id> implements Serializable {
	public static class Id implements Serializable {
		private static final long serialVersionUID = -2496391429556287464L;
		private final String id;
		public Id(String pId) {
			id = pId;
		}
		public String getId() { return id; }
	}
	private static final long serialVersionUID = -202745044892079742L;
	private final I id;

	public Bean(I pId) {
		id = pId;
	}
	public I getId() { return id; }
}
