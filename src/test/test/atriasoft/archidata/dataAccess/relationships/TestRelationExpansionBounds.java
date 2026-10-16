package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.dataAccess.RelationExpansion;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-06: the expansion of entity-typed relations is bounded in depth and stops on cycles. */
class TestRelationExpansionBounds {
	private static final String COLLECTION = "relationExpansionNode";

	/** Raw view used to write the links without any add-on logic. */
	@Collection(COLLECTION)
	public static class NodeRaw {
		@Id
		public ObjectId _id = null;
		public String name;
		public ObjectId parentId;
		public List<ObjectId> children;
	}

	/** Self-referencing view: the parent is expanded as an entity. */
	@Collection(COLLECTION)
	public static class NodeExpand {
		@Id
		public ObjectId _id = null;
		public String name;
		@ManyToOneDoc(targetEntity = NodeExpand.class, remoteField = "children")
		@Field("parentId")
		public NodeExpand parent;
		public List<ObjectId> children;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		RelationExpansion.setMaxDepth(RelationExpansion.DEFAULT_MAX_DEPTH);
		ConfigureDb.clear();
	}

	private static ObjectId insert(final String name) throws Exception {
		final NodeRaw node = new NodeRaw();
		node.name = name;
		return ConfigureDb.da.insert(node)._id;
	}

	private static void link(final ObjectId child, final ObjectId parent) throws Exception {
		final NodeRaw patch = new NodeRaw();
		patch.parentId = parent;
		ConfigureDb.da.updateById(patch, child, new FilterValue("parentId"));
	}

	@Test
	void testCycleStops() throws Exception {
		// a -> b -> a: used to recurse until StackOverflowError.
		final ObjectId a = insert("a");
		final ObjectId b = insert("b");
		link(a, b);
		link(b, a);
		final NodeExpand read = ConfigureDb.da.getById(NodeExpand.class, a);
		Assertions.assertEquals("a", read.name);
		Assertions.assertEquals("b", read.parent.name);
		// The second 'a' is read, but not expanded again.
		Assertions.assertEquals("a", read.parent.parent.name);
		Assertions.assertNull(read.parent.parent.parent);
		// Self-reference.
		final ObjectId self = insert("self");
		link(self, self);
		final NodeExpand selfRead = ConfigureDb.da.getById(NodeExpand.class, self);
		Assertions.assertEquals("self", selfRead.parent.name);
		Assertions.assertNull(selfRead.parent.parent);
	}

	@Test
	void testDepthIsBounded() throws Exception {
		// Chain n0 -> n1 -> ... -> n9 (no cycle).
		ObjectId previous = null;
		ObjectId first = null;
		final ObjectId[] ids = new ObjectId[10];
		for (int iii = 9; iii >= 0; iii--) {
			ids[iii] = insert("n" + iii);
			if (previous != null) {
				link(ids[iii], previous);
			}
			previous = ids[iii];
		}
		first = ids[0];
		RelationExpansion.setMaxDepth(3);
		try {
			NodeExpand node = ConfigureDb.da.getById(NodeExpand.class, first);
			int expanded = 0;
			while (node.parent != null) {
				node = node.parent;
				expanded++;
			}
			Assertions.assertEquals(3, expanded);
			Assertions.assertEquals("n3", node.name);
		} finally {
			RelationExpansion.setMaxDepth(RelationExpansion.DEFAULT_MAX_DEPTH);
		}
		Assertions.assertThrows(IllegalArgumentException.class, () -> RelationExpansion.setMaxDepth(-1));
		// The state does not leak: a new read starts again at depth 0.
		NodeExpand node = ConfigureDb.da.getById(NodeExpand.class, first);
		int expanded = 0;
		while (node.parent != null) {
			node = node.parent;
			expanded++;
		}
		Assertions.assertEquals(RelationExpansion.DEFAULT_MAX_DEPTH, expanded);
	}
}
