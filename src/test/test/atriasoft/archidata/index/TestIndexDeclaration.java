package test.atriasoft.archidata.index;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.index.IndexKey;
import org.atriasoft.archidata.index.IndexRegistry;
import org.atriasoft.archidata.index.IndexSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.index.model.IndexModelChild;
import test.atriasoft.archidata.index.model.IndexModelClassAnnotation;
import test.atriasoft.archidata.index.model.IndexModelContradiction;
import test.atriasoft.archidata.index.model.IndexModelFieldAnnotation;
import test.atriasoft.archidata.index.model.IndexModelPlain;
import test.atriasoft.archidata.index.model.IndexModelUnknownField;

/**
 * Tests of the index declaration: parsing, canonical naming, merge of the three declaration ways,
 * and the checks that must fail the resolution rather than produce a useless index.
 *
 * <p>No database is involved: this stage only turns declarations into specifications.
 */
public class TestIndexDeclaration {

	@BeforeEach
	public void resetRegistry() {
		IndexRegistry.clear();
	}

	private static Set<String> namesOf(final List<IndexSpec> specs) {
		return specs.stream().map(IndexSpec::getName).collect(Collectors.toSet());
	}

	private static IndexSpec byName(final List<IndexSpec> specs, final String name) {
		return specs.stream().filter(spec -> name.equals(spec.getName())).findFirst().orElseThrow();
	}

	// ========== Key parsing ==========

	@Test
	public void testKeyParsing() {
		Assertions.assertEquals(new IndexKey("createdAt", true), IndexKey.parse("createdAt"));
		Assertions.assertEquals(new IndexKey("createdAt", false), IndexKey.parse("-createdAt"));
		Assertions.assertEquals(new IndexKey("createdAt", true), IndexKey.parse("+createdAt"));
		Assertions.assertEquals(new IndexKey("address.city", true), IndexKey.parse(" address.city "));
		Assertions.assertEquals(1, IndexKey.parse("a").order());
		Assertions.assertEquals(-1, IndexKey.parse("-a").order());
		Assertions.assertEquals("address", IndexKey.parse("address.city.zone").topLevel());
	}

