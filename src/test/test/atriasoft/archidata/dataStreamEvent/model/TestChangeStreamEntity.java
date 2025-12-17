package test.atriasoft.archidata.dataStreamEvent.model;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;

/**
 * Test entity for Change Stream integration tests
 */
public class TestChangeStreamEntity extends GenericData {
	@Column(length = 0)
	public String name;

	@Column(length = 0)
	public String role;

	@Column
	public Integer value;

	@Column
	public Boolean active;

	public TestChangeStreamEntity() {}

	public TestChangeStreamEntity(final String name, final String role, final Integer value) {
		this.name = name;
		this.role = role;
		this.value = value;
		this.active = true;
	}
}
