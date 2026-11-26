package org.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.ws.rs.InternalServerErrorException;

/**
 * Static wrapper providing simplified access to MongoDB database operations.
 *
 * <p>
 * This class provides static methods that automatically manage database connections
 * via {@link DataAccessConnectionContext}. All methods use try-with-resources to
 * ensure proper connection lifecycle management.
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> Connection management is thread-local. Each thread
 * gets its own database connection which is automatically reused within the same thread.
 * </p>
 *
 * <p>
 * The examples of this class use {@code Task}, the entity of {@code sample/archidata/basic}: a {@code Long}
 * id, {@code title}, {@code description} and {@code done} fields, and a soft-delete flag.
 * </p>
 * <pre>
 * // Insert a new task
 * Task inserted = DataAccess.insert(new Task("Buy milk", "2 liters"));
 * Long taskId = inserted.getId();
 *
 * // Retrieve by ID
 * Task found = DataAccess.getById(Task.class, taskId);
 *
 * // Update
 * found.setTitle("Buy oat milk");
 * DataAccess.updateById(found, taskId);
 *
 * // Delete (a soft delete: Task has a deleted flag)
 * DataAccess.deleteById(Task.class, taskId);
 * </pre>
 */
public class DataAccess {
	private DataAccess() {
		// Utility class
	}

	// ========================================================================
	// Database management methods
	// ========================================================================

