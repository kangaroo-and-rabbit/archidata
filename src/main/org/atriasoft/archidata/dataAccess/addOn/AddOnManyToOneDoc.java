package org.atriasoft.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.commonTools.ListInDbTools;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Coté de la liste des éléméments ....
public class AddOnManyToOneDoc implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToOneDoc.class);

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToOneDoc.class;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		if (decorators == null) {
			return false;
		}
		if (Collection.class.isAssignableFrom(field.getType())) {
			return false;
		}
		final Class<?> objectClass = field.getType();
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

		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		if (!decorators.updateLinkWhenUpdate()) {
			return;
		}
		final Object previousDataValue = field.get(previousData);
		final Object insertedDataValue = insertedData;
		if (Objects.equals(previousDataValue, insertedDataValue)) {
			return;
		}
		if (previousDataValue != null) {
			actions.add((List<LazyGetter> actionsAsync) -> {
				ListInDbTools.removeLink(decorators.targetEntity(), previousDataValue, decorators.remoteField(),
						primaryKeyValue);
			});
		}
		if (insertedDataValue != null) {
			actions.add((List<LazyGetter> actionsAsync) -> {
				ListInDbTools.addLink(decorators.targetEntity(), insertedDataValue, decorators.remoteField(),
						primaryKeyValue);
			});
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
		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		if (decorators.addLinkWhenCreate()) {
			actions.add((List<LazyGetter> actionsAsync) -> {
				ListInDbTools.addLink(decorators.targetEntity(), insertedData, decorators.remoteField(),
						primaryKeyValue);
			});
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
		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		if (decorators == null) {
			// not a use-case !!! ==> must fail before...
			return;
		}

		if (field.getType() == decorators.targetEntity()) {
			final Field primaryRemoteField = AnnotationTools.getPrimaryKeyField(decorators.targetEntity());
			final Object dataRetrieve = doc.get(fieldName, primaryRemoteField.getType());
			final FieldName idField = AnnotationTools
					.getFieldName(AnnotationTools.getIdField(decorators.targetEntity()), options);
			// In the lazy mode, the request is done in asynchronous mode, they will be done
			// after...
			final LazyGetter lambda = (List<LazyGetter> actionsAsync) -> {
				final Object foreignData = ioDb.getRaw(decorators.targetEntity(),
						new Condition(new QueryCondition(idField.inTable(), "=", dataRetrieve)));
				if (foreignData == null) {
					return;
				}
				field.set(data, foreignData);
			};
			lazyCall.add(lambda);
			return;
		}
		final Object dataRetrieve = doc.get(fieldName, field.getType());
		field.set(data, dataRetrieve);
	}

	@Override
	public boolean asDeleteAction(final Field field) {
		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		return decorators.removeLinkWhenDelete();
	}

	@Override
	public void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Field field,
			final List<Object> previousDataThatIsDeleted,
			final List<LazyGetter> actions) throws Exception {
		final ManyToOneDoc decorators = field.getDeclaredAnnotation(ManyToOneDoc.class);
		final Field primaryKeyDeletedObjects = AnnotationTools.getPrimaryKeyField(clazz);
		for (final Object obj : previousDataThatIsDeleted) {
			final Object primaryKeyRemovedObject = primaryKeyDeletedObjects.get(obj);
			final Object parentKey = field.get(obj);
			if (parentKey != null) {
				actions.add((List<LazyGetter> actionsAsync) -> {
					ListInDbTools.removeLink(decorators.targetEntity(), parentKey, decorators.remoteField(),
							primaryKeyRemovedObject);
				});
			}
		}
	}
}
