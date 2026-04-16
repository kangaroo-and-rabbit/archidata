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

/**
 * Base implementation of {@link MigrationInterface} that executes migration as a sequence
 * of {@link AsyncCall} actions.
 *
 * <p>
 * Subclasses should override {@link #generateStep()} to define the migration steps
 * by calling {@link #addAction(AsyncCall)}.
 * </p>
 */
public class MigrationStep implements MigrationInterface {
	static final Logger LOGGER = LoggerFactory.getLogger(MigrationStep.class);
	private final List<AsyncCall> actions = new ArrayList<>();
	private boolean isGenerated = false;

	/**
	 * Returns the canonical class name as the migration name.
	 *
	 * @return the migration name
	 */
	@Override
	public String getName() {
		return getClass().getCanonicalName();
	}

	/**
	 * Displays the migration steps by logging each action index.
	 *
	 * @throws Exception if step generation fails
	 */
	public void display() throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		for (int iii = 0; iii < this.actions.size(); iii++) {
			LOGGER.info(" >>>> DB ACTION : {}/{}", iii, this.actions.size());
		}
	}

	/**
	 * Generates the migration steps by adding actions via {@link #addAction(AsyncCall)}.
	 *
	 * <p>
	 * Subclasses must override this method to define migration logic.
	 * </p>
	 *
	 * @throws Exception if step generation fails
	 */
	public void generateStep() throws Exception {
		throw new Exception("Forward is not implemented");
	}

	/**
	 * Applies all migration actions sequentially, updating the migration model after each step.
	 *
	 * @param da the database access interface
	 * @param model the migration record to update with progress
	 * @return {@code true} if all actions completed successfully
	 * @throws Exception if any action fails
	 */
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
					da.updateById(model, model.getId(), new FilterValue("stepId", "log"));
				} catch (final Exception e) {
					LOGGER.error("Failed to update migration step on error: {}", e.getMessage(), e);
				}
				return false;
			}
			model.logs.add(new MigrationMessage(iii + 1, "==> DONE"));
			LOGGER.info(" >>>> DB ACTION : {}/{} ==> DONE", iii + 1, this.actions.size());
			model.stepId = iii + 1;
			try {
				da.updateById(model, model.getId(), new FilterValue("stepId", "log"));
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

	/**
	 * Adds an asynchronous action to the migration step sequence.
	 *
	 * @param async the action to add
	 */
	public void addAction(final AsyncCall async) {
		this.actions.add(async);
	}

	/**
	 * Returns the total number of migration actions.
	 *
	 * @return the number of steps
	 * @throws Exception if step generation fails
	 */
	@Override
	public int getNumberOfStep() throws Exception {
		if (!this.isGenerated) {
			this.isGenerated = true;
			generateStep();
		}
		return this.actions.size();
	}

}
