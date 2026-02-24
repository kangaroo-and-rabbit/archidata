package org.atriasoft.archidata.dataAccess.commonTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to repair bidirectional relationship links that may become inconsistent
 * due to race conditions (e.g., simultaneous commits).
 *
 * <p>Supports {@link ManyToManyDoc}, {@link OneToManyDoc}, and {@link ManyToOneDoc} relationships.
 * The repair process reads the annotation on the given field to automatically discover the
 * remote class and remote field, then ensures both sides are consistent.
 */
public class LinkRepairTools {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinkRepairTools.class);

	private LinkRepairTools() {
		// Utility class
	}

	/**
	 * Repair bidirectional links for a given class and field.
	 *
	 * @param clazz                The source class containing the annotated field
	 * @param fieldName            The Java property name of the relationship field
	 * @param includeDeletedSource If true, also scan soft-deleted source documents
	 * @param includeDeletedRemote If true, also update links on soft-deleted remote documents
	 * @return A {@link RepairReport} summarizing what was found and fixed
	 * @throws Exception if any DB operation fails
	 */
	public static <T> RepairReport repairLinks(
			final Class<T> clazz,
			final String fieldName,
			final boolean includeDeletedSource,
			final boolean includeDeletedRemote) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final DbPropertyDescriptor fieldDesc = model.findByPropertyName(fieldName);
		if (fieldDesc == null) {
			throw new DataAccessException("Cannot find field '" + fieldName + "' in " + clazz.getCanonicalName());
		}
		final PropertyDescriptor prop = fieldDesc.getProperty();

		final ManyToManyDoc m2m = prop.getAnnotation(ManyToManyDoc.class);
		final OneToManyDoc o2m = prop.getAnnotation(OneToManyDoc.class);
		final ManyToOneDoc m2o = prop.getAnnotation(ManyToOneDoc.class);

		if (m2m != null) {
			return repairManyToMany(clazz, model, fieldDesc, m2m, includeDeletedSource, includeDeletedRemote);
		} else if (o2m != null) {
			return repairOneToMany(clazz, model, fieldDesc, o2m, includeDeletedSource, includeDeletedRemote);
		} else if (m2o != null) {
			return repairManyToOne(clazz, model, fieldDesc, m2o, includeDeletedSource, includeDeletedRemote);
		} else {
			throw new DataAccessException("Field '" + fieldName + "' in " + clazz.getCanonicalName()
					+ " has no @ManyToManyDoc, @OneToManyDoc or @ManyToOneDoc annotation");
		}
	}

	// ========== ManyToMany repair ==========

	@SuppressWarnings("unchecked")
	private static <T> RepairReport repairManyToMany(
			final Class<T> clazz,
			final DbClassModel model,
			final DbPropertyDescriptor fieldDesc,
			final ManyToManyDoc annotation,
			final boolean includeDeletedSource,
			final boolean includeDeletedRemote) throws Exception {
		final RepairReport report = new RepairReport();
		final Class<?> remoteClass = annotation.targetEntity();
		final String remoteFieldName = annotation.remoteField();

		final DbClassModel remoteModel = DbClassModel.of(remoteClass);
		final DbPropertyDescriptor remoteFieldDesc = remoteModel.findByPropertyName(remoteFieldName);
		if (remoteFieldDesc == null) {
			throw new DataAccessException(
					"Cannot find remote field '" + remoteFieldName + "' in " + remoteClass.getCanonicalName());
		}

		final PropertyDescriptor localProp = fieldDesc.getProperty();
		final PropertyDescriptor localPkProp = model.getPrimaryKey().getProperty();
		final String localFieldColumn = fieldDesc.getFieldName(null).inTable();

		final PropertyDescriptor remoteProp = remoteFieldDesc.getProperty();
		final PropertyDescriptor remotePkProp = remoteModel.getPrimaryKey().getProperty();
		final String remoteFieldColumn = remoteFieldDesc.getFieldName(null).inTable();

		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();

			// Step 1: Scan all source documents
			final List<T> sourceDocuments = DataAccess.gets(clazz, buildOptions(includeDeletedSource));
			for (final T sourceDoc : sourceDocuments) {
				report.incrementDocumentsScanned();
				final Object sourcePk = localPkProp.getValue(sourceDoc);
				final Object localValue = localProp.getValue(sourceDoc);
				if (!(localValue instanceof final Collection<?> localCollection)) {
					continue;
				}
				final List<Object> toRemove = new ArrayList<>();
				for (final Object remoteId : localCollection) {
					report.incrementLinksChecked();
					// Check if remote document exists
					final Object remoteDoc = getByIdWithDeleted(remoteClass, remoteId, includeDeletedRemote);
					if (remoteDoc == null) {
						// Remote doesn't exist => mark for removal
						toRemove.add(remoteId);
						report.incrementBrokenLinksRemoved();
						report.addDetail("M2M: Removed broken ref " + remoteId + " from " + clazz.getSimpleName() + "("
								+ sourcePk + ")." + fieldDesc.getProperty().getName());
					} else {
						// Remote exists, check if it contains source PK in its list
						final Object remoteValue = remoteProp.getValue(remoteDoc);
						if (!collectionContains(remoteValue, sourcePk)) {
							// Add source PK to remote list
							MongoLinkManager.addToList(db, remoteClass, remoteId, remoteFieldColumn, sourcePk);
							report.incrementMissingLinksAdded();
							report.addDetail("M2M: Added missing reverse link " + sourcePk + " to "
									+ remoteClass.getSimpleName() + "(" + remoteId + ")." + remoteFieldName);
						}
					}
				}
				// Remove broken refs from local list
				for (final Object brokenId : toRemove) {
					MongoLinkManager.removeFromList(db, clazz, sourcePk, localFieldColumn, brokenId);
				}
			}

			// Step 2: Scan all remote documents for orphaned reverse links
			final List<?> remoteDocuments = DataAccess.gets(remoteClass, buildOptions(includeDeletedRemote));
			// Build a set of source PKs for fast lookup
			final Set<Object> sourcePkSet = new HashSet<>();
			for (final T sourceDoc : sourceDocuments) {
				sourcePkSet.add(localPkProp.getValue(sourceDoc));
			}
			for (final Object remoteDoc : remoteDocuments) {
				report.incrementDocumentsScanned();
				final Object remotePk = remotePkProp.getValue(remoteDoc);
				final Object remoteValue = remoteProp.getValue(remoteDoc);
				if (!(remoteValue instanceof final Collection<?> remoteCollection)) {
					continue;
				}
				final List<Object> toRemoveFromRemote = new ArrayList<>();
				for (final Object sourceId : remoteCollection) {
					report.incrementLinksChecked();
					if (!sourcePkSet.contains(sourceId)) {
						// Source doesn't exist => mark for removal
						toRemoveFromRemote.add(sourceId);
						report.incrementBrokenLinksRemoved();
						report.addDetail("M2M: Removed broken reverse ref " + sourceId + " from "
								+ remoteClass.getSimpleName() + "(" + remotePk + ")." + remoteFieldName);
					} else {
						// Source exists, check it still lists this remote
						final Object sourceDoc = getByIdWithDeleted(clazz, sourceId, includeDeletedSource);
						if (sourceDoc != null) {
							final Object srcValue = localProp.getValue(sourceDoc);
							if (!collectionContains(srcValue, remotePk)) {
								toRemoveFromRemote.add(remotePk);
								report.incrementInconsistentLinksFixed();
								report.addDetail("M2M: Removed orphaned reverse ref " + remotePk + " from "
										+ remoteClass.getSimpleName() + "(" + remotePk + ")." + remoteFieldName
										+ " (source " + sourceId + " no longer lists it)");
							}
						}
					}
				}
				for (final Object orphanId : toRemoveFromRemote) {
					MongoLinkManager.removeFromList(db, remoteClass, remotePk, remoteFieldColumn, orphanId);
				}
			}
		}

		LOGGER.info("ManyToMany repair for {}.{}: {}", clazz.getSimpleName(), fieldDesc.getProperty().getName(),
				report);
		return report;
	}

	// ========== OneToMany repair ==========

	@SuppressWarnings("unchecked")
	private static <T> RepairReport repairOneToMany(
			final Class<T> clazz,
			final DbClassModel model,
			final DbPropertyDescriptor fieldDesc,
			final OneToManyDoc annotation,
			final boolean includeDeletedSource,
			final boolean includeDeletedRemote) throws Exception {
		final RepairReport report = new RepairReport();
		final Class<?> remoteClass = annotation.targetEntity();
		final String remoteFieldName = annotation.remoteField();

		final DbClassModel remoteModel = DbClassModel.of(remoteClass);
		final DbPropertyDescriptor remoteFieldDesc = remoteModel.findByPropertyName(remoteFieldName);
		if (remoteFieldDesc == null) {
			throw new DataAccessException(
					"Cannot find remote field '" + remoteFieldName + "' in " + remoteClass.getCanonicalName());
		}

		final PropertyDescriptor localProp = fieldDesc.getProperty();
		final PropertyDescriptor localPkProp = model.getPrimaryKey().getProperty();
		final String localFieldColumn = fieldDesc.getFieldName(null).inTable();

		final PropertyDescriptor remoteProp = remoteFieldDesc.getProperty();
		final PropertyDescriptor remotePkProp = remoteModel.getPrimaryKey().getProperty();
		final String remoteFieldColumn = remoteFieldDesc.getFieldName(null).inTable();

		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();

			// Step 1: Scan parent documents (source has List, remote has scalar)
			final List<T> parentDocuments = DataAccess.gets(clazz, buildOptions(includeDeletedSource));
			for (final T parentDoc : parentDocuments) {
				report.incrementDocumentsScanned();
				final Object parentPk = localPkProp.getValue(parentDoc);
				final Object localValue = localProp.getValue(parentDoc);
				if (!(localValue instanceof final Collection<?> childIdCollection)) {
					continue;
				}
				final List<Object> toRemove = new ArrayList<>();
				for (final Object childId : childIdCollection) {
					report.incrementLinksChecked();
					final Object childDoc = getByIdWithDeleted(remoteClass, childId, includeDeletedRemote);
					if (childDoc == null) {
						// Child doesn't exist => remove from parent list
						toRemove.add(childId);
						report.incrementBrokenLinksRemoved();
						report.addDetail("O2M: Removed broken child ref " + childId + " from " + clazz.getSimpleName()
								+ "(" + parentPk + ")." + fieldDesc.getProperty().getName());
					} else {
						// Child exists, check that its parent field points to this parent
						final Object childParentValue = remoteProp.getValue(childDoc);
						if (!parentPk.equals(childParentValue)) {
							// Fix child's parent field
							MongoLinkManager.setField(db, remoteClass, childId, remoteFieldColumn, parentPk);
							report.incrementInconsistentLinksFixed();
							report.addDetail("O2M: Fixed child " + remoteClass.getSimpleName() + "(" + childId + ")."
									+ remoteFieldName + " from " + childParentValue + " to " + parentPk);
						}
					}
				}
				for (final Object brokenId : toRemove) {
					MongoLinkManager.removeFromList(db, clazz, parentPk, localFieldColumn, brokenId);
				}
			}

			// Step 2: Scan children for orphaned references
			final List<?> childDocuments = DataAccess.gets(remoteClass, buildOptions(includeDeletedRemote));
			final Set<Object> parentPkSet = new HashSet<>();
			for (final T parentDoc : parentDocuments) {
				parentPkSet.add(localPkProp.getValue(parentDoc));
			}
			for (final Object childDoc : childDocuments) {
				report.incrementDocumentsScanned();
				final Object childPk = remotePkProp.getValue(childDoc);
				final Object childParentValue = remoteProp.getValue(childDoc);
				if (childParentValue == null) {
					continue;
				}
				report.incrementLinksChecked();
				if (!parentPkSet.contains(childParentValue)) {
					// Parent doesn't exist => set child's parent to null
					MongoLinkManager.setField(db, remoteClass, childPk, remoteFieldColumn, null);
					report.incrementBrokenLinksRemoved();
					report.addDetail("O2M: Nullified parent ref on child " + remoteClass.getSimpleName() + "(" + childPk
							+ ")." + remoteFieldName + " (parent " + childParentValue + " not found)");
				} else {
					// Parent exists, check if it lists this child
					final Object parentDoc = getByIdWithDeleted(clazz, childParentValue, includeDeletedSource);
					if (parentDoc != null) {
						final Object parentListValue = localProp.getValue(parentDoc);
						if (!collectionContains(parentListValue, childPk)) {
							// Add child to parent's list
							MongoLinkManager.addToList(db, clazz, childParentValue, localFieldColumn, childPk);
							report.incrementMissingLinksAdded();
							report.addDetail("O2M: Added missing child " + childPk + " to " + clazz.getSimpleName()
									+ "(" + childParentValue + ")." + fieldDesc.getProperty().getName());
						}
					}
				}
			}
		}

		LOGGER.info("OneToMany repair for {}.{}: {}", clazz.getSimpleName(), fieldDesc.getProperty().getName(), report);
		return report;
	}

	// ========== ManyToOne repair ==========

	@SuppressWarnings("unchecked")
	private static <T> RepairReport repairManyToOne(
			final Class<T> clazz,
			final DbClassModel model,
			final DbPropertyDescriptor fieldDesc,
			final ManyToOneDoc annotation,
			final boolean includeDeletedSource,
			final boolean includeDeletedRemote) throws Exception {
		final RepairReport report = new RepairReport();
		final Class<?> remoteClass = annotation.targetEntity();
		final String remoteFieldName = annotation.remoteField();

		final DbClassModel remoteModel = DbClassModel.of(remoteClass);
		final DbPropertyDescriptor remoteFieldDesc = remoteModel.findByPropertyName(remoteFieldName);
		if (remoteFieldDesc == null) {
			throw new DataAccessException(
					"Cannot find remote field '" + remoteFieldName + "' in " + remoteClass.getCanonicalName());
		}

		final PropertyDescriptor localProp = fieldDesc.getProperty();
		final PropertyDescriptor localPkProp = model.getPrimaryKey().getProperty();
		final String localFieldColumn = fieldDesc.getFieldName(null).inTable();

		final PropertyDescriptor remoteProp = remoteFieldDesc.getProperty();
		final PropertyDescriptor remotePkProp = remoteModel.getPrimaryKey().getProperty();
		final String remoteFieldColumn = remoteFieldDesc.getFieldName(null).inTable();

		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();

			// Step 1: Scan children (source has scalar, remote has List)
			final List<T> childDocuments = DataAccess.gets(clazz, buildOptions(includeDeletedSource));
			for (final T childDoc : childDocuments) {
				report.incrementDocumentsScanned();
				final Object childPk = localPkProp.getValue(childDoc);
				final Object parentId = localProp.getValue(childDoc);
				if (parentId == null) {
					continue;
				}
				report.incrementLinksChecked();
				final Object parentDoc = getByIdWithDeleted(remoteClass, parentId, includeDeletedRemote);
				if (parentDoc == null) {
					// Parent doesn't exist => set child's parent to null
					MongoLinkManager.setField(db, clazz, childPk, localFieldColumn, null);
					report.incrementBrokenLinksRemoved();
					report.addDetail("M2O: Nullified parent ref on " + clazz.getSimpleName() + "(" + childPk + ")."
							+ fieldDesc.getProperty().getName() + " (parent " + parentId + " not found)");
				} else {
					// Parent exists, check if it lists this child in its remote list
					final Object parentListValue = remoteProp.getValue(parentDoc);
					if (!collectionContains(parentListValue, childPk)) {
						// Add child to parent's list
						MongoLinkManager.addToList(db, remoteClass, parentId, remoteFieldColumn, childPk);
						report.incrementMissingLinksAdded();
						report.addDetail("M2O: Added missing child " + childPk + " to " + remoteClass.getSimpleName()
								+ "(" + parentId + ")." + remoteFieldName);
					}
				}
			}

			// Step 2: Scan parents for orphaned child refs
			final List<?> parentDocuments = DataAccess.gets(remoteClass, buildOptions(includeDeletedRemote));
			final Set<Object> childPkSet = new HashSet<>();
			for (final T childDoc : childDocuments) {
				childPkSet.add(localPkProp.getValue(childDoc));
			}
			for (final Object parentDoc : parentDocuments) {
				report.incrementDocumentsScanned();
				final Object parentPk = remotePkProp.getValue(parentDoc);
				final Object parentListValue = remoteProp.getValue(parentDoc);
				if (!(parentListValue instanceof final Collection<?> childIdCollection)) {
					continue;
				}
				final List<Object> toRemove = new ArrayList<>();
				for (final Object childId : childIdCollection) {
					report.incrementLinksChecked();
					if (!childPkSet.contains(childId)) {
						// Child doesn't exist => mark for removal
						toRemove.add(childId);
						report.incrementBrokenLinksRemoved();
						report.addDetail("M2O: Removed broken child ref " + childId + " from "
								+ remoteClass.getSimpleName() + "(" + parentPk + ")." + remoteFieldName);
					} else {
						// Child exists, check that its parent field points to this parent
						final Object childDoc = getByIdWithDeleted(clazz, childId, includeDeletedSource);
						if (childDoc != null) {
							final Object childParentValue = localProp.getValue(childDoc);
							if (!parentPk.equals(childParentValue)) {
								toRemove.add(childId);
								report.incrementInconsistentLinksFixed();
								report.addDetail("M2O: Removed orphaned ref " + childId + " from "
										+ remoteClass.getSimpleName() + "(" + parentPk + ")." + remoteFieldName
										+ " (child points to " + childParentValue + ")");
							}
						}
					}
				}
				for (final Object orphanId : toRemove) {
					MongoLinkManager.removeFromList(db, remoteClass, parentPk, remoteFieldColumn, orphanId);
				}
			}
		}

		LOGGER.info("ManyToOne repair for {}.{}: {}", clazz.getSimpleName(), fieldDesc.getProperty().getName(), report);
		return report;
	}

	// ========== Helpers ==========

	private static QueryOption[] buildOptions(final boolean includeDeleted) {
		if (includeDeleted) {
			return new QueryOption[] { new AccessDeletedItems() };
		}
		return new QueryOption[0];
	}

	private static <ID_TYPE> Object getByIdWithDeleted(
			final Class<?> clazz,
			final ID_TYPE id,
			final boolean includeDeleted) throws Exception {
		if (includeDeleted) {
			return DataAccess.getById(clazz, id, new AccessDeletedItems());
		}
		return DataAccess.getById(clazz, id);
	}

	private static boolean collectionContains(final Object value, final Object target) {
		if (value instanceof final Collection<?> collection) {
			return collection.contains(target);
		}
		return false;
	}
}
