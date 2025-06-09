package org.atriasoft.archidata.annotation.checker.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.UpdateTimestamp;

public class TableObjectGenericUpdateAt extends TableObjectGeneric {

	@UpdateTimestamp
	public Date updatedAt = null;
}
