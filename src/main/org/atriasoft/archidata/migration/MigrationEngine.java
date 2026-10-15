package org.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.model.Migration;
import org.atriasoft.archidata.migration.model.MigrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;

/**
 * Engine that manages and executes database migrations in order.
 *
 * <p>
 * Supports initial database setup and sequential (forward-only) migration application. Reverting
 * is not supported: see {@link #revertSingle}.
 * Tracks migration progress and logs in the database via {@link Migration} records.
 * </p>
 */
public class MigrationEngine {
	static final Logger LOGGER = LoggerFactory.getLogger(MigrationEngine.class);

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
	 * @param da the database access interface
	 * @return Model represent the last migration. If null then no migration has been done.
	 * @throws MigrationException if the migration table cannot be retrieved */
	public Migration getCurrentVersion(final DBAccessMongo da) throws MigrationException {
		try {
			List<Migration> data = null;
			try {
				data = da.gets(Migration.class, new ReadAllColumn());
			} catch (final Exception e) {
				// Previous version does not have the same timeCode...
				data = da.gets(Migration.class);
			}
			if (data == null) {
				LOGGER.error("Can not collect the migration table in the DB:{}");
				return null;
			}
			if (data.size() == 0) {
				LOGGER.error("Fail to Request migration table in the DB: empty size");
				return null;
			}
			LOGGER.info("List of migrations:");
			for (final Migration elem : data) {
				LOGGER.info("    - date={} name={} end={}", elem.getUpdatedAt(), elem.name, elem.terminated);
			}
			return data.get(data.size() - 1);
		} catch (final Exception ex) {
			LOGGER.error("Fail to Request migration table in the DB: {}", ex.getMessage(), ex);
		}
		throw new MigrationException("Can not retreive Migration model");
	}

	/** Process the automatic migration of the system The function wait the Administrator intervention to correct the bug.
	 * @param config Database connection configuration for the migration.
	 * @throws InterruptedException user interrupt the migration */
	public void migrateWaitAdmin(final DbConfig config) throws InterruptedException {
		try {
			migrateErrorThrow(config);
		} catch (final Exception ex) {
			LOGGER.error("Migration failed: {}", ex.getMessage(), ex);
			while (true) {
				LOGGER.error("ERROR: {}", ex.getMessage());
				LOGGER.error("========================================================================");
				LOGGER.error("== Fail to migrate ==> wait administrator interventions               ==");
				LOGGER.error("========================================================================");
				Thread.sleep(60 * 60 * 1000);
			}
		}
	}

