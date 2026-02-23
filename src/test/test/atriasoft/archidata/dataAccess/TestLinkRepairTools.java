package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.commonTools.LinkRepairTools;
import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.atriasoft.archidata.dataAccess.commonTools.RepairReport;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildTTT;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDParentIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRoot;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestLinkRepairTools {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestLinkRepairTools.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	// ==================== ManyToMany tests ====================

	@Order(1)
	@Test
	public void testRepairManyToMany_alreadyConsistent() throws Exception {
		// Create 2 roots and 2 remotes, link them properly via ManyToManyTools
		final TypeManyToManyDocOIDRoot root1 = ConfigureDb.da.insert(createM2MRoot("root1"));
		final TypeManyToManyDocOIDRemote remote1 = ConfigureDb.da.insert(createM2MRemote("remote1"));
		final TypeManyToManyDocOIDRemote remote2 = ConfigureDb.da.insert(createM2MRemote("remote2"));

		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocOIDRoot.class, root1.getOid(), "remote",
				remote1.getOid());
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocOIDRoot.class, root1.getOid(), "remote",
				remote2.getOid());

		// Repair should find nothing to fix
		final RepairReport report = LinkRepairTools.repairLinks(TypeManyToManyDocOIDRoot.class, "remote", false, false);
		LOGGER.info("M2M consistent report: {}", report);
		Assertions.assertEquals(0, report.getTotalFixes());
		Assertions.assertTrue(report.getLinksChecked() > 0);

		// Cleanup
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRoot.class, root1.getOid());
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRemote.class, remote1.getOid());
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRemote.class, remote2.getOid());
	}

	@Order(2)
	@Test
	public void testRepairManyToMany_missingRemoteLink() throws Exception {
		// Create root and remote, add link only on root side (skip bidirectional)
		final TypeManyToManyDocOIDRoot root1 = ConfigureDb.da.insert(createM2MRoot("root_m2m_missing"));
		final TypeManyToManyDocOIDRemote remote1 = ConfigureDb.da.insert(createM2MRemote("remote_m2m_missing"));

		// Directly add remote1's ID to root1's list without updating remote side
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel rootModel = DbClassModel.of(TypeManyToManyDocOIDRoot.class);
			final DbPropertyDescriptor remoteFieldDesc = rootModel.findByPropertyName("remote");
			final String fieldColumn = remoteFieldDesc.getFieldName(null).inTable();
			MongoLinkManager.addToList(db, TypeManyToManyDocOIDRoot.class, root1.getOid(), fieldColumn,
					remote1.getOid());
		}

		// Verify remote does NOT have root in its list
		final TypeManyToManyDocOIDRemote remoteBefore = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
				remote1.getOid());
		Assertions.assertNull(remoteBefore.remoteToParent);

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeManyToManyDocOIDRoot.class, "remote", false, false);
		LOGGER.info("M2M missing remote link report: {}", report);
		Assertions.assertEquals(1, report.getMissingLinksAdded());

		// Verify remote now has root in its list
		final TypeManyToManyDocOIDRemote remoteAfter = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
				remote1.getOid());
		Assertions.assertNotNull(remoteAfter.remoteToParent);
		Assertions.assertTrue(remoteAfter.remoteToParent.contains(root1.getOid()));

		// Cleanup
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRoot.class, root1.getOid());
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRemote.class, remote1.getOid());
	}

	@Order(3)
	@Test
	public void testRepairManyToMany_brokenRef() throws Exception {
		// Create root, add a non-existent ObjectId to its list
		final TypeManyToManyDocOIDRoot root1 = ConfigureDb.da.insert(createM2MRoot("root_m2m_broken"));
		final ObjectId fakeId = new ObjectId();

		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel rootModel = DbClassModel.of(TypeManyToManyDocOIDRoot.class);
			final DbPropertyDescriptor remoteFieldDesc = rootModel.findByPropertyName("remote");
			final String fieldColumn = remoteFieldDesc.getFieldName(null).inTable();
			MongoLinkManager.addToList(db, TypeManyToManyDocOIDRoot.class, root1.getOid(), fieldColumn, fakeId);
		}

		// Verify root has the fake ID
		final TypeManyToManyDocOIDRoot rootBefore = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
				root1.getOid());
		Assertions.assertNotNull(rootBefore.remote);
		Assertions.assertTrue(rootBefore.remote.contains(fakeId));

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeManyToManyDocOIDRoot.class, "remote", false, false);
		LOGGER.info("M2M broken ref report: {}", report);
		Assertions.assertEquals(1, report.getBrokenLinksRemoved());

		// Verify fake ID is removed
		final TypeManyToManyDocOIDRoot rootAfter = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
				root1.getOid());
		Assertions.assertNull(rootAfter.remote);

		// Cleanup
		ConfigureDb.da.deleteById(TypeManyToManyDocOIDRoot.class, root1.getOid());
	}

	// ==================== OneToMany tests ====================

	@Order(4)
	@Test
	public void testRepairOneToMany_childMissing() throws Exception {
		// Create parent with a reference to a non-existent child
		final TypeOneToManyDocOIDRoot parent = ConfigureDb.da.insert(createO2MRoot("parent_o2m_broken"));
		final ObjectId fakeChildId = new ObjectId();

		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel parentModel = DbClassModel.of(TypeOneToManyDocOIDRoot.class);
			final DbPropertyDescriptor remoteIdsDesc = parentModel.findByPropertyName("remoteIds");
			final String fieldColumn = remoteIdsDesc.getFieldName(null).inTable();
			MongoLinkManager.addToList(db, TypeOneToManyDocOIDRoot.class, parent.getOid(), fieldColumn, fakeChildId);
		}

		// Verify parent has the fake child ID
		final TypeOneToManyDocOIDRoot parentBefore = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				parent.getOid());
		Assertions.assertNotNull(parentBefore.remoteIds);
		Assertions.assertTrue(parentBefore.remoteIds.contains(fakeChildId));

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeOneToManyDocOIDRoot.class, "remoteIds", false,
				false);
		LOGGER.info("O2M child missing report: {}", report);
		Assertions.assertEquals(1, report.getBrokenLinksRemoved());

		// Verify fake ID is removed
		final TypeOneToManyDocOIDRoot parentAfter = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				parent.getOid());
		Assertions.assertNull(parentAfter.remoteIds);

		// Cleanup
		ConfigureDb.da.deleteById(TypeOneToManyDocOIDRoot.class, parent.getOid());
	}

	@Order(5)
	@Test
	public void testRepairOneToMany_childOrphan() throws Exception {
		// Create parent and child, but only set parent ref on child side (not in parent's list)
		final TypeOneToManyDocOIDRoot parent = ConfigureDb.da.insert(createO2MRoot("parent_o2m_orphan"));
		final TypeOneToManyDocOIDRemote child = new TypeOneToManyDocOIDRemote();
		child.data = "orphan_child";
		child.rootOid = parent.getOid();
		final TypeOneToManyDocOIDRemote insertedChild = ConfigureDb.da.insert(child);

		// The insert should have set rootOid via ManyToOneDoc, but let's verify parent does NOT list this child
		// by forcing: remove child from parent's list if it was auto-added
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel parentModel = DbClassModel.of(TypeOneToManyDocOIDRoot.class);
			final DbPropertyDescriptor remoteIdsDesc = parentModel.findByPropertyName("remoteIds");
			final String fieldColumn = remoteIdsDesc.getFieldName(null).inTable();
			MongoLinkManager.removeFromList(db, TypeOneToManyDocOIDRoot.class, parent.getOid(), fieldColumn,
					insertedChild.getOid());
		}

		// Verify parent does not list child
		final TypeOneToManyDocOIDRoot parentBefore = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				parent.getOid());
		Assertions.assertNull(parentBefore.remoteIds);

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeOneToManyDocOIDRoot.class, "remoteIds", false,
				false);
		LOGGER.info("O2M child orphan report: {}", report);
		Assertions.assertEquals(1, report.getMissingLinksAdded());

		// Verify parent now lists child
		final TypeOneToManyDocOIDRoot parentAfter = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				parent.getOid());
		Assertions.assertNotNull(parentAfter.remoteIds);
		Assertions.assertTrue(parentAfter.remoteIds.contains(insertedChild.getOid()));

		// Cleanup
		ConfigureDb.da.deleteById(TypeOneToManyDocOIDRemote.class, insertedChild.getOid());
		ConfigureDb.da.deleteById(TypeOneToManyDocOIDRoot.class, parent.getOid());
	}

	// ==================== ManyToOne tests ====================

	@Order(6)
	@Test
	public void testRepairManyToOne_parentMissing() throws Exception {
		// Create child pointing to a non-existent parent
		final ObjectId fakeParentId = new ObjectId();
		final TypeManyToOneDocOIDChildTTT child = new TypeManyToOneDocOIDChildTTT();
		child.otherData = "child_m2o_broken";
		child.parentOid = null; // insert without parent first
		final TypeManyToOneDocOIDChildTTT insertedChild = ConfigureDb.da.insert(child);

		// Directly set a fake parent ID on the child
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel childModel = DbClassModel.of(TypeManyToOneDocOIDChildTTT.class);
			final DbPropertyDescriptor parentOidDesc = childModel.findByPropertyName("parentOid");
			final String fieldColumn = parentOidDesc.getFieldName(null).inTable();
			MongoLinkManager.setField(db, TypeManyToOneDocOIDChildTTT.class, insertedChild.getOid(), fieldColumn,
					fakeParentId);
		}

		// Verify child has the fake parent ID
		final TypeManyToOneDocOIDChildTTT childBefore = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
				insertedChild.getOid());
		Assertions.assertEquals(fakeParentId, childBefore.parentOid);

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeManyToOneDocOIDChildTTT.class, "parentOid", false,
				false);
		LOGGER.info("M2O parent missing report: {}", report);
		Assertions.assertEquals(1, report.getBrokenLinksRemoved());

		// Verify child's parent is now null
		final TypeManyToOneDocOIDChildTTT childAfter = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
				insertedChild.getOid());
		Assertions.assertNull(childAfter.parentOid);

		// Cleanup
		ConfigureDb.da.deleteById(TypeManyToOneDocOIDChildTTT.class, insertedChild.getOid());
	}

	@Order(7)
	@Test
	public void testRepairManyToOne_parentNotListing() throws Exception {
		// Create parent and child, child points to parent but parent doesn't list child
		final TypeManyToOneDocOIDParentIgnore parent = new TypeManyToOneDocOIDParentIgnore();
		parent.data = "parent_m2o_notlisting";
		final TypeManyToOneDocOIDParentIgnore insertedParent = ConfigureDb.da.insert(parent);

		final TypeManyToOneDocOIDChildTTT child = new TypeManyToOneDocOIDChildTTT();
		child.otherData = "child_m2o_notlisting";
		child.parentOid = null;
		final TypeManyToOneDocOIDChildTTT insertedChild = ConfigureDb.da.insert(child);

		// Directly set parent on child without updating parent's list
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			final DbClassModel childModel = DbClassModel.of(TypeManyToOneDocOIDChildTTT.class);
			final DbPropertyDescriptor parentOidDesc = childModel.findByPropertyName("parentOid");
			final String fieldColumn = parentOidDesc.getFieldName(null).inTable();
			MongoLinkManager.setField(db, TypeManyToOneDocOIDChildTTT.class, insertedChild.getOid(), fieldColumn,
					insertedParent.getOid());
		}

		// Verify parent doesn't list child
		final TypeManyToOneDocOIDParentIgnore parentBefore = ConfigureDb.da
				.getById(TypeManyToOneDocOIDParentIgnore.class, insertedParent.getOid());
		Assertions.assertNull(parentBefore.childOids);

		// Repair
		final RepairReport report = LinkRepairTools.repairLinks(TypeManyToOneDocOIDChildTTT.class, "parentOid", false,
				false);
		LOGGER.info("M2O parent not listing report: {}", report);
		Assertions.assertEquals(1, report.getMissingLinksAdded());

		// Verify parent now lists child
		final TypeManyToOneDocOIDParentIgnore parentAfter = ConfigureDb.da
				.getById(TypeManyToOneDocOIDParentIgnore.class, insertedParent.getOid());
		Assertions.assertNotNull(parentAfter.childOids);
		Assertions.assertTrue(parentAfter.childOids.contains(insertedChild.getOid()));

		// Cleanup
		ConfigureDb.da.deleteById(TypeManyToOneDocOIDChildTTT.class, insertedChild.getOid());
		ConfigureDb.da.deleteById(TypeManyToOneDocOIDParentIgnore.class, insertedParent.getOid());
	}

	// ==================== Helpers ====================

	private static TypeManyToManyDocOIDRoot createM2MRoot(final String data) {
		final TypeManyToManyDocOIDRoot root = new TypeManyToManyDocOIDRoot();
		root.otherData = data;
		return root;
	}

	private static TypeManyToManyDocOIDRemote createM2MRemote(final String data) {
		final TypeManyToManyDocOIDRemote remote = new TypeManyToManyDocOIDRemote();
		remote.data = data;
		return remote;
	}

	private static TypeOneToManyDocOIDRoot createO2MRoot(final String data) {
		final TypeOneToManyDocOIDRoot root = new TypeOneToManyDocOIDRoot();
		root.otherData = data;
		return root;
	}
}
