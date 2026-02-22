package org.atriasoft.archidata.dataAccess.commonTools;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.exception.DataAccessException;

public class FieldTools {
	private FieldTools() {
		// Utility class
	}

	/**
	 * Set a specific field to null on ALL documents of the given class.
	 * Uses a single atomic {@code updateMany} with {@code $unset}.
	 *
	 * @param clazz Class to update in the DB.
	 * @param fieldName Name of the Java property to reset.
	 */
	public static <T> void setFieldAtNull(final Class<T> clazz, final String fieldName) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final DbPropertyDescriptor fieldDesc = model.findByPropertyName(fieldName);
		if (fieldDesc == null) {
			throw new DataAccessException(
					"Cannot find field '" + fieldName + "' in " + clazz.getCanonicalName());
		}
		final String fieldColumn = fieldDesc.getFieldName(null).inTable();
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			MongoLinkManager.setFieldToNullWhere(db, clazz, null, null, fieldColumn);
		}
	}
}