	private void listAvailableMigration() throws MigrationException {
		// check the integrity of the migrations:
		LOGGER.info("List of availlable Migration: ");
		for (final MigrationInterface elem : this.datas) {
			if (elem == null) {
				LOGGER.info("  - null");
				throw new MigrationException("Add a null migration");
			}
			LOGGER.info("  - {}", elem.getName());
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
	}

	/** Process the automatic migration of the system
	 * @param config Database connection configuration for the migration
	 * @throws MigrationException Error if access on the DB */
	public void migrateErrorThrow(final DbConfig config) throws MigrationException {
		LOGGER.info("Execute migration ... [BEGIN]");
		listAvailableMigration();
		try (final DBAccessMongo da = DBAccessMongo.createInterface(config)) {
			// STEP 2: Check migration table exist:
			LOGGER.info("Verify existance of migration table '{}'", "KAR_migration");
			final Migration currentVersion = getCurrentVersion(da);
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
					throw new MigrationException("An error occured in the last migration: '" + currentVersion.name
							+ "' defect @" + currentVersion.stepId + "/" + currentVersion.count);
				}
				LOGGER.info("Upgrade the system Current version: {}", currentVersion.name);
				boolean find = this.init != null && this.init.getName().equals(currentVersion.name);
				if (find) {
					toApply = this.datas;
				} else {
					LOGGER.info(" ===> Check what must be apply:");
					for (final MigrationInterface elem : this.datas) {
						LOGGER.info("     - {}", elem.getName());
						if (!find) {
							if (currentVersion.name.equals(elem.getName())) {
								LOGGER.info("        == current version");
								find = true;
							}
							continue;
						}
						LOGGER.info("        ++ add ");
						toApply.add(elem);
					}
					if (!find) {
						// Applying nothing would start the application on a schema of unknown state, and
						// applying everything would replay migrations already done: refuse to go on.
						LOGGER.error("The last recorded migration '{}' is neither the initialization nor a registered"
								+ " migration (renamed or removed?): refuse to migrate", currentVersion.name);
						throw new MigrationException("Unknown last migration '" + currentVersion.name
								+ "' in KAR_migration: it matches neither the initialization nor any registered"
								+ " migration (a deployed migration has been renamed or removed). Restore its name"
								+ " (getName()) in the migration list.");
					}
				}
			}
			final int id = 0;
			final int count = toApply.size();
			for (final MigrationInterface elem : toApply) {
				migrateSingle(da, elem, id, count);
			}
			if (needPlaceholder) {
				if (this.datas.size() == 0) {
					// No placeholder needed, the model have no migration in the current version...
				} else {
					// we insert a placeholder to simulate the last migration is well done.
					final String placeholderName = this.datas.get(this.datas.size() - 1).getName();
					Migration migrationResult = new Migration();
					migrationResult.name = placeholderName;
					migrationResult.stepId = 0;
					migrationResult.terminated = true;
					migrationResult.count = 0;
					migrationResult.logs.add(new MigrationMessage(0, "Place-holder for first initialization"));
					try {
						migrationResult = da.insert(migrationResult);
					} catch (final Exception e) {
						LOGGER.error("Failed to insert migration placeholder: {}", e.getMessage(), e);
					}
				}
			}
			LOGGER.info("Execute migration ... [ END ]");
		} catch (final InternalServerErrorException e) {
			LOGGER.error("Internal server error during migration: {}", e.getMessage(), e);
			throw new MigrationException("Internal server error during migration: " + e.getMessage());
		} catch (final IOException e) {
			LOGGER.error("IO error during migration: {}", e.getMessage(), e);
			throw new MigrationException("IO error during migration: " + e.getMessage());
		}
	}

	/**
	 * Executes a single migration step and updates the migration log in the database.
	 *
	 * @param da the database access interface
	 * @param elem the migration to apply
	 * @param id the current migration index
	 * @param count the total number of migrations to apply
	 * @throws MigrationException if the migration step fails
	 */
	public void migrateSingle(final DBAccessMongo da, final MigrationInterface elem, final int id, final int count)
			throws MigrationException {
		LOGGER.info("---------------------------------------------------------");
		LOGGER.info("-- Migrate: [{}/{}] {} [BEGIN]", id, count, elem.getName());
		LOGGER.info("---------------------------------------------------------");
		Migration migrationResult = new Migration();
		migrationResult.name = elem.getName();
		migrationResult.stepId = 0;
		migrationResult.terminated = false;
		migrationResult.logs.add(new MigrationMessage(0, "Start migration"));
		try {
			migrationResult.count = elem.getNumberOfStep();
		} catch (final Exception e) {
			LOGGER.error("Failed to get number of migration steps: {}", e.getMessage(), e);
			throw new MigrationException(
					"Fail to get number of migration step (maybe generation fail): " + e.getLocalizedMessage());
		}
		try {
			migrationResult = da.insert(migrationResult);
		} catch (final Exception e) {
			LOGGER.error("Failed to insert migration log: {}", e.getMessage(), e);
			throw new MigrationException(
					"Fail to insert migration Log in the migration table: " + e.getLocalizedMessage());
		}
		boolean ret = true;
		try {
			ret = elem.applyMigration(da, migrationResult);
		} catch (final Exception e) {
			migrationResult.logs.add(new MigrationMessage(0, "Fail in the migration apply "));
			migrationResult.logs.add(new MigrationMessage(0, e.getLocalizedMessage()));
			LOGGER.error("Migration apply failed: {}", e.getMessage(), e);
			try {
				da.updateById(migrationResult, migrationResult.getId(), new FilterValue("logs"));
			} catch (final Exception updateException) {
				LOGGER.error("Failed to update migration log (exception): {}", updateException.getMessage(),
						updateException);
			}
			throw new MigrationException("Migration fail: '" + migrationResult.name + "' defect @"
					+ migrationResult.stepId + "/" + migrationResult.count);
		}
		if (ret) {
			migrationResult.terminated = true;
			try {
				da.updateById(migrationResult, migrationResult.getId(), new FilterValue("terminated"));
			} catch (final Exception e) {
				LOGGER.error("Failed to update migration log (terminated): {}", e.getMessage(), e);
				throw new MigrationException(
						"Fail to update migration Log in the migration table: " + e.getLocalizedMessage());
			}
		} else {
			try {
				migrationResult.logs.add(new MigrationMessage(0, "Fail in the migration engine..."));
				da.updateById(migrationResult, migrationResult.getId(), new FilterValue("logs"));
			} catch (final Exception e) {
				LOGGER.error("Failed to update migration log (failure): {}", e.getMessage(), e);
				throw new MigrationException("Fail to update migration Log in the migration table: "
						+ e.getLocalizedMessage() + " WITH: An error occured in the migration (OUTSIDE detection): '"
						+ migrationResult.name + "' defect @" + migrationResult.stepId + "/" + migrationResult.count);
			}
			throw new MigrationException("An error occured in the migration (OUTSIDE detection): '"
					+ migrationResult.name + "' defect @" + migrationResult.stepId + "/" + migrationResult.count);
		}
		LOGGER.info("Migrate: [{}/{}] {} [ END ]", id, count, elem.getName());
	}

	/**
	 * Reverts migrations from the current version back to the specified migration name.
	 *
	 * <p>
	 * <b>Not supported:</b> {@link MigrationInterface} has no reverse operation, so every migration
	 * that would have to be reverted makes {@link #revertSingle} throw an
	 * {@link UnsupportedOperationException}. Write a new forward migration instead. The method only
	 * returns normally when there is nothing to revert.
	 * </p>
	 *
	 * @param da the database access interface
	 * @param migrationName the target migration name to revert to (exclusive)
	 * @throws MigrationException if retrieving the current version fails
	 * @throws UnsupportedOperationException if at least one migration would have to be reverted
	 */
	public void revertTo(final DBAccessMongo da, final String migrationName) throws MigrationException {
		final Migration currentVersion = getCurrentVersion(da);
		final List<MigrationInterface> toApply = new ArrayList<>();
		boolean find = false;
		for (int iii = this.datas.size() - 1; iii >= 0; iii--) {
			if (!find) {
				if (this.datas.get(iii).getName().equals(currentVersion.name)) {
					find = true;
				}
				continue;
			}
			if (this.datas.get(iii).getName().equals(migrationName)) {
				break;
			}
			toApply.add(this.datas.get(iii));
		}
		final int id = 0;
		final int count = toApply.size();
		for (final MigrationInterface elem : toApply) {
			revertSingle(da, elem, id, count);
		}
	}

	/**
	 * Reverts a single migration step: <b>not supported</b>.
	 *
	 * <p>
	 * {@link MigrationInterface} only describes the forward operation ({@code applyMigration}); there
	 * is no reverse action to run, so this method always throws. Undo a migration by writing a new
	 * forward migration.
	 * </p>
	 *
	 * @param da the database access interface
	 * @param elem the migration to revert
	 * @param id the current revert index
	 * @param count the total number of migrations to revert
	 * @throws UnsupportedOperationException always
	 */
	public void revertSingle(final DBAccessMongo da, final MigrationInterface elem, final int id, final int count) {
		LOGGER.error("Revert migration: {} is not supported (no reverse operation in MigrationInterface)",
				elem.getName());
		throw new UnsupportedOperationException("Reverting a migration is not supported ('" + elem.getName()
				+ "'): MigrationInterface has no reverse operation, write a new forward migration instead");
	}
}
