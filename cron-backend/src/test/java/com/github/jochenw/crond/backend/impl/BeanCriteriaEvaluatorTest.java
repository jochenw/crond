package com.github.jochenw.crond.backend.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;

import com.github.jochenw.crond.backend.model.Criteria;
import com.github.jochenw.crond.backend.model.Criteria.Operation;
import com.github.jochenw.crond.backend.model.beans.User;

public class BeanCriteriaEvaluatorTest {
	@Test
	public void testStringAttributes() {
		final List<User> users = new ArrayList<>();
		final User.Id id0 = new User.Id("0");
		final User user0 = new User(id0, "Jochen Wiedmann", "jochen@apache.org");
		users.add(user0);
		final User.Id id1 = new User.Id("1");
		final User user1 = new User(id1, "Michael Widenius", "monty@mysql.org");
		users.add(user1);
		final User.Id id2 = new User.Id("2");
		final User user2 = new User(id2, "Jim Jagielski", "jim@apache.org");
		users.add(user2);

		final Criteria.Predicate idLowerThan3Predicate = Criteria.of("idStr", Operation.LT, "3");
		final Predicate<User> idLowerThan3Matcher = BeanCriteriaEvaluator.matcher(User.class, idLowerThan3Predicate); 
		users.forEach((u) -> assertTrue(idLowerThan3Matcher.test(u)));
	}
}
