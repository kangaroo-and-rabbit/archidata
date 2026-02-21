package org.atriasoft.archidata.dataAccess.commonTools;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Utility for adding/removing values in array fields of MongoDB documents.
 *
 * <p>This is a convenience wrapper around {@link MongoLinkManager} that resolves
 * field names from Java property names using {@link DbClassModel}.
 *
 * <p>Delegates to atomic MongoDB operations ({@code $addToSet}, {@code $pull})
 * instead of the previous read-modify-write pattern.
 */
public class ListInDbTools {

	public static void addLink(
			final Class<?> clazz,
			final Object primaryKey,
			final String fieldName,
			final Object foreignKey) throws Exception {
		final String fieldColumn = resolveFieldColumn(clazz, fieldName);
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			MongoLinkManager.addToList(db, clazz, primaryKey, fieldColumn, foreignKey);
		}
	}

	public static void removeLink(
			final Class<?> clazz,
			final Object primaryKey,
			final String fieldName,
			final Object foreignKey) throws Exception {
		final String fieldColumn = resolveFieldColumn(clazz, fieldName);
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			MongoLinkManager.removeFromList(db, clazz, primaryKey, fieldColumn, foreignKey);
		}
	}

	private static String resolveFieldColumn(final Class<?> clazz, final String fieldName) throws Exception {
		if (fieldName == null) {
			throw new DataAccessException(
					"fieldName is null in ListInDbTools for class: " + clazz.getCanonicalName());
		}
		final DbClassModel model = DbClassModel.of(clazz);
		// Try property name first
		final DbPropertyDescriptor desc = model.findByPropertyName(fieldName);
		if (desc != null) {
			return desc.getFieldName(null).inTable();
		}
		// Fallback: try DB field name (for callers passing column names directly)
		final DbPropertyDescriptor descByDb = model.findByDbFieldName(fieldName);
		if (descByDb != null) {
			return descByDb.getFieldName(null).inTable();
		}
		throw new DataAccessException("Cannot find field '" + fieldName + "' in " + clazz.getCanonicalName());
	}
}
