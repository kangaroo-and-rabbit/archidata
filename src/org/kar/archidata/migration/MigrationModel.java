package org.kar.archidata.migration;

import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.annotation.SQLTableName;
import org.kar.archidata.model.GenericTable;

import com.fasterxml.jackson.annotation.JsonInclude;

@SQLTableName ("KAR_migration")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrationModel extends GenericTable{
    @SQLComment("Name of the migration")
	@SQLLimitSize(256)
    public String name;
    @SQLComment("index in the migration progression")
    public Integer index;
    @SQLComment("number of element in the migration")
    public Integer count;
    @SQLComment("Log generate by the migration")
    public String log;
}
