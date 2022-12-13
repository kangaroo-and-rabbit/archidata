package org.kar.archidata.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLTableName;
import org.kar.archidata.model.GenericTable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SQLTableName ("data")
@SQLIfNotExists
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Data extends GenericTable {

    @SQLNotNull
    @SQLLimitSize(128)
    @SQLComment("Sha512 of the data")
    public String sha512;
    @SQLNotNull
    @SQLLimitSize(128)
    @SQLComment("Mime -type of the media")
    public String mimeType;
    @SQLNotNull
    @SQLComment("Size in Byte of the data")
    public Long size;
}
