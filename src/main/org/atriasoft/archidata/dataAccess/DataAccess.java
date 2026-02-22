package org.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

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
 * Example usage with ObjectId:
 * </p>
 * <pre>
 * // Insert a new user
 * User user = new User();
 * user.name = "John Doe";
 * User inserted = DataAccess.insert(user);
 * ObjectId userId = inserted.id;
 *
 * // Retrieve by ID
 * User found = DataAccess.getById(User.class, userId);
 *
 * // Update
 * found.name = "Jane Doe";
 * DataAccess.updateById(found, userId);
 *
 * // Delete
 * DataAccess.deleteById(User.class, userId);
 * </pre>
 */
public class DataAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);

	private DataAccess() {
		// Utility class
	}

	// ========================================================================
	// Database management methods
	// ========================================================================

	/**
	 * Lists all collection names in the database.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * List&lt;String&gt; collections = DataAccess.listCollections("mydb");
	 * for (String name : collections) {
	 *     System.out.println("Collection: " + name);
	 * }
	 * </pre>
	 *
	 * @param name    Database name
	 * @param options Optional query options
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
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * List&lt;User&gt; users = Arrays.asList(user1, user2, user3);
	 * List&lt;User&gt; inserted = DataAccess.insertMultiple(users);
	 * ObjectId firstId = inserted.get(0).id;
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
	 * The entity is inserted and returned with its auto-generated ID and timestamps.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * User user = new User();
	 * user.name = "John Doe";
	 * user.email = "john@example.com";
	 *
	 * User inserted = DataAccess.insert(user);
	 * ObjectId userId = inserted.id; // Auto-generated
	 * Timestamp created = inserted.createdAt; // Auto-populated
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
	 * @return Query condition for the ID
	 * @throws DataAccessException if operation fails
	 * @throws IOException         if I/O error occurs
	 */
	public static <ID_TYPE> QueryCondition getTableIdCondition(
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
	 * <strong>Field filtering:</strong> By default, ALL fields are updated.
	 * Use {@code FilterValue} to update only specific fields.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * User updateData = new User();
	 * updateData.email = "newemail@example.com";
	 *
	 * // Update all fields
	 * DataAccess.updateById(updateData, userId);
	 *
	 * // Update only specific fields
	 * DataAccess.updateById(updateData, userId, new FilterValue(List.of("email")));
	 * </pre>
	 *
	 * @param <T>       The type of entity
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param data      Entity data to update
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @param option    Optional query options (e.g., FilterValue)
	 * @return Number of entities updated (typically 0 or 1)
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
	 * User updateData = new User();
	 * updateData.status = "inactive";
	 *
	 * ObjectId authorId = new ObjectId("507f1f77bcf86cd799439011");
	 * long count = DataAccess.update(updateData,
	 *     new Condition(new QueryCondition("authorId", "=", authorId)),
	 *     new FilterValue(List.of("status")));
	 * </pre>
	 *
	 * @param <T>     The type of entity
	 * @param data    Entity data with update values
	 * @param options Query options (FilterValue and Condition required)
	 * @return Number of entities updated
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
	 * @return Number of entities updated
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
	 * // Find user by email
	 * User user = DataAccess.get(User.class,
	 *     new Condition(new QueryCondition("email", "=", "john@example.com")));
	 *
	 * // Find document by author
	 * ObjectId authorId = new ObjectId("507f1f77bcf86cd799439011");
	 * Document doc = DataAccess.get(Document.class,
	 *     new Condition(new QueryCondition("authorId", "=", authorId)));
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
	 * // Get all active users
	 * List&lt;User&gt; users = DataAccess.gets(User.class,
	 *     new Condition(new QueryCondition("status", "=", "active")));
	 *
	 * // Get documents by author with limit
	 * ObjectId authorId = new ObjectId("507f1f77bcf86cd799439011");
	 * List&lt;Document&gt; docs = DataAccess.gets(Document.class,
	 *     new Condition(new QueryCondition("authorId", "=", authorId)),
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
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * if (DataAccess.existsById(User.class, userId)) {
	 *     System.out.println("User exists");
	 * }
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
	 * // Count active users
	 * long count = DataAccess.count(User.class,
	 *     new Condition(new QueryCondition("status", "=", "active")));
	 *
	 * // Count documents by author
	 * ObjectId authorId = new ObjectId("507f1f77bcf86cd799439011");
	 * long docCount = DataAccess.count(Document.class,
	 *     new Condition(new QueryCondition("authorId", "=", authorId)));
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
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * User user = DataAccess.getById(User.class, userId);
	 * if (user != null) {
	 *     System.out.println("Found: " + user.name);
	 * }
	 *
	 * // Get with all columns (including non-readable)
	 * User fullUser = DataAccess.getById(User.class, userId, new ReadAllColumn());
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
	 * List&lt;User&gt; allUsers = DataAccess.getAll(User.class);
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
	 * <strong>Automatic routing:</strong> If entity is {@code @SoftDeleted}, performs soft delete.
	 * Otherwise performs hard delete.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * long deleted = DataAccess.deleteById(User.class, userId);
	 * if (deleted &gt; 0) {
	 *     System.out.println("User deleted");
	 * }
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
	 * <strong>Automatic routing:</strong> If entity is {@code @SoftDeleted}, performs soft delete.
	 * Otherwise performs hard delete.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * // Delete all inactive users
	 * long count = DataAccess.delete(User.class,
	 *     new Condition(new QueryCondition("status", "=", "inactive")));
	 *
	 * // Delete documents by author
	 * ObjectId authorId = new ObjectId("507f1f77bcf86cd799439011");
	 * DataAccess.delete(Document.class,
	 *     new Condition(new QueryCondition("authorId", "=", authorId)));
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
	 * Physically deletes an entity by its ID (ignores {@code @SoftDeleted}).
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * DataAccess.deleteHardById(User.class, userId);
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
	 * Physically deletes entities matching conditions (ignores {@code @SoftDeleted}).
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
	 * <strong>Requirement:</strong> Entity must be annotated with {@code @SoftDeleted}.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * DataAccess.deleteSoftById(User.class, userId);
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
	 * <strong>Requirement:</strong> Entity must be annotated with {@code @SoftDeleted}.
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
	 * Restores (un-deletes) a soft-deleted entity by its ID.
	 *
	 * <p>
	 * <strong>Requirement:</strong> Entity must be annotated with {@code @SoftDeleted}.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 *
	 * // Soft delete
	 * DataAccess.deleteSoftById(User.class, userId);
	 *
	 * // Later, restore
	 * long restored = DataAccess.restoreById(User.class, userId);
	 * if (restored &gt; 0) {
	 *     System.out.println("User restored");
	 * }
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     Entity class
	 * @param id        Unique identifier (e.g., ObjectId)
	 * @return Number of entities restored (typically 0 or 1)
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
	 * Restores (un-deletes) a soft-deleted entity by its ID with options.
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
	 * <strong>Requirement:</strong> Entity must be annotated with {@code @SoftDeleted}.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Restore all users deleted in last 24 hours
	 * DataAccess.restore(User.class,
	 *     new Condition(new QueryCondition("deletedAt", "&gt;", yesterday)));
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
	 * // Drop entire users collection
	 * DataAccess.drop(User.class);
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
	 * // Remove all user documents but keep collection
	 * DataAccess.cleanAll(User.class);
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
	 * System.out.println("Inserted document with ID: " + insertedId);
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
	 *     new Condition(new QueryCondition("_id", "=", userId)));
	 *
	 * if (userDoc != null) {
	 *     String name = userDoc.getString("name");
	 *     Integer age = userDoc.getInteger("age");
	 *     System.out.println("User: " + name + ", age: " + age);
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
	 * // Get all users older than 25, ordered by name, limited to 10
	 * List&lt;Document&gt; users = DataAccess.getBsonDocuments("users",
	 *     new Condition(new QueryCondition("age", "&gt;", 25)),
	 *     new OrderBy("name", true),
	 *     new Limit(10));
	 *
	 * for (Document user : users) {
	 *     System.out.println(user.toJson());
	 * }
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
	 *     new Condition(new QueryCondition("age", "&gt;", 25)));
	 *
	 * System.out.println("Updated " + count + " documents");
	 * </pre>
	 *
	 * <p>
	 * <strong>Note:</strong> The updateDocument should use MongoDB update operators like
	 * $set, $inc, $push, etc. See MongoDB documentation for available operators.
	 * </p>
	 *
	 * @param collectionName Name of the collection to update
	 * @param updateDocument BSON Document containing MongoDB update operators
	 * @param options        Query options including conditions for filtering which documents to update
	 * @return Number of documents modified
	 * @throws DataAccessException          if update fails
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

}
