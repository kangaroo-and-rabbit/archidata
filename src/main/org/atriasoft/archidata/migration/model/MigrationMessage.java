package org.atriasoft.archidata.migration.model;

public class MigrationMessage {
	public Integer id;
	public String message;

	public MigrationMessage() {}

	public MigrationMessage(final Integer id, final String message) {
		this.id = id;
		this.message = message;
	}

}
