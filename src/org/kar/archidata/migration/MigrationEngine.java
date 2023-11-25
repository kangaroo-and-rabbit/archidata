package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.db.DBConfig;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.migration.model.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationEngine {
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationEngine.class);

	// List of order migrations
	private final List<MigrationInterface> datas;
	// initialization of the migration if the DB is not present...
	private MigrationInterface init;

	/** Migration engine constructor (empty). */
	public MigrationEngine() {
		this(new ArrayList<>(), null);
	}

	/** Migration engine constructor (specific mode).
	 * @param datas All the migration ordered.
	 * @param init Initialization migration model. */
	public MigrationEngine(final List<MigrationInterface> datas, final MigrationInterface init) {
		this.datas = datas;
		this.init = init;
	}

	/** Add a Migration in the list
	 * @param migration Migration to add. */
	public void add(final MigrationInterface migration) {
		this.datas.add(migration);
	}

	/** Set first initialization class
	 * @param migration migration class for first init. */
	public void setInit(final MigrationInterface migration) {
		this.init = migration;
	}

	/** Get the current version/migration name
	 * @return Model represent the last migration. If null then no migration has been done. */
	public Migration getCurrentVersion() {
		if (!DataAccess.isTableExist("KAR_migration")) {
			return null;
		}
		try {
			final List<Migration> data = DataAccess.gets(Migration.class, new QueryOptions(QueryOptions.READ_ALL_COLOMN));
			if (data == null) {
				LOGGER.error("Can not collect the migration table in the DB:{}");
				return null;
			}
			if (data.size() == 0) {
				LOGGER.error("Fail to Request migration table in the DB: empty size");
				return null;
			}
			LOGGER.debug("List of migrations:");
			for (final Migration elem : data) {
				LOGGER.debug("    - date={} name={} end={}", elem.updatedAt, elem.name, elem.terminated);
			}
			return data.get(data.size() - 1);
		} catch (final Exception ex) {
			LOGGER.error("Fail to Request migration table in the DB:{}", ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}

	/** Process the automatic migration of the system The function wait the Administrator intervention to correct the bug.
	 * @param config SQL connection for the migration.
	 * @throws InterruptedException user interrupt the migration */
	public void migrateWaitAdmin(final DBConfig config) throws InterruptedException {
		try {
			migrateErrorThrow(config);
		} catch (final Exception ex) {
			ex.printStackTrace();
			while (true) {
				LOGGER.error("ERROR: {}", ex.getMessage());
				LOGGER.error("========================================================================");
				LOGGER.error("== Fail to migrate ==> wait administrator interventions               ==");
				LOGGER.error("========================================================================");
				Thread.sleep(60 * 60 * 1000);
			}
		}
	}

	/** Process the automatic migration of the system
	 * @param config SQL connection for the migration
	 * @throws IOException Error if access on the DB */
	public void migrateErrorThrow(final DBConfig config) throws MigrationException {
		LOGGER.info("Execute migration ... [BEGIN]");
		// check the integrity of the migrations:
		for (final MigrationInterface elem : this.datas) {
			if (elem == null) {
				throw new MigrationException("Add a null migration");
			}
			if (elem == this.init) {
				throw new MigrationException("Add a migration that is the initialization migration");
			}
			if (this.init != null && elem.getName().equals(this.init.getName())) {
				throw new MigrationException("Two migration have the same name as initilaisation: " + elem.getName());
			}
			for (final MigrationInterface elemCheck : this.datas) {
				if (elem == elemCheck) {
					continue;
				}
				if (elem.getName().equals(elemCheck.getName())) {
					throw new MigrationException("Two migration have the same name...: " + elem.getName());
				}
			}
		}

		// STEP 1: Check the DB exist:
		LOGGER.info("Verify existance of '{}'", config.getDbName());
		boolean exist = DataAccess.isDBExist(config.getDbName());
		if (!exist) {
			LOGGER.warn("DB: '{}' DOES NOT EXIST ==> create one", config.getDbName());
			// create the local DB:
			DataAccess.createDB(config.getDbName());
		}
		exist = DataAccess.isDBExist(config.getDbName());
		while (!exist) {
			LOGGER.error("DB: '{}' DOES NOT EXIST after trying to create one ", config.getDbName());
			LOGGER.error("Waiting administrator create a new one, we check after 30 seconds...");
			try {
				Thread.sleep(30000);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			exist = DataAccess.isDBExist(config.getDbName());
		}
		LOGGER.info("DB '{}' exist.", config.getDbName());
		// STEP 2: Check migration table exist:
		LOGGER.info("Verify existance of migration table '{}'", "KAR_migration");
		exist = DataAccess.isTableExist("KAR_migration");
		if (!exist) {
			LOGGER.info("'{}' Does not exist create a new one...", "KAR_migration");
			// create the table:
			List<String> sqlQuery;
			try {
				sqlQuery = DataFactory.createTable(Migration.class);
			} catch (final Exception ex) {
				ex.printStackTrace();
				throw new MigrationException("Fail to create the local DB SQL model for migaration ==> wait administrator interventions");
			}
			LOGGER.info("Create Table with : {}", sqlQuery.get(0));
			try {
				DataAccess.executeQuerry(sqlQuery.get(0));
			} catch (SQLException | IOException ex) {
				ex.printStackTrace();
				throw new MigrationException("Fail to create the local DB model for migaration ==> wait administrator interventions");
			}
		}
		final Migration currentVersion = getCurrentVersion();
		List<MigrationInterface> toApply = new ArrayList<>();
		boolean needPlaceholder = false;
		if (currentVersion == null) {
			// This is a first migration
			LOGGER.info("First installation of the system ==> Create the DB");
			if (this.init == null) {
				// No initialization class ==> manage a historical creation mode...
				toApply = this.datas;
			} else {
				// Select Initialization class if it exist
				toApply.add(this.init);
				needPlaceholder = true;
			}
		} else {
			if (!currentVersion.terminated) {
				throw new MigrationException("An error occured in the last migration: '" + currentVersion.name + "' defect @" + currentVersion.stepId + "/" + currentVersion.count);
			}
			LOGGER.info("Upgrade the system Current version: {}", currentVersion.name);
			boolean find = this.init != null && this.init.getName() == currentVersion.name;
			if (currentVersion.name.equals(this.init.getName())) {
				toApply = this.datas;
			} else {
				for (int iii = 0; iii < this.datas.size(); iii++) {
					if (!find) {
						if (this.datas.get(iii).getName() == currentVersion.name) {
							find = true;
						}
						continue;
					}
					toApply.add(this.datas.get(iii));
				}
			}
		}
		DBEntry entry;
		try {
			entry = DBEntry.createInterface(config);
			final int id = 0;
			final int count = toApply.size();
			for (final MigrationInterface elem : toApply) {
				migrateSingle(entry, elem, id, count);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			throw new MigrationException("An error occured in the migration (can not access to the DB): '" + currentVersion.name + "' defect @" + currentVersion.stepId + "/" + currentVersion.count);
		}
		if (needPlaceholder) {
			if (this.datas.size() == 0) {
				// No placeholder needed, the model have no migration in the current version...
			} else {
				// we insert a placeholder to simulate the last migration is well done.
				final String placeholderName = this.datas.get(this.datas.size() - 1).getName();
				Migration migrationResult = new Migration();
				migrationResult.id = 1000L;
				migrationResult.name = placeholderName;
				migrationResult.stepId = 0;
				migrationResult.terminated = true;
				migrationResult.count = 0;
				migrationResult.log = "Place-holder for first initialization";
				try {
					migrationResult = DataAccess.insert(migrationResult);
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		LOGGER.info("Execute migration ... [ END ]");
	}

	public void migrateSingle(final DBEntry entry, final MigrationInterface elem, final int id, final int count) throws MigrationException {
		LOGGER.info("---------------------------------------------------------");
		LOGGER.info("-- Migrate: [{}/{}] {} [BEGIN]", id, count, elem.getName());
		LOGGER.info("---------------------------------------------------------");
		final StringBuilder log = new StringBuilder();
		log.append("Start migration\n");
		Migration migrationResult = new Migration();
		migrationResult.name = elem.getName();
		migrationResult.stepId = 0;
		migrationResult.terminated = false;
		migrationResult.count = elem.getNumberOfStep();
		migrationResult.log = log.toString();
		try {
			migrationResult = DataAccess.insert(migrationResult);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean ret = true;
		try {
			ret = elem.applyMigration(entry, log, migrationResult);
		} catch (final Exception e) {
			log.append("\nFail in the migration apply ");
			log.append(e.getLocalizedMessage());
			e.printStackTrace();
			throw new MigrationException("Migration fial: '" + migrationResult.name + "' defect @" + migrationResult.stepId + "/" + migrationResult.count);
		}
		if (ret) {
			migrationResult.terminated = true;
			try {
				DataAccess.update(migrationResult, migrationResult.id, List.of("terminated"));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				log.append("Fail in the migration engine...");
				migrationResult.log = log.toString();
				DataAccess.update(migrationResult, migrationResult.id, List.of("log"));
			} catch (final Exception e) {
				e.printStackTrace();
			}
			throw new MigrationException("An error occured in the migration (OUTSIDE detection): '" + migrationResult.name + "' defect @" + migrationResult.stepId + "/" + migrationResult.count);
		}
		LOGGER.info("Migrate: [{}/{}] {} [ END ]", id, count, elem.getName());
	}

	public void revertTo(final DBEntry entry, final String migrationName) {
		final Migration currentVersion = getCurrentVersion();
		final List<MigrationInterface> toApply = new ArrayList<>();
		boolean find = false;
		for (int iii = this.datas.size() - 1; iii >= 0; iii--) {
			if (!find) {
				if (this.datas.get(iii).getName() == currentVersion.name) {
					find = true;
				}
				continue;
			}
			if (this.datas.get(iii).getName() == currentVersion.name) {
				break;
			}
			toApply.add(this.datas.get(iii));
		}
		final int id = 0;
		final int count = toApply.size();
		for (final MigrationInterface elem : toApply) {
			revertSingle(entry, elem, id, count);
		}
	}

	public void revertSingle(final DBEntry entry, final MigrationInterface elem, final int id, final int count) {
		LOGGER.info("Revert migration: {} [BEGIN]", elem.getName());

		LOGGER.info("Revert migration: {} [ END ]", elem.getName());
	}
}
