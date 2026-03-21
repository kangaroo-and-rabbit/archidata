package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Base data model with a Long auto-generated primary key. Extends {@link GenericTiming} to include creation and update timestamps.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericData extends GenericTiming {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique Id of the object", example = "123456")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Long id = null;

	/**
	 * Gets the unique identifier of this object.
	 * @return the unique identifier
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the unique identifier of this object.
	 * @param id the unique identifier to set
	 */
	public void setId(final Long id) {
		this.id = id;
	}
}
