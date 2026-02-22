package org.atriasoft.archidata.migration.model;

import org.atriasoft.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.ws.rs.DefaultValue;

// For logs only
//public static final String TABLE_NAME = "KAR_migration";

@Table(name = "KAR_migration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration1 extends GenericDataSoftDelete {
	static final int VERSION_MIGRATION = 1;
	@Schema(description = "Name of the migration")
	@Column(length = 256)
	public String name;
	@Column(nullable = false)
	@DefaultValue("'0'")
	@Schema(description = "if the migration is well terminated or not")
	public Boolean terminated = false;
	@Schema(description = "index in the migration progression")
	public Integer stepId = 0;
	@Schema(description = "number of element in the migration")
	public Integer count;
	@Schema(description = "Log generate by the migration")
	@Column(length = 0)
	public String log = "";
}
