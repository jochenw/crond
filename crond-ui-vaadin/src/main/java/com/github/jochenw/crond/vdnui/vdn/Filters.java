package com.github.jochenw.crond.vdnui.vdn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.jochenw.afw.core.function.Predicates;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Strings;
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

	public static class ComparatorBuilder<O> {
		private List<Comparator<O>> comparators;

		public ComparatorBuilder<O> add(Comparator<O> pComparator) {
			if (comparators == null) {
				comparators = new ArrayList<>();
			}
			comparators.add(pComparator);
			return this;
		}

		public <T> ComparatorBuilder<O> add(Function<O,T> pMapper, Comparator<T> pComparator) {
			final Function<O,T> mapper = Objects.requireNonNull(pMapper, "Mapper");
			final Comparator<T> comparator = Objects.requireNonNull(pComparator, "Comparator");
			return add((o1,o2) -> {
				return comparator.compare(mapper.apply(o1), mapper.apply(o2));
			});
		}

		public ComparatorBuilder<O> add(Function<O,String> pMapper, boolean pAscending) {
			final Function<O,String> mapper = Objects.requireNonNull(pMapper, "Mapper");
			final BiFunction<Object,Object,Integer> nullChecker = (o1,o2) -> {
				if (o1 == null) {
					if (o2 == null) {
						return Integer.valueOf(0);
					} else {
						return Integer.valueOf(-1);
					}
				} else {
					if (o2 == null) {
						return Integer.valueOf(1);
					} else {
						return null;
					}
				}
			};
			return add((o1, o2) -> {
				if (!pAscending) { // Swap the objects, that are being compared.
					final O o0 = o1;
					o1 = o2;
					o2 = o0;
				}
				Integer res = nullChecker.apply(o1, o2);
				if (res == null) {
				    final String s1 = mapper.apply(o1);	
				    final String s2 = mapper.apply(o2);	
				    res = nullChecker.apply(s1,  s2);
				    if (res == null) {
				    	if (pAscending) {
				    		return s1.compareToIgnoreCase(s2);
				    	} else {
				    		return s2.compareToIgnoreCase(s1);
				    	}
				    }
				}
				return res.intValue();
			});
		}

		public Comparator<O> build() {
			if (comparators == null) {
				return null;
			} else if (comparators.size() == 1) {
				return comparators.get(0);
			} else {
				return (o1,o2) -> {
					for (int i = 0;  i < comparators.size();  i++) {
						final int res = comparators.get(i).compare(o1, o2);
						if (res != 0) {
							return res;
						}
					}
					return 0;
				};
			}
		}
	}
	public static class PredicateBuilder<O> {
		private List<Predicate<O>> predicates;

		public PredicateBuilder<O> add(Predicate<O> pPredicate) {
			if (predicates == null) {
				predicates = new ArrayList<>();
			}
			predicates.add(pPredicate);
			return this;
		}

		public PredicateBuilder<O> add(Function<O,String> pFunction, String pFilterString) {
			final Predicate<String> stringPredicate = asStringPredicate(pFilterString);
			if (stringPredicate != null) {
				final Predicate<O> predicate = (o) -> {
					if (o != null) {
						final String v = pFunction.apply(o);
						if (v != null) {
							return stringPredicate.test(v);
						}
					}
					return false;
				};
				return add(predicate);
			}
			return this;
		}
	
		protected Predicate<String> asStringPredicate(String pPattern) {
			final String pattern = Objects.requireNonNull(pPattern, "Pattern").replace('%', '*');
			if (pattern.indexOf('*') >= 0  ||  pattern.indexOf('?') >= 0) {
				return Strings.matcher(pattern);
			} else {
				final String patternLc = pattern.toLowerCase();
				return (s) -> s.toLowerCase().contains(patternLc);
			}
		}

		public Predicate<O> build() {
			if (predicates == null  ||  predicates.isEmpty()) {
				return Predicates.alwaysTrue();
			} else if (predicates.size() == 1) {
				return predicates.get(0);
			} else {
				return Predicates.allOf(predicates);
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

	public static <O> PredicateBuilder<O> predicate() {
		return new PredicateBuilder<>();
	}

	public static <O> PredicateBuilder<O> predicate(Class<O> pType) {
		return new PredicateBuilder<>();
	}

	public static <O> ComparatorBuilder<O> comparator() {
		return new ComparatorBuilder<>();
	}

	public static <O> ComparatorBuilder<O> comparator(Class<O> pTyoe) {
		return new ComparatorBuilder<>();
	}
 }
