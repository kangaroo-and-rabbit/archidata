package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLTableName;

import com.fasterxml.jackson.annotation.JsonInclude;

@SQLTableName ("applicationToken")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends GenericTable {
    @SQLNotNull
    public Long parentId;
    @SQLNotNull
    public String name;
    @SQLNotNull
    public Timestamp endValidityTime = null;
    @SQLNotNull
    public String token;
}
