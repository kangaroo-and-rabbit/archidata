package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableCoversGeneric;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.TableCoversGenericUpdateAt;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;

public class ListInDbTools {

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object primaryKey,
			final String fieldName,
			final Object foreignKey) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		// TODO: check the type of the keys...
		final String tableName = AnnotationTools.getTableName(clazz);
		final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
		final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);

		final Field localField = AnnotationTools.getFieldNamed(clazz, fieldName);
		final FieldName localFieldColomnName = AnnotationTools.getFieldName(localField, null);

		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("idOfTheObject", primaryKey.getClass()),
				new OptionSpecifyType("filedNameOfTheObject", foreignKey.getClass(), true));
		options.add(new OptionRenameColumn("idOfTheObject", primaryKeyColomnName.inTable()));
		options.add(new OptionRenameColumn("filedNameOfTheObject", localFieldColomnName.inTable()));
		options.add(new AccessDeletedItems());
		TableCoversGeneric data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = ioDb.get(TableCoversGenericUpdateAt.class, primaryKey, options.getAllArray());
		} else {
			data = ioDb.get(TableCoversGeneric.class, primaryKey, options.getAllArray());
		}
		if (data.filedNameOfTheObject == null) {
			data.filedNameOfTheObject = new ArrayList<>();
		}
		for (final Object elem : data.filedNameOfTheObject) {
			if (elem.equals(foreignKey)) {
				return;
			}
		}
		data.filedNameOfTheObject.add(foreignKey);
		ioDb.update(data, data.idOfTheObject, List.of("filedNameOfTheObject"), options.getAllArray());

	}

	public static void removeLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object primaryKey,
			final String fieldName,
			final Object foreignKey) throws Exception {
		final FieldName updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		// TODO: check the type of the keys...
		final String tableName = AnnotationTools.getTableName(clazz);
		final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
		final FieldName primaryKeyColomnName = AnnotationTools.getFieldName(primaryKeyField, null);

		final Field localField = AnnotationTools.getFieldNamed(clazz, fieldName);
		final FieldName localFieldColomnName = AnnotationTools.getFieldName(localField, null);

		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("idOfTheObject", primaryKey.getClass()),
				new OptionSpecifyType("filedNameOfTheObject", foreignKey.getClass(), true));
		options.add(new OptionRenameColumn("idOfTheObject", primaryKeyColomnName.inTable()));
		options.add(new OptionRenameColumn("filedNameOfTheObject", localFieldColomnName.inTable()));
		options.add(new AccessDeletedItems());
		TableCoversGeneric data = null;
		if (updateFieldName != null) {
			options.add(new OptionRenameColumn("updatedAt", updateFieldName.inTable()));
			data = ioDb.get(TableCoversGenericUpdateAt.class, primaryKey, options.getAllArray());
		} else {
			data = ioDb.get(TableCoversGeneric.class, primaryKey, options.getAllArray());
		}
		if (data.filedNameOfTheObject == null) {
			data.filedNameOfTheObject = new ArrayList<>();
		}
		boolean found = false;
		final List<Object> newList = new ArrayList<>();
		for (final Object elem : data.filedNameOfTheObject) {
			if (elem.equals(foreignKey)) {
				found = true;
				continue;
			}
			newList.add(elem);
		}
		if (!found) {
			return;
		}
		ioDb.update(data, data.idOfTheObject, List.of("filedNameOfTheObject"), options.getAllArray());
	}
}
