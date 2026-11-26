package test.atriasoft.archidata.checker.model;

import org.atriasoft.archidata.annotation.checker.CheckForeignKey;
import org.bson.types.ObjectId;

/** A reference whose target is not an entity: the database access fails. */
public class BrokenReference {
	@CheckForeignKey(target = String.class)
	public ObjectId ref;
}
