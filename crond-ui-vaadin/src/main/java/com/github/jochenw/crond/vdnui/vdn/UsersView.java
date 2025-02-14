package com.github.jochenw.crond.vdnui.vdn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.core.api.IModel;
import com.github.jochenw.crond.core.api.IModel.User;
import com.github.jochenw.crond.vdnui.MainView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;

public class UsersView extends VerticalLayout {
	public static class Filter {
		private String id, name, email;
	}
	private static class UiUser {
		private final Long id;
		private String email, name;

		public UiUser(User pUser) {
			id = pUser.getId();
			email = pUser.getEmail();
			name = pUser.getName();
		}

		public Long getId() {
			return id;
		}

		public String getEmail() {
			return email;
		}

		public String getName() {
			return name;
		}
	}

	private final MainView mainView;
	private final Filter filter = new Filter();
	private final Grid<UiUser> grid = new Grid<>();
	private final IModel model;

	public UsersView(MainView pMainView) {
		mainView = pMainView;
		final IComponentFactory componentFactory = mainView.getComponentFactory();
		model = componentFactory.requireInstance(IModel.class);
		init();
	}

	protected void init() {
		grid.addColumn((u) -> u.getId()).setHeader("Name").setId("id");
		grid.addColumn((u) -> u.getName()).setHeader("Name").setId("name");
		grid.addColumn((u) -> u.getEmail()).setHeader("Email").setId("email");
		grid.setSortableColumns("id", "name", "email");
		grid.setDataProvider(newDataProvider());
	}

	protected DataProvider<UiUser,Filter> newDataProvider() {
		final FetchCallback<UiUser,Filter> fetchCallback = this::fetchUsers;
		final CountCallback<UiUser,Filter> countCallback = this::countUsers;
		return DataProvider.fromFilteringCallbacks(fetchCallback, countCallback);
	}

	protected int countUsers(Query<UiUser,Filter> pQuery) {
		final int limit = pQuery.getLimit();
		final int offset = pQuery.getOffset();
		final Predicate<User> predicate = asPredicate(pQuery);
		MutableInteger count = new MutableInteger();
		model.forEachUser((u) -> count.inc());
		return count.intValue();
	}

	protected Stream<UiUser> fetchUsers(Query<UiUser,Filter> pQuery) {
		final int limit = pQuery.getLimit();
		final int offset = pQuery.getOffset();
		final Predicate<User> predicate = asPredicate(pQuery);
		final Comparator<User> comparator = asComparator(pQuery);
		final List<User> list = new ArrayList<>();
		model.forEachUser((u) -> {
			if (predicate == null  ||  predicate.test(u)) {
				list.add(u);
			}
		});
		if (comparator != null) {
			list.sort(comparator);
		}
		final Predicate<User> limitPredicate = new Predicate<User>() {
			private int skip = offset;
			private int permit = limit;
			@Override public boolean test(User pUser) {
				if (skip > 0) {
					--skip;
					return false;
				}
				if (permit > 0) {
					--permit;
				}
				return true;
			}
		};
		return list.stream().filter(limitPredicate).map((u) -> new UiUser(u));
	}

	final Predicate<User> asPredicate(Query<UiUser,Filter> pQuery) {
		Predicate<User> predicate = null;
		final String filterIdStr = filter.id == null ? null : filter.id.trim();
		if (filterIdStr != null  &&  filterIdStr.length() > 0) {
			final Predicate<String> idPredicate = Strings.matcher(filterIdStr.replace('%', '*'));
			predicate = (u) -> idPredicate.test(u.getId().toString());
		}
		final String filterNameStr = filter.name == null ? null : filter.name.trim();
		if (filterNameStr != null  &&  filterNameStr.length() > 0) {
			final Predicate<String> namePredicate = Strings.matcher(filterNameStr.replace('%', '?'));
			final Predicate<User> userNamePredicate = (u) -> namePredicate.test(u.getName());
			if (predicate == null) {
				predicate = userNamePredicate;
			} else {
				predicate = predicate.and(userNamePredicate);
			}
		}
		final String filterEmailStr = filter.email == null ? null : filter.email.trim();
		if (filterEmailStr != null  &&  filterEmailStr.length() > 0) {
			final Predicate<String> emailPredicate = Strings.matcher(filterEmailStr.replace('%', '*'));
			final Predicate<User> userEmailPredicate = (u) -> emailPredicate.test(u.getEmail());
			if (predicate == null) {
				predicate = userEmailPredicate;
			} else {
				predicate = predicate.and(userEmailPredicate);
			}
		}
		return predicate;
	}

	final Comparator<User> asComparator(Query<UiUser,Filter> pQuery) {
		final List<QuerySortOrder> list = pQuery.getSortOrders();
		if (list == null  ||  list.isEmpty()) {
			return null;
		}
		final List<Comparator<User>> comparators = new ArrayList<>();
		list.forEach((qso) -> {
			final Comparator<User> comparator;
			switch (qso.getSorted()) {
			case "id": comparator = (u1,u2) -> Long.compare(u1.getId(), u2.getId()); break;
			case "name": comparator = (u1,u2) -> compareStrings(u1.getName(), u2.getName()); break;
			case "email": comparator = (u1,u2) ->  compareStrings(u1.getEmail(), u2.getEmail()); break;
			default: throw new IllegalStateException("Invalid filter property: " + qso.getSorted());
			}
			switch (qso.getDirection()) {
			case ASCENDING: comparators.add(comparator); break;
			case DESCENDING: comparators.add(comparator.reversed()); break;
			default: throw new IllegalStateException("Invalid filter direction: " + qso.getDirection());
			}
		});
		if (comparators.size() == 1) {
			return comparators.get(0);
		} else {
			return (u1,u2) -> {
				for (int i = 0;  i < comparators.size();  i++) {
					final int result = comparators.get(i).compare(u1, u2);
					if (result != 0) {
						return result;
					}
				}
				return 0;
			};
		}
	}

	protected int compareStrings(String pValue1, String pValue2) {
		if (pValue1 == null) {
			if (pValue2 == null) {
				return 0;
			} else {
				return -1;
			}
		} else {
			if (pValue2 == null) {
				return 1;
			} else {
				return pValue1.compareToIgnoreCase(pValue2);
			}
		}
	}
}
