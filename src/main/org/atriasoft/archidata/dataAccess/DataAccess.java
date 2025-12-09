package org.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

/**
 * Data access is an abstraction class that permit to access on the DB with a
 * function wrapping that permit to minimize the SQL writing of SQL code. This
 * interface support the SQL and SQLite back-end.
 */
public class DataAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);

	public DataAccess() {

	}

	public static List<String> listCollections(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.listCollections(name, options);
		}
	}

	public static boolean isDBExist(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.isDBExist(name, options);
		}
	}

	public static boolean createDB(final String name)
			throws IOException, InternalServerErrorException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.createDB(name);
		}
	}

	public static boolean isTableExist(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.isTableExist(name, options);
		}
	}

	// TODO: manage insert batch...
	public static <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.insertMultiple(data, options);
		}
	}

	@SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
	public static <T> T insert(final T data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.insert(data, options);
		}
	}

	public static <ID_TYPE> QueryCondition getTableIdCondition(
			final Class<?> clazz,
			final ID_TYPE idKey,
			final QueryOptions options) throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getTableIdCondition(clazz, idKey, options);
		}
	}

	public static <T, ID_TYPE> long update(final T data, final ID_TYPE id) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.update(data, id);
		}
	}

	public static <T, ID_TYPE> long update(final T data, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.update(data, id, option);
		}
	}

	public static <T, ID_TYPE> long updateFull(final T data, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.updateFull(data, id, options);
		}
	}

	public static <T> long updateWhere(final T data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.updateWhere(data, options);
		}
	}

	public static <T> long updateWhere(final T data, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.updateWhere(data, options);
		}
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getWhere(clazz, options);
		}
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getWhere(clazz, options);
		}
	}

	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getsWhere(clazz, options);
		}
	}

	public static Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.conditionFusionOrEmpty(options, throwIfEmpty);
		}
	}

	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getsWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.count(clazz, id, options);
		}
	}

	public static long countWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.countWhere(clazz, options);
		}
	}

	public static long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.countWhere(clazz, options);
		}
	}

	@Nullable
	public static <T, ID_TYPE> T get(final Class<T> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.get(clazz, id, options);
		}
	}

	public static <T> List<T> gets(final Class<T> clazz) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.gets(clazz);
		}
	}

	public static <T> List<T> gets(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.gets(clazz, options);
		}
	}

	/**
	 * Delete items with the specific Id (cf @Id) and some options. If the Entity is
	 * manage as a softDeleted model, then it is flag as removed (if not already
	 * done before).
	 *
	 * @param <ID_TYPE> Type of the reference @Id
	 * @param clazz     Data model that might remove element
	 * @param id        Unique Id of the model
	 * @param options   (Optional) Options of the request
	 * @return Number of element that is removed.
	 */
	public static <ID_TYPE> long delete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.delete(clazz, id, options);
		}
	}

	/**
	 * Delete items with the specific condition and some options. If the Entity is
	 * manage as a softDeleted model, then it is flag as removed (if not already
	 * done before).
	 *
	 * @param clazz   Data model that might remove element.
	 * @param options (Optional) Options of the request.
	 * @return Number of element that is removed.
	 */
	public static long deleteWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteHard(clazz, id, options);
		}
	}

	public static long deleteHardWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteHardWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteSoft(clazz, id, options);
		}
	}

	public static long deleteSoftWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteSoftWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.unsetDelete(clazz, id);
		}
	}

	public static <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.unsetDelete(clazz, id, options);
		}
	}

	public static long unsetDeleteWhere(final Class<?> clazz, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.unsetDeleteWhere(clazz, options);
		}
	}

	public static void drop(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			db.drop(clazz, options);
		}
	}

	public static void cleanAll(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			db.cleanAll(clazz, options);
		}
	}

}
