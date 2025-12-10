package org.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.migration.model.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record Action(
		String action,
		AsyncCall async) {
	public Action(final String action) {
		this(action, null);
	}

	public Action(final AsyncCall async) {
		this(null, async);
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
				LOGGER.info(" >>>> DB ACTION : {}/{}\n{}", iii, this.actions.size(), action.action());
			} else {
				LOGGER.info(" >>>> DB ACTION : {}/{}\nAsync lambda", iii, this.actions.size());
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
	public boolean applyMigration(final DBAccessMongo da, final StringBuilder log, final Migration model)
			throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		for (int iii = 0; iii < this.actions.size(); iii++) {
			log.append("action [" + (iii + 1) + "/" + this.actions.size() + "]\n");
			LOGGER.info(" >>>> SQL ACTION : {}/{}", iii + 1, this.actions.size());
			final Action action = this.actions.get(iii);

			if (action.action() != null) {
				LOGGER.info("DB request: ```{}```", action.action());
				log.append("DB: " + action.action() + "\n");
			} else {
				LOGGER.info("DB request: <Lambda>");
				log.append("DB: <Lambda>\n");
			}
			try {
				if (action.action() != null) {

				} else {
					action.async().doRequest(da);
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
				LOGGER.info("DB request ERROR: ", ex.getMessage());
				log.append("DB request ERROR: " + ex.getMessage() + "\n");
				model.stepId = iii + 1;
				model.log = log.toString();
				try {
					da.updateById(model, model.id, new FilterValue("stepId", "log"));
				} catch (final Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			log.append("action [" + (iii + 1) + "/" + this.actions.size() + "] ==> DONE\n");
			LOGGER.info(" >>>> DB ACTION : {}/{} ==> DONE", iii + 1, this.actions.size());
			model.stepId = iii + 1;
			model.log = log.toString();
			try {
				da.updateById(model, model.id, new FilterValue("stepId", "log"));
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
	public boolean revertMigration(final DBAccessMongo da, final StringBuilder log) throws Exception {
		generateRevertStep();
		return false;
	}

	public void addAction(final String action) {
		this.actions.add(new Action(action));
	}

	public void addAction(final AsyncCall async) {
		this.actions.add(new Action(async));
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
