package test.atriasoft.archidata.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.backup.BackupSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestBackupSnapshotContent {

	@Test
	void theMapCannotBeChanged() {
		final Map<String, byte[]> given = new HashMap<>();
		given.put("one", new byte[] { 1 });
		final BackupSnapshot snapshot = new BackupSnapshot(given);

		given.put("two", new byte[] { 2 });

		Assertions.assertEquals(1, snapshot.size());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.collections().clear());
	}

	@Test
	void theOrderOfTheCollectionsIsKept() {
		final Map<String, byte[]> given = new LinkedHashMap<>();
		final List<String> names = List.of("z", "a", "m");
		for (final String name : names) {
			given.put(name, new byte[0]);
		}
		Assertions.assertEquals(names, new ArrayList<>(new BackupSnapshot(given).collections().keySet()));
	}
}
