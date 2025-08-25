package org.atriasoft.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.SystemException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOnOneToManyDoc implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnOneToManyDoc.class);

	@Override
	public Class<?> getAnnotationClass() {
		return OneToManyDoc.class;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final OneToManyDoc decorators = field.getDeclaredAnnotation(OneToManyDoc.class);
		if (decorators == null) {
			return false;
		}
		if (!Collection.class.isAssignableFrom(field.getType())) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	@Override
	public void insertData(
			final DBAccessMongo ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final Class<?> type = field.getType();
		final FieldName tableFieldName = AnnotationTools.getFieldName(field, options);
		ioDb.setValueToDb(null, type, rootObject, field, tableFieldName.inTable(), docSet, docUnSet);
	}

	@Override
	public boolean isUpdateAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object primaryKeyValue,
			final Field field,
			final Object insertedData,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final Object previousDataValue = field.get(previousData);
		Collection<?> previousDataCollection = new ArrayList<>();
		if (previousDataValue instanceof final Collection<?> tmpCollection) {
			previousDataCollection = tmpCollection;
		}
		final Object insertedDataValue = insertedData;
		Collection<?> insertedDataCollection = new ArrayList<>();
		if (insertedDataValue instanceof final Collection<?> tmpCollection) {
			insertedDataCollection = tmpCollection;
		}
		final OneToManyDoc oneToManyDoc = field.getAnnotation(OneToManyDoc.class);
		// add new Values
		for (final Object value : insertedDataCollection) {
			if (previousDataCollection.contains(value)) {
				continue;
			}
			/* TODO:
			 *   - get the remote object
			 *   - remove the previous link
			 *   - set the new value
			 */
			final ici un truc final a faire.
//			if (!oneToManyDoc.ignoreRemoteUpdateWhenOnAddItem()) {
//
//				// actions.add(() -> {
//				//
//				// });
//				throw new SystemException("@ManyToOneLocal ignoreRemoteUpdateWhenOnAddItem=false is not implemented");
//			}
		}
		// remove old values:
		for (final Object value : previousDataCollection) {
			if (insertedDataCollection.contains(value)) {
				continue;
			}
			switch (oneToManyDoc.cascade()) {
				case CascadeMode.DELETE:
					actions.add(() -> {
						ioDb.delete(oneToManyDoc.targetEntity(), value);
					});
					//break;
				case CascadeMode.SET_NULL:
					actions.add(() -> {
						// TODO: create a tool that set a field at null witout control like the link ???
						ManyToManyLocalTools.removeLinkRemote(ioDb, field, primaryKeyValue, value);
					});
					throw new SystemException(
							"@ManyToOneLocal cascade=CascadeMode.SET_NULL_ON_REMOVE is not implemented");
				//break;
				case CascadeMode.IGNORE:
					break;
			}
		}

	}

	/** Some action must be done asynchronously for update or remove element
	 * @param field
	 * @return */
	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return true;
	}

	/** When insert is mark async, this function permit to create or update the data.
	 * @param primaryKeyValue Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	@Override
	public void asyncInsert(
			final DBAccessMongo ioDb,
			final Object primaryKeyValue,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final Object insertedData = data;
		if (insertedData == null) {
			return;
		}
		if (insertedData instanceof final Collection<?> insertedDataCollection) {
			for (final Object value : insertedDataCollection) {
				//actions.add(() -> {
				//	ManyToManyLocalTools.addLinkRemote(ioDb, field, primaryKeyValue, value);
				//});
				// TODO: set the value to the new value
				// TODO: check if it change
				// TODO: if true: update the parent in consequence...
			}
		}
	}

	@Override
	public boolean isPreviousDataNeeded(final Field field) {
		return true;
	}

	@Override
	public boolean canInsert(final Field field) {
		return true;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fillFromDoc(
			final DBAccessMongo ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall)
			throws Exception, SQLException, IllegalArgumentException, IllegalAccessException {
		final String fieldName = AnnotationTools.getFieldName(field, options).inTable();
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		final OneToManyDoc decorators = field.getDeclaredAnnotation(OneToManyDoc.class);
		final ParameterizedType listType = (ParameterizedType) field.getGenericType();
		final Class<?> objectClass = (Class<?>) listType.getActualTypeArguments()[0];
		final Object dataRetrieve = doc.get(fieldName, field.getType());
		if (dataRetrieve == null) {
			return;
		}
		if (dataRetrieve instanceof final Collection<?> dataCollection) {
			if (objectClass == decorators.targetEntity()) {
				final List<Object> idList = (List<Object>) dataCollection;
				if (idList != null && idList.size() > 0) {
					final FieldName idField = AnnotationTools
							.getFieldName(AnnotationTools.getIdField(decorators.targetEntity()), options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final Object foreignData = ioDb.getsWhereRaw(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), idList)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
				return;
			}
			field.set(data, dataCollection);
		} else {
			throw new SystemException("@OneToManyLocal does not retreive a Collection");
		}
	}
}
