package org.atriasoft.archidata.tools;

import java.util.List;

public class ListTools {
	private ListTools() {
		// Utility class
	}

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
