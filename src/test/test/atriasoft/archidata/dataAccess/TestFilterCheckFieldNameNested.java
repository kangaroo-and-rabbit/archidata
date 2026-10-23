package test.atriasoft.archidata.dataAccess;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** CS-01: {@code Filters.checkFieldName(Class, String)} resolves every segment of a dotted path. */
class TestFilterCheckFieldNameNested {

	/** Embedded POJO with a hidden and an encrypted field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String passwordHash;
		@DataEncrypt
		public String pin;
		public Credentials parent;
	}

	/** Embedded record with a hidden component. */
	public record ApiKey(
			String label,
			@DataNotRead String secret) {}

	/** Embedded POJO without protected field. */
	public static class Plain {
		public String city;
		public Plain next;
	}

	/** Embedded class holding a polymorphic field. */
	public static class Wrapper {
		public String name;
		public Object inner;
	}

	/** Entity whose embedded class holds a polymorphic field: refused when its model is analysed. */
	@Collection("checkFieldNameNestedPoly")
	public static class PolyAccount {
		@Id
		public ObjectId _id = null;
		public String name;
		public Wrapper wrapper;
	}

	/** Abstract entity holding a polymorphic field: its model is checked like a concrete one, and refused. */
	@Collection("checkFieldNameNestedAbstract")
	public abstract static class AbstractPolyAccount {
		@Id
		public ObjectId _id = null;
		public String name;
		public Object payload;
	}

	/** Concrete base class. */
	public static class Base {
		public String label;
	}

	/** Subclass adding a hidden field. */
	public static class Derived extends Base {
		@DataNotRead
		public String secret;
	}

	/** Entity embedding an object, a list, a set and maps of objects. */
	@Collection("checkFieldNameNested")
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public ApiKey key;
		public List<Credentials> history;
		public Set<ApiKey> keys;
		public Map<String, ApiKey> map;
		public Map<String, List<Credentials>> mapOfLists;
		public List<Map<String, ApiKey>> listOfMaps;
		public List<Map<String, Plain>> listOfPlainMaps;
		public Plain plain;
		public List<Plain> plains;
		public Document attributes;
		public Map<String, String> labels;
		public java.math.BigDecimal price;
		public Base base;
		@DataNotRead
		public String token;
	}

	private static void rejected(final String path) {
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.checkFieldName(Account.class, path),
				path);
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> OrderItem.ofClientField(Account.class, path, Order.ASC), path);
	}

	private static void accepted(final String path) {
		Assertions.assertEquals(path, Filters.checkFieldName(Account.class, path));
		Assertions.assertDoesNotThrow(() -> OrderItem.ofClientField(Account.class, path, Order.ASC), path);
	}

	@Test
	void testRootBehavioursAreKept() {
		accepted("name");
		accepted("_id");
		accepted("name.sub");
		rejected("unknown");
		rejected("token");
		rejected("");
		rejected("$where");
	}

	@Test
	void testProtectedSubFieldOfEmbeddedObject() {
		rejected("credentials.passwordHash");
		rejected("credentials.pin");
		rejected("key.secret");
		rejected("credentials.parent.passwordHash");
		rejected("credentials.parent.parent.parent.pin");
	}

	@Test
	void testProtectedSubFieldOfList() {
		rejected("history.passwordHash");
		rejected("history.0.passwordHash");
		rejected("history.12.pin");
		rejected("keys.secret");
		rejected("keys.3.secret");
	}

	@Test
	void testProtectedSubFieldOfMap() {
		rejected("map.someKey.secret");
		rejected("map.0.secret");
		rejected("mapOfLists.k.passwordHash");
		rejected("mapOfLists.k.2.passwordHash");
		rejected("listOfMaps.k.secret");
		rejected("listOfMaps.0.k.secret");
	}

	@Test
	void testUnknownSubField() {
		rejected("credentials.nothing");
		rejected("history.0.nothing");
		rejected("map.someKey.nothing");
		rejected("credentials.0");
	}

	@Test
	void testWholeSubDocumentWithProtectedFields() {
		rejected("credentials");
		rejected("key");
		rejected("history");
		rejected("history.0");
		rejected("keys");
		rejected("map");
		rejected("map.someKey");
		rejected("mapOfLists");
		rejected("mapOfLists.k");
		rejected("listOfMaps");
		// The ambiguity "array index or map key" is refused when the values hold protected fields.
		rejected("listOfMaps.0");
	}

	@Test
	void testVisibleSubFieldsAreAccepted() {
		accepted("credentials.user");
		accepted("credentials.parent.user");
		accepted("key.label");
		accepted("history.user");
		accepted("history.0.user");
		accepted("history.7.parent.user");
		accepted("keys.label");
		accepted("map.someKey.label");
		accepted("map.0.label");
		accepted("mapOfLists.k.user");
		accepted("mapOfLists.k.0.user");
		// "listOfMaps.0.k.label" is ambiguous (index 0, or the key "0"): refused.
		rejected("listOfMaps.0.k.label");
		accepted("listOfMaps.k.label");
	}

	@Test
	void testEmbeddedWithoutProtectedFields() {
		accepted("plain");
		accepted("plain.city");
		accepted("plain.next.next.city");
		accepted("plains");
		accepted("plains.0");
		accepted("plains.0.city");
		accepted("plains.city");
		accepted("listOfPlainMaps");
		accepted("listOfPlainMaps.0.k.city");
		rejected("plain.nothing");
		rejected("plains.0.nothing");
	}

	@Test
	void testFreeFormTypesAcceptTheRestOfThePath() {
		accepted("attributes");
		accepted("attributes.any.thing");
		accepted("labels");
		accepted("labels.any.thing");
	}

	@Test
	void testConcreteJdkClassStaysAPlainValue() {
		accepted("price");
		// What a class archidata does not model holds is not known: nothing below it is safe.
		rejected("price.scale");
		rejected("price.secret");
	}

	@Test
	void testAbstractRootIsCheckedLikeAConcreteOne() {
		// Its fields are checked when the model is analysed: the polymorphic field refuses the whole model.
		final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> Filters.checkFieldName(AbstractPolyAccount.class, "payload.secret"));
		Assertions.assertTrue(ex.getMessage().contains("Cannot analyse the model of AbstractPolyAccount"),
				ex.getMessage());
	}

	@Test
	void testPolymorphicModelIsRefusedAsAWhole() {
		// Object, interfaces, abstract classes and raw collections are refused when the model is analysed
		// (TestDeclaredTypeRefusal): no path of such an entity can be checked.
		final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> Filters.checkFieldName(PolyAccount.class, "name"));
		Assertions.assertTrue(ex.getMessage().contains("Cannot analyse the model of PolyAccount"), ex.getMessage());
	}

	@Test
	void testConcreteBaseClassIsResolvedByItsDeclaredFields() {
		// Only the fields of the declared class are stored: the whole sub-document holds nothing else.
		accepted("base");
		accepted("base.label");
		// A field added by a subclass is unknown to the declared type.
		rejected("base.secret");
	}

	@Test
	void testAbsurdDepthIsRefused() {
		final StringBuilder path = new StringBuilder("credentials");
		for (int iii = 0; iii < 200; iii++) {
			path.append(".parent");
		}
		path.append(".user");
		rejected(path.toString());
	}
}
