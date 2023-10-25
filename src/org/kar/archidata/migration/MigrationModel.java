package org.kar.archidata.migration;

import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.model.GenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

// For logs only
//public static final String TABLE_NAME = "KAR_migration";

@Table(name = "KAR_migration")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationModel extends GenericDataSoftDelete {
	@SQLComment("Name of the migration")
	@Column(length = 256)
	public String name;
	@Column(nullable = false)
	@SQLDefault("'0'")
	@SQLComment("if the migration is well terminated or not")
	public Boolean terminated = false;
	@SQLComment("index in the migration progression")
	public Integer stepId = 0;
	@SQLComment("number of element in the migration")
	public Integer count;
	@SQLComment("Log generate by the migration")
	public String log = "";
}
