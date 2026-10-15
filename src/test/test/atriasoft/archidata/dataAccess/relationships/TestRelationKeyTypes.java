package test.atriasoft.archidata.dataAccess.relationships;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.model.GenericData;
import org.atriasoft.archidata.model.OIDGenericData;
import org.atriasoft.archidata.model.UUIDGenericData;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.Table;
import test.atriasoft.archidata.ConfigureDb;

/** Relations (ManyToOneDoc, OneToManyDoc, ManyToManyDoc) with UUID, Long and ObjectId keys. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRelationKeyTypes {

	// UUID
	@Table(name = "RelKeyUParent")
	public static class UParent extends UUIDGenericData {
		public String data;
		@OneToManyDoc(targetEntity = UChild.class, remoteField = "parentId")
		public List<UUID> childIds;
	}

	@Table(name = "RelKeyUChild")
	public static class UChild extends UUIDGenericData {
		public String data;
		@ManyToOneDoc(targetEntity = UParent.class, remoteField = "childIds", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
		public UUID parentId;
	}

	@Table(name = "RelKeyUParent")
	public static class UParentExpand extends UUIDGenericData {
		public String data;
		@OneToManyDoc(targetEntity = UChild.class, remoteField = "parentId")
		public List<UChild> childIds;
	}

	@Table(name = "RelKeyUChild")
	public static class UChildExpand extends UUIDGenericData {
		public String data;
		@ManyToOneDoc(targetEntity = UParent.class, remoteField = "childIds")
		public UParent parentId;
	}

	@Table(name = "RelKeyUMRoot")
	public static class UMRoot extends UUIDGenericData {
		@ManyToManyDoc(targetEntity = UMRemote.class, remoteField = "back")
		public List<UUID> remote;
	}

	@Table(name = "RelKeyUMRemote")
	public static class UMRemote extends UUIDGenericData {
		@ManyToManyDoc(targetEntity = UMRoot.class, remoteField = "remote")
		public List<UUID> back;
	}

	@Table(name = "RelKeyUMRoot")
	public static class UMRootExpand extends UUIDGenericData {
		@ManyToManyDoc(targetEntity = UMRemote.class, remoteField = "back")
		public List<UMRemote> remote;
	}

	// UUID -> ObjectId
	@Table(name = "RelKeyOTarget")
	public static class OTarget extends OIDGenericData {
		public String data;
		@OneToManyDoc(targetEntity = UToO.class, remoteField = "target")
		public List<UUID> uuidChildren;
	}

	@Table(name = "RelKeyUToO")
	public static class UToO extends UUIDGenericData {
		@ManyToOneDoc(targetEntity = OTarget.class, remoteField = "uuidChildren", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
		public ObjectId target;
	}

	// ObjectId -> UUID
	@Table(name = "RelKeyOChild")
	public static class OChild extends OIDGenericData {
		@ManyToOneDoc(targetEntity = UParentWithO.class, remoteField = "oChildren", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
		public UUID parent;
	}

	@Table(name = "RelKeyUParentO")
	public static class UParentWithO extends UUIDGenericData {
		@OneToManyDoc(targetEntity = OChild.class, remoteField = "parent")
		public List<ObjectId> oChildren;
	}

	// M2M UUID <-> ObjectId
	@Table(name = "RelKeyUOMRoot")
	public static class UOMRoot extends UUIDGenericData {
		@ManyToManyDoc(targetEntity = UOMRemote.class, remoteField = "back")
		public List<ObjectId> remote;
	}

	@Table(name = "RelKeyUOMRemote")
	public static class UOMRemote extends OIDGenericData {
		@ManyToManyDoc(targetEntity = UOMRoot.class, remoteField = "remote")
		public List<UUID> back;
	}

	// Long
	@Table(name = "RelKeyLParent")
	public static class LParent extends GenericData {
		public String data;
		@OneToManyDoc(targetEntity = LChild.class, remoteField = "parentId")
		public List<Long> childIds;
	}

	@Table(name = "RelKeyLChild")
	public static class LChild extends GenericData {
		@ManyToOneDoc(targetEntity = LParent.class, remoteField = "childIds", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
		public Long parentId;
	}

	@Table(name = "RelKeyLMRoot")
	public static class LMRoot extends GenericData {
		@ManyToManyDoc(targetEntity = LMRemote.class, remoteField = "back")
		public List<Long> remote;
	}

	@Table(name = "RelKeyLMRemote")
	public static class LMRemote extends GenericData {
		@ManyToManyDoc(targetEntity = LMRoot.class, remoteField = "remote")
		public List<Long> back;
	}

	@BeforeAll
	public void conf() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public void clean() throws Exception {
		ConfigureDb.clear();
	}

	private static UParent newUParent(final String d) {
		final UParent p = new UParent();
		p.data = d;
		return p;
	}

	// UUID
	@Test
	public void uuid_insertAndGet() throws Exception {
		final UParent p = ConfigureDb.da.insert(newUParent("a"));
		Assertions.assertNotNull(p.getUuid());
		final UParent r = ConfigureDb.da.getById(UParent.class, p.getUuid());
		Assertions.assertNotNull(r);
		Assertions.assertEquals(p.getUuid(), r.getUuid());
		Assertions.assertEquals("a", r.data);
	}

	@Test
	public void uuid_manyToOne_add_read_update_deleteChild() throws Exception {
		final UParent p1 = ConfigureDb.da.insert(newUParent("p1"));
		final UParent p2 = ConfigureDb.da.insert(newUParent("p2"));
		final UChild c = new UChild();
		c.parentId = p1.getUuid();
		final UChild ic = ConfigureDb.da.insert(c);
		Assertions.assertEquals(p1.getUuid(), ic.parentId);
		final UChild rc = ConfigureDb.da.getById(UChild.class, ic.getUuid());
		Assertions.assertEquals(p1.getUuid(), rc.parentId);
		UParent rp = ConfigureDb.da.getById(UParent.class, p1.getUuid(), new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(rp.childIds, "parent.childIds null after child insert");
		Assertions.assertEquals(List.of(ic.getUuid()), rp.childIds);
		final UChildExpand ex = ConfigureDb.da.getById(UChildExpand.class, ic.getUuid());
		Assertions.assertNotNull(ex.parentId, "expand of parent null");
		Assertions.assertEquals(p1.getUuid(), ex.parentId.getUuid());
		final UChild upd = new UChild();
		upd.parentId = p2.getUuid();
		Assertions.assertEquals(1, ConfigureDb.da.updateById(upd, ic.getUuid()));
		rp = ConfigureDb.da.getById(UParent.class, p1.getUuid(), new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertTrue(rp.childIds == null || rp.childIds.isEmpty());
		rp = ConfigureDb.da.getById(UParent.class, p2.getUuid(), new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(List.of(ic.getUuid()), rp.childIds);
		Assertions.assertEquals(1, ConfigureDb.da.deleteById(UChild.class, ic.getUuid()));
		rp = ConfigureDb.da.getById(UParent.class, p2.getUuid(), new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertTrue(rp.childIds == null || rp.childIds.isEmpty());
	}

	@Test
	public void uuid_oneToMany_expand_and_deleteParent() throws Exception {
		final UParent p = ConfigureDb.da.insert(newUParent("pp"));
		final UChild c = new UChild();
		c.parentId = p.getUuid();
		final UChild ic = ConfigureDb.da.insert(c);
		final UParentExpand ex = ConfigureDb.da.getById(UParentExpand.class, p.getUuid(), new ReadAllColumn());
		Assertions.assertNotNull(ex.childIds);
		Assertions.assertEquals(1, ex.childIds.size());
		Assertions.assertEquals(ic.getUuid(), ex.childIds.get(0).getUuid());
		Assertions.assertEquals(1, ConfigureDb.da.deleteById(UParent.class, p.getUuid()));
		Assertions.assertNotNull(ConfigureDb.da.getById(UChild.class, ic.getUuid()));
	}

	@Test
	public void uuid_manyToMany() throws Exception {
		final UMRoot root = ConfigureDb.da.insert(new UMRoot());
		final UMRemote r1 = ConfigureDb.da.insert(new UMRemote());
		final UMRemote r2 = ConfigureDb.da.insert(new UMRemote());
		ManyToManyTools.addLink(ConfigureDb.da, UMRoot.class, root.getUuid(), "remote", r1.getUuid());
		ManyToManyTools.addLink(ConfigureDb.da, UMRoot.class, root.getUuid(), "remote", r2.getUuid());
		UMRoot rr = ConfigureDb.da.getById(UMRoot.class, root.getUuid(), new ReadAllColumn());
		Assertions.assertEquals(List.of(r1.getUuid(), r2.getUuid()), rr.remote);
		final UMRemote rem = ConfigureDb.da.getById(UMRemote.class, r1.getUuid(), new ReadAllColumn());
		Assertions.assertEquals(List.of(root.getUuid()), rem.back);
		final UMRootExpand ex = ConfigureDb.da.getById(UMRootExpand.class, root.getUuid(), new ReadAllColumn());
		Assertions.assertNotNull(ex.remote);
		Assertions.assertEquals(2, ex.remote.size());
		ManyToManyTools.removeLink(ConfigureDb.da, UMRoot.class, root.getUuid(), "remote", r1.getUuid());
		rr = ConfigureDb.da.getById(UMRoot.class, root.getUuid(), new ReadAllColumn());
		Assertions.assertEquals(List.of(r2.getUuid()), rr.remote);
		final UMRemote rem1 = ConfigureDb.da.getById(UMRemote.class, r1.getUuid(), new ReadAllColumn());
		Assertions.assertTrue(rem1.back == null || rem1.back.isEmpty());
		Assertions.assertEquals(1, ConfigureDb.da.deleteById(UMRemote.class, r1.getUuid()));
	}

	@Test
	public void uuid_to_objectId_manyToOne() throws Exception {
		final OTarget t = new OTarget();
		t.data = "t";
		final OTarget it = ConfigureDb.da.insert(t);
		final UToO c = new UToO();
		c.target = it.getOid();
		final UToO ic = ConfigureDb.da.insert(c);
		Assertions.assertEquals(it.getOid(), ConfigureDb.da.getById(UToO.class, ic.getUuid()).target);
		OTarget rt = ConfigureDb.da.getById(OTarget.class, it.getOid(), new ReadAllColumn());
		Assertions.assertEquals(List.of(ic.getUuid()), rt.uuidChildren);
		ConfigureDb.da.deleteById(UToO.class, ic.getUuid());
		rt = ConfigureDb.da.getById(OTarget.class, it.getOid(), new ReadAllColumn());
		Assertions.assertTrue(rt.uuidChildren == null || rt.uuidChildren.isEmpty());
	}

	@Test
	public void objectId_to_uuid_manyToOne() throws Exception {
		final UParentWithO p = ConfigureDb.da.insert(new UParentWithO());
		final OChild c = new OChild();
		c.parent = p.getUuid();
		final OChild ic = ConfigureDb.da.insert(c);
		Assertions.assertEquals(p.getUuid(), ConfigureDb.da.getById(OChild.class, ic.getOid()).parent);
		final UParentWithO rp = ConfigureDb.da.getById(UParentWithO.class, p.getUuid(), new ReadAllColumn());
		Assertions.assertEquals(List.of(ic.getOid()), rp.oChildren);
		ConfigureDb.da.deleteById(OChild.class, ic.getOid());
		final UParentWithO rp2 = ConfigureDb.da.getById(UParentWithO.class, p.getUuid(), new ReadAllColumn());
		Assertions.assertTrue(rp2.oChildren == null || rp2.oChildren.isEmpty());
	}

	@Test
	public void uuid_to_objectId_manyToMany() throws Exception {
		final UOMRoot root = ConfigureDb.da.insert(new UOMRoot());
		final UOMRemote rem = ConfigureDb.da.insert(new UOMRemote());
		ManyToManyTools.addLink(ConfigureDb.da, UOMRoot.class, root.getUuid(), "remote", rem.getOid());
		Assertions.assertEquals(List.of(rem.getOid()),
				ConfigureDb.da.getById(UOMRoot.class, root.getUuid(), new ReadAllColumn()).remote);
		Assertions.assertEquals(List.of(root.getUuid()),
				ConfigureDb.da.getById(UOMRemote.class, rem.getOid(), new ReadAllColumn()).back);
		ManyToManyTools.removeLink(ConfigureDb.da, UOMRoot.class, root.getUuid(), "remote", rem.getOid());
		final List<ObjectId> l = ConfigureDb.da.getById(UOMRoot.class, root.getUuid(), new ReadAllColumn()).remote;
		Assertions.assertTrue(l == null || l.isEmpty());
	}

	// Long
	@Test
	public void long_insertAndGet() throws Exception {
		final LParent p = new LParent();
		p.data = "a";
		final LParent ip = ConfigureDb.da.insert(p);
		Assertions.assertNotNull(ip.getId());
		Assertions.assertEquals("a", ConfigureDb.da.getById(LParent.class, ip.getId()).data);
	}

	@Test
	public void long_manyToOne() throws Exception {
		final LParent p1 = ConfigureDb.da.insert(new LParent());
		final LParent p2 = ConfigureDb.da.insert(new LParent());
		final LChild c = new LChild();
		c.parentId = p1.getId();
		final LChild ic = ConfigureDb.da.insert(c);
		LParent rp = ConfigureDb.da.getById(LParent.class, p1.getId(), new ReadAllColumn());
		Assertions.assertEquals(List.of(ic.getId()), rp.childIds);
		final LChild upd = new LChild();
		upd.parentId = p2.getId();
		Assertions.assertEquals(1, ConfigureDb.da.updateById(upd, ic.getId()));
		rp = ConfigureDb.da.getById(LParent.class, p1.getId(), new ReadAllColumn());
		Assertions.assertTrue(rp.childIds == null || rp.childIds.isEmpty());
		rp = ConfigureDb.da.getById(LParent.class, p2.getId(), new ReadAllColumn());
		Assertions.assertEquals(List.of(ic.getId()), rp.childIds);
		ConfigureDb.da.deleteById(LChild.class, ic.getId());
		rp = ConfigureDb.da.getById(LParent.class, p2.getId(), new ReadAllColumn());
		Assertions.assertTrue(rp.childIds == null || rp.childIds.isEmpty());
	}

	@Test
	public void long_manyToMany() throws Exception {
		final LMRoot root = ConfigureDb.da.insert(new LMRoot());
		final LMRemote r1 = ConfigureDb.da.insert(new LMRemote());
		ManyToManyTools.addLink(ConfigureDb.da, LMRoot.class, root.getId(), "remote", r1.getId());
		Assertions.assertEquals(List.of(r1.getId()),
				ConfigureDb.da.getById(LMRoot.class, root.getId(), new ReadAllColumn()).remote);
		Assertions.assertEquals(List.of(root.getId()),
				ConfigureDb.da.getById(LMRemote.class, r1.getId(), new ReadAllColumn()).back);
		ManyToManyTools.removeLink(ConfigureDb.da, LMRoot.class, root.getId(), "remote", r1.getId());
		final List<Long> l = ConfigureDb.da.getById(LMRoot.class, root.getId(), new ReadAllColumn()).remote;
		Assertions.assertTrue(l == null || l.isEmpty());
	}
}
