package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.SqlWrapper;
import org.kar.archidata.db.DBEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationSqlStep implements MigrationInterface{
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationSqlStep.class);
	private List<String> actions = new ArrayList<>();
	
	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	@Override
	public boolean applyMigration(DBEntry entry, StringBuilder log) {
		for (int iii=0; iii<actions.size(); iii++) {
			log.append("action [" + iii + "/" + actions.size() + "]\n");
			LOGGER.debug(" >>>> SQL ACTION : {}/{}", iii, actions.size());
			String action = actions.get(iii);
			LOGGER.debug("SQL request: ```{}```", action);
			log.append("SQL: " + action + "\n");
			try {
				SqlWrapper.executeSimpleQuerry(action);
			} catch (SQLException ex) {
				ex.printStackTrace();
				LOGGER.debug("SQL request ERROR: ", ex.getMessage());
				log.append("SQL request ERROR: " + ex.getMessage() + "\n");
				return false;
			} catch (IOException ex) {
				ex.printStackTrace();
				LOGGER.debug("IO request ERROR: ", ex.getMessage());
				log.append("IO request ERROR: " + ex.getMessage() + "\n");
				return false;
			}
			log.append("action [" + iii + "/" + actions.size() + "] ==> DONE\n");
			LOGGER.debug(" >>>> SQL ACTION : {}/{} ==> DONE", iii, actions.size());
		}
		return true;
	}

	@Override
	public boolean revertMigration(DBEntry entry, StringBuilder log) {
		return false;
	}

	public void addAction(String action) {
		actions.add(action);
	}
	public void addClass(Class<?> clazz) throws Exception {
		actions.add(SqlWrapper.createTable(clazz));
	}

}
