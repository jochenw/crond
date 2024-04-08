package com.github.jochenw.crond.backend.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.function.BiConsumer;

import org.junit.Test;

import com.github.jochenw.afw.core.function.Functions;
import com.github.jochenw.crond.backend.model.Criteria.AndPredicate;
import com.github.jochenw.crond.backend.model.Criteria.AtomicPredicate;
import com.github.jochenw.crond.backend.model.Criteria.NotPredicate;
import com.github.jochenw.crond.backend.model.Criteria.Operation;
import com.github.jochenw.crond.backend.model.Criteria.OrPredicate;
import com.github.jochenw.crond.backend.model.Criteria.Predicate;


/** Test suite for {@link Criteria}.
 */
public class CriteriaTest {
	/** Test case for {@link Criteria#of(String, String, Object)}.
	 */
	@Test
	public void testOfStringStringObject() {
		Functions.assertFail(NullPointerException.class, "Attribute", () -> Criteria.of(null, "=", null));
		Functions.assertFail(NullPointerException.class, "Operation", () -> Criteria.of("attribute", (String) null, null));
		final AtomicPredicate predicate = Criteria.of("attribute", "=", "foo");
		assertPredicate(predicate, "attribute", Operation.EQ, "foo");
		assertEquals("foo", ((AtomicPredicate) predicate).getValue());
		final String expectedErrorMsg = "Invalid value for operation:Expected IS|ISNOT for value null, got EQ";
		Functions.assertFail(IllegalArgumentException.class, expectedErrorMsg, () -> Criteria.of("attribute", "=", null));
		final AtomicPredicate isPredicate = Criteria.of("attr", "is", null);
		assertPredicate(isPredicate, "attr", Operation.IS, null);
		final AtomicPredicate isNotPredicate = Criteria.of("attr", "isNot", null);
		assertPredicate(isNotPredicate, "attr", Operation.ISNOT, null);
	}

	/** Test for the various operations.
	 */
	@Test
	public void testOperations() {
		final BiConsumer<String,Operation> tester = (s,o) -> {
			final AtomicPredicate predicate = Criteria.of("attr", s, "foo");
			assertPredicate(predicate, "attr", o, "foo");
		};
		tester.accept("=", Operation.EQ);
		tester.accept("==", Operation.EQ);
		tester.accept("!=", Operation.NE);
		tester.accept("<>", Operation.NE);
		tester.accept("<", Operation.LT);
		tester.accept("<=", Operation.LE);
		tester.accept(">", Operation.GT);
		tester.accept(">=",  Operation.GE);
		tester.accept("LIKE", Operation.LIKE);
		tester.accept("Like", Operation.LIKE);
		tester.accept("IS", Operation.IS);
		tester.accept("is", Operation.IS);
		tester.accept("ISNOT", Operation.ISNOT);
		tester.accept("isNot", Operation.ISNOT);
		final String expectedErrorMsg = "Invalid value for operation: Expected =|==|<>|!=|<|<=|>|>=|IS|ISNOT|LIKE, got unknown";
		Functions.assertFail(IllegalArgumentException.class, expectedErrorMsg,
				() -> tester.accept("unknown", null));
	}

