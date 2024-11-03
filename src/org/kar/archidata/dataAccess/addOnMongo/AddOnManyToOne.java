package org.kar.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccessMorphia;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

public class AddOnManyToOne implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToOne.class;
	}

	@Override
	public String getSQLFieldType(final Field field) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		try {
			return DataFactory.convertTypeInSQL(field.getType(), fieldName);
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
			final DataAccessMorphia ioDb,
			final Field field,
			final Object rootObject,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		final Object data = field.get(rootObject);
		if (field.get(data) == null) {
			docUnSet.append(fieldName, "");
			return;
		} else if (field.getType() == Long.class) {
			final Long dataTyped = (Long) data;
			docSet.append(fieldName, dataTyped);
		} else if (field.getType() == Integer.class) {
			final Integer dataTyped = (Integer) data;
			docSet.append(fieldName, dataTyped);
		} else if (field.getType() == Short.class) {
			final Short dataTyped = (Short) data;
			docSet.append(fieldName, dataTyped);
		} else if (field.getType() == String.class) {
			final String dataTyped = (String) data;
			docSet.append(fieldName, dataTyped);
		} else if (field.getType() == UUID.class) {
			final UUID dataTyped = (UUID) data;
			docSet.append(fieldName, dataTyped);
		} else {
			final Field idField = AnnotationTools.getFieldOfId(field.getType());
			final Object uid = idField.get(data);
			if (uid == null) {
				docUnSet.append(fieldName, "");
			} else {
				docSet.append(fieldName, uid);
			}
		}
	}

	@Override
	public boolean canInsert(final Field field) {
		if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Short.class
				|| field.getType() == String.class || field.getType() == UUID.class) {
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
				|| classType == String.class || classType == UUID.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}

	@Override
	public void generateQuery(
			@NotNull final String tableName,
			@NotNull final String primaryKey,
			@NotNull final Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull final String name,
			@NotNull final CountInOut count,
			final QueryOptions options) throws Exception {
		if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Short.class
				|| field.getType() == String.class || field.getType() == UUID.class) {
			querySelect.append(" ");
			querySelect.append(tableName);
			querySelect.append(".");
			querySelect.append(name);
			count.inc();
			return;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			// no eager possible for no sql
			querySelect.append(" ");
			querySelect.append(tableName);
			querySelect.append(".");
			querySelect.append(name);
			count.inc();
		}
	}

	@Override
	public void fillFromDoc(
			final DataAccessMorphia ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {

		final String fieldName = AnnotationTools.getFieldName(field);
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		// local field to manage no remote object to retrieve.
		if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Short.class
				|| field.getType() == String.class || field.getType() == UUID.class) {
			ioDb.setValueFromDoc(field.getType(), data, field, doc, lazyCall);
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
				final Long foreignKey = doc.getLong(fieldName);
				if (foreignKey != null) {
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
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
				final UUID foreignKey = doc.get(fieldName, UUID.class);
				if (foreignKey != null) {
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
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
			final int fieldId) throws Exception {
		final Class<?> classType = field.getType();
		if (classType == Long.class || classType == Integer.class || classType == Short.class
				|| classType == String.class || classType == UUID.class) {
			DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
					postActionList, createIfNotExist, createDrop, fieldId, classType);
		} else {
			LOGGER.error("Support only the Long remote field of ecternal primary keys...");
			DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
					postActionList, createIfNotExist, createDrop, fieldId, Long.class);
		}
	}
}
