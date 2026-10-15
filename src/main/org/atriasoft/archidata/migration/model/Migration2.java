package org.atriasoft.archidata.migration.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.InitValue;
import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data model representing a migration record (version 2).
 *
 * <p>
 * Tracks migration name, progress, completion status, and a plain-text log string.
 * Stored in the {@code KAR_migration} collection.
 * </p>
 *
 * <p>
 * Second format: it adds the {@code version} field to {@link Migration1}. It is superseded by
 * {@link Migration}, which replaces the text log by structured {@link MigrationMessage} entries,
 * and is kept to read and upgrade older databases.
 * </p>
 */
// TODO: Add a migration Hash to be sure that the current migration init is correct and has not change...
@Collection("KAR_migration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration2 extends GenericDataSoftDelete {
	/** Creates an empty {@link Migration2} instance. */
	public Migration2() {}

	/** The current version of the migration model format. */
	static final int VERSION_MIGRATION = 2;
	/** Name of the migration. */
	@Schema(description = "Name of the migration")
	public String name;
	/** Version of the migration engine format. */
	@DataNotRead
	@InitValue("'2'")
	@Schema(description = "Version of the migration engine")
	public Integer version;
	/** Whether the migration completed successfully. */
	@InitValue("'0'")
	@Schema(description = "if the migration is well terminated or not")
	public Boolean terminated = false;
	/** Current step index in the migration progression. */
	@Schema(description = "index in the migration progression")
	public Integer stepId = 0;
	/** Total number of steps in the migration. */
	@Schema(description = "number of element in the migration")
	public Integer count;
	/** Plain-text log generated during the migration. */
	@Schema(description = "Log generate by the migration")
	public String log = "";
}
