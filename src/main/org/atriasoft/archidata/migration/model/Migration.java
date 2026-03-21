package org.atriasoft.archidata.migration.model;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.ws.rs.DefaultValue;

/**
 * Data model representing a migration record (version 3).
 *
 * <p>
 * Tracks migration name, progress, completion status, and structured log messages.
 * Stored in the {@code KAR_migration} collection.
 * </p>
 */
// TODO: Add a migration Hash to be sure that the current migration init is correct and has not change...
@Table(name = "KAR_migration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration extends GenericDataSoftDelete {
	/** The current version of the migration model format. */
	static final int VERSION_MIGRATION = 3;
	/** Name of the migration. */
	@Schema(description = "Name of the migration")
	@Column(length = 256)
	public String name;
	/** Version of the migration engine format. */
	@DataNotRead
	@DefaultValue("'3'")
	@Schema(description = "Version of the migration engine")
	public Integer version;
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
	/** Structured log messages generated during the migration. */
	@Schema(description = "Log generate by the migration")
	@Column(length = -1)
	public List<MigrationMessage> logs = new ArrayList<>();
}
