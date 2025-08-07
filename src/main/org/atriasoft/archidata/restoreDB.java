
package org.atriasoft.archidata;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.TimeZone;

import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class restoreDB {
	final static Logger LOGGER = LoggerFactory.getLogger(restoreDB.class);

	public static void main(final String[] args) throws IOException, DataAccessException {
		ConfigBaseVariable.dbType = "mongo";
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		LOGGER.info("program arguments length={}", args.length);
		for (final String elem : args) {
			LOGGER.info("    {}", elem);
		}
		if (args.length > 3 || args.length < 1) {
			System.out.println("Usage:");
			System.out.println("    restoreDB fileName [dbDestination] [collection]");
			System.out.println("      - fileName: file to restore (.tar.gz)");
			System.out.println("      - dbDestination: name of the destination DB");
			System.out.println("      - collection: name of a specific collection to restore");
			System.exit(-1);
		}
		final String fileName = args[0];
		final String dbName = args.length >= 2 ? args[1] : ConfigBaseVariable.getDBName();
		// overwrite the dbAccess to access to the correct DB
		ConfigBaseVariable.bdDatabase = dbName;
		final String collection = args.length >= 3 ? args[2] : null;
		final BackupEngine engine = new BackupEngine(Paths.get("./"), dbName, EngineBackupType.JSON_EXTENDED);
		if (!engine.restoreFile(Paths.get(fileName), collection)) {
			System.exit(-1);
		}
	}
}
