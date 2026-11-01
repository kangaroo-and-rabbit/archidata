package test.atriasoft.archidata.migration;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.migration.MigrationInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestMigrationEngineList {

	/** The engine keeps its own list: add() used to throw on an immutable list, and to write in the list of the caller. */
	@Test
	void addDoesNotTouchTheListGiven() {
		final List<MigrationInterface> given = new ArrayList<>();
		final MigrationEngine engine = new MigrationEngine(given, null);
		engine.add(new Migration1());
		Assertions.assertTrue(given.isEmpty());
		Assertions.assertDoesNotThrow(() -> new MigrationEngine(List.of(), null).add(new Migration1()));
	}
}
