package test.atriasoft.archidata.checker.model;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.checker.CheckForeignKey;
import org.bson.types.ObjectId;

/** A bean pointing at the three kinds of tag with {@code @CheckForeignKey}. */
public class TagReferences {
	@CheckForeignKey(target = OidTag.class)
	public ObjectId oidTag;
	@CheckForeignKey(target = LongTag.class)
	public Long longTag;
	@CheckForeignKey(target = UuidTag.class)
	public UUID uuidTag;
	public List<@CheckForeignKey(target = OidTag.class) ObjectId> oidTags;
}
