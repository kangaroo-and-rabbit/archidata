package org.kar.archidata.model;

import org.kar.archidata.annotation.DataComment;
import org.kar.archidata.annotation.DataDefault;
import org.kar.archidata.annotation.DataDeleted;
import org.kar.archidata.annotation.DataNotRead;

import jakarta.persistence.Column;

public class GenericDataSoftDelete extends GenericData {
	@DataNotRead
	@Column(nullable = false)
	@DataDefault("'0'")
	@DataDeleted
	@DataComment("When delete, they are not removed, they are just set in a deleted state")
	public Boolean deleted = null;
}
