package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.migration.model.Migration;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record Action(String action, List<String> filterDB) {
	public Action(final String action) {
		this(action, List.of());
	}

	public Action(final String action, final String filterDB) {
		this(action, List.of(filterDB));
	}
}

public class MigrationSqlStep implements MigrationInterface {
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationSqlStep.class);
	private final List<Action> actions = new ArrayList<>();

	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	public void display() {
		for (int iii = 0; iii < this.actions.size(); iii++) {
			final Action action = this.actions.get(iii);
			LOGGER.info(" >>>> SQL ACTION : {}/{} ==> filter='{}'\n{}", iii, this.actions.size(), action.filterDB(), action.action());
		}
	}

	@Override
	public boolean applyMigration(final DBEntry entry, final StringBuilder log, final Migration model) {
		for (int iii = 0; iii < this.actions.size(); iii++) {
			log.append("action [" + (iii + 1) + "/" + this.actions.size() + "]\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{}", iii + 1, this.actions.size());
			final Action action = this.actions.get(iii);

			LOGGER.info("SQL request: ```{}``` on '{}' current={}", action.action(), action.filterDB(), ConfigBaseVariable.getDBType());
			log.append("SQL: " + action.action() + " on " + action.filterDB() + "\n");
			boolean isValid = true;
			if (action.filterDB() != null && action.filterDB().size() > 0) {
				isValid = false;
				for (final String elem : action.filterDB()) {
					if (ConfigBaseVariable.getDBType().equals(elem)) {
						isValid = true;
					}
				}
			}
			if (!isValid) {
				log.append("==> Skip (DB is not compatible: " + ConfigBaseVariable.getDBType() + ")\n");
				LOGGER.info(" >>>> SQL ACTION : {}/{} ==> SKIP", iii + 1, this.actions.size());
				continue;
			}
			try {
				DataAccess.executeQuerry(action.action());
			} catch (SQLException | IOException ex) {
				ex.printStackTrace();
				LOGGER.info("SQL request ERROR: ", ex.getMessage());
				log.append("SQL request ERROR: " + ex.getMessage() + "\n");
				model.stepId = iii + 1;
				model.log = log.toString();
				try {
					DataAccess.update(model, model.id, List.of("stepId", "log"));
				} catch (final Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			log.append("action [" + (iii + 1) + "/" + this.actions.size() + "] ==> DONE\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{} ==> DONE", iii + 1, this.actions.size());
			model.stepId = iii + 1;
			model.log = log.toString();
			try {
				DataAccess.update(model, model.id, List.of("stepId", "log"));
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public boolean revertMigration(final DBEntry entry, final StringBuilder log) {
		return false;
	}

	public void addAction(final String action) {
		this.actions.add(new Action(action));
	}

	public void addAction(final String action, final String filterdBType) {
		this.actions.add(new Action(action, filterdBType));
	}

	public void addClass(final Class<?> clazz) throws Exception {
		final List<String> tmp = DataFactory.createTable(clazz);
		for (final String elem : tmp) {
			this.actions.add(new Action(elem));
		}
	}

	@Override
	public int getNumberOfStep() {
		return this.actions.size();
	}

}
