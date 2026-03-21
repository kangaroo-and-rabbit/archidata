package org.atriasoft.archidata.migration.model;

import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.ws.rs.DefaultValue;

/**
 * Data model representing a migration record (version 1).
 *
 * <p>
 * The original migration model format with plain-text log support.
 * Stored in the {@code KAR_migration} collection.
 * </p>
 */
// For logs only
//public static final String TABLE_NAME = "KAR_migration";

@Table(name = "KAR_migration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration1 extends GenericDataSoftDelete {
	/** The current version of the migration model format. */
	static final int VERSION_MIGRATION = 1;
	/** Name of the migration. */
	@Schema(description = "Name of the migration")
	@Column(length = 256)
	public String name;
	/** Whether the migration completed successfully. */
	@Column(nullable = false)
	@DefaultValue("'0'")
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
	@Column(length = 0)
	public String log = "";
}
