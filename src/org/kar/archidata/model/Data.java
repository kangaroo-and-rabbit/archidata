package org.kar.archidata.model;


import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLTableName;

import com.fasterxml.jackson.annotation.JsonInclude;

@SQLTableName ("data")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
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
