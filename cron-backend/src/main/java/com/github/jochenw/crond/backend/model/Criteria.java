package com.github.jochenw.crond.backend.model;

import java.io.Serializable;
import java.util.Objects;


public class Criteria implements Serializable {
	private static final long serialVersionUID = -4932425595493249002L;
	public enum Operation {
		LE, LT, EQ, NE, GE, GT, LIKE, IS, ISNOT;
	}
	public static interface Predicate extends Serializable {
		
	}
	public static class AtomicPredicate implements Predicate {
		private static final long serialVersionUID = 5041710797524930154L;
		private final String attribute;
		private final Operation operation;
		private final Object value;

		public AtomicPredicate(String pAttribute, Operation pOperation, Object pValue) {
			attribute = pAttribute;
			operation = pOperation;
			value = pValue;
		}

		public String getAttribute() { return attribute; }
		public Operation getOperation() { return operation; }
		public Object getValue() { return value; }
	}
	public static class AndPredicate implements Predicate {
		private static final long serialVersionUID = 3306039954850490063L;
		private final Predicate[] predicates;
		public AndPredicate(Predicate[] pPredicates) {
			predicates = pPredicates;
		}
		public Predicate[] getPredicates() { return predicates; }
	}
	public static class OrPredicate implements Predicate {
		private static final long serialVersionUID = 2928440716323480118L;
		private final Predicate[] predicates;
		public OrPredicate(Predicate[] pPredicates) {
			predicates = pPredicates;
		}
		public Predicate[] getPredicates() { return predicates; }
	}
	public static class NotPredicate implements Predicate {
		private static final long serialVersionUID = -856842834525413009L;
		private final Predicate predicate;
		public NotPredicate(Predicate pPredicate) {
			predicate = pPredicate;
		}
		public Predicate getPredicate() { return predicate; }
	}

	public static AtomicPredicate of(String pAttribute, Operation pOperation,
            Object pValue) {
		final String attribute = Objects.requireNonNull(pAttribute, "Attribute");
		final Operation op = Objects.requireNonNull(pOperation, "Operation");
		if (pValue == null) {
			if (op != Operation.IS  &&  op != Operation.ISNOT) {
				throw new IllegalArgumentException("Invalid value for operation:"
					+ "Expected IS|ISNOT for value null, got " + op);
			}
		}
		return new AtomicPredicate(attribute, op, pValue);
	}
	public static AtomicPredicate of(String pAttribute, String pOperation,
			                         Object pValue) {
		final String attribute = Objects.requireNonNull(pAttribute, "Attribute");
		final String opStr = Objects.requireNonNull(pOperation, "Operation");
		final Operation op;
		switch (opStr.trim().toUpperCase()) {
		case "=": op = Operation.EQ; break;
		case "==": op = Operation.EQ; break;
		case "<>": op = Operation.NE; break;
		case "!=": op = Operation.NE; break;
		case "<": op = Operation.LT; break;
		case "<=": op = Operation.LE; break;
		case ">": op = Operation.GT; break;
		case ">=": op = Operation.GE; break;
		case "IS": op = Operation.IS; break;
		case "ISNOT": op = Operation.ISNOT; break;
		case "LIKE": op = Operation.LIKE; break;
		default:
			throw new IllegalArgumentException("Invalid value for operation:"
					+ " Expected =|==|<>|!=|<|<=|>|>=|IS|ISNOT|LIKE, got " + opStr);
		}
		return of(attribute, op, pValue);
	}

	public static AndPredicate and(Predicate... pPredicates) {
		final Predicate[] predicates = Objects.requireNonNull(pPredicates, "Predicates");
		if (predicates.length == 0) {
			throw new IllegalArgumentException("Invalid argument for predicates:"
					+ " At least one predicate must be given.");
		}
		return new AndPredicate(predicates);
	}

	public static AndPredicate and(Object... pPredicates) {
		return and(asPredicates(pPredicates));
	}

	public static OrPredicate or(Object... pPredicates) {
		return or(asPredicates(pPredicates));
	}

	public static OrPredicate or(Predicate... pPredicates) {
		final Predicate[] predicates = Objects.requireNonNull(pPredicates, "Predicates");
		if (predicates.length == 0) {
			throw new IllegalArgumentException("Invalid argument for predicates:"
					+ " At least one predicate must be given.");
		}
		return new OrPredicate(predicates);
	}

	public static NotPredicate not(Predicate pPredicate) {
		final Predicate predicate = Objects.requireNonNull(pPredicate, "Predicate");
		return new NotPredicate(predicate);
	}

	public static NotPredicate not(String pAttribute, String pOperation, Object pValue) {
		final Predicate predicate = of(pAttribute, pOperation, pValue);
		return new NotPredicate(predicate);
	}

	public static NotPredicate not(String pAttribute, Operation pOperation, Object pValue) {
		final Predicate predicate = of(pAttribute, pOperation, pValue);
		return new NotPredicate(predicate);
	}

	public static Predicate[] asPredicates(Object... pPredicates) {
		final Object[] predicateObjects = Objects.requireNonNull(pPredicates, "Predicates");
		if (predicateObjects.length == 0) {
			throw new IllegalArgumentException("Invalid argument for predicates:"
					+ " At least one triplet must be given.");
		}
		if (predicateObjects.length % 3 != 0) {
			throw new IllegalArgumentException("Invalid argument for predicates:"
					+ " Expected a multiple of 3 objects, got " + predicateObjects.length);
		}
		final Predicate[] predicates = new Predicate[predicateObjects.length / 3];
		for (int i = 0;  i < predicateObjects.length;  ) {
			final Object attrObject = predicateObjects[i++];
			if (attrObject == null) {
				throw new IllegalArgumentException("Invalid value for predicates:"
						+ " Expected object " + (i-1)
						+ " to be an attribute name, thus non-null.");
			}
			if (!(attrObject instanceof String)) {
				throw new IllegalArgumentException("Invalid value for predicates:"
						+ " Expected object " + (i-1)
						+ " to be an attribute name, thus a string.");
			}
			final String attribute = (String) attrObject;
			final Object opObject = predicateObjects[i++];
			if (opObject == null) {
				throw new IllegalArgumentException("Invalid value for predicates:"
						+ " Expected object " + (i-1)
						+ " to be an operation, thus non-null.");
			}
			final Object valueObject = predicateObjects[i++];
			final Predicate predicate;
			if (opObject instanceof Operation) {
				predicate = new AtomicPredicate(attribute, (Operation) opObject, valueObject);
			} else if (opObject instanceof String) {
				predicate = of(attribute, (String) opObject, valueObject);
			} else {
				throw new IllegalArgumentException("Invalid value for predicates:"
						+ " Expected object " + (i-2)
						+ " to be an operation, thus a string, or an Operation object.");
			}
			predicates[(i-3)/3] = predicate;
		}
		return predicates;
	}
}
