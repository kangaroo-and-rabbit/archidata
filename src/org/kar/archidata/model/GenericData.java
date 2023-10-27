package org.kar.archidata.model;

import java.util.Date;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

public class GenericData {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@SQLComment("Primary key of the base")
	public Long id = null;
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
}
