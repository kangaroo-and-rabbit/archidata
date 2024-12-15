package org.kar.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.QueryOption;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

/** Data access is an abstraction class that permit to access on the DB with a function wrapping that permit to minimize the SQL writing of SQL code. This interface support the SQL and SQLite
 * back-end. */
public class DataAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);

	public DataAccess() {

	}

	public static boolean isDBExist(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.isDBExist(name, options);
		}
	}

	public static boolean createDB(final String name)
			throws IOException, InternalServerErrorException, DataAccessException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.createDB(name);
		}
	}

	public static boolean isTableExist(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.isTableExist(name, options);
		}
	}

	// TODO: manage insert batch...
	public static <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.insertMultiple(data, options);
		}
	}

	@SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
	public static <T> T insert(final T data, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.insert(data, options);
		}
	}

	// seems a good idea, but very dangerous if we not filter input data... if set an id it can be complicated...
	public static <T> T insertWithJson(final Class<T> clazz, final String jsonData) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.insertWithJson(clazz, jsonData);
		}
	}

	public static <ID_TYPE> QueryCondition getTableIdCondition(final Class<?> clazz, final ID_TYPE idKey)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.getTableIdCondition(clazz, idKey);
		}
	}

	/** Update an object with the inserted json data
	 *
	 * @param <T> Type of the object to insert
	 * @param <ID_TYPE> Master key on the object manage with @Id
	 * @param clazz Class reference of the insertion model
	 * @param id Key to insert data
	 * @param jsonData Json data (partial) values to update
	 * @return the number of object updated
	 * @throws Exception */
	public static <T, ID_TYPE> long updateWithJson(
			final Class<T> clazz,
			final ID_TYPE id,
			final String jsonData,
			final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.updateWithJson(clazz, id, jsonData, options);
		}
	}

	public static <T> long updateWhereWithJson(
			final Class<T> clazz,
			final String jsonData,
			final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.updateWhereWithJson(clazz, jsonData, options);
		}
	}

	public static <T, ID_TYPE> long update(final T data, final ID_TYPE id) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.update(data, id);
		}
	}

	/** @param <T>
	 * @param data
	 * @param id
	 * @param filterValue
	 * @return the affected rows.
	 * @throws Exception */
	public static <T, ID_TYPE> long update(
			final T data,
			final ID_TYPE id,
			final List<String> updateColomn,
			final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.update(data, id, updateColomn, options);
		}
	}

	public static <T> long updateWhere(final T data, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.updateWhere(data, options);
		}
	}

	public static <T> long updateWhere(final T data, final QueryOptions options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.updateWhere(data, options);
		}
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOptions options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.getWhere(clazz, options);
		}
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.getWhere(clazz, options);
		}
	}

	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.getsWhere(clazz, options);
		}
	}

	public static Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.conditionFusionOrEmpty(options, throwIfEmpty);
		}
	}

	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.getsWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.count(clazz, id, options);
		}
	}

	public static long countWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.countWhere(clazz, options);
		}
	}

	public static long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.countWhere(clazz, options);
		}
	}

	public static <T, ID_TYPE> T get(final Class<T> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.get(clazz, id, options);
		}
	}

	public static <T> List<T> gets(final Class<T> clazz) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.gets(clazz);
		}
	}

	public static <T> List<T> gets(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.gets(clazz, options);
		}
	}

	/** Delete items with the specific Id (cf @Id) and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param <ID_TYPE> Type of the reference @Id
	 * @param clazz Data model that might remove element
	 * @param id Unique Id of the model
	 * @param options (Optional) Options of the request
	 * @return Number of element that is removed. */
	public static <ID_TYPE> long delete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.delete(clazz, id, options);
		}
	}

	/** Delete items with the specific condition and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param clazz Data model that might remove element.
	 * @param condition Condition to remove elements.
	 * @param options (Optional) Options of the request.
	 * @return Number of element that is removed. */
	public static long deleteWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.deleteWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.deleteHard(clazz, id, options);
		}
	}

	public static long deleteHardWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.deleteHardWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.deleteSoft(clazz, id, options);
		}
	}

	public static long deleteSoftWhere(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.deleteSoftWhere(clazz, options);
		}
	}

	public static <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.unsetDelete(clazz, id);
		}
	}

	public static <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.unsetDelete(clazz, id, options);
		}
	}

	public static long unsetDeleteWhere(final Class<?> clazz, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DBAccess db = DBAccess.createInterface()) {
			return db.unsetDeleteWhere(clazz, options);
		}
	}

	public static void drop(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			db.drop(clazz, options);
		}
	}

	public static void cleanAll(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DBAccess db = DBAccess.createInterface()) {
			db.cleanAll(clazz, options);
		}
	}

}
