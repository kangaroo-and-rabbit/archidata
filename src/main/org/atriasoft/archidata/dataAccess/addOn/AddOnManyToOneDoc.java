package org.atriasoft.archidata.dataAccess.addOn;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.LazyGetterCollector;
import com.mongodb.client.model.Filters;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.AddOnFieldContext;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.codec.MongoFieldCodec;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
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
	public boolean isCompatibleField(final DbPropertyDescriptor desc) {
		final PropertyDescriptor prop = desc.getProperty();
		final ManyToOneDoc decorators = prop.getAnnotation(ManyToOneDoc.class);
		if (decorators == null) {
			return false;
		}
		if (Collection.class.isAssignableFrom(prop.getType())) {
			return false;
		}
		final Class<?> objectClass = prop.getType();
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
		final AddOnFieldContext ctx = desc.getAddonContext();
		if (!ctx.isUpdateLinkWhenUpdate()) {
			return;
		}
		final Object previousDataValue = prop.getValue(previousData);
		final Object insertedDataValue = insertedData;
		if (Objects.equals(previousDataValue, insertedDataValue)) {
			return;
		}
		final Class<?> targetEntity = ctx.getTargetEntity();
		final String remoteFieldColumn = ctx.getRemoteFieldColumn();
		if (previousDataValue != null) {
			actions.add((final List<LazyGetter> actionsAsync) -> {
				MongoLinkManager.removeFromList(ioDb, targetEntity, previousDataValue, remoteFieldColumn,
						primaryKeyValue);
			});
		}
		if (insertedDataValue != null) {
			actions.add((final List<LazyGetter> actionsAsync) -> {
				MongoLinkManager.addToList(ioDb, targetEntity, insertedDataValue, remoteFieldColumn, primaryKeyValue);
			});
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
		final AddOnFieldContext ctx = desc.getAddonContext();
		if (ctx.isAddLinkWhenCreate()) {
			final Class<?> targetEntity = ctx.getTargetEntity();
			final String remoteFieldColumn = ctx.getRemoteFieldColumn();
			actions.add((final List<LazyGetter> actionsAsync) -> {
				MongoLinkManager.addToList(ioDb, targetEntity, insertedData, remoteFieldColumn, primaryKeyValue);
			});
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

	@Override
	public void fillFromDoc(
			final DBAccessMongo ioDb,
			final Document doc,
			final DbPropertyDescriptor desc,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();
		final String fieldName = desc.getFieldName(options).inTable();
		if (!doc.containsKey(fieldName)) {
			prop.setValue(data, null);
			return;
		}
		final AddOnFieldContext ctx = desc.getAddonContext();

		if (ctx.isEntityReference()) {
			final Class<?> targetEntity = ctx.getTargetEntity();
			final Class<?> targetPkType = ctx.getTargetPkType();
			final String idFieldColumn = ctx.getTargetPkColumn();
			final Object dataRetrieve = doc.get(fieldName, targetPkType);
			if (dataRetrieve == null) {
				return;
			}
			if (batchCollector != null) {
				batchCollector.registerSingle(targetEntity, idFieldColumn, dataRetrieve, prop, data);
			} else {
				final LazyGetter lambda = (final List<LazyGetter> actionsAsync) -> {
					final Object foreignData = ioDb.getRaw(targetEntity,
							new Condition(Filters.eq(idFieldColumn, dataRetrieve)));
					if (foreignData == null) {
						return;
					}
					prop.setValue(data, foreignData);
				};
				lazyCall.add(lambda);
			}
			return;
		}
		final Object dataRetrieve = doc.get(fieldName, prop.getType());
		prop.setValue(data, dataRetrieve);
	}

	@Override
	public boolean asDeleteAction(final DbPropertyDescriptor desc) {
		final ManyToOneDoc decorators = desc.getProperty().getAnnotation(ManyToOneDoc.class);
		return decorators.removeLinkWhenDelete();
	}

	@Override
	public void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final DbPropertyDescriptor desc,
			final List<Object> previousDataThatIsDeleted,
			final List<LazyGetter> actions) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();
		final AddOnFieldContext ctx = desc.getAddonContext();
		final Class<?> targetEntity = ctx.getTargetEntity();
		final String remoteFieldColumn = ctx.getRemoteFieldColumn();
		final DbClassModel classModel = DbClassModel.of(clazz);
		final DbPropertyDescriptor pkDesc = classModel.getPrimaryKey();
		for (final Object obj : previousDataThatIsDeleted) {
			final Object primaryKeyRemovedObject = pkDesc.getProperty().getValue(obj);
			final Object parentKey = prop.getValue(obj);
			if (parentKey != null) {
				actions.add((final List<LazyGetter> actionsAsync) -> {
					MongoLinkManager.removeFromList(ioDb, targetEntity, parentKey, remoteFieldColumn,
							primaryKeyRemovedObject);
				});
			}
		}
	}
}
