package com.github.jochenw.crond.backend.model.beans;

import java.io.Serializable;
import java.util.Objects;

public class Bean<I extends Bean.Id> implements Serializable {
	public static class Id implements Serializable {
		private static final long serialVersionUID = -2496391429556287464L;
		private final String id;
		public Id(String pId) {
			id = pId;
		}
		public String getId() { return id; }
		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Id other = (Id) obj;
			return Objects.equals(id, other.id);
		}
		@Override
		public String toString() {
			return getId();
		}
	}
	private static final long serialVersionUID = -202745044892079742L;
	private final I id;

	public Bean(I pId) {
		id = pId;
	}
	public I getId() { return id; }
}
