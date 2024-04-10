package com.github.jochenw.crond.backend.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;

import com.github.jochenw.afw.core.function.Functions;
import com.github.jochenw.afw.core.function.Functions.TriConsumer;
import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.crond.backend.model.Criteria;
import com.github.jochenw.crond.backend.model.Criteria.Operation;
import com.github.jochenw.crond.backend.model.beans.User;

public class BeanCriteriaEvaluatorTest {
	@Test
	public void testAtomicStringAttribute() {
		final List<User> users = getUserList();

		final Criteria.Predicate idLowerThan3Predicate = Criteria.of("idStr", Operation.LT, "3");
		final Predicate<User> matcher = BeanCriteriaEvaluator.matcher(User.class, idLowerThan3Predicate);
		final Predicate<User> idLowerThan3Matcher = (u) -> {
			System.out.print("id: " + u.getId().getId());
			final boolean result = matcher.test(u);
			System.out.println(" -> " + result);
			return result;
		};
		users.forEach((u) -> assertTrue(idLowerThan3Matcher.test(u)));

		final Criteria.Predicate jochenWiedmannPredicate = Criteria.of("name", Operation.EQ, "Jochen Wiedmann");
		final Predicate<User> matcher2 = BeanCriteriaEvaluator.matcher(User.class, jochenWiedmannPredicate);
		users.forEach((u) -> {
			assertEquals("0".equals(u.getIdStr()), matcher2.test(u));
		});
	}

	private static class StringBean {
		private String value;

		public String getValue() { return value; }
		public void setValue(String pValue) { value = pValue; }
	}
	@Test
	public void testStringOperations() {
		final TriConsumer<String,Operation,Boolean> tester = (s,o,b) -> {
			final Criteria.Predicate cPredicate = Criteria.of("value", o, "5");
			final Predicate<StringBean> predicate = BeanCriteriaEvaluator.matcher(StringBean.class, cPredicate);
			final StringBean sb = new StringBean();
			sb.setValue(s);
			assertEquals(b.booleanValue(), predicate.test(sb));
		};
		tester.accept("5", Operation.EQ, true);
		tester.accept("6", Operation.EQ, false);
		tester.accept("6", Operation.NE, true);
		tester.accept("5", Operation.NE, false);
		tester.accept("4", Operation.LT, true);
		tester.accept("5", Operation.LT, false);
		tester.accept("4", Operation.LE, true);
		tester.accept("5", Operation.LE, true);
		tester.accept("6", Operation.LE, false);
		tester.accept("6", Operation.GT, true);
		tester.accept("5", Operation.GT, false);
		tester.accept("6", Operation.GE, true);
		tester.accept("5", Operation.GE, true);
		tester.accept("4", Operation.GE, false);
		tester.accept(null, Operation.IS, true);
		tester.accept("", Operation.IS, false);
		tester.accept(null, Operation.ISNOT, false);
		tester.accept("", Operation.ISNOT, true);
	}

	private List<User> getUserList() {
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
		return users;
	}

    @Test
    public void testNotPredicate() {
    	final List<User> users = getUserList();
		final Criteria.Predicate idNotLowerThan3Predicate = Criteria.not(Criteria.of("idStr", Operation.LT, "3"));
		final Predicate<User> notMatcher = BeanCriteriaEvaluator.matcher(User.class, idNotLowerThan3Predicate);
		users.forEach((u) -> assertFalse(notMatcher.test(u)));
	}

    @Test
    public void testAndPredicate() {
    	final List<User> users = getUserList();
		final Criteria.Predicate idLowerThan3Predicate = Criteria.of("idStr", Operation.LT, "3");
		final Criteria.Predicate jochenWiedmannPredicate = Criteria.of("name", Operation.EQ, "Jochen Wiedmann");
		final Criteria.Predicate andPredicate = Criteria.and(idLowerThan3Predicate, jochenWiedmannPredicate);
		final Predicate<User> andMatcher = BeanCriteriaEvaluator.matcher(User.class, andPredicate);
		users.forEach((u) -> {
			assertEquals("0".equals(u.getIdStr()), andMatcher.test(u));
		});
    }

