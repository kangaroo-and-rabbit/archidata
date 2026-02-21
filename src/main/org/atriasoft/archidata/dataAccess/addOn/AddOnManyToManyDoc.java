package org.atriasoft.archidata.dataAccess.addOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.DataAccessException;
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
	public boolean isCompatibleField(final DbPropertyDescriptor desc) {
		final PropertyDescriptor prop = desc.getProperty();
		return prop.hasAnnotation(ManyToManyDoc.class);
	}

	public boolean canRetreiveAnWrite(final DbPropertyDescriptor desc) {
		final PropertyDescriptor prop = desc.getProperty();
		if (prop.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = prop.getElementType();
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		final ManyToManyDoc decorators = prop.getAnnotation(ManyToManyDoc.class);
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
			final DbPropertyDescriptor desc,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final var codec = desc.getCodec();
		if (codec != null) {
			final FieldName tableFieldName = desc.getFieldName(options);
			codec.writeToDoc(null, tableFieldName.inTable(), rootObject, docSet, docUnSet);
		}
	}

	@Override
	public boolean isUpdateAsync(final DbPropertyDescriptor desc) {
		return true;
	}

	@Override
	public void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object primaryKeyValue,
			final DbPropertyDescriptor desc,
			final Object insertedData,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();
		final Object previousDataValue = prop.getValue(previousData);
		Collection<?> previousDataCollection = new ArrayList<>();
		if (previousDataValue instanceof final Collection<?> tmpCollection) {
			previousDataCollection = tmpCollection;
		}
		final Object insertedDataValue = insertedData;
		Collection<?> insertedDataCollection = new ArrayList<>();
		if (insertedDataValue instanceof final Collection<?> tmpCollection) {
			insertedDataCollection = tmpCollection;
		}
		// Resolve remote collection and field info once
		final ManyToManyDoc manyDoc = prop.getAnnotation(ManyToManyDoc.class);
		if (manyDoc == null || manyDoc.targetEntity() == null
				|| manyDoc.remoteField() == null || manyDoc.remoteField().isEmpty()) {
			return;
		}
		final String remoteFieldColumn = resolveRemoteFieldColumn(manyDoc);

		// add new Values (atomic $addToSet on remote document)
		for (final Object value : insertedDataCollection) {
			if (previousDataCollection.contains(value)) {
				continue;
			}
			actions.add((final List<LazyGetter> actionsAsync) -> {
				MongoLinkManager.addToList(ioDb, manyDoc.targetEntity(), value,
						remoteFieldColumn, primaryKeyValue);
			});
		}
		// remove old values (atomic $pull on remote document)
		for (final Object value : previousDataCollection) {
			if (insertedDataCollection.contains(value)) {
				continue;
			}
			actions.add((final List<LazyGetter> actionsAsync) -> {
				MongoLinkManager.removeFromList(ioDb, manyDoc.targetEntity(), value,
						remoteFieldColumn, primaryKeyValue);
			});
		}
	}

	/** Some action must be done asynchronously for update or remove element
	 * @param desc
	 * @return */
	@Override
	public boolean isInsertAsync(final DbPropertyDescriptor desc) throws Exception {
		return true;
	}

	/** When insert is mark async, this function permit to create or update the data.
	 * @param primaryKeyValue Local ID of the current table
	 * @param desc Property descriptor that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	@Override
	public void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final DbPropertyDescriptor desc,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final Object insertedData = data;
		if (insertedData == null) {
			return;
		}
		if (insertedData instanceof final Collection<?> insertedDataCollection) {
			final PropertyDescriptor prop = desc.getProperty();
			final ManyToManyDoc manyDoc = prop.getAnnotation(ManyToManyDoc.class);
			if (manyDoc == null || manyDoc.targetEntity() == null
					|| manyDoc.remoteField() == null || manyDoc.remoteField().isEmpty()) {
				return;
			}
			final String remoteFieldColumn = resolveRemoteFieldColumn(manyDoc);
			for (final Object value : insertedDataCollection) {
				actions.add((final List<LazyGetter> actionsAsync) -> {
					MongoLinkManager.addToList(ioDb, manyDoc.targetEntity(), value,
							remoteFieldColumn, primaryKeyValue);
				});
			}
		}
	}

	@Override
	public boolean isPreviousDataNeeded(final DbPropertyDescriptor desc) {
		return true;
	}

	@Override
	public boolean canInsert(final DbPropertyDescriptor desc) {
		return canRetreiveAnWrite(desc);
	}

	@Override
	public boolean canRetrieve(final DbPropertyDescriptor desc) {
		return canRetreiveAnWrite(desc);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fillFromDoc(
			final DBAccessMongo ioDb,
			final Document doc,
			final DbPropertyDescriptor desc,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();

		if (prop.getType() != List.class) {
			throw new SystemException("@ManyToManyLocal must contain a List");
		}
		final String fieldName = desc.getFieldName(options).inTable();
		if (!doc.containsKey(fieldName)) {
			prop.setValue(data, null);
			return;
		}
		final Object dataRetrieve = doc.get(fieldName, prop.getType());
		if (dataRetrieve instanceof final Collection<?> dataCollection) {
			if (dataCollection.isEmpty()) {
				prop.setValue(data, null);
				return;
			}
			final Class<?> objectClass = prop.getElementType();
			if (objectClass == Long.class) {
				final List<Long> dataParsed = (List<Long>) dataCollection;
				prop.setValue(data, dataParsed);
				return;
			}
			if (objectClass == UUID.class) {
				final List<UUID> dataParsed = (List<UUID>) dataCollection;
				prop.setValue(data, dataParsed);
				return;
			}
			if (objectClass == ObjectId.class) {
				final List<ObjectId> dataParsed = (List<ObjectId>) dataCollection;
				prop.setValue(data, dataParsed);
				return;
			}
			final ManyToManyDoc decorators = prop.getAnnotation(ManyToManyDoc.class);
			if (decorators == null) {
				return;
			}
			if (objectClass == decorators.targetEntity()) {
				final DbClassModel targetModel = DbClassModel.of(objectClass);
				final DbPropertyDescriptor targetPk = targetModel.getPrimaryKey();
				final Class<?> foreignKeyType = targetPk.getProperty().getType();
				final String idFieldColumn = targetPk.getFieldName(options).inTable();

				if (foreignKeyType == Long.class) {
					final List<Long> idList = (List<Long>) dataCollection;
					if (idList != null && !idList.isEmpty()) {
						final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
							final Object foreignData = ioDb.gets(decorators.targetEntity(),
									new Condition(new QueryInList<>(idFieldColumn, idList)));
							if (foreignData == null) {
								return;
							}
							prop.setValue(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				} else if (foreignKeyType == UUID.class) {
					final List<UUID> idList = (List<UUID>) dataCollection;
					if (idList != null && !idList.isEmpty()) {
						final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
							final List<UUID> childs = new ArrayList<>(idList);
							final Object foreignData = ioDb.gets(decorators.targetEntity(),
									new Condition(new QueryInList<>(idFieldColumn, childs)));
							if (foreignData == null) {
								return;
							}
							prop.setValue(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				} else if (foreignKeyType == ObjectId.class) {
					final List<ObjectId> idList = (List<ObjectId>) dataCollection;
					if (idList != null && !idList.isEmpty()) {
						final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
							final List<ObjectId> childs = new ArrayList<>(idList);
							final Object foreignData = ioDb.gets(decorators.targetEntity(),
									new Condition(new QueryInList<>(idFieldColumn, childs.toArray())));
							if (foreignData == null) {
								return;
							}
							prop.setValue(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				}
			}
		}
	}

	// ========== Private helpers ==========

	/**
	 * Resolve the DB column name of the remote field for a ManyToMany relationship.
	 */
	private static String resolveRemoteFieldColumn(final ManyToManyDoc manyDoc) throws Exception {
		final DbClassModel targetModel = DbClassModel.of(manyDoc.targetEntity());
		final DbPropertyDescriptor remoteDesc = targetModel.findByPropertyName(manyDoc.remoteField());
		if (remoteDesc == null) {
			throw new DataAccessException("Cannot find remote field '" + manyDoc.remoteField()
					+ "' in " + manyDoc.targetEntity().getSimpleName());
		}
		return remoteDesc.getFieldName(null).inTable();
	}
}
