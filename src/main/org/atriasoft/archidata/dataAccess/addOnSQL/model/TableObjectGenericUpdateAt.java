package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.UpdateTimestamp;

public class TableObjectGenericUpdateAt extends TableObjectGeneric {

	@UpdateTimestamp
	public Date updatedAt = null;
}
