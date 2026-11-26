package test.atriasoft.archidata.checker;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.atriasoft.archidata.checker.ValidationContext;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.checker.model.BrokenReference;
import test.atriasoft.archidata.checker.model.LongTag;
import test.atriasoft.archidata.checker.model.OidTag;
import test.atriasoft.archidata.checker.model.TagReferences;
import test.atriasoft.archidata.checker.model.UniqueNames;
import test.atriasoft.archidata.checker.model.UuidTag;

/**
 * Characterization of the two constraints that read the database: {@code @CheckForeignKey}
 * (the referenced entity must exist) and {@code @UniqueInBaseId} (no other entity may hold the
 * value, the entity being validated excluded through the {@link ValidationContext}).
 */
public class TestDatabaseValidators {
	private static ValidatorFactory factory;
	private static Validator validator;
	private static OidTag oidTag;
	private static OidTag deletedOidTag;
	private static LongTag longTag;
	private static UuidTag uuidTag;

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
		OidTag tag = new OidTag();
		tag.name = "oid-name";
		tag.label = "oid-label";
		oidTag = DataAccess.insert(tag);
		tag = new OidTag();
		tag.name = "deleted-name";
		deletedOidTag = DataAccess.insert(tag);
		DataAccess.deleteById(OidTag.class, deletedOidTag.getOid());
		final LongTag longValue = new LongTag();
		longValue.name = "long-name";
		longTag = DataAccess.insert(longValue);
		final UuidTag uuidValue = new UuidTag();
		uuidValue.name = "uuid-name";
		uuidTag = DataAccess.insert(uuidValue);
	}

	@AfterAll
	public static void removeDatabase() throws Exception {
		factory.close();
		ConfigureDb.clear();
	}

	@AfterEach
	public void closeContext() {
		if (ValidationContext.exists()) {
			ValidationContext.current().close();
		}
	}

	private static <T> Set<String> violations(final T bean) {
		final Set<String> out = new TreeSet<>();
		for (final ConstraintViolation<T> violation : validator.validate(bean)) {
			out.add(violation.getPropertyPath() + ": " + violation.getMessage());
		}
		return out;
	}

	// ---------------------------------------------------------------- @CheckForeignKey

	@Test
	public void testForeignKeyOfAnExistingEntity() throws Exception {
		final TagReferences bean = new TagReferences();
		bean.oidTag = oidTag.getOid();
		bean.longTag = longTag.getId();
		bean.uuidTag = uuidTag.getUuid();
		bean.oidTags = List.of(oidTag.getOid());
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testForeignKeyNullIsValid() {
		Assertions.assertEquals(Set.of(), violations(new TagReferences()));
	}

	@Test
	public void testForeignKeyOfAnUnknownEntity() {
		final TagReferences bean = new TagReferences();
		bean.oidTag = new ObjectId();
		bean.longTag = 123456L;
		bean.uuidTag = UUID.randomUUID();
		Assertions.assertEquals(Set.of("oidTag: Foreign-key does not exist in the DB",
				"longTag: Foreign-key does not exist in the DB", "uuidTag: Foreign-key does not exist in the DB"),
				violations(bean));
	}

	@Test
	public void testForeignKeyOnTheElementsOfAList() {
		final TagReferences bean = new TagReferences();
		bean.oidTags = List.of(oidTag.getOid(), new ObjectId());
		Assertions.assertEquals(Set.of("oidTags[1].<list element>: Foreign-key does not exist in the DB"),
				violations(bean));
		bean.oidTags = List.of();
		Assertions.assertEquals(Set.of(), violations(bean), "an empty list references nothing");
	}

	@Test
	public void testForeignKeyOfASoftDeletedEntityIsStillValid() {
		// the validator looks at the deleted entities too (AccessDeletedItems)
		final TagReferences bean = new TagReferences();
		bean.oidTag = deletedOidTag.getOid();
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	// ---------------------------------------------------------------- @UniqueInBaseId

	@Test
	public void testUniqueWithoutContextFails() {
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "free";
		final jakarta.validation.ValidationException exception = Assertions
				.assertThrows(jakarta.validation.ValidationException.class, () -> validator.validate(bean));
		Assertions.assertEquals("Context should be defined before call this validation!",
				exception.getCause().getMessage());
	}

	@Test
	public void testUniqueNullNeedsNoContext() {
		// null returns before the context is looked at
		Assertions.assertEquals(Set.of(), violations(new UniqueNames()));
	}

	@Test
	public void testUniqueFreeValueIsValid() {
		ValidationContext.open();
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "free";
		bean.longName = "free";
		bean.uuidName = "free";
		bean.label = "free";
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testUniqueTakenValueIsInvalid() {
		ValidationContext.open();
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "oid-name";
		bean.longName = "long-name";
		bean.uuidName = "uuid-name";
		bean.label = "oid-label";
		Assertions
				.assertEquals(
						Set.of("oidName: Value already exists in the DB.", "longName: Value already exists in the DB.",
								"uuidName: Value already exists in the DB.", "label: label is taken"),
						violations(bean));
	}

	@Test
	public void testUniqueIsCaseSensitive() {
		ValidationContext.open();
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "OID-NAME";
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testUniqueValueOfAnotherEntityIsStillTaken() {
		ValidationContext.open().put("oid", new ObjectId());
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "oid-name";
		Assertions.assertEquals(Set.of("oidName: Value already exists in the DB."), violations(bean));
	}

	@Test
	public void testUniqueValueOfTheEntityItselfIsValidWithItsObjectId() {
		ValidationContext.open().put("oid", oidTag.getOid());
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "oid-name";
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testUniqueWithANullIdInTheContextExcludesNobody() {
		ValidationContext.open().put("oid", null);
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "oid-name";
		Assertions.assertEquals(Set.of("oidName: Value already exists in the DB."), violations(bean));
	}

	@Test
	public void testUniqueWithAnIdOfTheWrongTypeFails() {
		ValidationContext.open().put("oid", "not-an-object-id");
		final UniqueNames bean = new UniqueNames();
		bean.oidName = "free";
		final jakarta.validation.ValidationException exception = Assertions
				.assertThrows(jakarta.validation.ValidationException.class, () -> validator.validate(bean));
		Assertions.assertEquals("ValidationContext: value for key 'oid' is not of type ObjectId, but String",
				exception.getCause().getMessage());
	}

	@Test
	public void testUniqueValueOfTheEntityItselfIsValidWithItsLongId() {
		ValidationContext.open().put("id", longTag.getId());
		final UniqueNames bean = new UniqueNames();
		bean.longName = "long-name";
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testUniqueValueOfAnotherLongIdIsStillTaken() {
		ValidationContext.open().put("id", longTag.getId() + 1);
		final UniqueNames bean = new UniqueNames();
		bean.longName = "long-name";
		Assertions.assertEquals(Set.of("longName: Value already exists in the DB."), violations(bean));
	}

	@Test
	public void testUniqueValueOfTheEntityItselfIsValidWithItsUuid() {
		ValidationContext.open().put("uuid", uuidTag.getUuid());
		final UniqueNames bean = new UniqueNames();
		bean.uuidName = "uuid-name";
		Assertions.assertEquals(Set.of(), violations(bean));
	}

	@Test
	public void testUniqueValueOfAnotherUuidIsStillTaken() {
		ValidationContext.open().put("uuid", UUID.randomUUID());
		final UniqueNames bean = new UniqueNames();
		bean.uuidName = "uuid-name";
		Assertions.assertEquals(Set.of("uuidName: Value already exists in the DB."), violations(bean));
	}

	@Test
	public void testForeignKeyOnAClassThatIsNotAnEntity() {
		final BrokenReference bean = new BrokenReference();
		bean.ref = oidTag.getOid();
		// the failure is reported as a violation, never as an exception (the default message is reported
		// next to it, so only the failure message is pinned)
		Assertions.assertTrue(violations(bean).contains("ref: fail to access on the DB"), violations(bean).toString());
	}
}
