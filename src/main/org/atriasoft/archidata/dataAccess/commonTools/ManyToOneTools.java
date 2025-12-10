package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.exception.DataAccessException;

public class ManyToOneTools {

	/**
	 * This function update the remote field with the correct value, it permit to update a remote filed after adding the decorator or correct a fail in the BDD.
	 * @param clazz Class that the correct data is present
	 * @param fieldName Name if the field to update
	 * @param resetRemote Clear the remote data before updating
	 */
	public static <T> void updateRemoteLinks(final Class<T> clazz, final String fieldName, final boolean resetRemote)
			throws Exception {
		final Field field = clazz.getField(fieldName);
		if (field == null) {
			throw new DataAccessException(
					"Fail to find the field name:'" + fieldName + "' in class: " + clazz.getCanonicalName());
		}
		final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
		if (primaryKeyField == null) {
			throw new DataAccessException(
					"Fail to find the primary field not found in class: " + clazz.getCanonicalName());
		}
		final ManyToOneDoc annotation = AnnotationTools.get(field, ManyToOneDoc.class);
		if (annotation == null) {
			throw new DataAccessException("Fail to find the annotation:'@ManyToManyDoc' in class: "
					+ clazz.getCanonicalName() + " for fieldName='" + fieldName + "'");
		}
		// Step 1 get all the data (prevent clear removing)
		final List<T> data = DataAccess.gets(clazz);
		// Step 2 clear the remote elements
		if (resetRemote) {
			FieldTools.setFieldAtNull(annotation.targetEntity(), annotation.remoteField());
		}
		// Step 3 force the system to update the values
		for (final T elem : data) {
			final Object dataTemp = field.get(elem);
			final Object primaryKey = primaryKeyField.get(elem);
			field.set(elem, null);
			DataAccess.updateById(elem, primaryKey);
			field.set(elem, dataTemp);
			DataAccess.updateById(elem, primaryKey);
		}
	}
}
