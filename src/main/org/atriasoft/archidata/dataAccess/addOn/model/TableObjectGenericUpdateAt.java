package org.atriasoft.archidata.dataAccess.addOn.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.UpdateTimestamp;

public class TableObjectGenericUpdateAt extends TableObjectGeneric {

	@UpdateTimestamp
	public Date updatedAt = null;
}