	/** Test case for {@link Criteria#and(Object...)}.
	 */
	@Test
	public void testAndObjects() {
		Functions.assertFail(NullPointerException.class, "Predicates", () -> Criteria.and((Object[]) null));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid argument for predicates: At least one triplet must be given.", () -> Criteria.and(new Object[0]));
		Functions.assertFail(NullPointerException.class, "Predicates", () -> Criteria.and((Predicate[]) null));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid argument for predicates: At least one predicate must be given.",
				() -> Criteria.and(new Predicate[0]));
		final AndPredicate andPredicate = Criteria.and("name", "=", "foo", "email", "!=", "bar");
		assertNotNull(andPredicate);
		final Predicate[] predicates = andPredicate.getPredicates();
		assertNotNull(predicates);
		assertEquals(2, predicates.length);
		assertPredicate(predicates[0], "name", Operation.EQ, "foo");
		assertPredicate(predicates[1], "email", Operation.NE, "bar");
	}

	/** Test case for {@link Criteria#or(Object...)}.
	 */
	@Test
	public void testOrObjects() {
		Functions.assertFail(NullPointerException.class, "Predicates", () -> Criteria.or((Object[]) null));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid argument for predicates: At least one triplet must be given.", () -> Criteria.or(new Object[0]));
		Functions.assertFail(NullPointerException.class, "Predicates", () -> Criteria.or((Predicate[]) null));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid argument for predicates: At least one predicate must be given.",
				() -> Criteria.or(new Predicate[0]));
		final OrPredicate orPredicate = Criteria.or("name", "=", "foo", "email", "!=", "bar");
		assertNotNull(orPredicate);
		final Predicate[] predicates = orPredicate.getPredicates();
		assertNotNull(predicates);
		assertEquals(2, predicates.length);
		assertPredicate(predicates[0], "name", Operation.EQ, "foo");
		assertPredicate(predicates[1], "email", Operation.NE, "bar");
	}

	@Test
	public void testNotObjects() {
		Functions.assertFail(NullPointerException.class, "Attribute", () -> Criteria.not(null, "=", null));
		Functions.assertFail(NullPointerException.class, "Operation", () -> Criteria.not("attr", (String) null, null));
		Functions.assertFail(NullPointerException.class, "Attribute", () -> Criteria.not(null, Operation.EQ, null));
		Functions.assertFail(NullPointerException.class, "Operation", () -> Criteria.not("attr", (Operation) null, null));
		final NotPredicate notPredicate = Criteria.not("attr", "=", "foo");
		assertNotNull(notPredicate);
		assertPredicate(notPredicate.getPredicate(), "attr", Operation.EQ, "foo");
		final NotPredicate notPredicate2 = Criteria.not("attr", Operation.EQ, "foo");
		assertNotNull(notPredicate2);
		assertPredicate(notPredicate2.getPredicate(), "attr", Operation.EQ, "foo");
		final Predicate predicate = notPredicate2.getPredicate();
		final NotPredicate notPredicate3 = Criteria.not(notPredicate2.getPredicate());
		assertNotNull(notPredicate3);
		assertSame(notPredicate2.getPredicate(), notPredicate3.getPredicate());
	}

	/** Test case for {@link Criteria#asPredicates(Object...)}.
	 */
	@Test
	public void testAsPredicates() {
		final String expectedErrorMsg = "Invalid argument for predicates: Expected a multiple of 3 objects, got ";
		Functions.assertFail(IllegalArgumentException.class, expectedErrorMsg + "1",
				() -> Criteria.asPredicates("attr"));
		Functions.assertFail(IllegalArgumentException.class, expectedErrorMsg + "2",
				() -> Criteria.asPredicates("attr", "="));
		Functions.assertFail(IllegalArgumentException.class, expectedErrorMsg + "4",
				() -> Criteria.asPredicates("attr", "=", "foo", "attribute"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 0 to be an attribute name, thus non-null.",
				() -> Criteria.asPredicates(null, "=", "foo"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 3 to be an attribute name, thus non-null.",
				() -> Criteria.asPredicates("attr", "=", "foo", null, "!=", "bar"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 1 to be an operation, thus non-null.",
				() -> Criteria.asPredicates("attr", null, "foo"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 4 to be an operation, thus non-null.",
				() -> Criteria.asPredicates("attr", "=", "foo", "attr2", null, "bar"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 0 to be an attribute name, thus a string.",
				() -> Criteria.asPredicates(new Object(), "=", "foo"));
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 3 to be an attribute name, thus a string.",
				() -> Criteria.asPredicates("attr", "=", "foo", new Object(), "!=", "bar"));
		final Predicate[] predicates = Criteria.asPredicates("attr", "=", "foo", "attr2", Operation.NE, "bar");
		assertNotNull(predicates);
		assertEquals(2, predicates.length);
		assertPredicate(predicates[0], "attr", Operation.EQ, "foo");
		assertPredicate(predicates[1], "attr2", Operation.NE, "bar");
		Functions.assertFail(IllegalArgumentException.class,
				"Invalid value for predicates: Expected object 1 to be an operation, thus a string, or an Operation object.",
				() -> Criteria.asPredicates("attr", new Object(), "foo"));
	}

	/** For coverage: Test the constructor.
	 */
	@Test
	public void testConstructor() {
		new Criteria();
	}

	protected void assertPredicate(Predicate pPredicate, String pAttribute, Operation pOp, Object pValue) {
		final AtomicPredicate aPredicate = (AtomicPredicate) pPredicate;
		assertNotNull(aPredicate);
		assertEquals(pAttribute, aPredicate.getAttribute());
		assertSame(pOp, aPredicate.getOperation());
		if (pValue == null) {
			assertNull(aPredicate.getValue());
		} else {
			assertEquals(pValue, aPredicate.getValue());
		}
	}
}
