package test.atriasoft.archidata.backup;

import java.nio.file.Path;

import org.atriasoft.archidata.backup.BackupEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestBackupChecksumPath {

	@Test
	void theChecksumSitsNextToTheArchive() {
		Assertions.assertEquals(Path.of("/backup/base.tar.gz.sha256"),
				BackupEngine.checksumPath(Path.of("/backup/base.tar.gz")));
	}

	@Test
	void aPathWithoutFileNameIsRefused() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> BackupEngine.checksumPath(Path.of("/")));
	}
}
