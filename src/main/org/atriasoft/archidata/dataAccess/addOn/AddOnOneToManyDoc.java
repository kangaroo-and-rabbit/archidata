package org.atriasoft.archidata.dataAccess.addOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.codec.MongoFieldCodec;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.DataAccessException;
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
	public boolean isCompatibleField(final DbPropertyDescriptor desc) {
		final PropertyDescriptor prop = desc.getProperty();
		final OneToManyDoc decorators = prop.getAnnotation(OneToManyDoc.class);
		if (decorators == null) {
			return false;
		}
		if (!Collection.class.isAssignableFrom(prop.getType())) {
			return false;
		}
		final Class<?> objectClass = prop.getElementType();
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
			final DbPropertyDescriptor desc,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final MongoFieldCodec codec = desc.getCodec();
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
		final OneToManyDoc oneToManyDoc = prop.getAnnotation(OneToManyDoc.class);
		final String remoteFieldColumn = resolveRemoteFieldColumn(oneToManyDoc);
		final String localFieldColumn = desc.getFieldName(null).inTable();

		// add new Values: set the remote scalar field to point to us
		for (final Object value : insertedDataCollection) {
			if (previousDataCollection.contains(value)) {
				continue;
			}
			actions.add((final List<LazyGetter> actionsAsync) -> {
				// Atomically set the remote field and get the previous value
				final Object previousValue = MongoLinkManager.setFieldAndGetPrevious(
						ioDb, oneToManyDoc.targetEntity(), value, remoteFieldColumn, primaryKeyValue);
				// If the child was previously owned by another parent, remove the child from
				// that parent's list
				if (previousValue != null) {
					actionsAsync.add((final List<LazyGetter> actionsAsync2) -> {
						MongoLinkManager.removeFromList(ioDb, previousData.getClass(), previousValue,
								localFieldColumn, value);
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
						ioDb.deleteById(oneToManyDoc.targetEntity(), value);
					});
					break;
				case CascadeMode.SET_NULL:
					actions.add((final List<LazyGetter> actionsAsync) -> {
						MongoLinkManager.setField(ioDb, oneToManyDoc.targetEntity(), value,
								remoteFieldColumn, null);
					});
					break;
				case CascadeMode.IGNORE:
					break;
			}
		}
	}

	@Override
	public boolean isInsertAsync(final DbPropertyDescriptor desc) throws Exception {
		return true;
	}

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
			final OneToManyDoc decorators = desc.getProperty().getAnnotation(OneToManyDoc.class);
			if (decorators.addLinkWhenCreate()) {
				final String remoteFieldColumn = resolveRemoteFieldColumn(decorators);
				final String localFieldColumn = desc.getFieldName(null).inTable();
				for (final Object value : insertedDataCollection) {
					actions.add((final List<LazyGetter> actionsAsync) -> {
						// Atomically set the remote field and get the previous owner
						final Object previousValue = MongoLinkManager.setFieldAndGetPrevious(
								ioDb, decorators.targetEntity(), value, remoteFieldColumn, primaryKeyValue);
						// If was owned by another parent, clean up that parent's list
						if (previousValue != null) {
							actionsAsync.add((final List<LazyGetter> actionsAsync2) -> {
								MongoLinkManager.removeFromList(ioDb, clazz, previousValue,
										localFieldColumn, value);
							});
						}
					});
				}
			}
		}
	}

	@Override
	public boolean isPreviousDataNeeded(final DbPropertyDescriptor desc) {
		return true;
	}

	@Override
	public boolean canInsert(final DbPropertyDescriptor desc) {
		return true;
	}

	@Override
	public boolean canRetrieve(final DbPropertyDescriptor desc) {
		return true;
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
		final String fieldName = desc.getFieldName(options).inTable();
		if (!doc.containsKey(fieldName)) {
			prop.setValue(data, null);
			return;
		}
		final OneToManyDoc decorators = prop.getAnnotation(OneToManyDoc.class);
		final Class<?> objectClass = prop.getElementType();
		final Object dataRetrieve = doc.get(fieldName, prop.getType());
		if (dataRetrieve == null) {
			return;
		}
		if (dataRetrieve instanceof final Collection<?> dataCollection) {
			if (dataCollection.isEmpty()) {
				prop.setValue(data, null);
				return;
			}
			if (objectClass == decorators.targetEntity()) {
				final List<Object> idList = (List<Object>) dataCollection;
				if (idList != null && !idList.isEmpty()) {
					final DbClassModel targetModel = DbClassModel.of(decorators.targetEntity());
					final String idFieldColumn = targetModel.getPrimaryKey().getFieldName(options).inTable();
					final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
						final Object foreignData = ioDb.getsRaw(decorators.targetEntity(),
								new Condition(new QueryInList<>(idFieldColumn, idList)));
						if (foreignData == null) {
							return;
						}
						prop.setValue(data, foreignData);
					};
					lazyCall.add(lambda);
				}
				return;
			}
			prop.setValue(data, dataCollection);
		} else {
			throw new SystemException("@OneToManyLocal does not retreive a Collection");
		}
	}

	@Override
	public boolean asDeleteAction(final DbPropertyDescriptor desc) {
		final OneToManyDoc decorators = desc.getProperty().getAnnotation(OneToManyDoc.class);
		return decorators.cascadeDelete() != CascadeMode.IGNORE;
	}

	@Override
	public void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final DbPropertyDescriptor desc,
			final List<Object> previousDataThatIsDeleted,
			final List<LazyGetter> actions) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();
		final OneToManyDoc decorators = prop.getAnnotation(OneToManyDoc.class);
		final String remoteFieldColumn = resolveRemoteFieldColumn(decorators);
		for (final Object obj : previousDataThatIsDeleted) {
			final Object childKeys = prop.getValue(obj);
			if (childKeys == null) {
				continue;
			}
			if (childKeys instanceof final Collection childCollection) {
				for (final Object childKey : childCollection) {
					switch (decorators.cascadeUpdate()) {
						case CascadeMode.DELETE:
							actions.add((final List<LazyGetter> actionsAsync) -> {
								ioDb.deleteById(decorators.targetEntity(), childKey);
							});
							break;
						case CascadeMode.SET_NULL:
							actions.add((final List<LazyGetter> actionsAsync) -> {
								MongoLinkManager.setField(ioDb, decorators.targetEntity(), childKey,
										remoteFieldColumn, null);
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

	// ========== Private helpers ==========

	private static String resolveRemoteFieldColumn(final OneToManyDoc oneToManyDoc) throws Exception {
		final DbClassModel targetModel = DbClassModel.of(oneToManyDoc.targetEntity());
		final DbPropertyDescriptor remoteDesc = targetModel.findByPropertyName(oneToManyDoc.remoteField());
		if (remoteDesc == null) {
			throw new DataAccessException("Cannot find remote field '" + oneToManyDoc.remoteField()
					+ "' in " + oneToManyDoc.targetEntity().getSimpleName());
		}
		return remoteDesc.getFieldName(null).inTable();
	}
}
