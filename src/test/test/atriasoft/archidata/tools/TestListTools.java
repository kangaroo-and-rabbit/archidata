package test.atriasoft.archidata.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.atriasoft.archidata.tools.ListTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestListTools {

	@Test
	void testBothNull() {
		Assertions.assertTrue(ListTools.checkListIdentical(null, null));
	}

	@Test
	void testFirstNullSecondNotNull() {
		Assertions.assertFalse(ListTools.checkListIdentical(null, List.of("a")));
	}

	@Test
	void testFirstNotNullSecondNull() {
		// list1 != list2 (not same ref), list1 != null, then list1.size() != list2.size() throws NPE
		// Actually the code checks: if (list1 == list2) => false, if (list1 == null) => false,
		// then list1.size() != list2.size() where list2 is null => NPE
		// This documents existing behavior
		Assertions.assertThrows(NullPointerException.class,
				() -> ListTools.checkListIdentical(List.of("a"), null));
	}

	@Test
	void testSameReference() {
		final List<String> list = List.of("a", "b", "c");
		Assertions.assertTrue(ListTools.checkListIdentical(list, list));
	}

	@Test
	void testEqualLists() {
		final List<String> list1 = Arrays.asList("a", "b", "c");
		final List<String> list2 = Arrays.asList("a", "b", "c");
		Assertions.assertTrue(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testDifferentSizes() {
		final List<String> list1 = List.of("a", "b");
		final List<String> list2 = List.of("a", "b", "c");
		Assertions.assertFalse(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testDifferentElements() {
		final List<String> list1 = List.of("a", "b", "c");
		final List<String> list2 = List.of("a", "x", "c");
		Assertions.assertFalse(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testEmptyLists() {
		final List<String> list1 = List.of();
		final List<String> list2 = List.of();
		Assertions.assertTrue(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testWithNullElements() {
		final List<String> list1 = new ArrayList<>();
		list1.add("a");
		list1.add(null);
		list1.add("c");
		final List<String> list2 = new ArrayList<>();
		list2.add("a");
		list2.add(null);
		list2.add("c");
		Assertions.assertTrue(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testWithNullVsNonNull() {
		final List<String> list1 = new ArrayList<>();
		list1.add(null);
		final List<String> list2 = new ArrayList<>();
		list2.add("a");
		Assertions.assertFalse(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testIntegerLists() {
		final List<Integer> list1 = List.of(1, 2, 3);
		final List<Integer> list2 = List.of(1, 2, 3);
		Assertions.assertTrue(ListTools.checkListIdentical(list1, list2));
	}

	@Test
	void testMixedTypeLists() {
		final List<Object> list1 = Arrays.asList("a", 1, 2.0);
		final List<Object> list2 = Arrays.asList("a", 1, 2.0);
		Assertions.assertTrue(ListTools.checkListIdentical(list1, list2));
	}
}