	/**
	 * Lists the names of the collections of the database the connection is bound to.
	 *
	 * <p>
	 * {@code name} and {@code options} are ignored: the database is the one of the connection, not the one named.
	 * </p>
	 *
	 * @param name    Ignored
	 * @param options Ignored
	 * @return List of collection names
	 * @throws InternalServerErrorException if operation fails
	 * @throws IOException                  if I/O error occurs
	 * @throws DataAccessException          if database access fails
	 */
	public static List<String> listCollections(final String name, final QueryOption... options)
			throws InternalServerErrorException, IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.listCollections(name, options);
		}
	}

	// ========================================================================
	// Insert methods
	// ========================================================================

	/**
	 * Inserts multiple entities into the database.
	 *
	 * <p>
	 * Each entity is inserted individually with its auto-generated ID.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * List&lt;Task&gt; inserted = DataAccess.insertMultiple(List.of(task1, task2, task3));
	 * Long firstId = inserted.get(0).getId();
	 * </pre>
	 *
	 * @param <T>     The type of entities
	 * @param data    List of entities to insert
	 * @param options Optional query options
	 * @return List of inserted entities with generated IDs
	 * @throws Exception if insertion fails
	 */
	public static <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.insertMultiple(data, options);
		}
	}

	/**
	 * Inserts a single entity into the database.
	 *
	 * <p>
	 * The entity is inserted, then read back by its generated primary key. The read-back follows the usual
	 * read rules: a {@code @DataNotRead} field such as {@code createdAt} is {@code null} in the result unless
	 * {@link org.atriasoft.archidata.dataAccess.options.ReadAllColumn} is passed.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Task inserted = DataAccess.insert(new Task("Buy milk", "2 liters"));
	 * Long taskId = inserted.getId(); // generated
	 *
	 * // createdAt is read back only with ReadAllColumn
	 * Task withTimestamps = DataAccess.insert(new Task("Buy bread", ""), new ReadAllColumn());
	 * Date created = withTimestamps.getCreatedAt();
	 * </pre>
	 *
	 * @param <T>     The type of entity
	 * @param data    Entity to insert
	 * @param options Optional query options
	 * @return The inserted entity with generated fields
	 * @throws Exception if insertion fails
	 */
	public static <T> T insert(final T data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.insert(data, options);
		}
	}

	// ========================================================================
	// Update methods
	// ========================================================================

	/**
	 * Generates a query condition for matching by ID.
	 *
	 * <p>
	 * This is a utility method used internally for building ID-based queries.
	 * </p>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param idKey     ID value
	 * @param options   Query options
	 * @return Bson filter for the ID
	 * @throws DataAccessException if operation fails
	 * @throws IOException         if I/O error occurs
	 */
	public static <ID_TYPE> Bson getTableIdCondition(
			final Class<?> clazz,
			final ID_TYPE idKey,
			final QueryOptions options) throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getTableIdCondition(clazz, idKey, options);
		}
	}

	/**
	 * Updates an entity identified by its ID.
	 *
	 * <p>
	 * <strong>Field filtering:</strong> without a {@code FilterValue}, every editable field is written, a
	 * {@code null} one included (it is unset); the {@code @DataNotRead} and {@code @DataEncrypt} fields are not
	 * part of that default. Use {@code FilterValue} to write only the named fields.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Task update = new Task();
	 * update.setTitle("Buy oat milk");
	 *
	 * // Write every editable field of update (the ones left null are unset)
	 * DataAccess.updateById(update, taskId);
	 *
	 * // Write only the title
	 * DataAccess.updateById(update, taskId, new FilterValue("title"));
	 * </pre>
	 *
	 * <p>A {@link Condition} in the options is ANDed with the id; the relation side effects follow it (none when
	 * it matches nothing or the document is soft-deleted), see {@link DBAccessMongo#updateById}.
	 *
	 * @param <T>       The type of entity
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param data      Entity data to update
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param option    Optional query options (e.g., FilterValue)
	 * @return Number of documents modified (0 or 1): the date of an {@code @UpdateTimestamp} field is always
	 *         written (unless {@code DirectData}), so a matched document counts 1; without such a field, a
	 *         matched document whose values do not change counts 0
	 * @throws Exception if update fails
	 */
	public static <T, ID_TYPE> long updateById(final T data, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.updateById(data, id, option);
		}
	}

	/**
	 * Updates entities matching specified conditions (varargs version).
	 *
	 * <p>
	 * <strong>Required:</strong> Must provide {@code FilterValue} and {@code Condition}.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * Task update = new Task();
	 * update.setDone(true);
	 *
	 * long count = DataAccess.update(update,
	 *     new Condition(Filters.eq("title", "Buy milk")),
	 *     new FilterValue("done"));
	 * </pre>
	 *
	 * @param <T>     The type of entity
	 * @param data    Entity data with update values
	 * @param options Query options (FilterValue and Condition required)
	 * @return Number of documents modified
	 * @throws Exception if update fails
	 */
	public static <T> long update(final T data, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.update(data, options);
		}
	}

	/**
	 * Updates entities matching specified conditions (QueryOptions version).
	 *
	 * @param <T>     The type of entity
	 * @param data    Entity data with update values
	 * @param options Query options object
	 * @return Number of documents modified
	 * @throws Exception if update fails
	 * @see #update(Object, QueryOption...)
	 */
	public static <T> long update(final T data, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.update(data, options);
		}
	}

	// ========================================================================
	// Read/Get methods
	// ========================================================================

	/**
	 * Retrieves a single entity matching conditions (QueryOptions version).
	 *
	 * @param <T>     The type of entity
	 * @param clazz   Entity class
	 * @param options Query options with conditions
	 * @return First matching entity or null
	 * @throws Exception if retrieval fails
	 * @see #get(Class, QueryOption...)
	 */
	public static <T> T get(final Class<T> clazz, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.get(clazz, options);
		}
	}

	/**
	 * Retrieves a single entity matching conditions.
	 *
	 * <p>
	 * Returns the first entity matching the conditions, or null if none found.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * // Find a task by title
	 * Task task = DataAccess.get(Task.class,
	 *     new Condition(Filters.eq("title", "Buy milk")));
	 * </pre>
	 *
	 * @param <T>     The type of entity
	 * @param clazz   Entity class
	 * @param options Query options (e.g., Condition)
	 * @return First matching entity or null
	 * @throws Exception if retrieval fails
	 */
	public static <T> T get(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.get(clazz, options);
		}
	}

	/**
	 * Retrieves all entities matching conditions (varargs version).
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * // Get all the open tasks
	 * List&lt;Task&gt; tasks = DataAccess.gets(Task.class,
	 *     new Condition(Filters.eq("done", false)));
	 *
	 * // Get the 10 newest tasks
	 * List&lt;Task&gt; newest = DataAccess.gets(Task.class,
	 *     OrderBy.desc(Task::getCreatedAt),
	 *     new Limit(10));
	 * </pre>
	 *
	 * @param <T>     The type of entity
	 * @param clazz   Entity class
	 * @param options Query options (e.g., Condition, Limit, OrderBy)
	 * @return List of matching entities (empty if none found)
	 * @throws Exception if retrieval fails
	 */
	public static <T> List<T> gets(final Class<T> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.gets(clazz, options);
		}
	}

	/**
	 * Merges multiple conditions from options into a single condition.
	 *
	 * <p>
	 * This is a utility method used internally for condition handling.
	 * </p>
	 *
	 * @param options       Query options containing conditions
	 * @param throwIfEmpty  If true, throws exception when no conditions found
	 * @return Merged condition
	 * @throws DataAccessException if no conditions and throwIfEmpty is true
	 * @throws IOException         if I/O error occurs
	 */
	public static Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.conditionFusionOrEmpty(options, throwIfEmpty);
		}
	}

	/**
	 * Retrieves all entities matching conditions (QueryOptions version).
	 *
	 * @param <T>     The type of entity
	 * @param clazz   Entity class
	 * @param options Query options object
	 * @return List of matching entities (empty if none found)
	 * @throws DataAccessException if retrieval fails
	 * @throws IOException         if I/O error occurs
	 * @see #gets(Class, QueryOption...)
	 */
	public static <T> List<T> gets(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.gets(clazz, options);
		}
	}

	// ========================================================================
	// Count methods
	// ========================================================================

	/**
	 * Checks if an entity with the specified ID exists.
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * boolean exists = DataAccess.existsById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return true if entity exists, false otherwise
	 * @throws Exception if check fails
	 */
	public static <ID_TYPE> boolean existsById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.existsById(clazz, id, options);
		}
	}

	/**
	 * Counts entities matching conditions (varargs version).
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * // Count the open tasks
	 * long open = DataAccess.count(Task.class,
	 *     new Condition(Filters.eq("done", false)));
	 * </pre>
	 *
	 * @param clazz   Entity class
	 * @param options Query options (e.g., Condition)
	 * @return Number of matching entities
	 * @throws Exception if count fails
	 */
	public static long count(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.count(clazz, options);
		}
	}

	/**
	 * Counts entities matching conditions (QueryOptions version).
	 *
	 * @param clazz   Entity class
	 * @param options Query options object
	 * @return Number of matching entities
	 * @throws Exception if count fails
	 * @see #count(Class, QueryOption...)
	 */
	public static long count(final Class<?> clazz, final QueryOptions options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.count(clazz, options);
		}
	}

	/**
	 * Retrieves an entity by its unique identifier.
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * Task task = DataAccess.getById(Task.class, taskId); // null when absent
	 *
	 * // Get with all columns (including the @DataNotRead ones such as createdAt)
	 * Task full = DataAccess.getById(Task.class, taskId, new ReadAllColumn());
	 * </pre>
	 *
	 * @param <T>       The type of entity
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options (e.g., ReadAllColumn)
	 * @return Entity with specified ID or null if not found
	 * @throws Exception if retrieval fails
	 */
	@Nullable
	public static <T, ID_TYPE> T getById(final Class<T> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getById(clazz, id, options);
		}
	}

	/**
	 * Retrieves all entities of the specified type.
	 *
	 * <p>
	 * <strong>Warning:</strong> This retrieves ALL entities without filtering.
	 * Use with caution on large datasets.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * List&lt;Task&gt; allTasks = DataAccess.getAll(Task.class);
	 * </pre>
	 *
	 * @param <T>   The type of entity
	 * @param clazz Entity class
	 * @return List of all entities (empty if none found)
	 * @throws Exception if retrieval fails
	 */
	public static <T> List<T> getAll(final Class<T> clazz) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getAll(clazz);
		}
	}

	// ========================================================================
	// Delete methods
	// ========================================================================

	/**
	 * Deletes an entity by its ID (automatic soft/hard routing).
	 *
	 * <p>
	 * <strong>Automatic routing:</strong> if the entity has a field annotated {@code @DataDeleted}, performs a
	 * soft delete. Otherwise performs a hard delete.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * long deleted = DataAccess.deleteById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return Number of entities deleted (typically 0 or 1)
	 * @throws Exception if deletion fails
	 */
	public static <ID_TYPE> long deleteById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteById(clazz, id, options);
		}
	}

	/**
	 * Deletes entities matching conditions (automatic soft/hard routing).
	 *
	 * <p>
	 * <strong>Automatic routing:</strong> if the entity has a field annotated {@code @DataDeleted}, performs a
	 * soft delete. Otherwise performs a hard delete.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * // Delete all the finished tasks
	 * long count = DataAccess.delete(Task.class,
	 *     new Condition(Filters.eq("done", true)));
	 * </pre>
	 *
	 * @param clazz   Entity class
	 * @param options Query options (Condition required)
	 * @return Number of entities deleted
	 * @throws Exception if deletion fails
	 */
	public static long delete(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.delete(clazz, options);
		}
	}

	/**
	 * Physically deletes an entity by its ID (a {@code @DataDeleted} field does not turn it into a soft delete).
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * DataAccess.deleteHardById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return Number of entities deleted (typically 0 or 1)
	 * @throws Exception if deletion fails
	 */
	public static <ID_TYPE> long deleteHardById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteHardById(clazz, id, options);
		}
	}

	/**
	 * Physically deletes entities matching conditions (a {@code @DataDeleted} field does not turn it into a soft
	 * delete).
	 *
	 * @param clazz   Entity class
	 * @param options Query options (Condition required)
	 * @return Number of entities deleted
	 * @throws Exception if deletion fails
	 * @see #deleteHardById(Class, Object, QueryOption...)
	 */
	public static long deleteHard(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteHard(clazz, options);
		}
	}

	/**
	 * Soft deletes an entity by its ID (marks as deleted).
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * DataAccess.deleteSoftById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return Number of entities soft deleted (typically 0 or 1)
	 * @throws Exception if soft deletion fails
	 */
	public static <ID_TYPE> long deleteSoftById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteSoftById(clazz, id, options);
		}
	}

	/**
	 * Soft deletes entities matching conditions (marks as deleted).
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * @param clazz   Entity class
	 * @param options Query options (Condition required)
	 * @return Number of entities soft deleted
	 * @throws Exception if soft deletion fails
	 * @see #deleteSoftById(Class, Object, QueryOption...)
	 */
	public static long deleteSoft(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.deleteSoft(clazz, options);
		}
	}

	// ========================================================================
	// Restore methods
	// ========================================================================

	/**
	 * Clears the soft delete flag of the entity of this identifier, when the call matches it: without option it
	 * does not match a soft-deleted entity, see below.
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * This overload takes no option, so it cannot carry {@code AccessDeletedItems}: its filter keeps the
	 * entities whose deleted field is false. A soft-deleted entity is not matched and stays deleted. An entity
	 * that is not deleted is matched: its flag is written to false again and its {@code @UpdateTimestamp} date
	 * is refreshed. To restore a soft-deleted entity, use {@link #restoreById(Class, Object, QueryOption...)}
	 * with {@code new AccessDeletedItems()}:
	 * </p>
	 * <pre>
	 * // Soft delete
	 * DataAccess.deleteSoftById(Task.class, taskId);
	 *
	 * // Later, restore: the option is needed to match the soft-deleted task
	 * long restored = DataAccess.restoreById(Task.class, taskId, new AccessDeletedItems());
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @return the number of documents modified: 0 for a soft-deleted entity, see above
	 * @throws DataAccessException if entity has no deleted field or restore fails
	 * @throws IOException         if I/O error occurs
	 */
	public static <ID_TYPE> long restoreById(final Class<?> clazz, final ID_TYPE id)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.restoreById(clazz, id);
		}
	}

	/**
	 * Restores (un-deletes) a soft-deleted entity by its ID with options. A soft-deleted entity matches only when
	 * the options contain {@code AccessDeletedItems}; without it the call restores nothing and returns 0.
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return Number of entities restored (typically 0 or 1)
	 * @throws DataAccessException if entity has no deleted field or restore fails
	 * @throws IOException         if I/O error occurs
	 * @see #restoreById(Class, Object)
	 */
	public static <ID_TYPE> long restoreById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.restoreById(clazz, id, options);
		}
	}

	/**
	 * Restores (un-deletes) soft-deleted entities matching conditions.
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * A document whose deleted field is {@code true} matches only when the call carries
	 * {@code AccessDeletedItems}. Without it, no soft-deleted document matches: the call restores nothing and
	 * returns 0.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Restore the soft-deleted tasks of a given title
	 * DataAccess.restore(Task.class,
	 *     new Condition(Filters.eq("title", "Buy milk")),
	 *     new AccessDeletedItems());
	 * </pre>
	 *
	 * @param clazz   Entity class
	 * @param options Query options (Condition required)
	 * @return Number of entities restored
	 * @throws DataAccessException if entity has no deleted field or restore fails
	 * @throws IOException         if I/O error occurs
	 */
	public static long restore(final Class<?> clazz, final QueryOption... options)
			throws DataAccessException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.restore(clazz, options);
		}
	}

	// ========================================================================
	// Collection management methods
	// ========================================================================

	/**
	 * Drops (permanently deletes) the entire collection for an entity class.
	 *
	 * <p>
	 * <strong>Warning:</strong> This permanently removes the collection and all documents.
	 * Cannot be undone.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Drop the entire tasks collection
	 * DataAccess.drop(Task.class);
	 * </pre>
	 *
	 * @param clazz   Entity class whose collection will be dropped
	 * @param options Optional query options (e.g., table name override)
	 * @throws Exception if drop fails
	 */
	public static void drop(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			db.drop(clazz, options);
		}
	}

	/**
	 * Deletes all documents from the collection (keeps collection structure).
	 *
	 * <p>
	 * Unlike {@link #drop(Class, QueryOption...)}, this preserves the collection
	 * structure (indexes, schema) but removes all documents.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Remove all the task documents but keep the collection
	 * DataAccess.cleanAll(Task.class);
	 * </pre>
	 *
	 * @param clazz   Entity class whose documents will be deleted
	 * @param options Optional query options (e.g., table name override)
	 * @throws Exception if clean fails
	 */
	public static void cleanAll(final Class<?> clazz, final QueryOption... options) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			db.cleanAll(clazz, options);
		}
	}

	// ========================================================================
	// BSON Direct Access API
	// ========================================================================

	/**
	 * Inserts a BSON Document directly into the specified collection.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and inserts a raw BSON document
	 * directly into MongoDB. Useful for advanced use cases requiring full document control.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * Document doc = new Document()
	 *     .append("_id", new ObjectId())
	 *     .append("name", "John Doe")
	 *     .append("email", "john@example.com")
	 *     .append("age", 30);
	 *
	 * ObjectId insertedId = DataAccess.insertBsonDocument("users", doc);
	 * </pre>
	 *
	 * @param collectionName Name of the collection to insert into
	 * @param document       BSON Document to insert
	 * @return The ObjectId of the inserted document (auto-generated if not provided)
	 * @throws DataAccessException          if insertion fails
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 */
	public static ObjectId insertBsonDocument(final String collectionName, final Document document)
			throws DataAccessException, InternalServerErrorException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.insertBsonDocument(collectionName, document);
		}
	}

	/**
	 * Retrieves a single BSON Document from the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and returns a raw BSON document
	 * directly from MongoDB. Useful for advanced use cases requiring full document control.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 *
	 * // Get document by ID
	 * Document userDoc = DataAccess.getBsonDocument("users",
	 *     new Condition(Filters.eq("_id", userId)));
	 *
	 * if (userDoc != null) {
	 *     String name = userDoc.getString("name");
	 *     Integer age = userDoc.getInteger("age");
	 * }
	 * </pre>
	 *
	 * @param collectionName Name of the collection to query
	 * @param options        Query options including conditions for filtering
	 * @return The first matching BSON Document or null if not found
	 * @throws DataAccessException          if retrieval fails
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 */
	@Nullable
	public static Document getBsonDocument(final String collectionName, final QueryOption... options)
			throws DataAccessException, InternalServerErrorException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getBsonDocument(collectionName, options);
		}
	}

	/**
	 * Retrieves multiple BSON Documents from the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and returns raw BSON documents
	 * directly from MongoDB. Useful for advanced use cases requiring full document control.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Get the 10 first users older than 25, ordered by name
	 * List&lt;Document&gt; users = DataAccess.getBsonDocuments("users",
	 *     new Condition(Filters.gt("age", 25)),
	 *     new OrderBy(new OrderItem("name", OrderItem.Order.ASC)),
	 *     new Limit(10));
	 * </pre>
	 *
	 * @param collectionName Name of the collection to query
	 * @param options        Query options including conditions, ordering, limits
	 * @return List of matching BSON Documents (empty list if none found)
	 * @throws DataAccessException          if retrieval fails
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 */
	public static List<Document> getBsonDocuments(final String collectionName, final QueryOption... options)
			throws DataAccessException, InternalServerErrorException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.getBsonDocuments(collectionName, options);
		}
	}

	/**
	 * Updates BSON Documents in the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and updates documents directly
	 * using MongoDB update operators. Useful for advanced use cases requiring full control
	 * over the update operation.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Update all users older than 25, set status to "active"
	 * Document updateOps = new Document("$set",
	 *     new Document("status", "active")
	 *         .append("updatedAt", new Date()));
	 *
	 * long count = DataAccess.updateBsonDocuments("users", updateOps,
	 *     new Condition(Filters.gt("age", 25)));
	 * </pre>
	 *
	 * <p>
	 * <strong>Note:</strong> The updateDocument should use MongoDB update operators like
	 * $set, $inc, $push, etc. See MongoDB documentation for available operators.
	 * </p>
	 *
	 * @param collectionName Name of the collection to update
	 * @param updateDocument BSON Document containing MongoDB update operators
	 * @param options        Query options including conditions for filtering which documents to update; a
	 *                       missing or empty condition is refused unless
	 *                       {@link org.atriasoft.archidata.dataAccess.options.AllowMassOperation} is given
	 * @return Number of documents modified
	 * @throws DataAccessException          if update fails, or if the condition is missing or empty
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 */
	public static long updateBsonDocuments(
			final String collectionName,
			final Document updateDocument,
			final QueryOption... options) throws DataAccessException, InternalServerErrorException, IOException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			return db.updateBsonDocuments(collectionName, updateDocument, options);
		}
	}

	// ========================================================================
	// Transaction support
	// ========================================================================

	/**
	 * Functional interface for actions to execute within a transaction.
	 *
	 * @see #inTransaction(TransactionalAction)
	 */
	@FunctionalInterface
	public interface TransactionalAction {
		/**
		 * Executes the transactional action.
		 *
		 * @param db the database access instance (within a transaction)
		 * @throws Exception if the action fails
		 */
		void execute(DBAccessMongo db) throws Exception;
	}

	/**
	 * Executes a block of operations within a single MongoDB transaction.
	 *
	 * <p>
	 * This method automatically manages the connection, transaction lifecycle,
	 * and commit/abort. If the action completes successfully, the transaction
	 * is committed. If an exception is thrown, the transaction is automatically aborted.
	 * </p>
	 *
	 * <p>
	 * <strong>Important:</strong> MongoDB transactions require a replica set.
	 * Standalone MongoDB instances do not support transactions.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * DataAccess.inTransaction(db -&gt; {
	 *     Task parent = db.insert(new Task("Parent", ""));
	 *     Task child = new Task("Child of " + parent.getId(), "");
	 *     db.insert(child);
	 * });
	 * // Both inserts are committed atomically, or both are rolled back on error.
	 * </pre>
	 *
	 * @param action The transactional action to execute
	 * @throws Exception if the action or transaction fails
	 */
	public static void inTransaction(final TransactionalAction action) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo db = ctx.get();
			try (TransactionContext tx = new TransactionContext(db)) {
				action.execute(db);
				tx.commit();
			}
		}
	}

}
