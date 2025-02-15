package com.github.jochenw.crond.vdnui.vdn;

import java.util.function.Predicate;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.textfield.TextField;

public class Filters {
	public static class FilterStatusBuilder {
		private StringBuilder sb;
		public FilterStatusBuilder attribute(String pName, String pValue) {
			if (pValue != null  &&  pValue.trim().length() > 0) {
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append(" AND ");
				}
				sb.append(pName);
				if (pValue.indexOf('*') >= 0
						||  pValue.indexOf('%') >= 0
						||  pValue.indexOf('?') >= 0) {
					sb.append(" LIKE '");
				} else {
					sb.append("='");
				}
				sb.append(pValue);
				sb.append('\'');
			}
			return this;
		}
		public FilterStatusBuilder attribute(String pName, TextField pField) {
			return attribute(pName, pField.getValue());
		}
		public String build(String pDefaultValue) {
			if (sb == null) {
				return pDefaultValue;
			} else {
				return sb.toString();
			}
		}
	}

	public static FilterStatusBuilder filterStatus() {
		return new FilterStatusBuilder();
	}

	public static <O> Predicate<O> limit(int pOffset, int pLimit) {
		final int pOffset1 = pOffset;
		final int pLimit1 = pLimit;
		return new Predicate<O>() {
			int offset = pOffset1;
			int limit = pLimit1;
			@Override
			public boolean test(O t) {
				if (offset > 0) {
					--offset;
					return false;
				}
				if (limit > 0) {
					return limit-- > 0;
				}
				return limit == -1;
			}
		};
	}
}
