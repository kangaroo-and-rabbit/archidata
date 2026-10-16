package test.atriasoft.archidata.dataAccess;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * An empty or blank {@code @Collection} or {@code @Field} name is rejected instead of producing an empty
 * collection name or silently falling back to the Java name.
 */
public class TestEmptyAnnotationNames {

	@Collection("")
	public static class EmptyCollection {
		public ObjectId oid;
	}

	@Collection("  ")
	public static class BlankCollection {
		public ObjectId oid;
	}

	public static class EmptyField {
		public ObjectId oid;
		@Field("")
		public String name;
	}

	@Collection("named")
	public static class Named {
		public ObjectId oid;
		@Field("full_name")
		public String name;
	}

	private static String errorOf(final Class<?> clazz) {
		final IntrospectionException ex = Assertions.assertThrows(IntrospectionException.class,
				() -> DbClassModel.of(clazz));
		final DataAccessException cause = Assertions.assertInstanceOf(DataAccessException.class, ex.getCause());
		return cause.getMessage();
	}

	@Test
	public void testEmptyCollectionIsRejected() {
		final String message = errorOf(EmptyCollection.class);
		Assertions.assertTrue(message.contains(EmptyCollection.class.getName()), message);
		Assertions.assertTrue(message.contains("@Collection(\"\")"), message);
		Assertions.assertThrows(DataAccessException.class,
				() -> AnnotationTools.getTableName(EmptyCollection.class, null));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> AnnotationTools.getTableName(EmptyCollection.class));
	}

	@Test
	public void testBlankCollectionIsRejected() {
		Assertions.assertThrows(DataAccessException.class, () -> AnnotationTools.checkTableName(BlankCollection.class));
	}

	@Test
	public void testEmptyFieldIsRejected() throws Exception {
		final String message = errorOf(EmptyField.class);
		Assertions.assertTrue(message.contains(EmptyField.class.getName() + ".name"), message);
		Assertions.assertTrue(message.contains("@Field(\"\")"), message);
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> AnnotationTools.getFieldNameRaw(EmptyField.class.getField("name")));
	}

	@Test
	public void testNamesAreRead() throws Exception {
		final DbClassModel model = DbClassModel.of(Named.class);
		Assertions.assertEquals("named", model.getTableName());
		Assertions.assertEquals("full_name", AnnotationTools.getFieldNameRaw(Named.class.getField("name")));
	}
}
