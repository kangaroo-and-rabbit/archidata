package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.Objects;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableObjectGeneric;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableObjectGenericUpdateAt;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;

public class OneToManyTools {

	private static void setRemoteFieldToNull(
			final Class<?> clazz,
			String primaryKeyTableName,
			final Object clazzPrimaryKeyValue,
			final String fieldTableName) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("primaryKey", clazzPrimaryKeyValue.getClass()));

		options.add(new OptionRenameColumn("primaryKey", primaryKeyTableName));
		options.add(new OptionRenameColumn("fieldToUpdate", fieldTableName));
		options.add(new AccessDeletedItems());
		TableObjectGeneric data = new TableObjectGeneric();
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
		}
		data.fieldToUpdate = null;
		options.add(new FilterValue("fieldToUpdate"));
		DataAccess.updateFull(data, clazzPrimaryKeyValue, options.getAllArray());
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
			final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);

			final Field remoteField = AnnotationTools.getFieldNamed(manyLocal.targetEntity(), manyLocal.remoteField());
			final FieldName localFieldName = AnnotationTools.getFieldName(remoteField, null);
			setRemoteFieldToNull(manyLocal.targetEntity(), primaryKeyColomnName.inTable(), remotePrimaryKeyValue,
					localFieldName.inTable());
		}
	}

	private static Object setRemoteFieldToValue(
			final Class<?> clazz,
			String primaryKeyTableName,
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
			data = DataAccess.get(TableObjectGenericUpdateAt.class, clazzPrimaryKeyValue, options.getAllArray());
		} else {
			data = DataAccess.get(TableObjectGeneric.class, clazzPrimaryKeyValue, options.getAllArray());
		}
		if (Objects.equals(data.fieldToUpdate, valueToSet)) {
			// The object has already the good value ==> Nothing to do ...
			return null;
		}
		Object previousValue = data.fieldToUpdate;
		data.fieldToUpdate = valueToSet;
		options.add(new FilterValue("fieldToUpdate"));
		DataAccess.updateFull(data, clazzPrimaryKeyValue, options.getAllArray());
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
			final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);

			final Field remoteField = AnnotationTools.getFieldNamed(manyLocal.targetEntity(), manyLocal.remoteField());
			final FieldName localFieldName = AnnotationTools.getFieldName(remoteField, null);
			return setRemoteFieldToValue(manyLocal.targetEntity(), primaryKeyColomnName.inTable(),
					remotePrimaryKeyValue, localFieldName.inTable(), valueToSet);
		}
	}

}
