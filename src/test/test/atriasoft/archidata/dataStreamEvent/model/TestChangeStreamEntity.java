package test.atriasoft.archidata.dataStreamEvent.model;

import org.atriasoft.archidata.model.GenericData;

/**
 * Test entity for Change Stream integration tests
 */
public class TestChangeStreamEntity extends GenericData {
	public String name;

	public String role;

	public Integer value;

	public Boolean active;

	public TestChangeStreamEntity() {}

	public TestChangeStreamEntity(final String name, final String role, final Integer value) {
		this.name = name;
		this.role = role;
		this.value = value;
		this.active = true;
	}
}
