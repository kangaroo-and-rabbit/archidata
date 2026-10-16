package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.atriasoft.archidata.db.BsonCodecs;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-08: no NoSQL injection through eq, field names or regex. */
public class TestFiltersInjection {

	/** Entity with a hidden field. */
	@Collection("filtersInjection")
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String login;
		@DataNotRead
		public String token;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		for (final String login : List.of("alice", "bob", "a.b")) {
			final Account account = new Account();
			account.login = login;
			account.token = "t-" + login;
			ConfigureDb.da.insert(account);
		}
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Test
	public void testEqIsExplicit() throws Exception {
		Assertions.assertEquals("{\"login\": {\"$eq\": \"bob\"}}",
				BsonCodecs.toBsonDocument(Filters.eq("login", "bob")).toJson());
		Assertions.assertEquals(1, ConfigureDb.da.count(Account.class, new Condition(Filters.eq("login", "bob"))));
		// A value parsed from an untyped client JSON: {"$ne": null} must not match every document.
		final Object injected = new Document("$ne", null);
		Assertions.assertEquals(0, ConfigureDb.da.count(Account.class, new Condition(Filters.eq("login", injected))));
		Assertions.assertEquals(0,
				ConfigureDb.da.count(Account.class, new Condition(Filters.eq("login", Map.of("$gt", "")))));
		// Method references go through the same path.
		Assertions.assertEquals("{\"_id\": {\"$eq\": 1}}", BsonCodecs.toBsonDocument(Filters.eq(1)).toJson());
	}

	@Test
	public void testFieldNamesAreChecked() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.eq("$where", "1 == 1"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.gt("$expr", 1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.exists("lo\0gin"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.in("", "a"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new OrderItem("$natural", Order.ASC));
		Assertions.assertEquals("login", Filters.checkFieldName(Account.class, "login"));
		Assertions.assertEquals("login.sub", Filters.checkFieldName(Account.class, "login.sub"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.checkFieldName(Account.class, "unknown"));
		// A hidden field cannot be used as a client filter or sort (oracle on its value).
		Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.checkFieldName(Account.class, "token"));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> OrderItem.ofClientField(Account.class, "token", Order.ASC));
		final List<Account> sorted = ConfigureDb.da.gets(Account.class,
				new OrderBy(OrderItem.ofClientField(Account.class, "login", Order.DESC)));
		Assertions.assertEquals("bob", sorted.get(0).login);
	}

	@Test
	public void testRegexLiteralQuotesTheText() throws Exception {
		// "a.b" as a regex also matches "alice"? no, but "." matches any char: check "." alone.
		Assertions.assertEquals(3, ConfigureDb.da.count(Account.class, new Condition(Filters.regex("login", "."))));
		Assertions.assertEquals(1,
				ConfigureDb.da.count(Account.class, new Condition(Filters.regexLiteral("login", "."))));
		Assertions.assertEquals(1,
				ConfigureDb.da.count(Account.class, new Condition(Filters.regexLiteral("login", "A.B", "i"))));
		Assertions.assertEquals(0,
				ConfigureDb.da.count(Account.class, new Condition(Filters.regexLiteral("login", "(a+)+$"))));
	}
}
