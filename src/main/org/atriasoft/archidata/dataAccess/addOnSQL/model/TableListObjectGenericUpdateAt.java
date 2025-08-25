package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.UpdateTimestamp;

public class TableListObjectGenericUpdateAt extends TableListObjectGeneric {

	@UpdateTimestamp
	public Date updatedAt = null;
}
