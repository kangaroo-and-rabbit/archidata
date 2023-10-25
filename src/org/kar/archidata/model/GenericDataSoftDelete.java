package org.kar.archidata.model;

import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLDeleted;
import org.kar.archidata.annotation.SQLNotRead;

import jakarta.persistence.Column;

public class GenericDataSoftDelete extends GenericData {
	@SQLNotRead
	@Column(nullable = false)
	@SQLDefault("'0'")
	@SQLDeleted
	@SQLComment("When delete, they are not removed, they are just set in a deleted state")
	public Boolean deleted = null;
}
