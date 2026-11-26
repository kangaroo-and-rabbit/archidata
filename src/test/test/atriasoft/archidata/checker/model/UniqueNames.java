package test.atriasoft.archidata.checker.model;

import org.atriasoft.archidata.annotation.checker.UniqueInBaseId;

/** A bean whose names must not exist yet in the collection of the target entity. */
public class UniqueNames {
	@UniqueInBaseId(target = OidTag.class)
	public String oidName;
	@UniqueInBaseId(target = LongTag.class)
	public String longName;
	@UniqueInBaseId(target = UuidTag.class)
	public String uuidName;
	@UniqueInBaseId(target = OidTag.class, nameOfField = "label", message = "label is taken")
	public String label;
}
