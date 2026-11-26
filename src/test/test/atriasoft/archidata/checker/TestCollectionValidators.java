package test.atriasoft.archidata.checker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import test.atriasoft.archidata.checker.model.ValidatedLists;

/**
 * Characterization of the archidata constraints that work on collections and lists:
 * {@code @CollectionItemNotNull}, {@code @CollectionItemUnique}, {@code @CollectionNotEmpty}
 * and {@code @ValueInList}. Each one is checked on a valid value, an invalid value, {@code null}
 * and an empty collection, through a plain Jakarta validator (no database, no server).
 */
public class TestCollectionValidators {
	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	public static void openValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	public static void closeValidator() {
		factory.close();
	}

	/** The violations as {@code path: message}, sorted, so that the assertion does not depend on the order. */
	private static Set<String> violations(final ValidatedLists bean) {
		final Set<String> out = new TreeSet<>();
		for (final ConstraintViolation<ValidatedLists> violation : validator.validate(bean)) {
			out.add(violation.getPropertyPath() + ": " + violation.getMessage());
		}
		return out;
	}

	@SafeVarargs
	private static <T> List<T> listOf(final T... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	@Test
	public void testNothingSetIsValid() {
		// null always passes: combine with @NotNull to require a value
		Assertions.assertEquals(Set.of(), violations(new ValidatedLists()));
	}

	@Test
	public void testCollectionItemNotNull() {
		final ValidatedLists bean = new ValidatedLists();
		bean.noNullItem = listOf("a", "b");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.noNullItem = listOf();
		Assertions.assertEquals(Set.of(), violations(bean), "an empty list holds no null item");
		bean.noNullItem = listOf("a", null, "b");
		Assertions.assertEquals(Set.of("noNullItem: Collection can not contain NULL item"), violations(bean));
		bean.noNullItem = listOf((String) null);
		Assertions.assertEquals(Set.of("noNullItem: Collection can not contain NULL item"), violations(bean));
		bean.noNullItem = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testCollectionItemUnique() {
		final ValidatedLists bean = new ValidatedLists();
		bean.uniqueItem = listOf("a", "b", "c");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.uniqueItem = listOf();
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.uniqueItem = listOf("a");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.uniqueItem = listOf("a", "b", "a");
		Assertions.assertEquals(Set.of("uniqueItem: Cannot insert multiple times the same elements"), violations(bean));
		// two null items are the same element
		bean.uniqueItem = listOf(null, null);
		Assertions.assertEquals(Set.of("uniqueItem: Cannot insert multiple times the same elements"), violations(bean));
		// a single null is unique
		bean.uniqueItem = listOf("a", null);
		Assertions.assertEquals(Set.of(), violations(bean));
		// the comparison is case sensitive
		bean.uniqueItem = listOf("a", "A");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.uniqueItem = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testCollectionNotEmpty() {
		final ValidatedLists bean = new ValidatedLists();
		bean.notEmpty = listOf("a");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.notEmpty = listOf((String) null);
		Assertions.assertEquals(Set.of(), violations(bean), "a list holding one null is not empty");
		bean.notEmpty = listOf();
		Assertions.assertEquals(Set.of("notEmpty: Collection can not be empty"), violations(bean));
		bean.notEmpty = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testCollectionNotEmptyOnASet() {
		final ValidatedLists bean = new ValidatedLists();
		bean.notEmptySet = new HashSet<>(Set.of(1, 2));
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.notEmptySet = new HashSet<>();
		Assertions.assertEquals(Set.of("notEmptySet: Collection can not be empty"), violations(bean));
		bean.notEmptySet = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testValueInList() {
		final ValidatedLists bean = new ValidatedLists();
		bean.color = "red";
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.color = "green";
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.color = "blue";
		Assertions.assertEquals(Set.of("color: must be in a specific list range."), violations(bean));
		// case sensitive, and an empty string is not an element of the list
		bean.color = "RED";
		Assertions.assertEquals(Set.of("color: must be in a specific list range."), violations(bean));
		bean.color = "";
		Assertions.assertEquals(Set.of("color: must be in a specific list range."), violations(bean));
		bean.color = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testValueInListOnTheElementsOfAList() {
		final ValidatedLists bean = new ValidatedLists();
		bean.sizes = listOf("small", "large");
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.sizes = listOf();
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.sizes = listOf("small", "medium");
		Assertions.assertEquals(Set.of("sizes[1].<list element>: must be in a specific list range."), violations(bean));
		// a null element passes, as a null field does
		bean.sizes = listOf("small", null);
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.sizes = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testCollectionConstraintsLetAnythingThatIsNotACollectionThrough() {
		final ValidatedLists bean = new ValidatedLists();
		bean.notACollection = "";
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.notACollection = "text";
		Assertions.assertEquals(Set.of(), violations(bean));
		bean.notACollection = null;
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testSeveralViolationsAreAllReported() {
		final ValidatedLists bean = new ValidatedLists();
		bean.noNullItem = listOf((String) null);
		bean.uniqueItem = listOf("a", "a");
		bean.notEmpty = listOf();
		bean.color = "blue";
		Assertions.assertEquals(
				Set.of("noNullItem: Collection can not contain NULL item",
						"uniqueItem: Cannot insert multiple times the same elements",
						"notEmpty: Collection can not be empty", "color: must be in a specific list range."),
				violations(bean));
	}
}
