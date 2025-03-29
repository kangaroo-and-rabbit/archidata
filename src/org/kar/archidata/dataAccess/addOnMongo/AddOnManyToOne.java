package org.kar.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.AnnotationTools.FieldName;
import org.kar.archidata.dataAccess.DBAccessMorphia;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToOne;

public class AddOnManyToOne implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToOne.class;
	}

	@Override
	public String getSQLFieldType(final Field field, final QueryOptions options) throws Exception {
		final FieldName fieldName = AnnotationTools.getFieldName(field, options);
		try {
			return DataFactory.convertTypeInSQL(field.getType(), fieldName.inTable());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		return elem.getDeclaredAnnotation(ManyToOne.class) != null;
	}

	@Override
	public void insertData(
			final DBAccessMorphia ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final FieldName fieldName = AnnotationTools.getFieldName(field, options);
		final Object data = field.get(rootObject);
		if (data == null) {
			docUnSet.append(fieldName.inTable(), "");
			return;
		} else if (field.getType() == Long.class) {
			final Long dataTyped = (Long) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else if (field.getType() == Integer.class) {
			final Integer dataTyped = (Integer) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else if (field.getType() == Short.class) {
			final Short dataTyped = (Short) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else if (field.getType() == String.class) {
			final String dataTyped = (String) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else if (field.getType() == UUID.class) {
			final UUID dataTyped = (UUID) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else if (field.getType() == ObjectId.class) {
			final ObjectId dataTyped = (ObjectId) data;
			docSet.append(fieldName.inTable(), dataTyped);
		} else {
			final Field idField = AnnotationTools.getFieldOfId(field.getType());
			final Object uid = idField.get(data);
			if (uid == null) {
				docUnSet.append(fieldName.inTable(), "");
			} else {
				docSet.append(fieldName.inTable(), uid);
			}
		}
	}

	@Override
	public boolean canInsert(final Field field) {
		if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Short.class
				|| field.getType() == String.class || field.getType() == UUID.class
				|| field.getType() == ObjectId.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		final Class<?> classType = field.getType();
		if (classType == Long.class || classType == Integer.class || classType == Short.class
				|| classType == String.class || classType == UUID.class || classType == ObjectId.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}

	@Override
	public void fillFromDoc(
			final DBAccessMorphia ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {

		final FieldName fieldName = AnnotationTools.getFieldName(field, options);
		if (!doc.containsKey(fieldName.inTable())) {
			field.set(data, null);
			return;
		}
		// local field to manage no remote object to retrieve.
		if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Short.class
				|| field.getType() == String.class || field.getType() == UUID.class
				|| field.getType() == ObjectId.class) {
			ioDb.setValueFromDoc(field.getType(), data, field, doc, lazyCall, options);
			return;
		}
		final Class<?> objectClass = field.getType();
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {

			final Field remotePrimaryKeyField = AnnotationTools.getFieldOfId(objectClass);
			final Class<?> remotePrimaryKeyType = remotePrimaryKeyField.getType();
			if (remotePrimaryKeyType == Long.class) {
				// here we have the field, the data and the the remote value ==> can create callback that generate the update of the value ...
				final Long foreignKey = doc.getLong(fieldName.inTable());
				if (foreignKey != null) {
					final LazyGetter lambda = () -> {
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.get(decorators.targetEntity(), foreignKey);
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (remotePrimaryKeyType == UUID.class) {
				// here we have the field, the data and the the remote value ==> can create callback that generate the update of the value ...
				final UUID foreignKey = doc.get(fieldName.inTable(), UUID.class);
				if (foreignKey != null) {
					final LazyGetter lambda = () -> {
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.get(decorators.targetEntity(), foreignKey);
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (remotePrimaryKeyType == ObjectId.class) {
				// here we have the field, the data and the the remote value ==> can create callback that generate the update of the value ...
				final ObjectId foreignKey = doc.get(fieldName.inTable(), ObjectId.class);
				if (foreignKey != null) {
					final LazyGetter lambda = () -> {
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.get(decorators.targetEntity(), foreignKey);
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			}
		}
	}

	// TODO : refacto this table to manage a generic table with dynamic name to be serialisable with the default system
	@Override
	public void createTables(
			final String tableName,
			final Field primaryField,
			final Field field,
			final StringBuilder mainTableBuilder,
			final List<String> preActionList,
			final List<String> postActionList,
			final boolean createIfNotExist,
			final boolean createDrop,
			final int fieldId,
			final QueryOptions options) throws Exception {
		final Class<?> classType = field.getType();
		if (classType == Long.class || classType == Integer.class || classType == Short.class
				|| classType == String.class || classType == UUID.class || classType == ObjectId.class) {
			DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
					postActionList, createIfNotExist, createDrop, fieldId, classType, options);
		} else {
			LOGGER.error("Support only the Long remote field of ecternal primary keys...");
			DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
					postActionList, createIfNotExist, createDrop, fieldId, Long.class, options);
		}
	}
}
