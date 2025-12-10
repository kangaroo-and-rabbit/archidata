package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOn.model.TableObjectGeneric;
import org.atriasoft.archidata.dataAccess.addOn.model.TableObjectGenericUpdateAt;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.exception.DataAccessException;

public class OneToManyTools {

	private static void setRemoteFieldToNull(
			final Class<?> clazz,
			final String primaryKeyTableName,
			final Object clazzPrimaryKeyValue,
			final String fieldTableName) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("primaryKey", clazzPrimaryKeyValue.getClass()));

		options.add(new OptionRenameColumn("primaryKey", primaryKeyTableName));
		options.add(new OptionRenameColumn("fieldToUpdate", fieldTableName));
		options.add(new AccessDeletedItems());
		final TableObjectGeneric data = new TableObjectGeneric();
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
		}
		data.fieldToUpdate = null;
		options.add(new FilterValue("fieldToUpdate"));
		DataAccess.updateById(data, clazzPrimaryKeyValue, options.getAllArray());
	}

	// the objective is to set a specific field at null
	public static void setRemoteNullRemote(final Field localField, final Object remotePrimaryKeyValue)
			throws Exception {
		final OneToManyDoc manyLocal = AnnotationTools.get(localField, OneToManyDoc.class);
		// Check if the element are correctly set:
		if (manyLocal == null || manyLocal.targetEntity() == null || manyLocal.remoteField() == null
				|| manyLocal.remoteField().isEmpty()) {
			return;
		}
		{
			// get local field to find the remote field name:
			final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(manyLocal.targetEntity());
			final FieldName primaryKeyColumnName = AnnotationTools.getFieldName(primaryKeyField, null);

			final Field remoteField = AnnotationTools.getFieldNamed(manyLocal.targetEntity(), manyLocal.remoteField());
			final FieldName localFieldName = AnnotationTools.getFieldName(remoteField, null);
			setRemoteFieldToNull(manyLocal.targetEntity(), primaryKeyColumnName.inTable(), remotePrimaryKeyValue,
					localFieldName.inTable());
		}
	}

	private static Object setRemoteFieldToValue(
			final Class<?> clazz,
			final String primaryKeyTableName,
			final Object clazzPrimaryKeyValue,
			final String fieldTableName,
			final Object valueToSet) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("primaryKey", clazzPrimaryKeyValue.getClass()),
				new OptionSpecifyType("fieldToUpdate", valueToSet.getClass()));

		options.add(new OptionRenameColumn("primaryKey", primaryKeyTableName));
		options.add(new OptionRenameColumn("fieldToUpdate", fieldTableName));
		options.add(new AccessDeletedItems());
		TableObjectGeneric data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = DataAccess.getById(TableObjectGenericUpdateAt.class, clazzPrimaryKeyValue, options.getAllArray());
		} else {
			data = DataAccess.getById(TableObjectGeneric.class, clazzPrimaryKeyValue, options.getAllArray());
		}
		if (Objects.equals(data.fieldToUpdate, valueToSet)) {
			// The object has already the good value ==> Nothing to do ...
			return null;
		}
		final Object previousValue = data.fieldToUpdate;
		data.fieldToUpdate = valueToSet;
		options.add(new FilterValue("fieldToUpdate"));
		DataAccess.updateById(data, clazzPrimaryKeyValue, options.getAllArray());
		return previousValue;
	}

	// return the previous value...
	public static Object setRemoteValue(
			final Field localField,
			final Object remotePrimaryKeyValue,
			final Object valueToSet) throws Exception {
		final OneToManyDoc manyLocal = AnnotationTools.get(localField, OneToManyDoc.class);
		// Check if the element are correctly set:
		if (manyLocal == null || manyLocal.targetEntity() == null || manyLocal.remoteField() == null
				|| manyLocal.remoteField().isEmpty()) {
			return null;
		}
		{
			// get local field to find the remote field name:
			final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(manyLocal.targetEntity());
			final FieldName primaryKeyColumnName = AnnotationTools.getFieldName(primaryKeyField, null);

			final Field remoteField = AnnotationTools.getFieldNamed(manyLocal.targetEntity(), manyLocal.remoteField());
			final FieldName localFieldName = AnnotationTools.getFieldName(remoteField, null);
			return setRemoteFieldToValue(manyLocal.targetEntity(), primaryKeyColumnName.inTable(),
					remotePrimaryKeyValue, localFieldName.inTable(), valueToSet);
		}
	}

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
		final OneToManyDoc annotation = AnnotationTools.get(field, OneToManyDoc.class);
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
