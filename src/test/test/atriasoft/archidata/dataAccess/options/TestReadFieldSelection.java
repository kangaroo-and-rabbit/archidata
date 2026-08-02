package test.atriasoft.archidata.dataAccess.options;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.ComplexSubObject;
import test.atriasoft.archidata.dataAccess.model.DeepNestedModel;
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;
import test.atriasoft.archidata.dataAccess.model.NestedSubObject;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildExpand;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildFFF;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDParentIgnore;

/**
 * Tests of the read field selection: {@code FilterValue} / {@code FilterOmit} restrict the MongoDB
 * projection, so an unwanted field is neither transferred nor mapped — and an excluded relation
 * field does not trigger its extra query.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestReadFieldSelection {

	public static class Model extends OIDGenericData {
		public String field1;
		public String field2;
		public Integer field3;
	}

	private ObjectId insertedId = null;

	@BeforeAll
	public void setup() throws Exception {
		ConfigureDb.configure();
		final Model data = new Model();
		data.field1 = "value1";
		data.field2 = "value2";
		data.field3 = 42;
		this.insertedId = ConfigureDb.da.insert(data).getOid();
	}

	@AfterAll
	public void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	private static Set<String> projectionOf(final QueryOptions options) throws Exception {
		return Set.copyOf(
				DbClassModel.of(Model.class).generateSelectFields(QueryOptions.readAllColumn(options), options));
	}

	// ========== The projection actually sent to MongoDB ==========

	@Test
	public void testProjectionWithoutOptionReadsEverythingButTheNotReadFields() throws Exception {
		final Set<String> fields = projectionOf(new QueryOptions());
		Assertions.assertEquals(Set.of("_id", "field1", "field2", "field3"), fields);
		// createdAt/updatedAt are @DataNotRead: they never reach the wire by default.
		Assertions.assertFalse(fields.contains("createdAt"));
	}

	@Test
	public void testProjectionRestrictedByFilterValue() throws Exception {
		final Set<String> fields = projectionOf(new QueryOptions(new FilterValue("field1")));
		// Only the requested field... plus the primary key, always read.
		Assertions.assertEquals(Set.of("_id", "field1"), fields);
	}

	@Test
	public void testProjectionRestrictedByFilterOmit() throws Exception {
		final Set<String> fields = projectionOf(new QueryOptions(new FilterOmit("field2", "field3")));
		Assertions.assertEquals(Set.of("_id", "field1"), fields);
	}

	@Test
	public void testProjectionCombinesFilterValueAndFilterOmit() throws Exception {
		final Set<String> fields = projectionOf(
				new QueryOptions(new FilterValue("field1", "field2"), new FilterOmit("field2")));
		Assertions.assertEquals(Set.of("_id", "field1"), fields);
	}

	@Test
	public void testFilterValueWinsOverReadAllColumn() throws Exception {
		final Set<String> fields = projectionOf(new QueryOptions(new ReadAllColumn(), new FilterValue("field1")));
		// ReadAllColumn lifts the @DataNotRead exclusion, it does not defeat an explicit whitelist.
		Assertions.assertEquals(Set.of("_id", "field1"), fields);
	}

	@Test
	public void testReadAllColumnAloneAddsTheNotReadFields() throws Exception {
		final Set<String> fields = projectionOf(new QueryOptions(new ReadAllColumn()));
		Assertions.assertTrue(fields.contains("createdAt"));
		Assertions.assertTrue(fields.contains("updatedAt"));
	}

	@Test
	public void testAmbiguousRestrictionIsRejected() throws Exception {
		final QueryOptions twoValues = new QueryOptions(new FilterValue("field1"), new FilterValue("field2"));
		Assertions.assertThrows(DataAccessException.class, () -> projectionOf(twoValues));
		final QueryOptions twoOmits = new QueryOptions(new FilterOmit("field1"), new FilterOmit("field2"));
		Assertions.assertThrows(DataAccessException.class, () -> projectionOf(twoOmits));
	}

	@Test
	public void testUnknownFieldIsRejected() throws Exception {
		// A typo must fail loudly: silently reading nothing is far harder to diagnose.
		final QueryOptions typoInclude = new QueryOptions(new FilterValue("feild1"));
		Assertions.assertThrows(DataAccessException.class, () -> projectionOf(typoInclude));
		final QueryOptions typoOmit = new QueryOptions(new FilterOmit("feild1"));
		Assertions.assertThrows(DataAccessException.class, () -> projectionOf(typoOmit));
		Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.getById(Model.class, this.insertedId, new FilterValue("feild1")));
	}

	// ========== Dotted paths inside an embedded sub-document ==========

	private static Set<String> nestedProjectionOf(final QueryOptions options) throws Exception {
		return Set.copyOf(DbClassModel.of(DeepNestedModel.class)
				.generateSelectFields(QueryOptions.readAllColumn(options), options));
	}

	@Test
	public void testDottedPathIsPushedToTheProjection() throws Exception {
		final Set<String> fields = nestedProjectionOf(
				new QueryOptions(new FilterValue("simpleObject.name", "simpleObject.count")));
		Assertions.assertEquals(Set.of("_id", "simpleObject.name", "simpleObject.count"), fields);
	}

	@Test
	public void testWholeFieldWinsOverItsSubPaths() throws Exception {
		final Set<String> fields = nestedProjectionOf(
				new QueryOptions(new FilterValue("simpleObject", "simpleObject.name")));
		Assertions.assertEquals(Set.of("_id", "simpleObject"), fields);
	}

	@Test
	public void testDottedPathOnlyReadsTheRequestedPartOfTheSubDocument() throws Exception {
		final DeepNestedModel data = new DeepNestedModel();
		data.simpleObject = new ComplexSubObject("alpha", 42, true, Enum2ForTest.ENUM_VALUE_1, List.of("a", "b"));
		data.nestedObject = new NestedSubObject("outer", null);
		final ObjectId id = ConfigureDb.da.insert(data).getOid();

		final DeepNestedModel retrieved = ConfigureDb.da.getById(DeepNestedModel.class, id,
				new FilterValue("simpleObject.name"));

		Assertions.assertNotNull(retrieved.simpleObject);
		// Only the requested part of the sub-document travelled...
		Assertions.assertEquals("alpha", retrieved.simpleObject.name);
		Assertions.assertNull(retrieved.simpleObject.count);
		Assertions.assertNull(retrieved.simpleObject.tags);
		// ... and the sibling top-level field was not read at all.
		Assertions.assertNull(retrieved.nestedObject);
		Assertions.assertEquals(id, retrieved.getOid());
	}

	@Test
	public void testDottedPathReachesADeeperLevel() throws Exception {
		final DeepNestedModel data = new DeepNestedModel();
		data.nestedObject = new NestedSubObject("outer",
				new ComplexSubObject("inner", 7, false, Enum2ForTest.ENUM_VALUE_3, List.of("z")));
		final ObjectId id = ConfigureDb.da.insert(data).getOid();

		final DeepNestedModel retrieved = ConfigureDb.da.getById(DeepNestedModel.class, id,
				new FilterValue("nestedObject.inner.name"));

		Assertions.assertNotNull(retrieved.nestedObject);
		Assertions.assertNull(retrieved.nestedObject.label);
		Assertions.assertNotNull(retrieved.nestedObject.inner);
		Assertions.assertEquals("inner", retrieved.nestedObject.inner.name);
		Assertions.assertNull(retrieved.nestedObject.inner.count);
	}

	@Test
	public void testDottedPathOnAnUnknownFieldIsRejected() throws Exception {
		final QueryOptions options = new QueryOptions(new FilterValue("simpleObjetc.name"));
		Assertions.assertThrows(DataAccessException.class, () -> nestedProjectionOf(options));
	}

	@Test
	public void testDottedPathIsRejectedInFilterOmit() throws Exception {
		// The projection is an inclusion: MongoDB cannot mix it with a sub-field exclusion.
		final QueryOptions options = new QueryOptions(new FilterOmit("simpleObject.name"));
		Assertions.assertThrows(DataAccessException.class, () -> nestedProjectionOf(options));
	}

	// ========== The objects built out of a restricted read ==========

	@Test
	public void testGetOnlyFillsTheRequestedFields() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, this.insertedId, new FilterValue("field1"));

		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("value1", retrieved.field1);
		Assertions.assertNull(retrieved.field2);
		Assertions.assertNull(retrieved.field3);
		// The primary key is always read: the object stays usable for a later update/delete.
		Assertions.assertEquals(this.insertedId, retrieved.getOid());
	}

	@Test
	public void testGetSkipsTheOmittedFields() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, this.insertedId, new FilterOmit("field2"));

		Assertions.assertEquals("value1", retrieved.field1);
		Assertions.assertNull(retrieved.field2);
		Assertions.assertEquals(42, retrieved.field3);
	}

	@Test
	public void testPrimaryKeyStaysReadEvenWhenOmitted() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, this.insertedId, new FilterOmit("_id"));

		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(this.insertedId, retrieved.getOid());
	}

	@Test
	public void testGetsAppliesTheSelectionToEveryRow() throws Exception {
		final List<Model> retrieved = ConfigureDb.da.gets(Model.class, new FilterValue("field3"));

		Assertions.assertFalse(retrieved.isEmpty());
		for (final Model item : retrieved) {
			Assertions.assertNotNull(item.getOid());
			Assertions.assertNull(item.field1);
			Assertions.assertNull(item.field2);
		}
	}

	@Test
	public void testUnrestrictedReadIsUnchanged() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, this.insertedId);

		Assertions.assertEquals("value1", retrieved.field1);
		Assertions.assertEquals("value2", retrieved.field2);
		Assertions.assertEquals(42, retrieved.field3);
	}

	// ========== Excluding a relation drops its extra queries ==========

	@Test
	public void testExcludedRelationSkipsItsExtraQuery() throws Exception {
		final TypeManyToOneDocOIDParentIgnore parent = new TypeManyToOneDocOIDParentIgnore();
		parent.data = "parent";
		final ObjectId parentId = ConfigureDb.da.insert(parent).getOid();
		final TypeManyToOneDocOIDChildFFF child = new TypeManyToOneDocOIDChildFFF("child", parentId);
		final ObjectId childId = ConfigureDb.da.insert(child).getOid();

		// Reading the expanded relation costs the main query plus the resolution of the parent.
		final long beforeExpanded = DBAccessMongo.statistic.countFind;
		final TypeManyToOneDocOIDChildExpand expanded = ConfigureDb.da.getById(TypeManyToOneDocOIDChildExpand.class,
				childId);
		final long expandedQueries = DBAccessMongo.statistic.countFind - beforeExpanded;
		Assertions.assertNotNull(expanded.parent);
		Assertions.assertEquals(parentId, expanded.parent.getOid());

		// Omitting the relation field: the parent is not resolved, so its query is not issued.
		final long beforeOmitted = DBAccessMongo.statistic.countFind;
		final TypeManyToOneDocOIDChildExpand omitted = ConfigureDb.da.getById(TypeManyToOneDocOIDChildExpand.class,
				childId, new FilterOmit("parentOid"));
		final long omittedQueries = DBAccessMongo.statistic.countFind - beforeOmitted;

		Assertions.assertNull(omitted.parent);
		Assertions.assertEquals("child", omitted.otherData);
		Assertions.assertTrue(omittedQueries < expandedQueries,
				"expected fewer queries without the relation: " + omittedQueries + " vs " + expandedQueries);
	}
}
