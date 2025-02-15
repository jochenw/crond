package com.github.jochenw.crond.vdnui.vdn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.afw.core.vdn.Filters.ComparatorBuilder;
import com.github.jochenw.afw.core.vdn.Filters.PredicateBuilder;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.core.api.IModel;
import com.github.jochenw.crond.core.api.IModel.User;
import com.github.jochenw.crond.vdnui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;

public class UsersView extends VerticalLayout {
	private static final long serialVersionUID = 7296277703803132058L;

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
		final TextField idFilterField = new TextField();
		final TextField nameFilterField = new TextField();
		final TextField emailFilterField = new TextField();
		final TextField statusLabel = new TextField();
		statusLabel.setReadOnly(true);
		statusLabel.setAriaLabel("Filter status");
		final Runnable statusUpdater = () -> {
			final String statusText = Filters.filterStatus()
					.attribute("Id", idFilterField)
					.attribute("Name", nameFilterField)
					.attribute("Email", emailFilterField)
					.build("None (Show all users)");
			statusLabel.setValue(statusText);
			grid.getDataProvider().refreshAll();
		};
		idFilterField.setId("idFilter");
		idFilterField.setAriaLabel("Id");
		idFilterField.addValueChangeListener((e) -> {
			filter.id = e.getValue();
			statusUpdater.run();
		});
		nameFilterField.addValueChangeListener((e) -> {
			filter.name = e.getValue();
			statusUpdater.run();
		});
		emailFilterField.addValueChangeListener((e) -> {
			filter.email = e.getValue();
			statusUpdater.run();
		});
		final HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.add(idFilterField, nameFilterField, emailFilterField);
		add(filterLayout);
		final HorizontalLayout statusLayout = new HorizontalLayout();
		statusLabel.setWidthFull();
		final Button newUserButton = new Button("New User");
		statusLayout.add(statusLabel, newUserButton);
		statusLabel.setWidthFull();
		newUserButton.setWidth(null);
		statusLayout.setAlignSelf(Alignment.START, statusLabel);
		statusLayout.setAlignSelf(Alignment.END, newUserButton);
		newUserButton.addClickListener((e) -> newUser());
		add(statusLayout);
		grid.addColumn((u) -> u.getId()).setSortable(true).setHeader("Id").setKey("id");
		grid.addColumn((u) -> u.getName()).setSortable(true).setHeader("Name").setKey("name");
		grid.addColumn((u) -> u.getEmail()).setSortable(true).setHeader("Email").setKey("email");
		grid.setDataProvider(newDataProvider());
		add(grid);
		statusUpdater.run();
	}

	protected void newUser() {
		final Dialog dialog = new Dialog();
		dialog.setModal(true);
		final Button okButton = new Button("Okay");
		final VerticalLayout vl = new VerticalLayout();
		final NativeLabel label = new NativeLabel("Creating new user");
		vl.add(label);
		vl.add(okButton);
		okButton.addClickListener((e) -> dialog.close());
		dialog.add(vl);
		dialog.open();
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
		final PredicateBuilder<User> pb = com.github.jochenw.afw.core.vdn.Filters.predicate(User.class);
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
		final ComparatorBuilder<User> cb = com.github.jochenw.afw.core.vdn.Filters.comparator();
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
