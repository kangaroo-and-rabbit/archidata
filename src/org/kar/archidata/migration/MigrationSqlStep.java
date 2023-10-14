package org.kar.archidata.migration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.db.DBEntry;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationSqlStep implements MigrationInterface {
	final static Logger LOGGER = LoggerFactory.getLogger(MigrationSqlStep.class);
	private final List<String> actions = new ArrayList<>();

	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	public void display() {
		for (int iii = 0; iii < this.actions.size(); iii++) {
			final String action = this.actions.get(iii);
			LOGGER.info(" >>>> SQL ACTION : {}/{} ==> \n{}", iii, this.actions.size(), action);
		}
	}

	@Override
	public boolean applyMigration(final DBEntry entry, final StringBuilder log, final MigrationModel model) {
		for (int iii = 0; iii < this.actions.size(); iii++) {
			log.append("action [" + iii + "/" + this.actions.size() + "]\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{}", iii, this.actions.size());
			final String action = this.actions.get(iii);
			LOGGER.info("SQL request: ```{}```", action);
			log.append("SQL: " + action + "\n");
			try {
				SqlWrapper.executeQuerry(action);
			} catch (SQLException | IOException ex) {
				ex.printStackTrace();
				LOGGER.info("SQL request ERROR: ", ex.getMessage());
				log.append("SQL request ERROR: " + ex.getMessage() + "\n");
				model.stepId = iii + 1;
				model.log = log.toString();
				try {
					SqlWrapper.update(model, model.id, List.of("stepId", "log"));
				} catch (final Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			log.append("action [" + iii + "/" + this.actions.size() + "] ==> DONE\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{} ==> DONE", iii, this.actions.size());
			model.stepId = iii + 1;
			model.log = log.toString();
			try {
				SqlWrapper.update(model, model.id, List.of("stepId", "log"));
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
		this.actions.add(action);
	}

	public void addClass(final Class<?> clazz) throws Exception {
		final List<String> tmp = SqlWrapper.createTable(clazz, false);
		this.actions.addAll(tmp);
	}

	@Override
	public int getNumberOfStep() {
		return this.actions.size();
	}

}