    @Test
    public void testOrPredicate() {
    	final List<User> users = getUserList();
		final Criteria.Predicate idLowerThan3Predicate = Criteria.of("idStr", Operation.LT, "3");
		final Criteria.Predicate jochenWiedmannPredicate = Criteria.of("name", Operation.EQ, "Jochen Wiedmann");
		final Criteria.Predicate orPredicate = Criteria.or(idLowerThan3Predicate, jochenWiedmannPredicate);
		final Predicate<User> orMatcher = BeanCriteriaEvaluator.matcher(User.class, orPredicate);
		users.forEach((u) -> {
			assertTrue(orMatcher.test(u));
		});

		final Criteria.Predicate idNotLowerThan3Predicate = Criteria.not(Criteria.of("idStr", Operation.LT, "3"));
		final Criteria.Predicate orPredicate2 = Criteria.or(jochenWiedmannPredicate, idNotLowerThan3Predicate);
		final Predicate<User> orMatcher2 = BeanCriteriaEvaluator.matcher(User.class, orPredicate2);
		users.forEach((u) -> {
			assertEquals("0".equals(u.getIdStr()), orMatcher2.test(u));
		});
		
    }

    private static class BooleanMethods {
    	private final boolean attribute;
    	private Throwable error;

    	public BooleanMethods(boolean pAttribute) { attribute = pAttribute; }
    	public boolean isAttribute() { return attribute; }
    	public boolean hasAttribute2() { return attribute; }
    	private void setError(Throwable pError) { error = pError; }
    	public String getError() throws Throwable {
    		if (error == null) {
    			return null;
    		} else {
    			throw error;
    		}
    	}
    }
    @Test
    public void testCoverage() {
    	// Some details, that are for 100% coverage only.
    	Functions.assertFail(IllegalStateException.class, null, () -> BeanCriteriaEvaluator.matcher(User.class, new Criteria.Predicate() {}));
    	final BeanCriteriaEvaluator bce = new BeanCriteriaEvaluator();
   		assertNotNull(bce.findGetter(BooleanMethods.class, "attribute"));
   		assertNotNull(bce.findGetter(BooleanMethods.class, "attribute2"));
   		final Criteria.Predicate cPredicate = Criteria.of("error", Operation.IS, null);
   		final Predicate<BooleanMethods> predicate = bce.asMatcher(BooleanMethods.class, cPredicate);
   		final Criteria.Predicate cNotPredicate = Criteria.of("error", Operation.ISNOT, null);
   		final Predicate<BooleanMethods> notPredicate = bce.asMatcher(BooleanMethods.class, cNotPredicate);
   		final BooleanMethods bm = new BooleanMethods(false);
   		assertTrue(predicate.test(bm));
   		assertFalse(notPredicate.test(bm));
   		final RuntimeException rte = new RuntimeException();
   		bm.setError(rte);
   		try {
   			predicate.test(bm);
   			fail("Expected Exception");
   		} catch (RuntimeException e) {
   			assertSame(rte, e);
   		}
   		final OutOfMemoryError oome = new OutOfMemoryError();
   		bm.setError(oome);
   		try {
   			predicate.test(bm);
   			fail("Expected Exception");
   		} catch (OutOfMemoryError e) {
   			assertSame(oome, e);
   		}
   		final IOException ioe = new IOException();
   		bm.setError(ioe);
   		try {
   			predicate.test(bm);
   			fail("Expected Exception");
   		} catch (UncheckedIOException e) {
   			assertSame(ioe, e.getCause());
   		}
   		final SQLException se = new SQLException();
   		bm.setError(se);
   		try {
   			predicate.test(bm);
   			fail("Expected Exception");
   		} catch (UndeclaredThrowableException e) {
   			assertSame(se, e.getCause());
   		}
   		try {
   			bce.findGetter(User.class, "notAvailable");
   			fail("Expected Exception");
   		} catch (UndeclaredThrowableException e) {
   			NoSuchMethodException nsme = (NoSuchMethodException) e.getCause();
   			assertEquals(User.class.getName() + ".getNotAvailable()", nsme.getMessage());
   		}
    }
}
