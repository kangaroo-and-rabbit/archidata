package org.atriasoft.archidata.dataAccess.addOn;

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
import org.atriasoft.archidata.dataAccess.commonTools.ListInDbTools;
import org.atriasoft.archidata.dataAccess.commonTools.OneToManyTools;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.FailException;
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
			actions.add((final List<LazyGetter> actionsAsync) -> {
				// Replace the remote value with the current element and get the previous value
				// of the data.
				final Object previousValue = OneToManyTools.setRemoteValue(field, value, primaryKeyValue);
				// The previous value is not us (otherwise the function return null and we are
				// in a really bad case...)
				// we need to remove the value from the list of OneToMany. It not manage by the
				// plug-in due to the fact we update the only one filed manually
				if (previousValue != null) {
					actionsAsync.add((final List<LazyGetter> actionsAsync2) -> {
						final String fieldName = AnnotationTools.getFieldName(field, null).inTable();
						ListInDbTools.removeLink(previousData.getClass(), previousValue, fieldName, value);
					});
				}
			});

		}
		// remove old values:
		for (final Object value : previousDataCollection) {
			if (insertedDataCollection.contains(value)) {
				continue;
			}
			switch (oneToManyDoc.cascadeUpdate()) {
				case CascadeMode.DELETE:
					actions.add((final List<LazyGetter> actionsAsync) -> {
						ioDb.delete(oneToManyDoc.targetEntity(), value);
					});
					break;
				case CascadeMode.SET_NULL:
					actions.add((final List<LazyGetter> actionsAsync) -> {
						OneToManyTools.setRemoteNullRemote(field, value);
					});
					break;
				case CascadeMode.IGNORE:
					break;
			}
		}
	}

	/**
	 * Some action must be done asynchronously for update or remove element
	 *
	 * @param field
	 * @return
	 */
	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return true;
	}

	/**
	 * When insert is mark async, this function permit to create or update the data.
	 *
	 * @param primaryKeyValue Local ID of the current table
	 * @param field           Field that is updated.
	 * @param data            Data that might be inserted.
	 * @param actions         Asynchronous action to do after main request.
	 */
	@Override
	public void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
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

				final OneToManyDoc decorators = field.getDeclaredAnnotation(OneToManyDoc.class);
				if (decorators.addLinkWhenCreate()) {
					actions.add((final List<LazyGetter> actionsAsync) -> {
						// Replace the remote value with the current element and get the previous value
						// of the data.
						final Object previousValue = OneToManyTools.setRemoteValue(field, value, primaryKeyValue);
						// The previous value is not us (otherwise the function return null and we are
						// in a really bad case...)
						// we need to remove the value from the list of OneToMany. It not manage by the
						// plug-in due to the fact we update the only one filed manually
						if (previousValue != null) {
							actionsAsync.add((final List<LazyGetter> actionsAsync2) -> {
								final String fieldName = AnnotationTools.getFieldName(field, null).inTable();
								ListInDbTools.removeLink(clazz, previousValue, fieldName, value);
							});
						}
					});
				}
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
					// In the lazy mode, the request is done in asynchronous mode, they will be done
					// after...
					final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
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

	@Override
	public boolean asDeleteAction(final Field field) {
		final OneToManyDoc decorators = field.getDeclaredAnnotation(OneToManyDoc.class);
		return decorators.cascadeDelete() != CascadeMode.IGNORE;
	}

	@Override
	public void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Field field,
			final List<Object> previousDataThatIsDeleted,
			final List<LazyGetter> actions) throws Exception {
		final OneToManyDoc decorators = field.getDeclaredAnnotation(OneToManyDoc.class);
		for (final Object obj : previousDataThatIsDeleted) {
			final Object childKeys = field.get(obj);
			if (childKeys == null) {
				continue;
			}
			if (childKeys instanceof final Collection childCollection) {
				for (final Object childKey : childCollection) {
					switch (decorators.cascadeUpdate()) {
						case CascadeMode.DELETE:
							actions.add((final List<LazyGetter> actionsAsync) -> {
								ioDb.delete(decorators.targetEntity(), childKey);
							});
							break;
						case CascadeMode.SET_NULL:
							actions.add((final List<LazyGetter> actionsAsync) -> {
								OneToManyTools.setRemoteNullRemote(field, childKey);
							});
							break;
						case CascadeMode.IGNORE:
							break;
					}
				}
			} else {
				throw new FailException("can not remove a remote kes stored in other than a Collection<T>");
			}
		}
	}
}
