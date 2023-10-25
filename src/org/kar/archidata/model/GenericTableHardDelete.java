package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class GenericTableHardDelete {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@SQLComment("Primary key of the base")
	public Long id = null;
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
