package test.atriasoft.archidata.dataAccess.relationships;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A relation field whose key type is not ObjectId, Long or UUID must be rejected, naming the class and the field. */
public class TestRelationUnsupportedKey {

	@Table(name = "RelBadManyToOne")
	public static class BadManyToOne {
		@Id
		public String id;
		@ManyToOneDoc(targetEntity = BadOneToMany.class, remoteField = "childIds")
		public String parentId;
	}

	@Table(name = "RelBadOneToMany")
	public static class BadOneToMany {
		@Id
		public String id;
		@OneToManyDoc(targetEntity = BadManyToOne.class, remoteField = "parentId")
		public List<String> childIds;
	}

	@Table(name = "RelBadManyToMany")
	public static class BadManyToMany {
		@Id
		public String id;
		@ManyToManyDoc(targetEntity = BadManyToMany.class, remoteField = "other")
		public List<String> other;
	}

	private static void assertRejected(final Class<?> clazz, final String field, final String annotation) {
		final IntrospectionException e = Assertions.assertThrows(IntrospectionException.class,
				() -> DbClassModel.of(clazz));
		Assertions.assertInstanceOf(DataAccessException.class, e.getCause());
		final String message = e.getCause().getMessage();
		Assertions.assertTrue(message.contains(clazz.getName()), message);
		Assertions.assertTrue(message.contains("." + field + " "), message);
		Assertions.assertTrue(message.contains(annotation), message);
	}

	@Test
	public void testManyToOneStringKey() {
		assertRejected(BadManyToOne.class, "parentId", "@ManyToOneDoc");
	}

	@Test
	public void testOneToManyStringKey() {
		assertRejected(BadOneToMany.class, "childIds", "@OneToManyDoc");
	}

	@Test
	public void testManyToManyStringKey() {
		assertRejected(BadManyToMany.class, "other", "@ManyToManyDoc");
	}
}
