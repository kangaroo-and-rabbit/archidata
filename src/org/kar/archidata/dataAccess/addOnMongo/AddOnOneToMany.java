package org.kar.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.AnnotationTools.FieldName;
import org.kar.archidata.dataAccess.DBAccessMorphia;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.options.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.OneToMany;

public class AddOnOneToMany implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnOneToMany.class);
	static final String SEPARATOR_LONG = "-";

	@Override
	public Class<?> getAnnotationClass() {
		return OneToMany.class;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		return decorators != null;
	}

	@Override
	public void insertData(
			final DBAccessMorphia ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		throw new IllegalAccessException("Can not generate an inset of @OneToMany");
	}

	@Override
	public boolean canInsert(final Field field) {
		return false;
	}

	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		// TODO: can be implemented later...
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return false;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	// in first implementation we did not keep the data in the 2 Objects, bun we will do it after to have a faster table interactions.
	@Override
	public void fillFromDoc(
			final DBAccessMorphia ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not OneToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}

		final FieldName fieldName = AnnotationTools.getFieldName(field, options);
		// in step 1 the fields are not stored in the local element
		//		if (!doc.containsKey(fieldName.inTable())) {
		//			field.set(data, null);
		//			return;
		//		}

		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			if (true) {
				// DEVELOPMENT step 1 we search all the element in the list:
				// get the curentObject primary key
				final Field primaryField = AnnotationTools.getPrimaryKeyField(data.getClass());
				final String primaryKeyName = AnnotationTools.getFieldNameRaw(primaryField);
				final Object primaryKey = doc.get(primaryKeyName, primaryField.getType());
				// get the remotes objects
				final List<?> returnValue = ioDb.getsWhere(decorators.targetEntity(),
						new Condition(new QueryCondition(decorators.mappedBy(), "=", primaryKey)));
				// extract the primary key of the remote objects
				final Field remotePrimaryField = AnnotationTools.getPrimaryKeyField(decorators.targetEntity());
				final String remotePrimaryKeyName = AnnotationTools.getFieldNameRaw(remotePrimaryField);
				final List<Object> listOfRemoteKeys = new ArrayList<>();
				for (final var item : returnValue) {
					listOfRemoteKeys.add(remotePrimaryField.get(item));
				}
				// inject in the current data field
				if (listOfRemoteKeys.size() != 0) {
					field.set(data, listOfRemoteKeys);
				}
			} else {
				// DEVELOPMENT In step 2 this will work well:
				final Object value = doc.get(fieldName.inTable(), field.getType());
				field.set(data, value);
			}
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			// Maybe in a second step we do not like this but this way is efficient too.
			// get the curentObject primary key
			final Field primaryField = AnnotationTools.getPrimaryKeyField(data.getClass());
			final String primaryKeyName = AnnotationTools.getFieldNameRaw(primaryField);
			final Object primaryKey = doc.get(primaryKeyName, primaryField.getType());
			// get the remotes objects
			final List<?> returnValue = ioDb.getsWhere(decorators.targetEntity(),
					new Condition(new QueryCondition(decorators.mappedBy(), "=", primaryKey)));
			// inject in the current data field
			if (returnValue.size() != 0) {
				field.set(data, returnValue);
			}
		}
	}

	// TODO : refacto this table to manage a generic table with dynamic name to be serialize with the default system
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
		// This is a remote field ==> nothing to generate (it is stored in the remote object
	}
}
