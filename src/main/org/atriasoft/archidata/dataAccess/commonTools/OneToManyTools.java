package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableCoversGeneric;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableCoversGenericUpdateAt;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableFieldUpdate;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;

public class OneToManyTools {
	
	private static void setRemoteFieldToNull(final Class<?> clazz, final Object primaryKey, final String fieldName)
			throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("idOfTheObject", clazzPrimaryKeyValue.getClass()),
				new OptionSpecifyType("filedNameOfTheObject", valueToRemove.getClass(), true));
		options.add(new OptionRenameColumn("idOfTheObject", clazzPrimaryKeyName));
		options.add(new OptionRenameColumn("filedNameOfTheObject", fieldNameToUpdate));
		options.add(new AccessDeletedItems());
		TableFieldUpdate data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = DataAccess.get(TableCoversGenericUpdateAt.class, clazzPrimaryKeyValue, options.getAllArray());
		} else {
			data = DataAccess.get(TableCoversGeneric.class, clazzPrimaryKeyValue, options.getAllArray());
		}
		if (data.filedNameOfTheObject == null) {
			return;
		}
		final List<Object> newList = new ArrayList<>();
		for (final Object elem : data.filedNameOfTheObject) {
			if (elem.equals(valueToRemove)) {
				continue;
			}
			newList.add(elem);
		}
		data.filedNameOfTheObject = newList;
		if (data.filedNameOfTheObject.isEmpty()) {
			data.filedNameOfTheObject = null;
		}
		options.add(new FilterValue("filedNameOfTheObject"));
		DataAccess.updateFull(data, data.idOfTheObject, options.getAllArray());
	}
	
	public static void setRemoteNullRemote(
			final Field localField,
			final Object localPrimaryKeyValue,
			final Object remotePrimaryKeyValue) throws Exception {
		final OneToManyDoc manyLocal = AnnotationTools.get(localField, OneToManyDoc.class);
		// Update the remote elements:
		if (manyLocal == null || manyLocal.targetEntity() == null || manyLocal.remoteField() == null
				|| manyLocal.remoteField().isEmpty()) {
			return;
		}
		{
			//get local field to find the remote field name:
			final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(manyLocal.targetEntity());
			final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);
			final Field remoteField = AnnotationTools.getFieldNamed(manyLocal.targetEntity(), manyLocal.remoteField());
			final FieldName localFieldName = AnnotationTools.getFieldName(remoteField, null);
			setRemoteNullLocal(manyLocal.targetEntity(), primaryKeyColomnName.inTable(), remotePrimaryKeyValue,
					localFieldName.inTable(), localPrimaryKeyValue);
		}
	}
	
}
