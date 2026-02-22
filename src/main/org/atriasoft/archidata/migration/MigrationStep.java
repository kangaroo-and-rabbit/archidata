package org.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.migration.model.Migration;
import org.atriasoft.archidata.migration.model.MigrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationStep implements MigrationInterface {
	static final Logger LOGGER = LoggerFactory.getLogger(MigrationStep.class);
	private final List<AsyncCall> actions = new ArrayList<>();
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
			LOGGER.info(" >>>> DB ACTION : {}/{}", iii, this.actions.size());
		}
	}

	public void generateStep() throws Exception {
		throw new Exception("Forward is not implemented");
	}

	@Override
	public boolean applyMigration(final DBAccessMongo da, final Migration model) throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		for (int iii = 0; iii < this.actions.size(); iii++) {
			model.logs.add(new MigrationMessage(iii + 1, "action [" + this.actions.size() + "]"));
			LOGGER.info(" >>>> DB ACTION : {}/{}", iii + 1, this.actions.size());
			final AsyncCall action = this.actions.get(iii);
			try {
				action.doRequest(da);
			} catch (final IOException ex) {
				LOGGER.error("DB request ERROR: {}", ex.getMessage(), ex);
				model.logs.add(new MigrationMessage(iii + 1, "DB request ERROR: " + ex.getMessage()));
				model.stepId = iii + 1;
				try {
					da.updateById(model, model.id, new FilterValue("stepId", "log"));
				} catch (final Exception e) {
					LOGGER.error("Failed to update migration step on error: {}", e.getMessage(), e);
				}
				return false;
			}
			model.logs.add(new MigrationMessage(iii + 1, "==> DONE"));
			LOGGER.info(" >>>> DB ACTION : {}/{} ==> DONE", iii + 1, this.actions.size());
			model.stepId = iii + 1;
			try {
				da.updateById(model, model.id, new FilterValue("stepId", "log"));
			} catch (final Exception e) {
				LOGGER.error("Failed to update migration step progress: {}", e.getMessage(), e);
			}
			try {
				Thread.sleep(2);
			} catch (final InterruptedException e) {
				LOGGER.debug("Migration step sleep interrupted: {}", e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
		return true;
	}

	public void addAction(final AsyncCall async) {
		this.actions.add(async);
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
