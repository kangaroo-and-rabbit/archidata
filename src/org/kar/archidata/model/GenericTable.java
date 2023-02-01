package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.SQLAutoIncrement;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLCreateTime;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLNotRead;
import org.kar.archidata.annotation.SQLPrimaryKey;
import org.kar.archidata.annotation.SQLUpdateTime;

public class GenericTable {
    @SQLAutoIncrement // Add AUTO_INCREMENT modifier
    @SQLPrimaryKey // Create a PRIMARY KEY based on this field
    @SQLNotNull
    @SQLComment("Primary key of the base")
	public Long id = null;
    @SQLNotRead
    @SQLNotNull
    @SQLDefault("'0'")
    @SQLComment("When delete, they are not removed, they are just set in a deleted state")
	public Boolean deleted = null;
    @SQLNotRead
    @SQLCreateTime
    @SQLNotNull
    @SQLComment("Create time of the object")
    @SQLDefault("CURRENT_TIMESTAMP(3)")
	public Timestamp create_date = null;
    @SQLNotRead
    @SQLUpdateTime
    @SQLNotNull
    @SQLComment("When update the object")
    @SQLDefault("CURRENT_TIMESTAMP(3)")
	public Timestamp modify_date = null;
}
