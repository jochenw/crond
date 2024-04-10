package com.github.jochenw.crond.backend.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Predicate;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Reflection;
import com.github.jochenw.crond.backend.model.Criteria;
import com.github.jochenw.crond.backend.model.Criteria.AndPredicate;
import com.github.jochenw.crond.backend.model.Criteria.AtomicPredicate;
import com.github.jochenw.crond.backend.model.Criteria.NotPredicate;
import com.github.jochenw.crond.backend.model.Criteria.Operation;
import com.github.jochenw.crond.backend.model.Criteria.OrPredicate;


public class BeanCriteriaEvaluator {
	private static final BeanCriteriaEvaluator INSTANCE = new BeanCriteriaEvaluator();

	public static <O> Predicate<O> matcher(Class<O> pType, Criteria.Predicate pPredicate) {
		return INSTANCE.asMatcher(pType, pPredicate);
	}

	protected <O> Predicate<O> asMatcher(Class<O> pType, Criteria.Predicate pPredicate) {
		if (pPredicate instanceof NotPredicate) {
			final Predicate<O> pred = asMatcher(pType, ((NotPredicate) pPredicate).getPredicate()); 
			return (o) -> !pred.test(o);
		} else if (pPredicate instanceof AndPredicate) {
			final Criteria.Predicate[] critPreds = ((AndPredicate) pPredicate).getPredicates();
			@SuppressWarnings("unchecked")
			final Predicate<O>[] preds = (Predicate[]) Array.newInstance(Predicate.class, critPreds.length);
			for (int i = 0;  i < preds.length;  i++) {
				preds[i] = asMatcher(pType, critPreds[i]);
			}
			return (o) -> {
				for (Predicate<O> predicate : preds) {
					if (!predicate.test(o)) {
						return false;
					}
				}
				return true;
			};
		} else if (pPredicate instanceof OrPredicate) {
			final Criteria.Predicate[] critPreds = ((OrPredicate) pPredicate).getPredicates();
			@SuppressWarnings("unchecked")
			final Predicate<O>[] preds = (Predicate[]) Array.newInstance(Predicate.class, critPreds.length);
			for (int i = 0;  i < preds.length;  i++) {
				preds[i] = asMatcher(pType, critPreds[i]);
			}
			return (o) -> {
				for (Predicate<O> predicate : preds) {
					if (predicate.test(o)) {
						return true;
					}
				}
				return false;
			};
		} else if (pPredicate instanceof AtomicPredicate) {
			final AtomicPredicate atomicPredicate = (AtomicPredicate) pPredicate;
			final Method method = findGetter(pType, atomicPredicate.getAttribute());
			final Predicate<Object> valuePredicate = asAttributePredicate(method.getReturnType(), atomicPredicate.getOperation(), atomicPredicate.getValue());
			return (o) -> {
				Object value;
				try {
					Reflection.makeAcccessible(method);
					value = method.invoke(o);
				} catch (InvocationTargetException e) {
					throw Exceptions.show(e.getCause());
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
				return valuePredicate.test(value);
			};
		} else {
			throw new IllegalStateException("Invalid type of predicate: " + pPredicate.getClass().getName());
		}
	}

	protected Method findGetter(Class<?> pType, String pAttribute) {
		final String suffix = Character.toUpperCase(pAttribute.charAt(0)) + pAttribute.substring(1);
		NoSuchMethodException nsme = null;
		for (String prefix : Arrays.asList("get", "is", "has")) {
			final Method method;
			try {
				method = pType.getMethod(prefix + suffix);
			} catch (NoSuchMethodException e) {
				if (nsme == null) {
					nsme = e;
				}
				continue;
			}
			final Class<?> returnType = method.getReturnType();
			if (returnType != Void.class  &&  returnType != Void.TYPE  &&  returnType != null) {
				return method;
			}
		}
		throw new UndeclaredThrowableException(nsme);
	}

	protected Predicate<Object> asAttributePredicate(Class<?> pReturnType, Operation pOperation, Object pValue) {
		switch (pOperation) {
		case IS: return (o) -> o == null;
		case ISNOT: return (o) -> o != null;
		default:
			break;
		}

		if (pReturnType == String.class) {
			switch (pOperation) {
			case EQ, NE, LT, LE, GT, GE: break;
			default: throw new IllegalStateException("Operation " + pOperation + " not implemented for type " + pReturnType.getName());
			}
			final String expect;
			if (pValue instanceof String) {
				expect = (String) pValue;
			} else {
				throw new IllegalArgumentException("Invalid object type for String comparison: " + pValue.getClass().getName());
			}
			return (o) -> {
				final String str = (String) o;
				switch (pOperation) {
				case EQ: return expect.equals(str);
				case NE: return !expect.equals(str);
				case LT: return expect.compareTo(str) > 0;
				case LE: return expect.compareTo(str) >= 0;
				case GT: return expect.compareTo(str) < 0;
				case GE: return expect.compareTo(str) <= 0;
				default: throw new IllegalStateException("Operation " + pOperation + " not implemented for type " + pReturnType.getName());
				}
			};
		} else if (pReturnType == BigDecimal.class) {
			switch (pOperation) {
			case EQ, NE, LT, LE, GT, GE: break;
			default: throw new IllegalStateException("Operation " + pOperation + " not implemented for type " + pReturnType.getName());
			}
			final BigDecimal expect;
			if (pValue instanceof String) {
				expect = new BigDecimal((String) pValue);
			} else if (pValue instanceof BigDecimal) {
				expect = (BigDecimal) pValue;
			} else {
				throw new IllegalArgumentException("Invalid object type for String comparison: " + pValue.getClass().getName());
			}
			return (o) -> {
				final BigDecimal num = (BigDecimal) o;
				switch (pOperation) {
				case EQ: return expect.compareTo(num) == 0;
				case NE: return expect.compareTo(num) != 0;
				case LT: return expect.compareTo(num) > 0;
				case LE: return expect.compareTo(num) >= 0;
				case GT: return expect.compareTo(num) < 0;
				case GE: return expect.compareTo(num) <= 0;
				default: throw new IllegalStateException("Operation " + pOperation + " not implemented for type " + pReturnType.getName());
				}
			};
		} else {
			throw new IllegalStateException("Operation " + pOperation + " not implemented for type " + pReturnType.getName());
		}

	}
}
