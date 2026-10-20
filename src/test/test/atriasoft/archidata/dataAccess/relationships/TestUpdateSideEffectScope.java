package test.atriasoft.archidata.dataAccess.relationships;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.model.OIDGenericData;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import test.atriasoft.archidata.ConfigureDb;

/**
 * CS-04: the relation side effects of an update only follow the document the update reaches.
 *
 * <p>An application scopes its writes with an owner filter ({@code updateById(body, id, new Condition(owner))}).
 * When that filter matches nothing, or when the target is soft-deleted (excluded by the update), the main update
 * changes nothing and no remote document may change either: no cascade delete, no set-null, no re-parenting, no
 * {@code $pull} / {@code $addToSet} of the target's id on remote documents.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestUpdateSideEffectScope {

	/** Child of the one-to-many parents: a plain back reference, no relation annotation. */
	@Collection("CS04Child")
	public static class ScopeChild extends OIDGenericData {
		public String data;
		public ObjectId parentId;

		public ScopeChild() {}

		public ScopeChild(final String data) {
			this.data = data;
		}
	}

	/** Parent whose removed children are deleted on update. */
	@Collection("CS04ParentDelete")
	public static class ScopeParentDelete extends OIDGenericDataSoftDelete {
		public ObjectId ownerId;
		@OneToManyDoc(targetEntity = ScopeChild.class, remoteField = "parentId", addLinkWhenCreate = true, cascadeUpdate = CascadeMode.DELETE, cascadeDelete = CascadeMode.IGNORE)
		public List<ObjectId> children;
	}

	/** Parent whose removed children lose their back reference on update. */
	@Collection("CS04ParentSetNull")
	public static class ScopeParentSetNull extends OIDGenericDataSoftDelete {
		public ObjectId ownerId;
		@OneToManyDoc(targetEntity = ScopeChild.class, remoteField = "parentId", addLinkWhenCreate = true, cascadeUpdate = CascadeMode.SET_NULL, cascadeDelete = CascadeMode.IGNORE)
		public List<ObjectId> children;
	}

	/** Many-to-many root, scoped by owner. */
	@Collection("CS04M2MRoot")
	public static class ScopeM2MRoot extends OIDGenericDataSoftDelete {
		public ObjectId ownerId;
		@ManyToManyDoc(targetEntity = ScopeM2MRemote.class, remoteField = "roots")
		public List<ObjectId> remotes;
	}

	/** Many-to-many remote side. */
	@Collection("CS04M2MRemote")
	public static class ScopeM2MRemote extends OIDGenericData {
		public String data;
		@ManyToManyDoc(targetEntity = ScopeM2MRoot.class, remoteField = "remotes")
		public List<ObjectId> roots;
	}

	/** Many-to-one child, scoped by owner. */
	@Collection("CS04M2OChild")
	public static class ScopeM2OChild extends OIDGenericDataSoftDelete {
		public ObjectId ownerId;
		@ManyToOneDoc(targetEntity = ScopeM2OParent.class, remoteField = "childIds", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
		public ObjectId parentId;
	}

	/** Parent of the many-to-one children (holds the reverse list). */
	@Collection("CS04M2OParent")
	public static class ScopeM2OParent extends OIDGenericData {
		public String data;
		@OneToManyDoc(targetEntity = ScopeM2OChild.class, remoteField = "parentId", addLinkWhenCreate = false, cascadeUpdate = CascadeMode.IGNORE, cascadeDelete = CascadeMode.IGNORE)
		public List<ObjectId> childIds;
	}

	@BeforeAll
	public void configure() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public void clean() throws Exception {
		ConfigureDb.clear();
	}

	private static <T> T readAll(final Class<T> clazz, final ObjectId id) throws Exception {
		return ConfigureDb.da.getById(clazz, id, new AccessDeletedItems(), new ReadAllColumn());
	}

	private static boolean isEmpty(final List<?> list) {
		return list == null || list.isEmpty();
	}

	private static ScopeChild insertChild(final String data) throws Exception {
		return ConfigureDb.da.insert(new ScopeChild(data));
	}

	private static ScopeParentDelete insertParentDelete(final ObjectId owner, final List<ObjectId> children)
			throws Exception {
		final ScopeParentDelete parent = new ScopeParentDelete();
		parent.ownerId = owner;
		parent.children = new ArrayList<>(children);
		return ConfigureDb.da.insert(parent);
	}

	private static ScopeParentSetNull insertParentSetNull(final ObjectId owner, final List<ObjectId> children)
			throws Exception {
		final ScopeParentSetNull parent = new ScopeParentSetNull();
		parent.ownerId = owner;
		parent.children = new ArrayList<>(children);
		return ConfigureDb.da.insert(parent);
	}

	// ---------------------------------------------------------------------------------------------
	// @OneToManyDoc, cascadeUpdate = DELETE
	// ---------------------------------------------------------------------------------------------

	@Test
	public void oneToManyDelete_ownerConditionMatchesNothing_childrenKept() throws Exception {
		final ObjectId victim = new ObjectId();
		final ObjectId attacker = new ObjectId();
		final ScopeChild c1 = insertChild("c1");
		final ScopeChild c2 = insertChild("c2");
		final ScopeParentDelete parent = insertParentDelete(victim, List.of(c1.getOid(), c2.getOid()));
		Assertions.assertEquals(parent.getOid(), readAll(ScopeChild.class, c1.getOid()).parentId);

		// Full update (no FilterValue) of the victim's document, scoped to the attacker.
		final ScopeParentDelete body = new ScopeParentDelete();
		body.children = new ArrayList<>();
		final long count = ConfigureDb.da.updateById(body, parent.getOid(),
				new Condition(Filters.eq("ownerId", attacker)));
		Assertions.assertEquals(0, count);

		final ScopeChild r1 = readAll(ScopeChild.class, c1.getOid());
		final ScopeChild r2 = readAll(ScopeChild.class, c2.getOid());
		Assertions.assertNotNull(r1, "the victim's child was cascade-deleted by an update that matched nothing");
		Assertions.assertNotNull(r2, "the victim's child was cascade-deleted by an update that matched nothing");
		Assertions.assertEquals(parent.getOid(), r1.parentId);
		Assertions.assertEquals(parent.getOid(), r2.parentId);
		final ScopeParentDelete stored = readAll(ScopeParentDelete.class, parent.getOid());
		Assertions.assertEquals(victim, stored.ownerId);
		Assertions.assertEquals(List.of(c1.getOid(), c2.getOid()), stored.children);
	}

	@Test
	public void oneToManyDelete_ownerConditionMatches_cascadeRuns() throws Exception {
		final ObjectId owner = new ObjectId();
		final ScopeChild c1 = insertChild("c1");
		final ScopeChild c2 = insertChild("c2");
		final ScopeParentDelete parent = insertParentDelete(owner, List.of(c1.getOid(), c2.getOid()));

		final ScopeParentDelete body = new ScopeParentDelete();
		body.ownerId = owner;
		body.children = new ArrayList<>(List.of(c2.getOid()));
		final long count = ConfigureDb.da.updateById(body, parent.getOid(),
				new Condition(Filters.eq("ownerId", owner)));
		Assertions.assertEquals(1, count);

		Assertions.assertNull(readAll(ScopeChild.class, c1.getOid()), "the removed child must be cascade-deleted");
		final ScopeChild r2 = readAll(ScopeChild.class, c2.getOid());
		Assertions.assertNotNull(r2);
		Assertions.assertEquals(parent.getOid(), r2.parentId);
	}

	@Test
	public void oneToManyDelete_softDeletedTarget_childrenKept() throws Exception {
		final ObjectId owner = new ObjectId();
		final ScopeChild c1 = insertChild("c1");
		final ScopeChild c2 = insertChild("c2");
		final ScopeParentDelete parent = insertParentDelete(owner, List.of(c1.getOid(), c2.getOid()));
		Assertions.assertEquals(1, ConfigureDb.da.deleteSoftById(ScopeParentDelete.class, parent.getOid()));

		final ScopeParentDelete body = new ScopeParentDelete();
		body.ownerId = owner;
		body.children = new ArrayList<>();
		// The update excludes the soft-deleted document: with or without the owner's condition, nothing changes.
		Assertions.assertEquals(0, ConfigureDb.da.updateById(body, parent.getOid()));
		Assertions.assertEquals(0, ConfigureDb.da.updateById(body, parent.getOid(),
				new Condition(Filters.eq("ownerId", owner)), new FilterValue("children")));

		Assertions.assertNotNull(readAll(ScopeChild.class, c1.getOid()),
				"a child of a soft-deleted parent was cascade-deleted by an update that skipped the parent");
		Assertions.assertNotNull(readAll(ScopeChild.class, c2.getOid()),
				"a child of a soft-deleted parent was cascade-deleted by an update that skipped the parent");

		// With AccessDeletedItems the update reaches the soft-deleted parent: its side effects run.
		final ScopeParentDelete keepC2 = new ScopeParentDelete();
		keepC2.children = new ArrayList<>(List.of(c2.getOid()));
		Assertions.assertEquals(1, ConfigureDb.da.updateById(keepC2, parent.getOid(), new AccessDeletedItems(),
				new FilterValue("children")));
		Assertions.assertNull(readAll(ScopeChild.class, c1.getOid()));
		Assertions.assertNotNull(readAll(ScopeChild.class, c2.getOid()));
	}

	@Test
	public void oneToMany_missingId_noErrorNoSideEffect() throws Exception {
		final ScopeChild c1 = insertChild("c1");
		final ScopeParentDelete body = new ScopeParentDelete();
		body.children = new ArrayList<>(List.of(c1.getOid()));
		Assertions.assertEquals(0, ConfigureDb.da.updateById(body, new ObjectId()));
		Assertions.assertNull(readAll(ScopeChild.class, c1.getOid()).parentId);
	}

	// ---------------------------------------------------------------------------------------------
	// @OneToManyDoc, cascadeUpdate = SET_NULL (and re-parenting of the listed ids)
	// ---------------------------------------------------------------------------------------------

	@Test
	public void oneToManySetNull_ownerConditionMatchesNothing_noUnlinkNoReparent() throws Exception {
		final ObjectId victim = new ObjectId();
		final ObjectId attacker = new ObjectId();
		final ObjectId third = new ObjectId();
		final ScopeChild c1 = insertChild("c1");
		final ScopeChild c3 = insertChild("c3");
		final ScopeParentSetNull victimParent = insertParentSetNull(victim, List.of(c1.getOid()));
		final ScopeParentSetNull otherParent = insertParentSetNull(third, List.of(c3.getOid()));
		Assertions.assertEquals(otherParent.getOid(), readAll(ScopeChild.class, c3.getOid()).parentId);

		// Partial update of the victim's children, scoped to the attacker, listing another parent's child.
		final ScopeParentSetNull body = new ScopeParentSetNull();
		body.children = new ArrayList<>(List.of(c3.getOid()));
		final long count = ConfigureDb.da.updateById(body, victimParent.getOid(),
				new Condition(Filters.eq("ownerId", attacker)), new FilterValue("children"));
		Assertions.assertEquals(0, count);

		Assertions.assertEquals(victimParent.getOid(), readAll(ScopeChild.class, c1.getOid()).parentId,
				"the victim's child was unlinked by an update that matched nothing");
		Assertions.assertEquals(otherParent.getOid(), readAll(ScopeChild.class, c3.getOid()).parentId,
				"another parent's child was re-parented by an update that matched nothing");
		Assertions.assertEquals(List.of(c3.getOid()), readAll(ScopeParentSetNull.class, otherParent.getOid()).children);
		Assertions.assertEquals(List.of(c1.getOid()),
				readAll(ScopeParentSetNull.class, victimParent.getOid()).children);
	}

	@Test
	public void oneToManySetNull_ownerConditionMatches_unlinkRuns() throws Exception {
		final ObjectId owner = new ObjectId();
		final ScopeChild c1 = insertChild("c1");
		final ScopeParentSetNull parent = insertParentSetNull(owner, List.of(c1.getOid()));

		final ScopeParentSetNull body = new ScopeParentSetNull();
		body.children = new ArrayList<>();
		Assertions.assertEquals(1, ConfigureDb.da.updateById(body, parent.getOid(),
				new Condition(Filters.eq("ownerId", owner)), new FilterValue("children")));
		final ScopeChild r1 = readAll(ScopeChild.class, c1.getOid());
		Assertions.assertNotNull(r1);
		Assertions.assertNull(r1.parentId, "the removed child must lose its back reference");
	}

	// ---------------------------------------------------------------------------------------------
	// @ManyToManyDoc
	// ---------------------------------------------------------------------------------------------

	@Test
	public void manyToMany_ownerConditionMatchesNothing_remotesUnchanged() throws Exception {
		final ObjectId victim = new ObjectId();
		final ObjectId attacker = new ObjectId();
		final ScopeM2MRemote remoteA = new ScopeM2MRemote();
		remoteA.data = "A";
		final ScopeM2MRemote insertedA = ConfigureDb.da.insert(remoteA);
		final ScopeM2MRemote remoteB = new ScopeM2MRemote();
		remoteB.data = "B";
		final ScopeM2MRemote insertedB = ConfigureDb.da.insert(remoteB);
		final ScopeM2MRoot root = new ScopeM2MRoot();
		root.ownerId = victim;
		root.remotes = new ArrayList<>(List.of(insertedA.getOid()));
		final ScopeM2MRoot insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertEquals(List.of(insertedRoot.getOid()),
				readAll(ScopeM2MRemote.class, insertedA.getOid()).roots);

		final ScopeM2MRoot body = new ScopeM2MRoot();
		body.remotes = new ArrayList<>(List.of(insertedB.getOid()));
		final long count = ConfigureDb.da.updateById(body, insertedRoot.getOid(),
				new Condition(Filters.eq("ownerId", attacker)), new FilterValue("remotes"));
		Assertions.assertEquals(0, count);

		Assertions.assertEquals(List.of(insertedRoot.getOid()), readAll(ScopeM2MRemote.class, insertedA.getOid()).roots,
				"the victim's id was pulled from a remote by an update that matched nothing");
		Assertions.assertTrue(isEmpty(readAll(ScopeM2MRemote.class, insertedB.getOid()).roots),
				"the victim's id was added to a remote by an update that matched nothing");
		Assertions.assertEquals(List.of(insertedA.getOid()),
				readAll(ScopeM2MRoot.class, insertedRoot.getOid()).remotes);

		// The owner's own update keeps working.
		Assertions.assertEquals(1, ConfigureDb.da.updateById(body, insertedRoot.getOid(),
				new Condition(Filters.eq("ownerId", victim)), new FilterValue("remotes")));
		Assertions.assertTrue(isEmpty(readAll(ScopeM2MRemote.class, insertedA.getOid()).roots));
		Assertions.assertEquals(List.of(insertedRoot.getOid()),
				readAll(ScopeM2MRemote.class, insertedB.getOid()).roots);
	}

	@Test
	public void manyToMany_ownerConditionMatches_remoteLinksUpdated() throws Exception {
		final ObjectId owner = new ObjectId();
		final ScopeM2MRemote kept = ConfigureDb.da.insert(new ScopeM2MRemote());
		final ScopeM2MRemote removed = ConfigureDb.da.insert(new ScopeM2MRemote());
		final ScopeM2MRemote added = ConfigureDb.da.insert(new ScopeM2MRemote());
		final ScopeM2MRoot root = new ScopeM2MRoot();
		root.ownerId = owner;
		root.remotes = new ArrayList<>(List.of(kept.getOid(), removed.getOid()));
		final ScopeM2MRoot insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertEquals(List.of(insertedRoot.getOid()), readAll(ScopeM2MRemote.class, kept.getOid()).roots);
		Assertions.assertEquals(List.of(insertedRoot.getOid()), readAll(ScopeM2MRemote.class, removed.getOid()).roots);

		final ScopeM2MRoot body = new ScopeM2MRoot();
		body.remotes = new ArrayList<>(List.of(kept.getOid(), added.getOid()));
		Assertions.assertEquals(1, ConfigureDb.da.updateById(body, insertedRoot.getOid(),
				new Condition(Filters.eq("ownerId", owner)), new FilterValue("remotes")));

		Assertions.assertEquals(List.of(insertedRoot.getOid()), readAll(ScopeM2MRemote.class, kept.getOid()).roots);
		Assertions.assertTrue(isEmpty(readAll(ScopeM2MRemote.class, removed.getOid()).roots),
				"the id of the root must be pulled from the remote that is no longer listed");
		Assertions.assertEquals(List.of(insertedRoot.getOid()), readAll(ScopeM2MRemote.class, added.getOid()).roots,
				"the id of the root must be added to the newly listed remote");
		Assertions.assertEquals(List.of(kept.getOid(), added.getOid()),
				readAll(ScopeM2MRoot.class, insertedRoot.getOid()).remotes);
	}

	// ---------------------------------------------------------------------------------------------
	// @ManyToOneDoc
	// ---------------------------------------------------------------------------------------------

	private static ScopeM2OParent insertM2OParent(final String data) throws Exception {
		final ScopeM2OParent parent = new ScopeM2OParent();
		parent.data = data;
		return ConfigureDb.da.insert(parent);
	}

	private static ScopeM2OChild insertM2OChild(final ObjectId owner, final ObjectId parentId) throws Exception {
		final ScopeM2OChild child = new ScopeM2OChild();
		child.ownerId = owner;
		child.parentId = parentId;
		return ConfigureDb.da.insert(child);
	}

	@Test
	public void manyToOne_ownerConditionMatchesNothing_parentsUnchanged() throws Exception {
		final ObjectId victim = new ObjectId();
		final ObjectId attacker = new ObjectId();
		final ScopeM2OParent p1 = insertM2OParent("p1");
		final ScopeM2OParent p2 = insertM2OParent("p2");
		final ScopeM2OChild child = insertM2OChild(victim, p1.getOid());
		Assertions.assertEquals(List.of(child.getOid()), readAll(ScopeM2OParent.class, p1.getOid()).childIds);

		final ScopeM2OChild body = new ScopeM2OChild();
		body.parentId = p2.getOid();
		final long count = ConfigureDb.da.updateById(body, child.getOid(),
				new Condition(Filters.eq("ownerId", attacker)));
		Assertions.assertEquals(0, count);

		Assertions.assertEquals(List.of(child.getOid()), readAll(ScopeM2OParent.class, p1.getOid()).childIds,
				"the victim's id was pulled from its parent by an update that matched nothing");
		Assertions.assertTrue(isEmpty(readAll(ScopeM2OParent.class, p2.getOid()).childIds),
				"the victim's id was added to another parent by an update that matched nothing");
		Assertions.assertEquals(p1.getOid(), readAll(ScopeM2OChild.class, child.getOid()).parentId);
	}

	@Test
	public void manyToOne_softDeletedTarget_parentsUnchanged() throws Exception {
		final ObjectId owner = new ObjectId();
		final ScopeM2OParent p1 = insertM2OParent("p1");
		final ScopeM2OParent p2 = insertM2OParent("p2");
		final ScopeM2OChild child = insertM2OChild(owner, p1.getOid());
		Assertions.assertEquals(1, ConfigureDb.da.deleteSoftById(ScopeM2OChild.class, child.getOid()));

		final ScopeM2OChild body = new ScopeM2OChild();
		body.ownerId = owner;
		body.parentId = p2.getOid();
		Assertions.assertEquals(0,
				ConfigureDb.da.updateById(body, child.getOid(), new Condition(Filters.eq("ownerId", owner))));

		Assertions.assertTrue(isEmpty(readAll(ScopeM2OParent.class, p2.getOid()).childIds),
				"a soft-deleted child was linked to a parent by an update that skipped it");
		Assertions.assertEquals(p1.getOid(), readAll(ScopeM2OChild.class, child.getOid()).parentId);
	}
}
