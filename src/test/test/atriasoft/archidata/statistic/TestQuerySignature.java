package test.atriasoft.archidata.statistic;

import java.util.List;

import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.statistic.QuerySignature;
import org.atriasoft.archidata.dataAccess.statistic.QuerySignature.Operation;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the normalization of a query into an ESR-ordered {@link QuerySignature}.
 */
public class TestQuerySignature {

	@Test
	public void testValuesAreStrippedOut() {
		final QuerySignature first = QuerySignature.of("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		final QuerySignature second = QuerySignature.of("user", Operation.FIND, Filters.eq("name", "alice"), null,
				null);
		Assertions.assertEquals(first.getKey(), second.getKey(), "same shape, different values => same signature");
		Assertions.assertEquals("user|FIND|E:name|S:|R:", first.getKey());
	}

	@Test
	public void testEqualityFieldsAreSortedAlphabetically() {
		final QuerySignature direct = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.eq("zeta", 1), Filters.eq("alpha", 2)), null, null);
		final QuerySignature reversed = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.eq("alpha", 2), Filters.eq("zeta", 1)), null, null);
		Assertions.assertEquals(direct.getKey(), reversed.getKey(), "equality order is not significant");
		Assertions.assertEquals(List.of("alpha", "zeta"), direct.getEquality());
	}

	@Test
	public void testRangeOperatorsGoToRangeBucket() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.eq("companyId", 7), Filters.gt("age", 18)), null, null);
		Assertions.assertEquals(List.of("companyId"), signature.getEquality());
		Assertions.assertEquals(List.of("age"), signature.getRange());
	}

	@Test
	public void testInIsAnEqualityOperator() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.in("role", "admin", "moderator"), null, null);
		Assertions.assertEquals(List.of("role"), signature.getEquality());
		Assertions.assertEquals(List.of(), signature.getRange());
	}

	@Test
	public void testRangeWinsOverEqualityOnTheSameField() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.gte("age", 18), Filters.lte("age", 60)), null, null);
		Assertions.assertEquals(List.of(), signature.getEquality());
		Assertions.assertEquals(List.of("age"), signature.getRange());
	}

	@Test
	public void testSortKeepsItsOrderAndDirection() {
		final Document sort = new Document("createdAt", -1).append("name", 1);
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND, Filters.eq("companyId", 7), sort,
				null);
		Assertions.assertEquals(List.of("createdAt:-1", "name:1"), signature.getSort());
		Assertions.assertEquals("user|FIND|E:companyId|S:createdAt:-1,name:1|R:", signature.getKey());
	}

	@Test
	public void testSuggestedIndexFollowsEsrOrder() {
		final Document sort = new Document("createdAt", -1);
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.eq("companyId", 7), Filters.eq("active", true), Filters.gt("age", 18)), sort, null);
		Assertions.assertEquals("{\"active\": 1, \"companyId\": 1, \"createdAt\": -1, \"age\": 1}",
				signature.getSuggestedIndex());
	}

	@Test
	public void testFieldBothSortedAndRangedAppearsOnceInTheIndex() {
		// sort("age") + gt("age", 18): "age" lands in S and in R, but an index key can only name a
		// field once -- {"age": -1, "age": 1} would be rejected by MongoDB.
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND, Filters.gt("age", 18),
				new Document("age", -1), null);
		Assertions.assertEquals("{\"age\": -1}", signature.getSuggestedIndex());
	}

	@Test
	public void testFieldBothEqualAndSortedAppearsOnceInTheIndex() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND, Filters.eq("name", "bob"),
				new Document("name", 1).append("age", -1), null);
		Assertions.assertEquals("{\"name\": 1, \"age\": -1}", signature.getSuggestedIndex());
	}

	@Test
	public void testFullScanHasNoSuggestedIndex() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND, null, null, null);
		Assertions.assertNull(signature.getSuggestedIndex());
		Assertions.assertEquals("user|FIND|E:|S:|R:", signature.getKey());
	}

	@Test
	public void testOrIsFlagged() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.or(Filters.eq("name", "bob"), Filters.eq("login", "bob")), null, null);
		Assertions.assertTrue(signature.isContainsOr(), "an $or cannot be served by a single compound index");
		Assertions.assertEquals(List.of("login", "name"), signature.getEquality());
	}

	@Test
	public void testAndIsNotFlaggedAsOr() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND,
				Filters.and(Filters.eq("name", "bob"), Filters.eq("login", "bob")), null, null);
		Assertions.assertFalse(signature.isContainsOr());
	}

	@Test
	public void testElemMatchExpandsToDottedPaths() {
		final QuerySignature signature = QuerySignature.of("order", Operation.FIND,
				Filters.elemMatch("items", Filters.and(Filters.eq("sku", "A1"), Filters.gt("qty", 2))), null, null);
		Assertions.assertEquals(List.of("items.sku"), signature.getEquality());
		Assertions.assertEquals(List.of("items.qty"), signature.getRange());
	}

	@Test
	public void testOperationIsPartOfTheSignature() {
		final QuerySignature find = QuerySignature.of("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		final QuerySignature count = QuerySignature.of("user", Operation.COUNT, Filters.eq("name", "bob"), null, null);
		Assertions.assertNotEquals(find.getKey(), count.getKey());
	}

	@Test
	public void testCollectionIsPartOfTheSignature() {
		final QuerySignature user = QuerySignature.of("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		final QuerySignature group = QuerySignature.of("group", Operation.FIND, Filters.eq("name", "bob"), null, null);
		Assertions.assertNotEquals(user.getKey(), group.getKey());
	}

	@Test
	public void testSoftDeleteFieldIsReportedButKeptOutOfTheIndex() {
		final QuerySignature signature = QuerySignature.of("user", Operation.FIND, Filters.eq("companyId", 7), null,
				"deleted");
		Assertions.assertEquals("deleted", signature.getSoftDeleteField());
		Assertions.assertEquals("{\"companyId\": 1}", signature.getSuggestedIndex());
	}
}