	@Test
	public void testMalformedKeyIsRejected() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndexKey.parse(""));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndexKey.parse("-"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndexKey.parse(".city"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndexKey.parse("address."));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndexKey.parse("address..city"));
	}

	// ========== Canonical naming ==========

	@Test
	public void testCanonicalNameOfASimpleIndex() {
		Assertions.assertEquals("kar_companyId_1_publishedAt_-1",
				IndexSpec.asc("companyId").thenDesc("publishedAt").getName());
		Assertions.assertEquals("kar_email_1", IndexSpec.asc("email").getName());
	}

	@Test
	public void testNameEmbedsAHashAsSoonAsOptionsAreSet() {
		final String plain = IndexSpec.asc("email").getName();
		final String unique = IndexSpec.asc("email").unique().getName();
		final String sparse = IndexSpec.asc("email").sparse().getName();

		Assertions.assertEquals("kar_email_1", plain);
		// A different definition must never reuse the name of another one: the synchronization
		// identifies an index by its name, and would otherwise leave a stale definition in place.
		Assertions.assertNotEquals(plain, unique);
		Assertions.assertNotEquals(unique, sparse);
		Assertions.assertTrue(unique.startsWith("kar_email_1_"));
	}

	@Test
	public void testDottedPathAlwaysGetsAHash() {
		// "a.b" and "a_b" flatten to the same readable name: only the hash keeps them apart.
		final String dotted = IndexSpec.asc("address.city").getName();
		final String flat = IndexSpec.asc("address_city").getName();
		Assertions.assertNotEquals(dotted, flat);
		Assertions.assertTrue(dotted.startsWith("kar_address_city_1_"));
	}

	@Test
	public void testLongNameIsTruncatedAndHashed() {
		IndexSpec spec = IndexSpec.asc("aVeryLongFieldNameNumber0");
		for (int iii = 1; iii < 8; iii++) {
			spec = spec.thenAsc("aVeryLongFieldNameNumber" + iii);
		}
		final String name = spec.getName();
		Assertions.assertTrue(name.startsWith("kar_"));
		Assertions.assertTrue(name.length() <= 110, "name too long: " + name.length());
		// Still deterministic across calls.
		Assertions.assertEquals(name, spec.getName());
	}

	@Test
	public void testExplicitNameIsAlwaysManaged() {
		// Without the prefix the synchronization would see a foreign index and drop it.
		Assertions.assertEquals("kar_my_index", IndexSpec.asc("email").name("my_index").getName());
		Assertions.assertEquals("kar_my_index", IndexSpec.asc("email").name("kar_my_index").getName());
	}

	@Test
	public void testKeysDocument() {
		Assertions.assertEquals("{\"companyId\": 1, \"publishedAt\": -1}",
				IndexSpec.asc("companyId").thenDesc("publishedAt").toKeysDocument().toJson());
	}

	// ========== Structural validation ==========

	@Test
	public void testStructuralValidation() {
		Assertions.assertThrows(DataAccessException.class, () -> IndexSpec.asc("a").thenAsc("a").validate("test"));
		Assertions.assertThrows(DataAccessException.class,
				() -> IndexSpec.asc("a").thenAsc("b").expireAfterSeconds(10).validate("test"));
		Assertions.assertThrows(DataAccessException.class,
				() -> IndexSpec.asc("a").partialFilter("{not json").validate("test"));
		Assertions.assertDoesNotThrow(() -> IndexSpec.asc("a").expireAfterSeconds(10).validate("test"));
		Assertions.assertDoesNotThrow(() -> IndexSpec.asc("a").partialFilter("{\"archived\": false}").validate("test"));
	}

	// ========== Declaration through the class annotation ==========

	@Test
	public void testClassAnnotationsAreResolved() throws Exception {
		final List<IndexSpec> specs = IndexRegistry.resolve(IndexModelClassAnnotation.class);

		Assertions.assertEquals(2, specs.size());
		final IndexSpec compound = byName(specs, "kar_companyId_1_publishedAt_-1");
		Assertions.assertEquals(List.of(new IndexKey("companyId", true), new IndexKey("publishedAt", false)),
				compound.getKeys());
		Assertions.assertFalse(compound.isUnique());
		// The declaration names the column ("email"), not the Java property ("mailAddress").
		final IndexSpec unique = specs.stream().filter(IndexSpec::isUnique).findFirst().orElseThrow();
		Assertions.assertEquals("email", unique.getKeys().get(0).path());
	}

	@Test
	public void testResolutionIsStableAndCached() throws Exception {
		Assertions.assertEquals(IndexRegistry.resolve(IndexModelClassAnnotation.class),
				IndexRegistry.resolve(IndexModelClassAnnotation.class));
	}

	// ========== Declaration through the property annotation ==========

	@Test
	public void testFieldAnnotationsAreResolved() throws Exception {
		final List<IndexSpec> specs = IndexRegistry.resolve(IndexModelFieldAnnotation.class);

		Assertions.assertEquals(3, specs.size());
		final IndexSpec login = specs.stream()//
				.filter(spec -> "login".equals(spec.getKeys().get(0).path())).findFirst().orElseThrow();
		Assertions.assertTrue(login.isUnique());
		final IndexSpec lastSeen = specs.stream()//
				.filter(spec -> "lastSeenAt".equals(spec.getKeys().get(0).path())).findFirst().orElseThrow();
		Assertions.assertFalse(lastSeen.getKeys().get(0).ascending());
		final IndexSpec session = specs.stream()//
				.filter(spec -> "sessionExpireAt".equals(spec.getKeys().get(0).path())).findFirst().orElseThrow();
		Assertions.assertEquals(3600, session.getExpireAfterSeconds());
		// @Column(unique = true) creates nothing: it only raises a warning.
		Assertions.assertTrue(specs.stream().noneMatch(spec -> "legacyKey".equals(spec.getKeys().get(0).path())));
	}

	// ========== Programmatic declaration ==========

	@Test
	public void testProgrammaticDeclarationWithMethodReferences() throws Exception {
		IndexRegistry.declare(IndexModelPlain.class, //
				IndexSpec.asc(IndexModelPlain::getCompanyId).thenDesc(IndexModelPlain::getCreatedOn), //
				IndexSpec.asc(IndexModelPlain::getEmail).unique());

		final List<IndexSpec> specs = IndexRegistry.resolve(IndexModelPlain.class);

		Assertions.assertEquals(2, specs.size());
		Assertions.assertTrue(namesOf(specs).contains("kar_companyId_1_createdOn_-1"));
		final IndexSpec unique = specs.stream().filter(IndexSpec::isUnique).findFirst().orElseThrow();
		Assertions.assertEquals("email", unique.getKeys().get(0).path());
	}

	@Test
	public void testProgrammaticAndAnnotationAreMerged() throws Exception {
		IndexRegistry.declare(IndexModelClassAnnotation.class, IndexSpec.asc("address.city"));

		final List<IndexSpec> specs = IndexRegistry.resolve(IndexModelClassAnnotation.class);

		Assertions.assertEquals(3, specs.size());
		Assertions.assertTrue(specs.stream().anyMatch(spec -> "address.city".equals(spec.getKeys().get(0).path())));
	}

	@Test
	public void testDeclaringTheSameIndexTwiceIsDeduplicated() throws Exception {
		IndexRegistry.declare(IndexModelClassAnnotation.class, //
				IndexSpec.asc("companyId").thenDesc("publishedAt"), //
				IndexSpec.asc("companyId").thenDesc("publishedAt"));

		// Same as the class annotation: one index, not three.
		Assertions.assertEquals(2, IndexRegistry.resolve(IndexModelClassAnnotation.class).size());
	}

	@Test
	public void testClearForgetsTheProgrammaticDeclarations() throws Exception {
		IndexRegistry.declare(IndexModelPlain.class, IndexSpec.asc("email"));
		Assertions.assertEquals(1, IndexRegistry.resolve(IndexModelPlain.class).size());

		IndexRegistry.clear(IndexModelPlain.class);
		Assertions.assertEquals(0, IndexRegistry.resolve(IndexModelPlain.class).size());
	}

	// ========== Rejected declarations ==========

	@Test
	public void testContradictoryDeclarationsAreRejected() {
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> IndexRegistry.resolve(IndexModelContradiction.class));
		Assertions.assertTrue(ex.getMessage().contains("Contradictory"), ex.getMessage());
	}

	@Test
	public void testUnknownFieldInAnnotationIsRejected() {
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> IndexRegistry.resolve(IndexModelUnknownField.class));
		Assertions.assertTrue(ex.getMessage().contains("emailAdress"), ex.getMessage());
	}

	@Test
	public void testUnknownFieldInProgrammaticDeclarationIsRejected() {
		IndexRegistry.declare(IndexModelPlain.class, IndexSpec.asc("compayId"));
		Assertions.assertThrows(DataAccessException.class, () -> IndexRegistry.resolve(IndexModelPlain.class));
	}

	@Test
	public void testUnknownPrefixOfADottedPathIsRejected() {
		IndexRegistry.declare(IndexModelClassAnnotation.class, IndexSpec.asc("adress.city"));
		Assertions.assertThrows(DataAccessException.class,
				() -> IndexRegistry.resolve(IndexModelClassAnnotation.class));
	}

	// ========== Inheritance ==========

	@Test
	public void testIndexesAreInherited() throws Exception {
		final List<IndexSpec> specs = IndexRegistry.resolve(IndexModelChild.class);

		Assertions.assertEquals(Set.of("kar_archivedAt_-1", "kar_label_1"), namesOf(specs));
	}
}
