package org.kar.archidata.migration;

import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.SqlWrapper;
import org.kar.archidata.db.DBEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationEngine {
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationEngine.class);
	
	// List of order migrations
	private final List<MigrationInterface> datas;
	// initialization of the migration if the DB is not present...
	private MigrationInterface init;
	
	public MigrationEngine() {
		this(new ArrayList<MigrationInterface>(), null);
	}
	public MigrationEngine( List<MigrationInterface> datas, MigrationInterface init) {
		this.datas = datas;
		this.init = init;
	}

	public void add(MigrationInterface migration) {
		datas.add(migration);
	}
	public void setInit(MigrationInterface migration) {
		init = migration;
	}
	/**
	 * Get the current version/migration name
	 * @return String represent the last migration. If null then no migration has been done.
	 */
	public String getCurrentVersion() {
		// TODO: check if the DB exist :
		if (SqlWrapper.isTableExist("migration")) {
			
		}
		
		// check if migration table exist:
		
		// get the current migration
		try {
			List<MigrationModel> data = SqlWrapper.gets(MigrationModel.class, false);
			if (data == null) {
				LOGGER.error("Can not collect the migration table in the DB:{}" );
				return null;
			}
			if (data.size() == 0) {
				LOGGER.error("Fail to Request migration table in the DB: empty size");
				return null;
			}
			return data.get(data.size()-1).name;
		} catch (Exception ex) {
			LOGGER.error("Fail to Request migration table in the DB:{}", ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Get the current migration log generated
	 * @return String represent migration log (separate with \\n)
	 */
	public String getLastLog() {
		try {
			List<MigrationModel> data = SqlWrapper.gets(MigrationModel.class, false);
			if (data == null) {
				LOGGER.error("Can not collect the migration table in the DB:{}");
				return null;
			}
			if (data.size() == 0) {
				LOGGER.error("Fail to Request migration table in the DB: empty size");
				return null;
			}
			return data.get(data.size()-1).log;
		} catch (Exception ex) {
			LOGGER.error("Fail to Request migration table in the DB:{}", ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}
	
	public void migrate(DBEntry entry) {
		String currentVersion = getCurrentVersion();
		List<MigrationInterface> toApply = new ArrayList<>();
		boolean find = false;
		for (int iii=0; iii<this.datas.size(); iii++) {
			if ( ! find) {	
				if (this.datas.get(iii).getName() == currentVersion) {
					find = true;
				}
				continue;
			}
			toApply.add(this.datas.get(iii));
		}
		for (MigrationInterface elem : toApply) {
			migrateSingle(entry, elem);
		}
	}
	public void migrateSingle(DBEntry entry, MigrationInterface elem) {
		LOGGER.info("Revert migration: {} [BEGIN]", elem.getName());
		StringBuilder log = new StringBuilder();
		if (elem.applyMigration(entry, log)) {
			
		}
		LOGGER.info("Revert migration: {} [ END ]", elem.getName());
	}

	public void revertTo(DBEntry entry, String migrationName) {
		String currentVersion = getCurrentVersion();
		List<MigrationInterface> toApply = new ArrayList<>();
		boolean find = false;
		for (int iii=this.datas.size()-1; iii>=0; iii--) {
			if ( ! find) {	
				if (this.datas.get(iii).getName() == currentVersion) {
					find = true;
				}
				continue;
			}
			if (this.datas.get(iii).getName() == currentVersion) {
				break;
			}
			toApply.add(this.datas.get(iii));
		}
		for (MigrationInterface elem : toApply) {
			revertSingle(entry, elem);
		}
	}
	public void revertSingle(DBEntry entry, MigrationInterface elem) {
		LOGGER.info("Revert migration: {} [BEGIN]", elem.getName());
		
		LOGGER.info("Revert migration: {} [ END ]", elem.getName());
	}
}
