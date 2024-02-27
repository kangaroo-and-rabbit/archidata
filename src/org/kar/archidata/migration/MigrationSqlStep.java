package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.migration.model.Migration;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record Action(String action, AsyncCall async, List<String> filterDB) {
	public Action(final String action) {
		this(action, null, List.of());
	}

	public Action(final String action, final String filterDB) {
		this(action, null, List.of(filterDB));
	}
	public Action(final AsyncCall async) {
		this(null, async, List.of());
	}

	public Action(final AsyncCall async, final String filterDB) {
		this(null, async, List.of(filterDB));
	}
}

public class MigrationSqlStep implements MigrationInterface {
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationSqlStep.class);
	private final List<Action> actions = new ArrayList<>();
	private boolean isGenerated = false;

	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	public void display() throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		for (int iii = 0; iii < this.actions.size(); iii++) {
			final Action action = this.actions.get(iii);
			if (action.action() != null) {
				LOGGER.info(" >>>> SQL ACTION : {}/{} ==> filter='{}'\n{}", iii, this.actions.size(), action.filterDB(), action.action());
			} else {
				LOGGER.info(" >>>> SQL ACTION : {}/{} ==> filter='{}'\nAsync lambda", iii, this.actions.size(), action.filterDB());
			}
		}
	}

	public void generateStep() throws Exception {
		throw new Exception("Forward is not implemented");
	}

	public void generateRevertStep() throws Exception {
		throw new Exception("Backward is not implemented");
	}

	@Override
	public boolean applyMigration(final DBEntry entry, final StringBuilder log, final Migration model) throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		for (int iii = 0; iii < this.actions.size(); iii++) {
			log.append("action [" + (iii + 1) + "/" + this.actions.size() + "]\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{}", iii + 1, this.actions.size());
			final Action action = this.actions.get(iii);

			if (action.action() != null) {
				LOGGER.info("SQL request: ```{}``` on '{}' current={}", action.action(), action.filterDB(), ConfigBaseVariable.getDBType());
				log.append("SQL: " + action.action() + " on " + action.filterDB() + "\n");
			} else {
				LOGGER.info("SQL request: <Lambda> on '{}' current={}", action.filterDB(), ConfigBaseVariable.getDBType());
				log.append("SQL: <Lambda> on " + action.filterDB() + "\n");
			}
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
				if (action.action() != null) {
					DataAccess.executeQuerry(action.action());
				} else {
					action.async().doRequest();
				}
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
				Thread.sleep(2);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public boolean revertMigration(final DBEntry entry, final StringBuilder log) throws Exception {
		generateRevertStep();
		return false;
	}

	public void addAction(final String action) {
		this.actions.add(new Action(action));
	}
	public void addAction(final AsyncCall async) {
		this.actions.add(new Action(async));
	}

	public void addAction(final String action, final String filterdBType) {
		this.actions.add(new Action(action, filterdBType));
	}
	public void addAction(final AsyncCall async, final String filterdBType) {
		this.actions.add(new Action(async, filterdBType));
	}

	public void addClass(final Class<?> clazz) throws Exception {
		final List<String> tmp = DataFactory.createTable(clazz);
		for (final String elem : tmp) {
			this.actions.add(new Action(elem));
		}
	}

	@Override
	public int getNumberOfStep() throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		return this.actions.size();
	}

}
