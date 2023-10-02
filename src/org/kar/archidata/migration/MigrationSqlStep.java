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
	private List<String> actions = new ArrayList<>();
	
	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	@Override
	public boolean applyMigration(DBEntry entry, StringBuilder log, MigrationModel model) {
		for (int iii=0; iii<actions.size(); iii++) {
			log.append("action [" + iii + "/" + actions.size() + "]\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{}", iii, actions.size());
			String action = actions.get(iii);
			LOGGER.info("SQL request: ```{}```", action);
			log.append("SQL: " + action + "\n");
			try {
				SqlWrapper.executeQuerry(action);
			} catch (SQLException | IOException ex) {
				ex.printStackTrace();
				LOGGER.info("SQL request ERROR: ", ex.getMessage());
				log.append("SQL request ERROR: " + ex.getMessage() + "\n");
				model.stepId = iii+1;
				model.log = log.toString();
			    try {
			    	SqlWrapper.update(model, model.id, List.of("stepId", "log"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			log.append("action [" + iii + "/" + actions.size() + "] ==> DONE\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{} ==> DONE", iii, actions.size());
			model.stepId = iii+1;
			model.log = log.toString();
		    try {
		    	SqlWrapper.update(model, model.id, List.of("stepId", "log"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		List<String> tmp = SqlWrapper.createTable(clazz, false);
		actions.addAll(tmp);
	}

	@Override
	public int getNumberOfStep() {
		return actions.size();
	}

}
