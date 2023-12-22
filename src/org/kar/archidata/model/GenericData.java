package org.kar.archidata.model;

import java.util.Date;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataComment;
import org.kar.archidata.annotation.DataNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

public class GenericData {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@DataComment("Primary key of the base")
	public Long id = null;
	@DataNotRead
	@CreationTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@DataComment("Create time of the object")
	@NotNull
	public Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@DataComment("When update the object")
	@NotNull
	public Date updatedAt = null;
}
