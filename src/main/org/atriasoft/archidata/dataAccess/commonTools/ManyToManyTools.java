package org.atriasoft.archidata.dataAccess.commonTools;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Utility for managing ManyToMany bidirectional links.
 *
 * <p>All link operations now use atomic MongoDB operations ({@code $addToSet}, {@code $pull})
 * via {@link MongoLinkManager} instead of read-modify-write.
 */
public class ManyToManyTools {

	/**
	 * Add a bidirectional ManyToMany link.
	 * Atomically adds valueToAdd to the local field's array AND adds
	 * clazzPrimaryKeyValue to the remote field's array.
	 */
	public static void addLink(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToAdd) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final DbPropertyDescriptor fieldDesc = model.findByPropertyName(fieldNameToUpdate);
		if (fieldDesc == null) {
			throw new DataAccessException(
					"Cannot find field '" + fieldNameToUpdate + "' in " + clazz.getCanonicalName());
		}
		final String localFieldColumn = fieldDesc.getFieldName(null).inTable();

		// Add to local array
		MongoLinkManager.addToList(ioDb, clazz, clazzPrimaryKeyValue, localFieldColumn, valueToAdd);

		// Add to remote array (bidirectional)
		final ManyToManyDoc manyDoc = fieldDesc.getProperty().getAnnotation(ManyToManyDoc.class);
		if (manyDoc != null && manyDoc.targetEntity() != null
				&& manyDoc.remoteField() != null && !manyDoc.remoteField().isEmpty()) {
			final DbClassModel targetModel = DbClassModel.of(manyDoc.targetEntity());
			final DbPropertyDescriptor remoteDesc = targetModel.findByPropertyName(manyDoc.remoteField());
			if (remoteDesc == null) {
				throw new DataAccessException("Cannot find remote field '" + manyDoc.remoteField()
						+ "' in " + manyDoc.targetEntity().getSimpleName());
			}
			final String remoteFieldColumn = remoteDesc.getFieldName(null).inTable();
			MongoLinkManager.addToList(ioDb, manyDoc.targetEntity(), valueToAdd,
					remoteFieldColumn, clazzPrimaryKeyValue);
		}
	}

	/**
	 * Remove a bidirectional ManyToMany link.
	 * Atomically removes valueToRemove from the local field's array AND removes
	 * clazzPrimaryKeyValue from the remote field's array.
	 */
	public static void removeLink(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToRemove) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final DbPropertyDescriptor fieldDesc = model.findByPropertyName(fieldNameToUpdate);
		if (fieldDesc == null) {
			throw new DataAccessException(
					"Cannot find field '" + fieldNameToUpdate + "' in " + clazz.getCanonicalName());
		}
		final String localFieldColumn = fieldDesc.getFieldName(null).inTable();

		// Remove from local array
		MongoLinkManager.removeFromList(ioDb, clazz, clazzPrimaryKeyValue, localFieldColumn, valueToRemove);

		// Remove from remote array (bidirectional)
		final ManyToManyDoc manyDoc = fieldDesc.getProperty().getAnnotation(ManyToManyDoc.class);
		if (manyDoc != null && manyDoc.targetEntity() != null
				&& manyDoc.remoteField() != null && !manyDoc.remoteField().isEmpty()) {
			final DbClassModel targetModel = DbClassModel.of(manyDoc.targetEntity());
			final DbPropertyDescriptor remoteDesc = targetModel.findByPropertyName(manyDoc.remoteField());
			if (remoteDesc == null) {
				throw new DataAccessException("Cannot find remote field '" + manyDoc.remoteField()
						+ "' in " + manyDoc.targetEntity().getSimpleName());
			}
			final String remoteFieldColumn = remoteDesc.getFieldName(null).inTable();
			MongoLinkManager.removeFromList(ioDb, manyDoc.targetEntity(), valueToRemove,
					remoteFieldColumn, clazzPrimaryKeyValue);
		}
	}

	/**
	 * Rebuild all remote links for a ManyToMany field.
	 * Useful after adding the annotation or correcting data.
	 */
	public static <T> void updateRemoteLinks(
			final Class<T> clazz,
			final String fieldName,
			final boolean resetRemote) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final DbPropertyDescriptor fieldDesc = model.findByPropertyName(fieldName);
		if (fieldDesc == null) {
			throw new DataAccessException(
					"Cannot find field '" + fieldName + "' in " + clazz.getCanonicalName());
		}
		final DbPropertyDescriptor pkDesc = model.getPrimaryKey();
		if (pkDesc == null) {
			throw new DataAccessException(
					"Cannot find primary key in " + clazz.getCanonicalName());
		}
		final ManyToManyDoc annotation = fieldDesc.getProperty().getAnnotation(ManyToManyDoc.class);
		if (annotation == null) {
			throw new DataAccessException("Cannot find @ManyToManyDoc in "
					+ clazz.getCanonicalName() + " for field '" + fieldName + "'");
		}
		// Step 1: get all data
		final List<T> data = DataAccess.gets(clazz);
		// Step 2: clear remote elements if requested
		if (resetRemote) {
			FieldTools.setFieldAtNull(annotation.targetEntity(), annotation.remoteField());
		}
		// Step 3: force re-update by clearing and re-setting
		final PropertyDescriptor fieldProp = fieldDesc.getProperty();
		final PropertyDescriptor pkProp = pkDesc.getProperty();
		for (final T elem : data) {
			final Object dataTemp = fieldProp.getValue(elem);
			final Object primaryKey = pkProp.getValue(elem);
			fieldProp.setValue(elem, null);
			DataAccess.updateById(elem, primaryKey);
			fieldProp.setValue(elem, dataTemp);
			DataAccess.updateById(elem, primaryKey);
		}
	}
}
