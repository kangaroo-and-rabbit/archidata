package org.atriasoft.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.bson.Document;
import org.bson.types.ObjectId;
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
			final DBAccessMongo ioDb,
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
			final DBAccessMongo ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not OneToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}

		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
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
			final List<Object> listOfRemoteKeys = new ArrayList<>();
			for (final var item : returnValue) {
				listOfRemoteKeys.add(remotePrimaryField.get(item));
			}
			// inject in the current data field
			if (listOfRemoteKeys.size() != 0) {
				field.set(data, listOfRemoteKeys);
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
}
