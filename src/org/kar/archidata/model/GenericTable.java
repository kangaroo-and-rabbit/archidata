package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLDeleted;
import org.kar.archidata.annotation.SQLNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

//@SQLWhere(clause = "deleted=false")
public class GenericTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@SQLComment("Primary key of the base")
	public Long id = null;
	@SQLNotRead
	@Column(nullable = false)
	@SQLDefault("'0'")
	@SQLDeleted
	@SQLComment("When delete, they are not removed, they are just set in a deleted state")
	public Boolean deleted = null;
	@SQLNotRead
	@CreationTimestamp
	@Column(nullable = false)
	@SQLComment("Create time of the object")
	public Timestamp createdAt = null;
	@SQLNotRead
	@UpdateTimestamp
	@Column(nullable = false)
	@SQLComment("When update the object")
	public Timestamp updatedAt = null;
}
/* TODO Later:
	@SQLNotRead
	@CreationTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@SQLComment("Create time of the object")
	public Date createdAt = null;

	@SQLNotRead
	@UpdateTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@SQLComment("When update the object")
	public Date updatedAt = null;
*/
