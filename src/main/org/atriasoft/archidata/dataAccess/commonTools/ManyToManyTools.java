package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableListObjectGeneric;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableListObjectGenericUpdateAt;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;

public class ManyToManyTools {

	private static void addLinkLocal(
			final DBAccess ioDb,
			final Class<?> clazz,
			final String clazzPrimaryKeyName,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToAdd) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("idOfTheObject", clazzPrimaryKeyValue.getClass()),
				new OptionSpecifyType("filedNameOfTheObject", valueToAdd.getClass(), true));
		options.add(new OptionRenameColumn("idOfTheObject", clazzPrimaryKeyName));
		options.add(new OptionRenameColumn("filedNameOfTheObject", fieldNameToUpdate));
		options.add(new AccessDeletedItems());
		TableListObjectGeneric data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = ioDb.get(TableListObjectGenericUpdateAt.class, clazzPrimaryKeyValue, options.getAllArray());
		} else {
			data = ioDb.get(TableListObjectGeneric.class, clazzPrimaryKeyValue, options.getAllArray());
		}
		if (data.filedNameOfTheObject == null) {
			data.filedNameOfTheObject = new ArrayList<>();
		}
		for (final Object elem : data.filedNameOfTheObject) {
			if (elem.equals(valueToAdd)) {
				return;
			}
		}
		data.filedNameOfTheObject.add(valueToAdd);
		options.add(new FilterValue("filedNameOfTheObject"));
		ioDb.updateFull(data, data.idOfTheObject, options.getAllArray());
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToAdd) throws Exception {
		final Field localField = AnnotationTools.getFieldNamed(clazz, fieldNameToUpdate);
		{
			//get local field to find the remote field name:
			final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
			final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);
			final FieldName localFieldName = AnnotationTools.getFieldName(localField, null);
			addLinkLocal(ioDb, clazz, primaryKeyColomnName.inTable(), clazzPrimaryKeyValue, localFieldName.inTable(),
					valueToAdd);
		}
		addLinkRemote(ioDb, localField, clazzPrimaryKeyValue, valueToAdd);
	}

	public static void addLinkRemote(
			final DBAccess ioDb,
			final Field localField,
			final Object localPrimaryKeyValue,
			final Object remotePrimaryKeyValue) throws Exception {
		final ManyToManyDoc manyLocal = AnnotationTools.get(localField, ManyToManyDoc.class);
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
			addLinkLocal(ioDb, manyLocal.targetEntity(), primaryKeyColomnName.inTable(), remotePrimaryKeyValue,
					localFieldName.inTable(), localPrimaryKeyValue);
		}
	}

	private static void removeLinkLocal(
			final DBAccess ioDb,
			final Class<?> clazz,
			final String clazzPrimaryKeyName,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToRemove) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("idOfTheObject", clazzPrimaryKeyValue.getClass()),
				new OptionSpecifyType("filedNameOfTheObject", valueToRemove.getClass(), true));
		options.add(new OptionRenameColumn("idOfTheObject", clazzPrimaryKeyName));
		options.add(new OptionRenameColumn("filedNameOfTheObject", fieldNameToUpdate));
		options.add(new AccessDeletedItems());
		TableListObjectGeneric data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = ioDb.get(TableListObjectGenericUpdateAt.class, clazzPrimaryKeyValue, options.getAllArray());
		} else {
			data = ioDb.get(TableListObjectGeneric.class, clazzPrimaryKeyValue, options.getAllArray());
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
		ioDb.updateFull(data, data.idOfTheObject, options.getAllArray());
	}

	public static void removeLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object clazzPrimaryKeyValue,
			final String fieldNameToUpdate,
			final Object valueToRemove) throws Exception {

		final Field localField = AnnotationTools.getFieldNamed(clazz, fieldNameToUpdate);
		{
			//get local field to find the remote field name:
			final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
			final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);
			final FieldName localFieldName = AnnotationTools.getFieldName(localField, null);
			removeLinkLocal(ioDb, clazz, primaryKeyColomnName.inTable(), clazzPrimaryKeyValue, localFieldName.inTable(),
					valueToRemove);
		}
		removeLinkRemote(ioDb, localField, clazzPrimaryKeyValue, valueToRemove);
	}

	public static void removeLinkRemote(
			final DBAccess ioDb,
			final Field localField,
			final Object localPrimaryKeyValue,
			final Object remotePrimaryKeyValue) throws Exception {
		final ManyToManyDoc manyLocal = AnnotationTools.get(localField, ManyToManyDoc.class);
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
			removeLinkLocal(ioDb, manyLocal.targetEntity(), primaryKeyColomnName.inTable(), remotePrimaryKeyValue,
					localFieldName.inTable(), localPrimaryKeyValue);
		}
	}

}
