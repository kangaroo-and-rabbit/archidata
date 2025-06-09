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
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.SystemException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOnManyToManyDoc implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToManyDoc.class);

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToManyDoc.class;
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final ManyToManyDoc decorators = elem.getDeclaredAnnotation(ManyToManyDoc.class);
		return decorators != null;
	}

	public boolean canRetreiveAnWrite(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		final ManyToManyDoc decorators = field.getDeclaredAnnotation(ManyToManyDoc.class);
		if (decorators == null) {
			return false;
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
		// add new Values
		for (final Object value : insertedDataCollection) {
			if (previousDataCollection.contains(value)) {
				continue;
			}
			actions.add(() -> {
				ManyToManyTools.addLinkRemote(ioDb, field, primaryKeyValue, value);
			});
		}
		// remove old values:
		for (final Object value : previousDataCollection) {
			if (insertedDataCollection.contains(value)) {
				continue;
			}
			actions.add(() -> {
				ManyToManyTools.removeLinkRemote(ioDb, field, primaryKeyValue, value);
			});
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
				actions.add(() -> {
					ManyToManyTools.addLinkRemote(ioDb, field, primaryKeyValue, value);
				});
			}
		}
	}

	@Override
	public boolean isPreviousDataNeeded(final Field field) {
		return true;
	}

	@Override
	public boolean canInsert(final Field field) {
		return canRetreiveAnWrite(field);
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return canRetreiveAnWrite(field);
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

		if (field.getType() != List.class) {
			throw new SystemException("@ManyToManyLocal must contain a List");
		}
		final String fieldName = AnnotationTools.getFieldName(field, options).inTable();
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		final Object dataRetrieve = doc.get(fieldName, field.getType());
		if (dataRetrieve instanceof final Collection<?> dataCollection) {
			final ParameterizedType listType = (ParameterizedType) field.getGenericType();
			final Class<?> objectClass = (Class<?>) listType.getActualTypeArguments()[0];
			if (objectClass == Long.class) {
				final List<Long> dataParsed = (List<Long>) dataCollection;
				field.set(data, dataParsed);
				return;
			}
			if (objectClass == UUID.class) {
				final List<UUID> dataParsed = (List<UUID>) dataCollection;
				field.set(data, dataParsed);
				return;
			}
			if (objectClass == ObjectId.class) {
				final List<ObjectId> dataParsed = (List<ObjectId>) dataCollection;
				field.set(data, dataParsed);
				return;
			}
			final ManyToManyDoc decorators = field.getDeclaredAnnotation(ManyToManyDoc.class);
			if (decorators == null) {
				return;
			}
			if (objectClass == decorators.targetEntity()) {
				final Class<?> foreignKeyType = AnnotationTools.getPrimaryKeyField(objectClass).getType();
				if (foreignKeyType == Long.class) {
					final List<Long> idList = (List<Long>) dataCollection;
					if (idList != null && idList.size() > 0) {
						final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
								options);
						// In the lazy mode, the request is done in asynchronous mode, they will be done after...
						final LazyGetter lambda = () -> {
							// TODO: update to have get with abstract types ....
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryInList<>(idField.inTable(), idList)));
							if (foreignData == null) {
								return;
							}
							field.set(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				} else if (foreignKeyType == UUID.class) {
					final List<UUID> idList = (List<UUID>) dataCollection;
					if (idList != null && idList.size() > 0) {
						final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
								options);
						// In the lazy mode, the request is done in asynchronous mode, they will be done after...
						final LazyGetter lambda = () -> {
							final List<UUID> childs = new ArrayList<>(idList);
							// TODO: update to have get with abstract types ....
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryInList<>(idField.inTable(), childs)));
							if (foreignData == null) {
								return;
							}
							field.set(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				} else if (foreignKeyType == ObjectId.class) {
					final List<ObjectId> idList = (List<ObjectId>) dataCollection;
					if (idList != null && idList.size() > 0) {
						final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
								options);
						// In the lazy mode, the request is done in asynchronous mode, they will be done after...
						final LazyGetter lambda = () -> {
							final List<ObjectId> childs = new ArrayList<>(idList);
							// TODO: update to have get with abstract types ....
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryInList<>(idField.inTable(), childs)));
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
	}
}
