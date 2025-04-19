package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

public class LinkTableGeneric extends OIDGenericDataSoftDelete {
	public LinkTableGeneric() {
		// nothing to do...
	}

	public LinkTableGeneric(final Object object1Id, final Object object2Id) {
		this.object1Id = object1Id;
		this.object2Id = object2Id;
	}

	@Schema(description = "Object reference 1")
	@Column(nullable = false)
	public Object object1Id;
	@Schema(description = "Object reference 2")
	@Column(nullable = false)
	public Object object2Id;

}
