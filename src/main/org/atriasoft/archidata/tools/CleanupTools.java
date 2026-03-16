package org.atriasoft.archidata.tools;

import java.util.Date;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.ForceHardDelete;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

/**
 * Utility class for cleaning up deleted records from the database.
 *
 * <p>Provides methods to permanently remove soft-deleted and async-hard-deleted
 * records older than a given threshold date. Field names are resolved
 * automatically from the entity's annotations via {@link DbClassModel}.
 */
public final class CleanupTools {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanupTools.class);

	private CleanupTools() {}

	/**
	 * Permanently deletes soft-deleted records (with {@code @DataDeleted} flag set to true)
	 * that were last updated before the given threshold date.
	 *
	 * <p>The entity class must have both a {@code @DataDeleted} field and an
	 * {@code @UpdateTimestamp} field, otherwise a {@link DataAccessException} is thrown.
	 *
	 * @param clazz         The entity class to clean up
	 * @param thresholdDate Records updated before this date will be permanently deleted
	 * @return The number of records physically deleted
	 * @throws Exception if the entity lacks required fields or the delete operation fails
	 */
	public static long cleanupSoftDeletedRecords(final Class<?> clazz, final Date thresholdDate) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String deletedFieldName = model.getDeletedFieldName();
		if (deletedFieldName == null) {
			throw new DataAccessException("Cannot cleanup soft-deleted records: class " + clazz.getSimpleName()
					+ " has no @DataDeleted field");
		}
		final String updateTsFieldName = resolveUpdateTimestampFieldName(model, clazz);
		final Condition condition = new Condition(
				Filters.and(Filters.eq(deletedFieldName, true), Filters.lt(updateTsFieldName, thresholdDate)));
		final long count = DataAccess.deleteHard(clazz, condition, new AccessDeletedItems(), new ForceHardDelete());
		LOGGER.info("Cleanup soft-deleted {}: {} records removed (threshold: {})", clazz.getSimpleName(), count,
				thresholdDate);
		return count;
	}

	/**
	 * Permanently deletes async-hard-deleted records (with {@code @DataAsyncHardDeleted}
	 * flag set to true) that were last updated before the given threshold date.
	 *
	 * <p>The entity class must have both a {@code @DataAsyncHardDeleted} field and an
	 * {@code @UpdateTimestamp} field, otherwise a {@link DataAccessException} is thrown.
	 *
	 * @param clazz         The entity class to clean up
	 * @param thresholdDate Records updated before this date will be permanently deleted
	 * @return The number of records physically deleted
	 * @throws Exception if the entity lacks required fields or the delete operation fails
	 */
	public static long cleanupAsyncHardDeletedRecords(final Class<?> clazz, final Date thresholdDate) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String asyncHardDeletedFieldName = model.getAsyncHardDeletedFieldName();
		if (asyncHardDeletedFieldName == null) {
			throw new DataAccessException("Cannot cleanup async-hard-deleted records: class " + clazz.getSimpleName()
					+ " has no @DataAsyncHardDeleted field");
		}
		final String updateTsFieldName = resolveUpdateTimestampFieldName(model, clazz);
		final Condition condition = new Condition(
				Filters.and(Filters.eq(asyncHardDeletedFieldName, true), Filters.lt(updateTsFieldName, thresholdDate)));
		final long count = DataAccess.deleteHard(clazz, condition, new AccessDeletedItems(), new ForceHardDelete());
		LOGGER.info("Cleanup async-hard-deleted {}: {} records removed (threshold: {})", clazz.getSimpleName(), count,
				thresholdDate);
		return count;
	}

	private static String resolveUpdateTimestampFieldName(final DbClassModel model, final Class<?> clazz)
			throws DataAccessException {
		final DbPropertyDescriptor updateTsDesc = model.getUpdateTimestamp();
		if (updateTsDesc == null) {
			throw new DataAccessException(
					"Cannot cleanup records: class " + clazz.getSimpleName() + " has no @UpdateTimestamp field");
		}
		return updateTsDesc.getDbFieldName();
	}
}
