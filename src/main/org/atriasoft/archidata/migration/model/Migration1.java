package org.atriasoft.archidata.migration.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.InitValue;
import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data model representing a migration record (version 1).
 *
 * <p>
 * The original migration model format with plain-text log support (field {@code log}, no
 * {@code version} field). Stored in the {@code KAR_migration} collection.
 * </p>
 *
 * <p>
 * Kept only to read and upgrade databases written by the first engine; new code uses
 * {@link Migration}. See also {@link Migration2}.
 * </p>
 */
// For logs only
//public static final String TABLE_NAME = "KAR_migration";

@Collection("KAR_migration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration1 extends GenericDataSoftDelete {
	/** Creates an empty {@link Migration1} instance. */
	public Migration1() {}

	/** The current version of the migration model format. */
	static final int VERSION_MIGRATION = 1;
	/** Name of the migration. */
	@Schema(description = "Name of the migration")
	public String name;
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
