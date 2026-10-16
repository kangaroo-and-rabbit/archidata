package test.atriasoft.archidata.dataAccess.options;

import java.io.IOException;

import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.Skip;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-07: {@link Limit} and {@link Skip} are validated and never truncated to an {@code int}. */
class TestLimitSkipBounds {

	public static class Model {
		@Id
		public ObjectId _id = null;
		public String name;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
		final Model model = new Model();
		model.name = "only";
		ConfigureDb.da.insert(model);
	}

	@AfterAll
	static void cleanup() throws IOException {
		Limit.setMaxValue(Limit.DEFAULT_MAX_VALUE);
		ConfigureDb.clear();
	}

	@Test
	void limitRejectsZeroNegativeAndOversized() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit(-1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit(Limit.getMaxValue() + 1));
		// 2^32 used to become limit(0), that is no limit at all.
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit(1L << 32));
		Assertions.assertEquals(Limit.DEFAULT_MAX_VALUE, new Limit(Limit.DEFAULT_MAX_VALUE).getValue());
	}

	@Test
	void limitMaximumIsConfigurable() {
		Limit.setMaxValue(20);
		try {
			Assertions.assertEquals(20, new Limit(20).getValue());
			Assertions.assertThrows(IllegalArgumentException.class, () -> new Limit(21));
		} finally {
			Limit.setMaxValue(Limit.DEFAULT_MAX_VALUE);
		}
		Assertions.assertThrows(IllegalArgumentException.class, () -> Limit.setMaxValue(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> Limit.setMaxValue(1L + Integer.MAX_VALUE));
	}

	@Test
	void skipRejectsValuesBeyondInt() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Skip(-1));
		// 2^31 used to become a negative skip, 2^32 a skip of 0.
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Skip(1L << 31));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Skip(1L << 32));
		Assertions.assertEquals(0, ConfigureDb.da.gets(Model.class, new Skip(Skip.MAX_VALUE), new Limit(1)).size());
		Assertions.assertEquals(1, ConfigureDb.da.gets(Model.class, new Skip(0), new Limit(1)).size());
	}
}
