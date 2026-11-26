package test.atriasoft.archidata.checker.model;

import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;

/** An entity keyed by an ObjectId, the target of the database backed constraints. */
public class OidTag extends OIDGenericDataSoftDelete {
	public String name;
	public String label;
}
