package com.github.jochenw.crond.vdnui.vdn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.jochenw.afw.core.util.MutableBoolean;
import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.core.api.IModel;
import com.github.jochenw.crond.core.api.IModel.User;
import com.github.jochenw.crond.core.beans.UserImpl;
import com.github.jochenw.crond.vdnui.MainView;
import com.github.jochenw.crond.vdnui.vdn.Filters.ComparatorBuilder;
import com.github.jochenw.crond.vdnui.vdn.Filters.PredicateBuilder;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;

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
		final User user = UserImpl.of(null, null, null);
		editUser(user);
	}

	public static class EditableUser implements User {
		private final Long id;
		public EditableUser(Long pId) { id = pId; }
		private String name, email;
		@Override
		public Long getId() { return id; }
		@Override
		public String getEmail() { return email; }
		@Override
		public String getName() { return name; }
		public void setName(String pName) { name = pName; }
		public void setEmail(String pEmail) { email = pEmail; }
	}

	protected void editUser(User pUser) {
		final Button saveButton = new Button("Save");
		final Consumer<Binder<EditableUser>> saveButtonEnabler = (b) -> {
			saveButton.setEnabled(b.isValid());
		};
		final Long id = (pUser == null ||  pUser.getId() == null) ? null : pUser.getId();
		final EditableUser eUser = new EditableUser(id);
		if (pUser != null) {
			eUser.setEmail(pUser.getEmail());
			eUser.setName(pUser.getName());
		}
		final FormLayout fl = new FormLayout();
		final Binder<EditableUser> binder = new Binder<>();
		final TextField nameField = new TextField();
		binder.forField(nameField)
		    .withValidator((s) -> {
		    	final boolean b = !Strings.isEmpty(s);
		    	saveButtonEnabler.accept(binder);
		    	return b;
		    }, "Name must not be empty")
		    .bind(EditableUser::getName, EditableUser::setName);
		fl.addFormItem(nameField, "Name");
		final TextField emailField = new TextField();
		binder.forField(emailField)
			.withValidator((e) -> {
				final User existingEmailUser = model.getUserByEmail(e);
				final boolean b = existingEmailUser == null
						||  existingEmailUser.getId() == id;
		    	saveButtonEnabler.accept(binder);
				return b;
			}, "Email address isn't unique")
			.bind(EditableUser::getEmail, EditableUser::setEmail);
		fl.addFormItem(emailField, "Email");
    	saveButton.setEnabled(binder.isValid());
    	saveButton.addClickListener((e) -> {
    		if (id == null) {
    			model.addUser(eUser.getName(), eUser.getEmail());
    		} else {
    			model.updateUser(UserImpl.of(id, eUser.getEmail(), eUser.getName()));
    		}
    	});
    	fl.add(saveButton);
		
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
	    final PredicateBuilder<User> pb =  Filters.predicate();
	    return pb.add((User u) -> u.getId().toString(), filter.id)
	    		 .add(User::getName, filter.name)
	    		 .add(User::getEmail, filter.email)
	    		 .build();
	}

	final Comparator<User> asComparator(Query<UiUser,Filter> pQuery) {
		final List<QuerySortOrder> list = pQuery.getSortOrders();
		if (list == null  ||  list.isEmpty()) {
			return null;
		}
		final ComparatorBuilder<User> cb = Filters.comparator();
		
		list.forEach((qso) -> {
			final Function<User,String> getter;
			switch (qso.getSorted()) {
			case "id": {
					getter = (u) -> u.getId().toString();
				}
				break;
			case "name": {
					getter = User::getName;
				}
				break;
			case "email": {
				    getter = User::getEmail;
			    }
			    break;
			default: throw new IllegalStateException("Invalid filter property: " + qso.getSorted());
			}
			cb.add(getter, qso.getDirection() == SortDirection.ASCENDING);
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
