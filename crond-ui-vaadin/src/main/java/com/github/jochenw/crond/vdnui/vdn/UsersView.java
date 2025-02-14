package com.github.jochenw.crond.vdnui.vdn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.afw.core.vdn.Filters;
import com.github.jochenw.afw.core.vdn.Filters.ComparatorBuilder;
import com.github.jochenw.afw.core.vdn.Filters.PredicateBuilder;
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
		grid.addColumn((u) -> u.getId()).setSortable(true).setHeader("Id").setKey("id");
		grid.addColumn((u) -> u.getName()).setSortable(true).setHeader("Name").setKey("name");
		grid.addColumn((u) -> u.getEmail()).setSortable(true).setHeader("Email").setKey("email");
		grid.setDataProvider(newDataProvider());
		add(grid);
	}

	protected DataProvider<UiUser,Filter> newDataProvider() {
		final FetchCallback<UiUser,Filter> fetchCallback = this::fetchUsers;
		final CountCallback<UiUser,Filter> countCallback = this::countUsers;
		return DataProvider.fromFilteringCallbacks(fetchCallback, countCallback);
	}

	protected int countUsers(Query<UiUser,Filter> pQuery) {
		final Predicate<User> predicate = asPredicate(pQuery);
		MutableInteger count = new MutableInteger();
		model.forEachUser((u) -> {
			if (predicate.test(u)) {
				count.inc();
			}
		});
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
		final Predicate<User> limitPredicate = Filters.limit(offset, limit);
		return list.stream().filter(limitPredicate).map((u) -> new UiUser(u));
	}

	final Predicate<User> asPredicate(Query<UiUser,Filter> pQuery) {
		final PredicateBuilder<User> pb = Filters.predicate(User.class);
		final String filterIdStr = filter.id == null ? null : filter.id.trim();
		if (filterIdStr != null  &&  filterIdStr.length() > 0) {
			final Predicate<String> idPredicate = Strings.matcher(filterIdStr.replace('%', '*'));
			pb.add((u) -> u.getId().toString(), idPredicate);
		}
		final String filterNameStr = filter.name == null ? null : filter.name.trim();
		if (filterNameStr != null  &&  filterNameStr.length() > 0) {
			final Predicate<String> namePredicate = Strings.matcher(filterNameStr.replace('%', '?'));
			pb.add(User::getName, namePredicate);
		}
		final String filterEmailStr = filter.email == null ? null : filter.email.trim();
		if (filterEmailStr != null  &&  filterEmailStr.length() > 0) {
			final Predicate<String> emailPredicate = Strings.matcher(filterEmailStr.replace('%', '*'));
			pb.add(User::getEmail, emailPredicate);
		}
		return pb.build();
	}

	final Comparator<User> asComparator(Query<UiUser,Filter> pQuery) {
		final List<QuerySortOrder> list = pQuery.getSortOrders();
		if (list == null  ||  list.isEmpty()) {
			return null;
		}
		final ComparatorBuilder<User> cb = Filters.comparator();
		list.forEach((qso) -> {
			final Function<User,Object> getter;
			final Comparator<Object> comparator;
			switch (qso.getSorted()) {
			case "id": {
				    getter = (u) -> u.getId();
				    comparator = (o1,o2) -> {
				    	return ((Long) o1).compareTo((Long) o2);
				    };
			    }
				break;
			case "name": {
				    getter = User::getName;
				    comparator = (o1,o2) -> {
				    	return compareStrings((String) o1, (String) o2);
				    };
			    }
				break;
			case "email": {
				    getter = User::getEmail;
				    comparator = (o1,o2) -> {
				    	return compareStrings((String) o1, (String) o2);
				    };
			    }
			    break;
			default: throw new IllegalStateException("Invalid filter property: " + qso.getSorted());
			}
			switch (qso.getDirection()) {
			case ASCENDING: cb.add(getter, comparator);
			case DESCENDING: cb.add(getter, comparator.reversed());
			default: throw new IllegalStateException("Invalid filter direction: " + qso.getDirection());
			}
		});
		return cb.build();
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
