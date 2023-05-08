package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.SqlWrapper;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.db.DBConfig;
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
	public MigrationModel getCurrentVersion() {
		if (!SqlWrapper.isTableExist("KAR_migration")) {
			return null;
		}
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
			LOGGER.debug("List of migrations:");
			for (MigrationModel elem : data) {
				LOGGER.debug("    - date={} name={} end{}", elem.modify_date, elem.name, elem.terminated);				
			}
			return data.get(data.size()-1);
		} catch (Exception ex) {
			LOGGER.error("Fail to Request migration table in the DB:{}", ex.getMessage());
			ex.printStackTrace();
		}
		return null;
	}
	
	public void migrate(DBConfig config) throws InterruptedException, IOException {
		LOGGER.info("Execute migration ... [BEGIN]");

		// STEP 1: Check the DB exist:
		LOGGER.info("Verify existance of '{}'", config.getDbName());
		boolean exist = SqlWrapper.isDBExist(config.getDbName());
		if(!exist) {
			LOGGER.warn("DB: '{}' DOES NOT EXIST ==> create one", config.getDbName());
			// create the local DB:
			SqlWrapper.createDB(config.getDbName());
		}
		exist = SqlWrapper.isDBExist(config.getDbName());
		while (!exist) {
			LOGGER.error("DB: '{}' DOES NOT EXIST after trying to create one ", config.getDbName());
			LOGGER.error("Waiting administrator create a new one, we check after 30 seconds...");
			Thread.sleep(30000);
			exist = SqlWrapper.isDBExist(config.getDbName());
		}
		LOGGER.info("DB '{}' exist.", config.getDbName());
		// STEP 2: Check migration table exist:
		LOGGER.info("Verify existance of migration table '{}'", "KAR_migration");
		exist = SqlWrapper.isTableExist("KAR_migration");
		if (!exist) {
			// create the table:
			List<String> sqlQuery;
			try {
				sqlQuery = SqlWrapper.createTable(MigrationModel.class, false);
			} catch (Exception ex) {
				ex.printStackTrace();
				while (true) {
					LOGGER.error("Fail to create the local DB SQL model for migaration ==> wait administrator interventions");
					Thread.sleep(60*60*1000);
				}
			}
			LOGGER.info("Create Table with : {}", sqlQuery.get(0));
			try {
				SqlWrapper.executeQuerry(sqlQuery.get(0));//.replace("`", "").replace("\t", ""));
			} catch (SQLException | IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
				while (true) {
					LOGGER.error("Fail to create the local DB model for migaration ==> wait administrator interventions");
					Thread.sleep(60*60*1000);
				}
			}
		}
		MigrationModel currentVersion = getCurrentVersion();
		List<MigrationInterface> toApply = new ArrayList<>();
		if (currentVersion == null) {
			//This is a first migration
			LOGGER.info("First installation of the system ==> Create the DB");
			if (this.init == null) {
				toApply = this.datas;
			} else {
				toApply.add(this.init);
			}
		} else {
			if (currentVersion.terminated == false) {
				while(true) {
					LOGGER.error("An error occured in the last migration: '{}' defect @{}/{} ==> wait administrator interventions", currentVersion.name , currentVersion.stepId, currentVersion.count);
					Thread.sleep(60*60*1000);
				}
			}
			LOGGER.info("Upgrade the system Current version: {}", currentVersion);
			boolean find = false;
			for (int iii=0; iii<this.datas.size(); iii++) {
				if ( ! find) {	
					if (this.datas.get(iii).getName() == currentVersion.name) {
						find = true;
					}
					continue;
				}
				toApply.add(this.datas.get(iii));
			}
		}
		DBEntry entry = DBEntry.createInterface(config);
		int id = 0;
		int count = toApply.size();
		for (MigrationInterface elem : toApply) {
			migrateSingle(entry, elem, id, count);
		}
		LOGGER.info("Execute migration ... [ END ]");
	}
	public void migrateSingle(DBEntry entry, MigrationInterface elem, int id, int count) {
		LOGGER.info("Migrate: [{}/{}] {} [BEGIN]", id, count, elem.getName());
		StringBuilder log = new StringBuilder();
		log.append("Start migration");
		MigrationModel migrationResult = new MigrationModel();
	    migrationResult.name = elem.getName();
		migrationResult.stepId = 0;
		migrationResult.terminated = false;
	    migrationResult.count = elem.getNumberOfStep();
	    migrationResult.log = log.toString();
	    try {
	    	migrationResult = SqlWrapper.insert(migrationResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (elem.applyMigration(entry, log, migrationResult)) {
			migrationResult.terminated = true;
		    try {
		    	SqlWrapper.update(migrationResult, migrationResult.id, List.of("terminated"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		LOGGER.info("Migrate: [{}/{}] {} [ END ]", id, count, elem.getName());
	}

	public void revertTo(DBEntry entry, String migrationName) {
		MigrationModel currentVersion = getCurrentVersion();
		List<MigrationInterface> toApply = new ArrayList<>();
		boolean find = false;
		for (int iii=this.datas.size()-1; iii>=0; iii--) {
			if ( ! find) {	
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
		int id = 0;
		int count = toApply.size();
		for (MigrationInterface elem : toApply) {
			revertSingle(entry, elem, id, count);
		}
	}
	public void revertSingle(DBEntry entry, MigrationInterface elem, int id, int count) {
		LOGGER.info("Revert migration: {} [BEGIN]", elem.getName());
		
		LOGGER.info("Revert migration: {} [ END ]", elem.getName());
	}
}
