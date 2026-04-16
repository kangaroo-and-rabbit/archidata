package org.atriasoft.archidata.tools;

import java.util.List;

/**
 * Utility class for list comparison operations.
 */
public class ListTools {
	private ListTools() {
		// Utility class
	}

	/**
	 * Checks whether two lists are identical element-by-element using {@link Object#equals(Object)}.
	 *
	 * <p>Returns {@code true} if both lists have the same size and all elements at
	 * corresponding positions are equal. Two {@code null} references at the same position
	 * are considered equal.</p>
	 *
	 * @param list1 The first list (may be {@code null}).
	 * @param list2 The second list (may be {@code null}).
	 * @return {@code true} if the lists are identical, {@code false} otherwise.
	 */
	public static boolean checkListIdentical(final List<?> list1, final List<?> list2) {
		if (list1 == list2) {
			return true;
		}
		if (list1 == null) {
			return false;
		}
		if (list1.size() != list2.size()) {
			return false;
		}
		for (int iii = 0; iii < list1.size(); iii++) {
			final Object aaa = list1.get(iii);
			final Object bbb = list2.get(iii);
			if (aaa == bbb) {
				continue;
			}
			if (aaa == null) {
				return false;
			}
			if (!aaa.equals(bbb)) {
				return false;
			}
		}
		return true;
	}
}
