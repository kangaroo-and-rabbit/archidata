package org.atriasoft.archidata.dataAccess;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.addOn.AddOnManyToManyDoc;
import org.atriasoft.archidata.dataAccess.addOn.AddOnManyToOneDoc;
import org.atriasoft.archidata.dataAccess.addOn.AddOnOneToManyDoc;
import org.atriasoft.archidata.dataAccess.addOn.DataAccessAddOn;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbFieldAction;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules;
import org.atriasoft.archidata.dataAccess.model.ReadFieldSelector;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.atriasoft.archidata.dataAccess.model.codec.MongoFieldCodec;
import org.atriasoft.archidata.dataAccess.model.codec.MongoTypeReader;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.AllowMassOperation;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.DirectData;
import org.atriasoft.archidata.dataAccess.options.DirectPrimaryKey;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ForceHardDelete;
import org.atriasoft.archidata.dataAccess.options.ForceReadOnlyField;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.statistic.QuerySignature.Operation;
import org.atriasoft.archidata.dataAccess.statistic.QueryStatistics;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.dataAccess.options.Skip;
import org.atriasoft.archidata.dataAccess.options.TransmitKey;
import org.atriasoft.archidata.db.BsonCodecs;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIo;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.UuidUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import jakarta.ws.rs.InternalServerErrorException;

/**
 * Entity-level access to MongoDB: insert, read, update, delete (soft or hard), count, raw BSON access and
 * transactions, driven by the model of the entity classes ({@link DbClassModel}).
 *
 * <p>
 * An instance owns one connection ({@link DbIoMongo}) opened by its constructor and released by
 * {@link #close()}, which aborts a transaction still active. It is not meant to be shared between threads:
 * {@link org.atriasoft.archidata.checker.DataAccessConnectionContext} keeps one per thread and the static
 * facade {@link DataAccess} goes through it. A transaction ({@link #startTransaction()}, or
 * {@link TransactionContext}) covers every operation made on the same instance until it is committed or aborted.
 * </p>
 *
 * <p>
 * The examples of this class use {@code Task}, the entity of {@code sample/archidata/basic}: a {@code Long} id,
 * {@code title}, {@code description} and {@code done} fields, and a soft-delete flag.
 * </p>
 */
public final class DBAccessMongo implements Closeable {
	/** Collection holding the sequences of {@link #getNextSequenceLongValue(String, String)}. */
	public static final String COLLECTION_COUNTERS = "counters";
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccessMongo.class);
	/** Maximum number of rounds of {@link #runLazyActions(List)} before it drops the actions still queued. */
	private static final int MAX_LAZY_ROUNDS = 500;

	/**
	 * Tracks MongoDB operation statistics for monitoring and debugging purposes.
	 */
	public static class MongoDbStatistic {
		/** Creates a new MongoDbStatistic with all counters initialized to zero. */
		public MongoDbStatistic() {}

		/** Number of countDocuments operations executed. */
		public long countCountDocuments = 0L;
		/** Number of deleteMany operations executed. */
		public long countDeleteMany = 0L;
		/** Number of drop operations executed. */
		public long countDrop = 0L;
		/** Number of find operations executed. */
		public long countFind = 0L;
		/** Number of findOneAndUpdate operations executed. */
		public long countFindOneAndUpdate = 0L;
		/** Number of insertOne operations executed. */
		public long countInsertOne = 0L;
		/** Number of updateMany operations executed. */
		public long countUpdateMany = 0L;
		/** Number of runCommand operations executed. */
		public long countRunCommand = 0L;

		/** Logs all accumulated statistics at INFO level. */
		public void display() {
			LOGGER.info("""
					statistic on access on DB:
					    - insertOne        = {}
					    - updateMany       = {}
					    - find             = {}
					    - findOneAndUpdate = {}
					    - countDocuments   = {}
					    - deleteMany       = {}
					    - drop             = {}
					    - runCommand       = {}
					""", //
					String.format("%10d", this.countInsertOne), //
					String.format("%10d", this.countUpdateMany), //
					String.format("%10d", this.countFind), //
					String.format("%10d", this.countFindOneAndUpdate), //
					String.format("%10d", this.countCountDocuments), //
					String.format("%10d", this.countDeleteMany), //
					String.format("%10d", this.countDrop), //
					String.format("%10d", this.countRunCommand)); //
		}
	}

	/** Global statistics for all MongoDB operations performed through this class. */
	public static final MongoDbStatistic statistic = new MongoDbStatistic();

	/**
	 * {@code collection.findOneAndUpdate(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @param update the update
	 * @param options the options of the driver call
	 * @return the document the driver returns
	 */
	private Document findOneAndUpdateIn(
			final MongoCollection<Document> collection,
			final Bson filter,
			final Bson update,
			final FindOneAndUpdateOptions options) {
		return this.session != null ? collection.findOneAndUpdate(this.session, filter, update, options)
				: collection.findOneAndUpdate(filter, update, options);
	}

	/**
	 * {@code collection.insertOne(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param document the document to insert
	 * @return the result of the driver
	 */
	private InsertOneResult insertOneIn(final MongoCollection<Document> collection, final Document document) {
		return this.session != null ? collection.insertOne(this.session, document) : collection.insertOne(document);
	}

	/**
	 * {@code collection.updateMany(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @param update the update
	 * @return the result of the driver
	 */
	private UpdateResult updateManyIn(
			final MongoCollection<Document> collection,
			final Bson filter,
			final Bson update) {
		return this.session != null ? collection.updateMany(this.session, filter, update)
				: collection.updateMany(filter, update);
	}

	/**
	 * {@code collection.deleteMany(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the result of the driver
	 */
	private DeleteResult deleteManyIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.deleteMany(this.session, filter) : collection.deleteMany(filter);
	}

	/**
	 * {@code collection.find(filter)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the cursor of the driver
	 */
	private FindIterable<Document> findIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.find(this.session, filter) : collection.find(filter);
	}

	/**
	 * {@code collection.find()} without filter, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @return the cursor of the driver
	 */
	private FindIterable<Document> findAllIn(final MongoCollection<Document> collection) {
		return this.session != null ? collection.find(this.session) : collection.find();
	}

	/**
	 * {@code collection.countDocuments(filter)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the number of documents
	 */
	private long countIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.countDocuments(this.session, filter)
				: collection.countDocuments(filter);
	}

	/**
	 * {@code collection.countDocuments()} without filter, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @return the number of documents
	 */
	private long countAllIn(final MongoCollection<Document> collection) {
		return this.session != null ? collection.countDocuments(this.session) : collection.countDocuments();
	}

	/**
	 * Runs the lazy actions collected by a read, an insert, an update or a delete, then the actions those
	 * actions queue in turn, round after round.
	 *
	 * <p>The loop stops after {@link #MAX_LAZY_ROUNDS} rounds: the actions still queued then are dropped
	 * without any log.
	 *
	 * @param actions the actions of the first round (not modified)
	 * @throws Exception when an action fails
	 */
	private static void runLazyActions(final List<LazyGetter> actions) throws Exception {
		List<LazyGetter> actionsAsync = actions;
		for (int round = 0; round < MAX_LAZY_ROUNDS && actionsAsync.size() != 0; round++) {
			final List<LazyGetter> actionsAsyncNew = new ArrayList<>();
			for (final LazyGetter action : actionsAsync) {
				action.doRequest(actionsAsyncNew);
			}
			actionsAsync = actionsAsyncNew;
		}
	}

	/**
	 * Returns the soft-delete field a query implicitly filters on, for {@link QueryStatistics}.
	 *
	 * @param options the query options, checked for {@link AccessDeletedItems}
	 * @param deletedFieldName the soft-delete field of the model, or {@code null} if it has none
	 * @return the soft-delete field actually excluded, or {@code null} when the query does not
	 *         exclude soft-deleted documents
	 */
	private static String statisticSoftDeleteField(final QueryOptions options, final String deletedFieldName) {
		if (deletedFieldName == null) {
			return null;
		}
		if (options != null && options.exist(AccessDeletedItems.class)) {
			return null;
		}
		return deletedFieldName;
	}

	/**
	 * Builds the sort document of a query, or {@code null} when the query is not sorted.
	 *
	 * @param options the query options holding the {@link OrderBy} entries
	 * @return the sort document ({@code field -> 1|-1}, in order), or {@code null}
	 */
	private static Document generateSortOrNull(final QueryOptions options) {
		final List<OrderBy> orders = options.get(OrderBy.class);
		if (orders.isEmpty()) {
			return null;
		}
		final Document sorts = new Document();
		for (final OrderBy order : orders) {
			order.generateSort(sorts);
		}
		return sorts;
	}

	// by default we manage some add-on that permit to manage non-native model (like
	// json serialization, List of external key as String list...)
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToManyDoc());
		addOn.add(new AddOnManyToOneDoc());
		addOn.add(new AddOnOneToManyDoc());
		// Synchronize add-ons with the DbClassModel cache layer
		DbClassModel.setAddOns(addOn);
	}

	/**
	 * Registers a custom add-on to extend database access functionality.
	 *
	 * <p>
	 * Add-ons provide specialized handling for complex field types (e.g., OneToMany, ManyToOne).
	 * This method allows registration of custom add-ons beyond the default ones.
	 * </p>
	 *
	 * @param addOn The add-on implementation to register
	 */
	public static void addAddOn(final DataAccessAddOn addOn) {
		DBAccessMongo.addOn.add(addOn);
		// Keep DbClassModel in sync with registered add-ons
		DbClassModel.setAddOns(DBAccessMongo.addOn);
	}

	/**
	 * Creates a new DBAccessMongo instance using default database configuration.
	 *
	 * <p>
	 * The default configuration is loaded from environment variables or configuration files.
	 * </p>
	 *
	 * @return A new DBAccessMongo instance
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 * @throws DataAccessException          if database access configuration is invalid
	 */
	public static DBAccessMongo createInterface()
			throws InternalServerErrorException, IOException, DataAccessException {
		return DBAccessMongo.createInterface(DbIoFactory.create());
	}

	/**
	 * Creates a new DBAccessMongo instance using the specified database configuration.
	 *
	 * @param config Custom database configuration
	 * @return A new DBAccessMongo instance
	 * @throws InternalServerErrorException if database connection fails
	 * @throws IOException                  if I/O error occurs
	 */
	public static DBAccessMongo createInterface(final DbConfig config)
			throws InternalServerErrorException, IOException {
		return DBAccessMongo.createInterface(DbIoFactory.create(config));
	}

	/**
	 * Creates a new DBAccessMongo instance using an existing database I/O interface.
	 *
	 * @param io Existing database I/O interface
	 * @return A new DBAccessMongo instance
	 * @throws InternalServerErrorException if database connection fails
	 */
	public static DBAccessMongo createInterface(final DbIo io) throws InternalServerErrorException {
		if (io instanceof final DbIoMongo ioMorphia) {
			try {
				return new DBAccessMongo(ioMorphia);
			} catch (final IOException e) {
				LOGGER.error("Failed to create DB interface: {}", e.getMessage(), e);
				throw new InternalServerErrorException("Fail to create DB interface.");
			}
		}
		throw new InternalServerErrorException("unknown DB interface ... ");
	}

	private final DbIoMongo db;
	private ClientSession session = null;

	/**
	 * Constructs a new DBAccessMongo instance with the specified MongoDB I/O interface.
	 *
	 * <p>
	 * This constructor opens the database connection. Use {@link #close()} to properly
	 * release resources when done.
	 * </p>
	 *
	 * @param db MongoDB I/O interface
	 * @throws IOException if connection fails
	 */
	public DBAccessMongo(final DbIoMongo db) throws IOException {
		this.db = db;
		db.open();
	}

	/**
	 * Closes the database connection and releases associated resources.
	 *
	 * <p>
	 * This method should be called when the DBAccessMongo instance is no longer needed.
	 * Typically used with try-with-resources pattern. If a transaction is still active,
	 * it will be automatically aborted before closing.
	 * </p>
	 *
	 * @throws IOException if closing connection fails
	 */
	@Override
	public void close() throws IOException {
		if (this.session != null) {
			try {
				this.session.abortTransaction();
				LOGGER.warn("Transaction was still active during close() - aborted");
			} catch (final Exception ex) {
				LOGGER.error("Failed to abort transaction during close: {}", ex.getMessage(), ex);
			} finally {
				this.session.close();
				this.session = null;
			}
		}
		this.db.close();
	}

	/**
	 * Returns the underlying MongoDB I/O interface.
	 *
	 * @return The MongoDB I/O interface
	 */
	public DbIoMongo getInterface() {
		return this.db;
	}

	/**
	 * Starts a new MongoDB transaction on this connection.
	 *
	 * <p>
	 * After calling this method, all subsequent CRUD operations on this
	 * DBAccessMongo instance will participate in the transaction until
	 * {@link #commitTransaction()} or {@link #abortTransaction()} is called.
	 * </p>
	 *
	 * <p>
	 * <strong>Important:</strong> MongoDB transactions require a replica set.
	 * Standalone MongoDB instances do not support transactions.
	 * </p>
	 *
	 * @throws DataAccessException if a transaction is already active
	 */
	public void startTransaction() throws DataAccessException {
		if (this.session != null) {
			throw new DataAccessException("A transaction is already active on this connection");
		}
		this.session = this.db.getClient().startSession();
		this.session.startTransaction();
		LOGGER.debug("Transaction started");
	}

	/**
	 * Commits the active transaction.
	 *
	 * @throws DataAccessException if no transaction is active
	 */
	public void commitTransaction() throws DataAccessException {
		if (this.session == null) {
			throw new DataAccessException("No active transaction to commit");
		}
		try {
			this.session.commitTransaction();
			LOGGER.debug("Transaction committed");
		} finally {
			this.session.close();
			this.session = null;
		}
	}

	/**
	 * Aborts the active transaction, rolling back all changes.
	 *
	 * @throws DataAccessException if no transaction is active
	 */
	public void abortTransaction() throws DataAccessException {
		if (this.session == null) {
			throw new DataAccessException("No active transaction to abort");
		}
		try {
			this.session.abortTransaction();
			LOGGER.debug("Transaction aborted");
		} finally {
			this.session.close();
			this.session = null;
		}
	}

	/**
	 * Returns the current ClientSession, or null if no transaction is active.
	 *
	 * <p>
	 * This is used internally by {@link org.atriasoft.archidata.dataAccess.mongo.MongoLinkManager}
	 * and add-ons to pass the session to MongoDB driver calls.
	 * </p>
	 *
	 * @return The active ClientSession, or null
	 */
	public ClientSession getSession() {
		return this.session;
	}

	/**
	 * Returns whether a transaction is currently active.
	 *
	 * @return true if a transaction is active
	 */
	public boolean isTransactionActive() {
		return this.session != null;
	}

	/**
	 * Generates a query condition for matching an entity by its primary key ID.
	 *
	 * <p>
	 * This method automatically detects the ID field type and creates the appropriate
	 * condition for querying by ID.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 * Bson condition = db.getTableIdCondition(User.class, userId, options);
	 * // condition will be: "_id" = ObjectId("507f1f77bcf86cd799439011")
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     The entity class
	 * @param idKey     The ID value to match
	 * @param options   Query options
	 * @return A Bson filter for matching the entity by ID
	 * @throws DataAccessException if the class has no @Id field
	 */
	public <ID_TYPE> Bson getTableIdCondition(final Class<?> clazz, final ID_TYPE idKey, final QueryOptions options)
			throws DataAccessException {
		final DbClassModel model;
		try {
			model = DbClassModel.of(clazz);
		} catch (final IntrospectionException e) {
			throw new DataAccessException("Failed to introspect class: " + clazz.getSimpleName(), e);
		}
		final DbPropertyDescriptor pkDesc = model.getPrimaryKey();
		if (pkDesc == null) {
			throw new DataAccessException(
					"The class have no annotation @Id ==> can not determine the default type searching");
		}
		if (idKey == null) {
			throw new DataAccessException("Try to identify the ID type and object was null.");
		}
		final FieldName fieldName = pkDesc.getFieldName(options);
		Class<?> typeClass = pkDesc.getProperty().getType();
		if (typeClass == Object.class) {
			final List<OptionSpecifyType> specificTypes = options.get(OptionSpecifyType.class);
			for (final OptionSpecifyType specify : specificTypes) {
				if (specify.name.equals(fieldName.inStruct())) {
					typeClass = specify.clazz;
					LOGGER.trace("Detect overwrite of typing ... '{}' => '{}'", typeClass.getCanonicalName(),
							specify.clazz.getCanonicalName());
					break;
				}
			}
		}
		if (idKey.getClass() != typeClass) {
			if (idKey.getClass() == Condition.class) {
				throw new DataAccessException(
						"Try to identify the ID type on a condition 'close' internal API error use xxxWhere(...) instead.");
			}
			throw new DataAccessException("Request update with the wrong type ...");
		}
		return Filters.eq(fieldName.inTable(), idKey);
	}

	/**
	 * Inserts multiple entities into the database.
	 *
	 * <p>
	 * Each entity is inserted individually and returned with its generated ID.
	 * The order of insertion is maintained in the returned list.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * List&lt;Task&gt; inserted = db.insertMultiple(List.of(task1, task2, task3));
	 * Long firstId = inserted.get(0).getId();
	 * </pre>
	 *
	 * @param <T>     The type of the entities
	 * @param data    List of entities to insert
	 * @param options Optional query options (e.g., table selection)
	 * @return List of inserted entities with generated IDs
	 * @throws Exception if insertion operation fails
	 */
	public <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		final List<T> out = new ArrayList<>();
		for (final T elem : data) {
			final T tmp = insert(elem, options);
			out.add(tmp);
		}
		return out;
	}

	/**
	 * Inserts a single entity into the database.
	 *
	 * <p>
	 * The entity is inserted and then read back by its generated primary key, which is generated
	 * according to the type of the key field (ObjectId, UUID, or Long). The read-back follows the usual read
	 * rules: a {@code @DataNotRead} field such as {@code createdAt} is {@code null} in the result unless a
	 * {@link ReadAllColumn} option is passed.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Task inserted = db.insert(new Task("Buy milk", "2 liters"));
	 * Long taskId = inserted.getId(); // generated
	 *
	 * // createdAt is read back only with ReadAllColumn
	 * Task withTimestamps = db.insert(new Task("Buy bread", ""), new ReadAllColumn());
	 * Date created = withTimestamps.getCreatedAt();
	 * </pre>
	 *
	 * @param <T>    The type of the entity
	 * @param data   Entity to insert
	 * @param option Optional query options (e.g., table selection, field filtering)
	 * @return The inserted entity read back, with its generated ID
	 * @throws Exception if insertion operation fails
	 */
	@SuppressWarnings("unchecked")
	public <T> T insert(final T data, final QueryOption... option) throws Exception {
		final Object insertedId = insertPrimaryKey(data, option);
		final QueryOptions options = new QueryOptions(option);
		final QueryOptions injectedOptions = new QueryOptions();
		final List<OverrideTableName> override = options.get(OverrideTableName.class);
		if (override.size() != 0) {
			injectedOptions.add(override.get(0));
		}
		forwardOptions(options, injectedOptions, OptionSpecifyType.class);
		forwardOptions(options, injectedOptions, ReadAllColumn.class);
		forwardOptions(options, injectedOptions, AccessDeletedItems.class);
		return (T) getById(data.getClass(), insertedId, injectedOptions.getAllArray());
	}

	/**
	 * Updates an entity identified by its ID with the provided data.
	 *
	 * <p>
	 * <strong>Field filtering behavior:</strong> By default, every editable field of the data object is
	 * updated ({@link FilterValue#getEditableFieldsNames(Class)}), a {@code null} value included. The protected
	 * fields ({@code @DataNotRead}, {@code @DataEncrypt}) are never part of this default: a standard read leaves
	 * them {@code null}, so a read-then-update would erase them. They are written only when a {@link FilterValue}
	 * names them explicitly. To update only specific fields, use the {@link FilterValue} option.
	 * </p>
	 *
	 * <p>
	 * <strong>Scope:</strong> a {@link Condition} passed in the options is ANDed with the id (for instance an owner
	 * filter). The relation side effects of the update (cascade on {@code @OneToManyDoc} children, links of
	 * {@code @ManyToManyDoc} / {@code @ManyToOneDoc}) follow it: when the condition matches nothing, or the
	 * document is soft-deleted (without {@link AccessDeletedItems}), the update returns 0 and no remote document
	 * changes.
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
	 * db.updateById(update, taskId);
	 *
	 * // Write only the title and the done flag
	 * db.updateById(update, taskId, new FilterValue("title", "done"));
	 * </pre>
	 *
	 * @param <T>       The type of the entity
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param data      The entity data to update
	 * @param id        The unique identifier of the entity (e.g., ObjectId)
	 * @param option    Optional query options (e.g., FilterValue, table selection)
	 * @return Number of documents modified (0 or 1): the date of an {@code @UpdateTimestamp} field is always
	 *         written (unless {@code DirectData}), so a matched document counts 1; without such a field, a
	 *         matched document whose values do not change counts 0
	 * @throws Exception if update operation fails
	 */
	public <T, ID_TYPE> long updateById(final T data, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		// Ensure FilterValue is present - if not provided, update the editable fields (protected ones excluded)
		if (!options.exist(FilterValue.class)) {
			options.add(FilterValue.getEditableFieldsNames(data.getClass()));
		}
		options.add(new Condition(getTableIdCondition(data.getClass(), id, options)));
		options.add(new TransmitKey(id));
		return update(data, options);
	}

	/**
	 * Updates entities matching the specified conditions.
	 *
	 * <p>
	 * <strong>Required option:</strong> You MUST provide a {@link FilterValue} option to specify which
	 * fields should be updated. This prevents accidental full-row updates.
	 * </p>
	 *
	 * <p>
	 * <strong>Condition specification:</strong> Use {@link Condition} option to specify which entities
	 * to update. Without conditions, the operation will fail with an exception.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Task update = new Task();
	 * update.setDone(true);
	 *
	 * // Mark done every task with this title
	 * long count = db.update(update,
	 *     new Condition(Filters.eq("title", "Buy milk")),
	 *     new FilterValue("done"));
	 * </pre>
	 *
	 * @param <T>    The type of the entity
	 * @param data   The entity data containing the update values
	 * @param option Query options including FilterValue (required) and Condition
	 * @return Number of documents modified: the date of an {@code @UpdateTimestamp} field is always written
	 *         (unless {@code DirectData}), so a matched document counts 1; without such a field, a matched
	 *         document whose values do not change counts 0
	 * @throws Exception if update operation fails or FilterValue is missing
	 */
	public <T> long update(final T data, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return update(data, options);
	}

	/**
	 * Retrieves a single entity matching the specified conditions (QueryOptions variant).
	 *
	 * <p>
	 * If multiple entities match the conditions, only the first one is returned.
	 * If no entity matches, returns null.
	 * </p>
	 *
	 * @param <T>     The type of the entity
	 * @param clazz   The class of the entity
	 * @param options Query options including conditions, filters, etc.
	 * @return The first matching entity or null if none found
	 * @throws Exception if retrieval operation fails
	 */
	public <T> T get(final Class<T> clazz, final QueryOptions options) throws Exception {
		options.add(new Limit(1));
		final List<T> values = gets(clazz, options);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}

	/**
	 * Retrieves a single entity matching the specified conditions (internal raw version).
	 *
	 * @param clazz   The class of the entity
	 * @param options Query options
	 * @return The first matching entity as Object or null
	 * @throws Exception if retrieval operation fails
	 */
	public Object getRaw(final Class<?> clazz, final QueryOptions options) throws Exception {
		options.add(new Limit(1));
		final List<Object> values = getsRaw(clazz, options);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}

	/**
	 * Retrieves a single entity matching the specified conditions.
	 *
	 * <p>
	 * If multiple entities match the conditions, only the first one is returned.
	 * If no entity matches, returns null.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Find a task by title
	 * Task task = db.get(Task.class,
	 *     new Condition(Filters.eq("title", "Buy milk")));
	 * </pre>
	 *
	 * @param <T>    The type of the entity
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions, filters, etc.
	 * @return The first matching entity or null if none found
	 * @throws Exception if retrieval operation fails
	 */
	public <T> T get(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return get(clazz, options);
	}

	/**
	 * Retrieves a single entity matching the specified conditions (internal raw version).
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options
	 * @return The first matching entity as Object or null
	 * @throws Exception if retrieval operation fails
	 */
	public Object getRaw(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getRaw(clazz, options);
	}

	/**
	 * Retrieves all entities of the specified type matching the conditions.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Get all the open tasks
	 * List&lt;Task&gt; tasks = db.gets(Task.class,
	 *     new Condition(Filters.eq("done", false)));
	 *
	 * // Get the 10 newest tasks
	 * List&lt;Task&gt; newest = db.gets(Task.class,
	 *     new Limit(10),
	 *     OrderBy.desc(Task::getCreatedAt));
	 * </pre>
	 *
	 * @param <T>    The type of the entity
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions, filters, limits, etc.
	 * @return List of matching entities (empty list if none found)
	 * @throws Exception if retrieval operation fails
	 */
	public <T> List<T> gets(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return gets(clazz, options);
	}

	/**
	 * Retrieves all entities of the specified type matching the conditions (internal raw version).
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options
	 * @return List of matching entities as Objects
	 * @throws Exception if retrieval operation fails
	 */
	public List<Object> getsRaw(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getsRaw(clazz, options);
	}

	/**
	 * Retrieves all entities of the specified type matching the conditions (QueryOptions variant).
	 *
	 * @param <T>     The type of the entity
	 * @param clazz   The class of the entity
	 * @param options Query options including conditions, filters, limits, etc.
	 * @return List of matching entities (empty list if none found)
	 * @throws DataAccessException if a data access error occurs
	 * @throws IOException         if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> gets(final Class<T> clazz, final QueryOptions options) throws DataAccessException, IOException {
		final List<Object> out = getsRaw(clazz, options);
		return (List<T>) out;
	}

	/**
	 * Retrieves an entity by its unique identifier.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Task task = db.getById(Task.class, taskId); // null when absent
	 *
	 * // Get with all columns (including the @DataNotRead ones such as createdAt)
	 * Task full = db.getById(Task.class, taskId, new ReadAllColumn());
	 * </pre>
	 *
	 * @param <T>       The type of the entity
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier (e.g., ObjectId)
	 * @param option    Optional query options (e.g., table selection, column filters)
	 * @return The entity with the specified ID or null if not found
	 * @throws Exception if retrieval operation fails
	 */
	public <T, ID_TYPE> T getById(final Class<T> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return get(clazz, options.getAllArray());
	}

	/**
	 * Retrieves all entities of the specified type (no conditions).
	 *
	 * <p>
	 * <strong>Warning:</strong> This method retrieves ALL entities from the table and takes no option. To
	 * bound the result, use {@link #gets(Class, QueryOption...)} with {@link Limit} and {@link Skip}.
	 * </p>
	 *
	 * @param <T>   The type of the entity
	 * @param clazz The class of the entity
	 * @return List of all entities (empty list if none found)
	 * @throws Exception if retrieval operation fails
	 */
	public <T> List<T> getAll(final Class<T> clazz) throws Exception {
		return gets(clazz);
	}

	/**
	 * Checks if an entity with the specified ID exists.
	 *
	 * <p>
	 * This method verifies the existence of an entity by its unique identifier.
	 * Returns true if the entity exists, false otherwise.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * boolean exists = db.existsById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier to check (e.g., ObjectId)
	 * @param option    Optional query options (e.g., table selection)
	 * @return true if an entity with this ID exists, false otherwise
	 * @throws Exception if existence check operation fails
	 */
	public <ID_TYPE> boolean existsById(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return count(clazz, options) > 0;
	}

	/**
	 * Counts the number of entities matching the specified conditions.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Count the open tasks
	 * long open = db.count(Task.class,
	 *     new Condition(Filters.eq("done", false)));
	 * </pre>
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions, filters, etc.
	 * @return The number of matching entities
	 * @throws Exception if count operation fails
	 */
	public long count(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return count(clazz, options);
	}

	/**
	 * Deletes an entity by its unique identifier.
	 *
	 * <p>
	 * <strong>Automatic routing behavior:</strong> If the entity class has a field
	 * annotated {@code @DataDeleted}, this method performs a soft delete (marking as deleted).
	 * Otherwise, it performs a hard delete (physical removal).
	 * </p>
	 *
	 * <p>
	 * Use {@link #deleteHardById} or {@link #deleteSoftById} if you need explicit control
	 * over the delete behavior.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Automatic routing (soft delete if the class has a @DataDeleted field, hard delete otherwise)
	 * long deleted = db.deleteById(Task.class, taskId);
	 *
	 * // Explicit hard delete (physical removal, whatever the @DataDeleted field)
	 * db.deleteHardById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier of the entity to delete (e.g., ObjectId)
	 * @param options   Optional query options
	 * @return Number of entities deleted (typically 0 or 1)
	 * @throws Exception if delete operation fails
	 */
	public <ID_TYPE> long deleteById(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		if (model.getDeletedFieldName() != null) {
			return deleteSoftById(clazz, id, options);
		} else {
			return deleteHardById(clazz, id, options);
		}
	}

	/**
	 * Deletes entities matching the specified conditions.
	 *
	 * <p>
	 * <strong>Automatic routing behavior:</strong> If the entity class has a field
	 * annotated {@code @DataDeleted}, this method performs a soft delete (marking as deleted).
	 * Otherwise, it performs a hard delete (physical removal).
	 * </p>
	 *
	 * <p>
	 * Use {@link #deleteHard} or {@link #deleteSoft} if you need explicit control
	 * over the delete behavior.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Delete all the finished tasks
	 * long deleted = db.delete(Task.class,
	 *     new Condition(Filters.eq("done", true)));
	 * </pre>
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions
	 * @return Number of entities deleted
	 * @throws Exception if delete operation fails
	 */
	public long delete(final Class<?> clazz, final QueryOption... option) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		if (model.getDeletedFieldName() != null) {
			return deleteSoft(clazz, option);
		} else {
			return deleteHard(clazz, option);
		}
	}

	/**
	 * Lists the names of the collections of the database this connection is bound to.
	 *
	 * <p>
	 * {@code name} and {@code option} are ignored: the database is the one of the connection, whatever name is passed.
	 * </p>
	 *
	 * @param name   Ignored
	 * @param option Ignored
	 * @return List of collection names
	 * @throws InternalServerErrorException if listing fails
	 */
	public List<String> listCollections(final String name, final QueryOption... option)
			throws InternalServerErrorException {
		return this.db.getDatabase().listCollectionNames().into(new ArrayList<>());
	}

	/**
	 * Renames a MongoDB collection.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * db.renameCollection("old_users", "users");
	 * </pre>
	 *
	 * @param source      Current collection name
	 * @param destination New collection name
	 */
	public void renameCollection(final String source, final String destination) {
		final MongoCollection<Document> previousCollection = this.db.getDatabase().getCollection(source);
		previousCollection
				.renameCollection(new com.mongodb.MongoNamespace(this.db.getDatabase().getName(), destination));
	}

	/**
	 * Deletes (drops) an entire database.
	 *
	 * <p>
	 * <strong>Warning:</strong> This permanently deletes all collections and documents
	 * in the specified database. Use with caution.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * db.deleteDatabase("test_database");
	 * </pre>
	 *
	 * @param name DataBase name to delete
	 * @return true if deletion succeeds
	 */
	public boolean deleteDatabase(final String name) {
		final MongoDatabase database = this.db.getClient().getDatabase(name);
		database.drop();
		return true;
	}

	/**
	 * Creates an ascending index on a specific field in a collection.
	 *
	 * <p>
	 * Indexes improve query performance for frequently queried fields.
	 * </p>
	 *
	 * <p>
	 * Example usage with ObjectId field:
	 * </p>
	 * <pre>
	 * // Create index on authorId field for faster lookups
	 * db.ascendingIndex("documents", "authorId");
	 *
	 * // Create index on email field
	 * db.ascendingIndex("users", "email");
	 * </pre>
	 *
	 * @param collectionName Name of the collection
	 * @param fieldName      Name of the field to index
	 */
	public void ascendingIndex(final String collectionName, final String fieldName) {
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		collection.createIndex(Indexes.ascending(fieldName));
	}

	/**
	 * Disables schema validation for an existing MongoDB collection.
	 *
	 * This method removes any JSON Schema validator associated with the specified
	 * collection and turns off server-side validation entirely. Documents can be
	 * inserted or updated without being checked against any schema.
	 *
	 * @param collection the name of the MongoDB collection to disable schema
	 *                   validation on.
	 */
	public void disableSchema(final String collection) {
		final Document command = new Document("collMod", collection)//
				.append("validator", new Document())//
				.append("validationLevel", "off");
		statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}

	/**
	 * Sets or updates a JSON Schema validator for an existing MongoDB collection.
	 *
	 * The provided schema will be strictly enforced on all insert and update
	 * operations for the specified collection. This ensures that documents conform
	 * to the defined structure.
	 *
	 * @param collection the name of the MongoDB collection to apply the schema to.
	 * @param schema     a BSON Document representing the JSON Schema to enforce
	 *                   (must follow MongoDB's schema validation format).
	 */
	public void enableSchema(final String collection, final Document schema) {
		final Document command = new Document("collMod", collection)//
				.append("validator", schema)//
				.append("validationLevel", "strict");
		statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}

	/**
	 * Converts a Java object into a MongoDB-compatible BSON representation.
	 *
	 * @param <T>  The type of the object to convert
	 * @param data The object to convert (may be null)
	 * @return The BSON representation of the object, or null if the input is null
	 * @throws Exception if conversion fails
	 */
	public <T> Object convertInDocument(final T data) throws Exception {
		if (data == null) {
			return null;
		}
		// Use pre-compiled codec: resolves the writer once per type (cached via TypeInfo)
		return MongoCodecFactory.buildWriter(TypeInfo.ofRaw(data.getClass())).toMongo(data);
	}

	private Object retreiveValueEnum(final Class<?> objectClass, final String temporaryString)
			throws DataAccessException {
		final Object[] arr = objectClass.getEnumConstants();
		for (final Object elem : arr) {
			if (elem.toString().equals(temporaryString)) {
				return elem;
			}
		}
		throw new DataAccessException("Enum value does not exist in the Model val='" + temporaryString + "' model="
				+ objectClass.getCanonicalName());

	}

	/**
	 * Converts a string default value to the appropriate Java type based on the field's type.
	 *
	 * @param data  The string representation of the default value
	 * @param field The target field whose type determines the conversion
	 * @return The converted value matching the field's type
	 * @throws Exception if the conversion fails or the type is unsupported
	 */
	protected Object convertDefaultField(final String data, final Field field) throws Exception {
		final String value = data.startsWith("'") && data.endsWith("'") ? data.substring(1, data.length() - 1) : data;
		final Class<?> type = field.getType();
		if (type == Long.class || type == long.class) {
			return Long.parseLong(value);
		}
		if (type == Integer.class || type == int.class) {
			return Integer.parseInt(value);
		}
		if (type == Float.class || type == float.class) {
			return Float.parseFloat(value);
		}
		if (type == Double.class || type == double.class) {
			return Double.parseDouble(value);
		}
		if (type == Boolean.class || type == boolean.class) {
			return Boolean.parseBoolean(value);
		}
		if (type == Date.class) {
			return new Date(Long.parseLong(value));
		}
		if (type == Instant.class) {
			return Instant.parse(value);
		}
		if (type == OffsetDateTime.class) {
			return OffsetDateTime.parse(value);
		}
		if (type == LocalDate.class) {
			return LocalDate.parse(value);
		}
		if (type == LocalTime.class) {
			return LocalTime.parse(value);
		}
		if (type == String.class) {
			return value;
		}
		if (type.isEnum()) {
			return retreiveValueEnum(type, value);
		}
		throw new DataAccessException(
				"Request default of unknow native type " + type.getCanonicalName() + " => " + value);
	}

	/**
	 * Retrieves and atomically increments a sequence counter for generating unique Long IDs.
	 *
	 * <p>Uses a dedicated "counters" collection to store sequences per collection/field.
	 *
	 * @param collectionName The collection name used as the counter key
	 * @param fieldName      The field name for the sequence (defaults to "sequence_id" if null or empty)
	 * @return The next sequence value
	 * @throws IllegalStateException if the database returns no counter
	 */
	public long getNextSequenceLongValue(final String collectionName, final String fieldName) {
		final String counterField = fieldName == null || fieldName.isEmpty() ? "sequence_id" : fieldName;
		// Collection "counters" to store the sequences if Ids
		final MongoCollection<Document> countersCollection = this.db.getDatabase().getCollection(COLLECTION_COUNTERS);

		// Filter to find the specific counter for the collections
		final Document filter = new Document("_id", collectionName);

		// Update the field <fieldName> of 1
		final Document update = new Document("$inc", new Document(counterField, 1L));

		// get the value after updated it
		final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
				.upsert(true); // create field if not exist

		// Real creation of the unique counter.
		statistic.countFindOneAndUpdate++;
		final Document updatedCounter = findOneAndUpdateIn(countersCollection, filter, update, options);

		// Return the new sequence value...
		final Long value = updatedCounter == null ? null : updatedCounter.getLong(counterField);
		if (value == null) {
			throw new IllegalStateException("No value for the sequence " + collectionName + "." + counterField);
		}
		return value;
	}

	/**
	 * Inserts an entity into the database and returns its generated primary key.
	 *
	 * <p>Handles auto-generation of primary keys (ObjectId, UUID, or Long), creation and
	 * update timestamps, default values for deleted fields, and add-on field processing.
	 *
	 * @param <T>    The type of the entity
	 * @param data   The entity to insert
	 * @param option Optional query options (e.g., DirectData, DirectPrimaryKey)
	 * @return The generated primary key
	 * @throws Exception if insertion fails
	 */
	public <T> Object insertPrimaryKey(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);
		final boolean directdata = options.exist(DirectData.class);
		final boolean directPrimaryKey = options.exist(DirectPrimaryKey.class);
		final DbClassModel model = DbClassModel.of(clazz);

		final String collectionName = model.getTableName(options);
		Object uniqueId = null;
		// real add in the BDD:
		try {
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Document docSet = new Document();
			final Document docUnSet = new Document();

			uniqueId = fillInsertDocument(model, data, options, collectionName, directdata, directPrimaryKey, docSet,
					docUnSet);

			statistic.countInsertOne++;
			final InsertOneResult result = insertOneIn(collection, docSet);
		} catch (final Exception ex) {
			LOGGER.error("Fail Mongo request: {} ({})", ex.getMessage(), ex.getClass().getSimpleName(), ex);
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage(), ex);
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		for (final DbPropertyDescriptor desc : model.getAsyncInsertFields()) {
			final Object fieldValue = desc.getProperty().getValue(data);
			desc.getAddOn().asyncInsert(this, clazz, uniqueId, desc, fieldValue, asyncActions, options);
		}
		runLazyActions(asyncActions);
		return uniqueId;
	}

	/**
	 * Fills the document to insert from the fields of the entity, walked in declaration order (to preserve
	 * the field ordering of the {@link Document}).
	 *
	 * <p>Not every field is written: a regular field without codec, or {@code null} without
	 * {@code @InitValue}, is left out; a relation field is written by its add-on only if the add-on can
	 * insert; under {@link DirectData} the timestamps and the deleted flag are not written.
	 *
	 * @param <T>              the type of the entity
	 * @param model            the model of the entity
	 * @param data             the entity to insert
	 * @param options          the options of the insert
	 * @param collectionName   the collection written
	 * @param directdata       {@code true} when the insert carries {@link DirectData}
	 * @param directPrimaryKey {@code true} when the insert carries {@link DirectPrimaryKey}
	 * @param docSet           the document to insert, completed
	 * @param docUnSet         the {@code $unset} document handed to the add-ons, completed
	 * @return the primary key of the new document ({@code null} when the model has none)
	 * @throws Exception if a field cannot be written or a primary key is missing or unexpected
	 */
	private <T> Object fillInsertDocument(
			final DbClassModel model,
			final T data,
			final QueryOptions options,
			final String collectionName,
			final boolean directdata,
			final boolean directPrimaryKey,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final Class<?> clazz = data.getClass();
		Object uniqueId = null;
		// Iterate all fields in declaration order to preserve Document field ordering
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			final DbFieldAction action = desc.getAction();
			final FieldName fieldName = desc.getFieldName(options);
			final PropertyDescriptor prop = desc.getProperty();
			final Field field = prop.getField();

			switch (action) {
				case PRIMARY_KEY: {
					if (!directdata && !directPrimaryKey) {
						uniqueId = generatePrimaryKey(clazz, data, prop, collectionName, fieldName, docSet);
					} else {
						uniqueId = appendProvidedPrimaryKey(data, desc, fieldName, docSet);
					}
					break;
				}
				case CREATION_TIMESTAMP:
				case UPDATE_TIMESTAMP: {
					if (!directdata) {
						docSet.append(fieldName.inTable(), Date.from(Instant.now()));
					}
					break;
				}
				case DELETED: {
					if (!directdata) {
						appendDeletedFlagForInsert(desc, field, fieldName, docSet);
					}
					break;
				}
				case ADDON: {
					if (!desc.canInsert()) {
						break;
					}
					desc.getAddOn().insertData(this, desc, data, options, docSet, docUnSet);
					break;
				}
				case NOT_READ:
				case NORMAL:
				default: {
					appendNormalFieldForInsert(desc, field, fieldName, data, docSet);
					break;
				}
			}
		}
		return uniqueId;
	}

	/**
	 * Generates the primary key of an entity inserted without one, and writes it in the document to insert.
	 *
	 * <p>An {@link ObjectId} and a {@link UUID} are generated here, a {@code Long} comes from the sequence
	 * of the collection ({@link #getNextSequenceLongValue}).
	 *
	 * @param <T>            the type of the entity
	 * @param clazz          the entity class
	 * @param data           the entity to insert
	 * @param prop           the property of the primary key
	 * @param collectionName the collection written
	 * @param fieldName      the names of the primary key field
	 * @param docSet         the document to insert, completed
	 * @return the generated primary key
	 * @throws Exception if the entity already holds a primary key or its type cannot be generated
	 */
	private <T> Object generatePrimaryKey(
			final Class<?> clazz,
			final T data,
			final PropertyDescriptor prop,
			final String collectionName,
			final FieldName fieldName,
			final Document docSet) throws Exception {
		Object uniqueId;
		if (prop.getValue(data) != null) {
			throw new DataAccessException(
					"Unexpected comportment, try to add an object with a primary key. use option 'DirectPrimaryKey.class' to do that");
		}
		final Class<?> pkType = prop.getType();
		if (pkType == ObjectId.class) {
			uniqueId = new ObjectId();
			docSet.append(fieldName.inTable(), uniqueId);
		} else if (pkType == UUID.class) {
			uniqueId = UuidUtils.nextUUID();
			docSet.append(fieldName.inTable(), uniqueId);
		} else if (pkType == Long.class || pkType == long.class) {
			final long id = getNextSequenceLongValue(collectionName, fieldName.inTable());
			uniqueId = id;
			docSet.append(fieldName.inTable(), id);
		} else {
			throw new DataAccessException(
					"TODO: Manage the ID primary key for type: " + clazz.getCanonicalName() + " => " + pkType);
		}
		return uniqueId;
	}

	/**
	 * Writes in the document to insert the primary key the caller provided ({@link DirectData} or
	 * {@link DirectPrimaryKey}).
	 *
	 * @param <T>       the type of the entity
	 * @param data      the entity to insert
	 * @param desc      the primary key field
	 * @param fieldName the names of the primary key field
	 * @param docSet    the document to insert, completed
	 * @return the primary key of the entity
	 * @throws Exception if the entity holds no primary key, or an untyped one of an unsupported type
	 */
	private static <T> Object appendProvidedPrimaryKey(
			final T data,
			final DbPropertyDescriptor desc,
			final FieldName fieldName,
			final Document docSet) throws Exception {
		final Object primaryKeyValue = desc.getProperty().getValue(data);
		if (primaryKeyValue == null) {
			throw new DataAccessException(
					"Fail to Insert data in DB.. when use 'DirectData.class' or 'DirectPrimaryKey.class' you need to provide a primary key...");
		}
		if (DeclaredTypeRules.isUntypedPrimaryKey(desc)) {
			DeclaredTypeRules.checkPrimaryKeyValue(primaryKeyValue, "Field " + MongoCodecFactory.qualifiedName(desc));
		}
		docSet.append(fieldName.inTable(), primaryKeyValue);
		return primaryKeyValue;
	}

	/**
	 * Writes the soft-delete flag of an entity inserted without {@link DirectData}: the declared initial value,
	 * or {@code false}.
	 *
	 * @param desc      the deleted field
	 * @param field     the Java field
	 * @param fieldName the names of the deleted field
	 * @param docSet    the document to insert, completed
	 * @throws Exception if the initial value cannot be converted
	 */
	private void appendDeletedFlagForInsert(
			final DbPropertyDescriptor desc,
			final Field field,
			final FieldName fieldName,
			final Document docSet) throws Exception {
		// Always written, even without a declared default value: the read
		// filter is an equality on 'false' (see Condition#getFilter), so a
		// document inserted without the field would be invisible to every
		// query -- including the read back of this very insert.
		final String defVal = desc.getInitValue();
		final Object defaultValue = defVal != null ? convertDefaultField(defVal, field) : Boolean.FALSE;
		docSet.append(fieldName.inTable(), defaultValue);
	}

	/**
	 * Writes a regular field of an entity to insert, through its codec; a {@code null} value takes the
	 * declared initial value, or the field is left out.
	 *
	 * @param <T>       the type of the entity
	 * @param desc      the field
	 * @param field     the Java field
	 * @param fieldName the names of the field
	 * @param data      the entity to insert
	 * @param docSet    the document to insert, completed
	 * @throws Exception if the initial value cannot be converted or the codec fails
	 */
	private <T> void appendNormalFieldForInsert(
			final DbPropertyDescriptor desc,
			final Field field,
			final FieldName fieldName,
			final T data,
			final Document docSet) throws Exception {
		final PropertyDescriptor prop = desc.getProperty();
		final MongoFieldCodec codec = desc.getCodec();
		if (codec == null) {
			return;
		}
		Object currentInsertValue = prop.getValue(data);

		if (currentInsertValue == null && field != null) {
			final String defVal = desc.getInitValue();
			if (defVal == null) {
				return;
			}
			currentInsertValue = convertDefaultField(defVal, field);
		}

		final Class<?> type = prop.getType();
		if (!type.isPrimitive()) {
			if (prop.getValue(data) == null) {
				if (currentInsertValue != null) {
					// The @InitValue goes through the codec like any value: same BSON form, and
					// encrypted on a @DataEncrypt field.
					docSet.append(fieldName.inTable(), codec.convertToMongo(currentInsertValue));
				}
				return;
			}
		}
		codec.writeToDoc(null, fieldName.inTable(), data, docSet, null);
	}

	/**
	 * Updates entities matching the specified conditions (QueryOptions variant).
	 *
	 * <p>
	 * <strong>Required option:</strong> You MUST provide a {@link FilterValue} option to specify which
	 * fields should be updated. This prevents accidental full-row updates.
	 * </p>
	 *
	 * <p>
	 * <strong>Condition specification:</strong> Use {@link Condition} option to specify which entities
	 * to update. Without conditions, the operation will fail with an exception.
	 * </p>
	 *
	 * <p>
	 * <strong>Relation side effects</strong> ({@code @OneToManyDoc}, {@code @ManyToManyDoc},
	 * {@code @ManyToOneDoc} fields written by the update) follow the same filter as the update: the stored state
	 * is read with the merged {@link Condition}s, the primary key of {@link TransmitKey} and the soft-delete
	 * exclusion (unless {@link AccessDeletedItems}), and the side effects run only when the update matched a
	 * document. A condition that matches nothing, or a soft-deleted target, changes no remote document. Run the
	 * call inside a {@link TransactionContext} to make the update and its side effects atomic.
	 * </p>
	 *
	 * @param <T>     The type of the entity
	 * @param data    The entity data containing the update values
	 * @param options Query options including FilterValue (required) and Condition
	 * @return Number of documents modified: the date of an {@code @UpdateTimestamp} field is always written
	 *         (unless {@code DirectData}), so a matched document counts 1; without such a field, a matched
	 *         document whose values do not change counts 0
	 * @throws Exception if update operation fails or FilterValue is missing
	 */
	public <T> long update(final T data, final QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions queryOptions = options == null ? new QueryOptions() : options;
		final boolean directdata = queryOptions.exist(DirectData.class);
		final boolean forceReadOnlyField = queryOptions.exist(ForceReadOnlyField.class);
		final Condition condition = conditionFusionOrEmpty(queryOptions, true);
		final FilterValue filterKey = singleFilterValueForUpdate(queryOptions);
		final FilterOmit filterOmitKey = optionalFilterOmitForUpdate(queryOptions);

		final DbClassModel model = DbClassModel.of(clazz);
		final List<LazyGetter> asyncActions = new ArrayList<>();

		final Object previousData = readPreviousDataIfNeeded(model, clazz, condition, queryOptions);

		// real add in the BDD:
		try {
			final String collectionName = model.getTableName(queryOptions);
			final String deletedFieldName = model.getDeletedFieldName();
			final Bson filters = matchAllIfNull(condition.getFilter(collectionName, queryOptions, deletedFieldName));
			QueryStatistics.record(collectionName, Operation.UPDATE, condition.getFilter(), null,
					statisticSoftDeleteField(queryOptions, deletedFieldName));
			final Document docSet = new Document();
			final Document docUnSet = new Document();

			appendUpdateTimestamp(model, queryOptions, directdata, docSet);
			// A relation field to synchronize whose stored document the update does not reach.
			final boolean relationSideEffectSkipped = appendAddonFieldsForUpdate(model, data, queryOptions, filterKey,
					filterOmitKey, forceReadOnlyField, previousData, docSet, docUnSet, asyncActions);
			appendRegularFieldsForUpdate(model, data, queryOptions, filterKey, filterOmitKey, forceReadOnlyField,
					docSet, docUnSet);

			// Do the query ...
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Document actions = buildUpdateActions(docSet, docUnSet);

			statistic.countUpdateMany++;
			final UpdateResult ret = updateManyIn(collection, filters, actions);
			return finishUpdate(ret, collectionName, asyncActions, relationSideEffectSkipped);
		} catch (final Exception ex) {
			LOGGER.error("Error in update: {}", ex.getMessage(), ex);
			throw ex;
		}
	}

	/**
	 * Returns the only {@link FilterValue} of an update.
	 *
	 * @param queryOptions the options of the update
	 * @return the {@link FilterValue}
	 * @throws DataAccessException if the options hold none or more than one
	 */
	private static FilterValue singleFilterValueForUpdate(final QueryOptions queryOptions) throws DataAccessException {
		final List<FilterValue> filterKeys = queryOptions.get(FilterValue.class);
		if (filterKeys.size() != 1) {
			throw new DataAccessException("request a gets without/or with more 1 FilterValue of values");
		}
		return filterKeys.get(0);
	}

	/**
	 * Returns the optional {@link FilterOmit} of an update.
	 *
	 * @param queryOptions the options of the update
	 * @return the {@link FilterOmit}, or {@code null} when the options hold none
	 * @throws DataAccessException if the options hold more than one
	 */
	private static FilterOmit optionalFilterOmitForUpdate(final QueryOptions queryOptions) throws DataAccessException {
		final List<FilterOmit> filterOmitKeys = queryOptions.get(FilterOmit.class);
		FilterOmit filterOmitKey = null;
		if (filterOmitKeys.size() > 1) {
			throw new DataAccessException("request a gets without/or with more 1 FilterOmit of values");
		} else if (filterOmitKeys.size() == 1) {
			filterOmitKey = filterOmitKeys.get(0);
		}
		return filterOmitKey;
	}

	/**
	 * Loads the stored state of the document an update targets, when an add-on field of the model needs it.
	 *
	 * <p>The relation add-ons diff the stored document against the new values: it is loaded with the filter of
	 * the update itself, so that a document the update does not reach never feeds a side effect.
	 *
	 * @param model        the model of the entity
	 * @param clazz        the entity class
	 * @param condition    the merged condition of the update
	 * @param queryOptions the options of the update
	 * @return the stored entity, or {@code null} when no add-on needs it, when no single {@link TransmitKey} is
	 *         given or when the update does not reach the document
	 * @throws Exception if the read fails
	 */
	private Object readPreviousDataIfNeeded(
			final DbClassModel model,
			final Class<?> clazz,
			final Condition condition,
			final QueryOptions queryOptions) throws Exception {
		Object previousData = null;
		if (model.needsPreviousDataForUpdate()) {
			final List<TransmitKey> transmitKey = queryOptions.get(TransmitKey.class);
			if (transmitKey.size() == 1) {
				previousData = readPreviousDataForUpdate(clazz, transmitKey.get(0).getKey(), condition, queryOptions);
			}
		}
		return previousData;
	}

	/**
	 * Adds the date of the {@code @UpdateTimestamp} field to the {@code $set} document, unconditionally (not
	 * subject to {@link FilterValue}), unless {@code directdata}.
	 *
	 * @param model        the model of the entity
	 * @param queryOptions the options of the update
	 * @param directdata   {@code true} when the update carries {@link DirectData}
	 * @param docSet       the {@code $set} document, completed
	 */
	private static void appendUpdateTimestamp(
			final DbClassModel model,
			final QueryOptions queryOptions,
			final boolean directdata,
			final Document docSet) {
		final DbPropertyDescriptor updateTsDesc = model.getUpdateTimestamp();
		if (updateTsDesc != null && !directdata) {
			final FieldName fieldName = updateTsDesc.getFieldName(queryOptions);
			docSet.append(fieldName.inTable(), Date.from(Instant.now()));
		}
	}

	/**
	 * Tells whether an update writes a field: it is named by the {@link FilterValue}, not by the
	 * {@link FilterOmit}, and not read-only for the API unless {@link ForceReadOnlyField}.
	 *
	 * @param desc               the field
	 * @param fieldName          the names of the field
	 * @param filterKey          the fields to write
	 * @param filterOmitKey      the fields to leave out, may be {@code null}
	 * @param forceReadOnlyField {@code true} when the update carries {@link ForceReadOnlyField}
	 * @return {@code true} if the field is written
	 */
	private static boolean isFieldWrittenByUpdate(
			final DbPropertyDescriptor desc,
			final FieldName fieldName,
			final FilterValue filterKey,
			final FilterOmit filterOmitKey,
			final boolean forceReadOnlyField) {
		if (filterOmitKey != null && filterOmitKey.getValues().contains(fieldName.inStruct())) {
			return false;
		}
		if (!filterKey.getValues().contains(fieldName.inStruct())) {
			return false;
		}
		if (!forceReadOnlyField && desc.isApiReadOnly()) {
			return false;
		}
		return true;
	}

	/**
	 * Runs the add-ons of the fields an update writes: schedules the asynchronous update of those that have
	 * one, and lets those that can insert complete the {@code $set} and {@code $unset} documents.
	 *
	 * @param <T>                the type of the entity
	 * @param model              the model of the entity
	 * @param data               the entity holding the new values
	 * @param queryOptions       the options of the update
	 * @param filterKey          the fields to write
	 * @param filterOmitKey      the fields to leave out, may be {@code null}
	 * @param forceReadOnlyField {@code true} when the update carries {@link ForceReadOnlyField}
	 * @param previousData       the stored state of the document, may be {@code null}
	 * @param docSet             the {@code $set} document, completed
	 * @param docUnSet           the {@code $unset} document, completed
	 * @param asyncActions       the deferred actions, completed
	 * @return {@code true} if the asynchronous update of a relation field that needs the stored state was not
	 *         scheduled because {@code previousData} is {@code null}
	 * @throws Exception if an add-on fails or no single {@link TransmitKey} is given for an asynchronous update
	 */
	private <T> boolean appendAddonFieldsForUpdate(
			final DbClassModel model,
			final T data,
			final QueryOptions queryOptions,
			final FilterValue filterKey,
			final FilterOmit filterOmitKey,
			final boolean forceReadOnlyField,
			final Object previousData,
			final Document docSet,
			final Document docUnSet,
			final List<LazyGetter> asyncActions) throws Exception {
		boolean relationSideEffectSkipped = false;
		// --- Handle addon fields ---
		for (final DbPropertyDescriptor desc : model.getAddonFields()) {
			final FieldName fieldName = desc.getFieldName(queryOptions);
			// Apply filter/omit logic
			if (!isFieldWrittenByUpdate(desc, fieldName, filterKey, filterOmitKey, forceReadOnlyField)) {
				continue;
			}
			if (desc.isAsyncUpdate()) {
				final List<TransmitKey> transmitKey = queryOptions.get(TransmitKey.class);
				if (transmitKey.size() != 1) {
					throw new DataAccessException(
							"Fail to transmit Key to update the async update... (must have only 1)");
				}
				if (previousData == null && desc.isPreviousDataNeeded()) {
					relationSideEffectSkipped = true;
				} else {
					desc.getAddOn().asyncUpdate(this, previousData, transmitKey.get(0).getKey(), desc,
							desc.getProperty().getValue(data), asyncActions, queryOptions);
				}
			}
			if (!desc.canInsert()) {
				continue;
			}
			desc.getAddOn().insertData(this, desc, data, queryOptions, docSet, docUnSet);
		}
		return relationSideEffectSkipped;
	}

	/**
	 * Writes the regular fields an update writes, through their pre-compiled codecs, in the {@code $set} and
	 * {@code $unset} documents.
	 *
	 * @param <T>                the type of the entity
	 * @param model              the model of the entity
	 * @param data               the entity holding the new values
	 * @param queryOptions       the options of the update
	 * @param filterKey          the fields to write
	 * @param filterOmitKey      the fields to leave out, may be {@code null}
	 * @param forceReadOnlyField {@code true} when the update carries {@link ForceReadOnlyField}
	 * @param docSet             the {@code $set} document, completed
	 * @param docUnSet           the {@code $unset} document, completed
	 * @throws Exception if a codec fails
	 */
	private <T> void appendRegularFieldsForUpdate(
			final DbClassModel model,
			final T data,
			final QueryOptions queryOptions,
			final FilterValue filterKey,
			final FilterOmit filterOmitKey,
			final boolean forceReadOnlyField,
			final Document docSet,
			final Document docUnSet) throws Exception {
		// --- Handle regular fields (via pre-compiled codecs) ---
		for (final DbPropertyDescriptor desc : model.getRegularFields()) {
			final FieldName fieldName = desc.getFieldName(queryOptions);
			// Apply filter/omit logic
			if (!isFieldWrittenByUpdate(desc, fieldName, filterKey, filterOmitKey, forceReadOnlyField)) {
				continue;
			}
			final MongoFieldCodec codec = desc.getCodec();
			if (codec == null) {
				continue;
			}
			final PropertyDescriptor prop = desc.getProperty();
			if (!prop.getType().isPrimitive()) {
				final Object tmp = prop.getValue(data);
				if (tmp == null && desc.getInitValue() != null) {
					continue;
				}
			}
			if (filterKey.isFullUpdate() && MongoCodecFactory.hasProtectedSubFields(prop.getTypeInfo())) {
				appendEmbeddedVisibleFieldsForFullUpdate(desc, fieldName, prop, data, docSet, docUnSet);
				continue;
			}
			codec.writeToDoc(null, fieldName.inTable(), data, docSet, docUnSet);
		}
	}

	/**
	 * Writes, in a full update, a field whose type holds protected sub-fields.
	 *
	 * <p>A read leaves the protected fields of embedded objects null: never rewrite them as a whole in a full
	 * update. An embedded object is written field by field (dot notation), a list, set or map of such objects
	 * is kept as stored (name it in a {@link FilterValue} to rewrite it).
	 *
	 * @param <T>       the type of the entity
	 * @param desc      the field
	 * @param fieldName the names of the field
	 * @param prop      the property of the field
	 * @param data      the entity holding the new values
	 * @param docSet    the {@code $set} document, completed
	 * @param docUnSet  the {@code $unset} document, completed
	 * @throws Exception if the embedded object cannot be written
	 */
	private static <T> void appendEmbeddedVisibleFieldsForFullUpdate(
			final DbPropertyDescriptor desc,
			final FieldName fieldName,
			final PropertyDescriptor prop,
			final T data,
			final Document docSet,
			final Document docUnSet) throws Exception {
		if (MongoCodecFactory.isEmbeddedObjectType(prop.getTypeInfo())) {
			final Object embedded = prop.getValue(data);
			if (embedded == null) {
				docUnSet.append(fieldName.inTable(), "");
			} else {
				MongoCodecFactory.writeEmbeddedVisibleFields(fieldName.inTable(), desc, embedded, docSet, docUnSet);
			}
		}
	}

	/**
	 * Builds the update document of the driver.
	 *
	 * @param docSet   the {@code $set} document
	 * @param docUnSet the {@code $unset} document
	 * @return a document holding {@code $set} and {@code $unset} when they are not empty
	 */
	private static Document buildUpdateActions(final Document docSet, final Document docUnSet) {
		final Document actions = new Document();
		if (!docSet.isEmpty()) {
			actions.append("$set", docSet);
		}
		if (!docUnSet.isEmpty()) {
			actions.append("$unset", docUnSet);
		}
		return actions;
	}

	/**
	 * Ends an update once the driver answered: runs the relation side effects when the update reached a
	 * document, drops them otherwise.
	 *
	 * @param ret                       the answer of the driver
	 * @param collectionName            the collection updated
	 * @param asyncActions              the deferred relation actions, run when the update matched a document
	 * @param relationSideEffectSkipped {@code true} when a relation field was written without being synchronized
	 * @return the number of documents modified
	 * @throws Exception if a deferred action fails
	 */
	private long finishUpdate(
			final UpdateResult ret,
			final String collectionName,
			final List<LazyGetter> asyncActions,
			final boolean relationSideEffectSkipped) throws Exception {
		if (ret.getMatchedCount() == 0) {
			// The update reached no document (caller's condition, soft delete): its relation side effects
			// must not run either, they would escape the caller's scope.
			if (!asyncActions.isEmpty()) {
				LOGGER.debug("update of {} matched no document: {} relation side effect(s) dropped", collectionName,
						asyncActions.size());
			}
			return ret.getModifiedCount();
		}
		if (relationSideEffectSkipped) {
			LOGGER.warn("update of {} applied to a document not reachable when its previous state was read "
					+ "(concurrent write?): its relation fields were written without synchronizing the remote "
					+ "documents", collectionName);
		}
		runLazyActions(asyncActions);
		return ret.getModifiedCount();
	}

	/**
	 * Adds to a read the options of one type that the caller of a write passed, all of them and in order.
	 *
	 * <p>The callers differ on purpose in what they forward of the other types: {@code insert} forwards the
	 * first {@link OverrideTableName} only, and forwards {@link ReadAllColumn} and {@link AccessDeletedItems}
	 * as the caller gave them; {@code readPreviousDataForUpdate} forwards every {@link OverrideTableName},
	 * and always adds its own {@link ReadAllColumn} and, when the caller has one, a new
	 * {@link AccessDeletedItems}.
	 *
	 * @param from the options of the write
	 * @param to the options of the read, completed
	 * @param type the type of option to forward
	 */
	private static <T extends QueryOption> void forwardOptions(
			final QueryOptions from,
			final QueryOptions to,
			final Class<T> type) {
		for (final T elem : from.get(type)) {
			to.add(elem);
		}
	}

	/**
	 * Reads the stored state of the document an update targets, for the relation side effects of its add-on
	 * fields.
	 *
	 * <p>The read uses the filter of the update itself: the merged conditions of the caller, the soft-delete
	 * exclusion (unless {@link AccessDeletedItems}) and the collection ({@link OverrideTableName}), restricted to
	 * the transmitted primary key. A document the update does not reach is not returned.
	 *
	 * @param clazz      the entity class
	 * @param primaryKey the primary key transmitted by {@link TransmitKey}
	 * @param condition  the merged condition of the update
	 * @param options    the options of the update
	 * @return the stored entity (all columns), or {@code null} when the update does not reach it
	 * @throws Exception if the read fails
	 */
	private Object readPreviousDataForUpdate(
			final Class<?> clazz,
			final Object primaryKey,
			final Condition condition,
			final QueryOptions options) throws Exception {
		final QueryOptions readOptions = new QueryOptions();
		readOptions.add(condition);
		readOptions.add(new Condition(getTableIdCondition(clazz, primaryKey, options)));
		forwardOptions(options, readOptions, OverrideTableName.class);
		forwardOptions(options, readOptions, OptionSpecifyType.class);
		if (options.exist(AccessDeletedItems.class)) {
			readOptions.add(new AccessDeletedItems());
		}
		readOptions.add(new ReadAllColumn());
		return getRaw(clazz, readOptions);
	}

	/**
	 * Generates the list of field names to include in a MongoDB projection.
	 *
	 * @param clazz   The entity class
	 * @param options Query options that may affect field selection
	 * @return List of database field names to include in the query projection
	 * @throws Exception if class introspection fails
	 */
	public List<String> generateSelectField(final Class<?> clazz, final QueryOptions options) throws Exception {
		final boolean readAllFields = QueryOptions.readAllColumn(options);
		final DbClassModel dbModel = DbClassModel.of(clazz);
		return dbModel.generateSelectFields(readAllFields, options);
	}

	/**
	 * Merges all {@link Condition} options into a single condition using logical AND.
	 *
	 * <p>With {@code throwIfEmpty} (the writes: update, delete, restore, {@code updateBsonDocuments}), a missing
	 * condition, or a condition without filter or with an empty filter ({@code new Condition()},
	 * {@code Filters.empty()}), throws an exception unless the {@link AllowMassOperation} option is present:
	 * an empty filter would target the whole collection. Otherwise returns an empty condition.
	 *
	 * @param options      The query options containing zero or more conditions
	 * @param throwIfEmpty If true, throws a {@link DataAccessException} when no (non-empty) condition is provided
	 * @return The merged condition
	 * @throws DataAccessException if throwIfEmpty is true, no non-empty condition is present and
	 *                             {@link AllowMassOperation} is absent
	 */
	public Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException {
		if (options == null) {
			if (throwIfEmpty) {
				throw new DataAccessException("request a write without any condition");
			}
			return new Condition();
		}
		final boolean allowMass = options.exist(AllowMassOperation.class);
		final List<Bson> filters = new ArrayList<>();
		for (final Condition elem : options.get(Condition.class)) {
			final Bson filter = elem.getFilter();
			// A read keeps an empty filter (it matches everything, like no filter): only a write pays the
			// encoding of the filter needed to tell whether it is empty.
			if (throwIfEmpty ? !isEmptyFilter(filter) : filter != null) {
				filters.add(filter);
			}
		}
		if (filters.isEmpty()) {
			if (throwIfEmpty && !allowMass) {
				throw new DataAccessException(
						"request a write without any condition (or with an empty one): add a filter, or the option "
								+ "AllowMassOperation to target the whole collection");
			}
			return new Condition();
		}
		if (filters.size() == 1) {
			return new Condition(filters.get(0));
		}
		return new Condition(Filters.and(filters));
	}

	/**
	 * Returns the filter, or an empty document (match all) when it is {@code null}: a write accepted by
	 * {@link #conditionFusionOrEmpty} with {@link AllowMassOperation} has no filter on a collection without
	 * soft delete, and the driver refuses a {@code null} filter.
	 *
	 * @param filter the filter, may be {@code null}
	 * @return a non-null filter
	 */
	private static Bson matchAllIfNull(final Bson filter) {
		return filter != null ? filter : new Document();
	}

	/**
	 * Tells whether a filter matches every document: {@code null} or an empty document.
	 *
	 * <p>A {@link Document} or a {@link BsonDocument} (what {@code Filters.empty()} returns) is checked
	 * directly; any other filter is encoded once to see its content.
	 *
	 * @param filter the filter
	 * @return {@code true} if the filter does not restrict anything
	 */
	private static boolean isEmptyFilter(final Bson filter) {
		if (filter == null) {
			return true;
		}
		if (filter instanceof final Document document) {
			return document.isEmpty();
		}
		if (filter instanceof final BsonDocument document) {
			return document.isEmpty();
		}
		return BsonCodecs.toBsonDocument(filter).isEmpty();
	}

	/**
	 * Retrieves all entities of the specified type matching the conditions (raw QueryOptions variant).
	 *
	 * @param clazz   The class of the entity
	 * @param options Query options including conditions, filters, limits, etc.
	 * @return List of matching entities as Objects
	 * @throws DataAccessException if a data access error occurs
	 * @throws IOException         if an I/O error occurs
	 */
	public List<Object> getsRaw(final Class<?> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final DbClassModel model;
		try {
			model = DbClassModel.of(clazz);
		} catch (final IntrospectionException e) {
			throw new DataAccessException("Failed to introspect class: " + clazz.getSimpleName(), e);
		}
		final String deletedFieldName = model.getDeletedFieldName();
		final String collectionName = model.getTableName(options);
		final List<Object> outs = new ArrayList<>();
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		try {
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			traceFilter(filters, clazz, options);
			final Document sorts = generateSortOrNull(options);
			QueryStatistics.record(collectionName, Operation.FIND, condition.getFilter(), sorts,
					statisticSoftDeleteField(options, deletedFieldName));
			statistic.countFind++;
			FindIterable<Document> retFind = findWithFilterOrAll(collection, filters);
			if (sorts != null) {
				retFind = retFind.sort(sorts);
			}
			retFind = applySkipOption(retFind, options);
			retFind = applyLimitOption(retFind, options);
			retFind = applyProjectionForRead(retFind, clazz, options);

			LOGGER.trace("GetsWhere ...");
			readFoundDocuments(retFind, clazz, model, options, lazyCall, outs);
		} catch (final Exception ex) {
			LOGGER.error("Failed to retrieve data: {}", ex.getMessage(), ex);
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
		return outs;
	}

	/**
	 * Writes the filter of a read in the TRACE log, masked, or {@code None} when there is none.
	 *
	 * @param filters the filter of the read, may be {@code null}
	 * @param clazz   the entity class
	 * @param options the options of the read
	 */
	private static void traceFilter(final Bson filters, final Class<?> clazz, final QueryOptions options) {
		if (filters != null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("filter = {}", TraceMasker.toMaskedJson(filters, clazz, options, true));
			}
		} else {
			LOGGER.trace("filter = None");
		}
	}

	/**
	 * Starts a find on a collection: filtered, or on every document when the filter is {@code null}.
	 *
	 * @param collection the collection to read
	 * @param filters    the filter, may be {@code null}
	 * @return the find, not yet executed
	 */
	private FindIterable<Document> findWithFilterOrAll(final MongoCollection<Document> collection, final Bson filters) {
		final FindIterable<Document> retFind;
		if (filters != null) {
			retFind = findIn(collection, filters);
		} else {
			retFind = findAllIn(collection);
		}
		return retFind;
	}

	/**
	 * Applies the {@link Skip} option of a read to a find.
	 *
	 * @param retFind the find
	 * @param options the options of the read
	 * @return the find, skipping the requested number of documents
	 * @throws DataAccessException if the options hold more than one {@link Skip}
	 */
	private static FindIterable<Document> applySkipOption(
			final FindIterable<Document> retFind,
			final QueryOptions options) throws DataAccessException {
		FindIterable<Document> out = retFind;
		final List<Skip> skips = options.get(Skip.class);
		if (skips.size() == 1) {
			out = out.skip(Math.toIntExact(skips.get(0).getValue()));
		} else if (skips.size() > 1) {
			throw new DataAccessException("Request with multiple 'skip'...");
		}
		return out;
	}

	/**
	 * Applies the {@link Limit} option of a read to a find.
	 *
	 * @param retFind the find
	 * @param options the options of the read
	 * @return the find, limited to the requested number of documents
	 * @throws DataAccessException if the options hold more than one {@link Limit}
	 */
	private static FindIterable<Document> applyLimitOption(
			final FindIterable<Document> retFind,
			final QueryOptions options) throws DataAccessException {
		FindIterable<Document> out = retFind;
		final List<Limit> limits = options.get(Limit.class);
		if (limits.size() == 1) {
			out = out.limit(Math.toIntExact(limits.get(0).getValue()));
		} else if (limits.size() > 1) {
			throw new DataAccessException("Request with multiple 'limit'...");
		}
		return out;
	}

	/**
	 * Restricts a find to the fields to read of the entity, and to {@code _id}.
	 *
	 * @param retFind the find
	 * @param clazz   the entity class
	 * @param options the options of the read
	 * @return the find, projected
	 * @throws Exception if the fields to read cannot be determined
	 */
	private FindIterable<Document> applyProjectionForRead(
			final FindIterable<Document> retFind,
			final Class<?> clazz,
			final QueryOptions options) throws Exception {
		// Select values to read
		final List<String> listFields = generateSelectField(clazz, options);
		listFields.add("_id");
		return retFind.projection(Projections.include(listFields.toArray(new String[0])));
	}

	/**
	 * Runs a find, converts each document to an entity and runs the deferred loads of their relations.
	 *
	 * <p>The entities are added to {@code outs}; the cursor is closed on return. An entity whose relations
	 * cannot be expanded (depth limit, cycle) is converted with its relation fields left unloaded.
	 *
	 * @param retFind  the find, not yet executed
	 * @param clazz    the entity class
	 * @param model    the model of the entity
	 * @param options  the options of the read
	 * @param lazyCall the deferred loads, completed then run
	 * @param outs     the entities read, completed
	 * @throws Exception if the read or a conversion fails
	 */
	private void readFoundDocuments(
			final FindIterable<Document> retFind,
			final Class<?> clazz,
			final DbClassModel model,
			final QueryOptions options,
			final List<LazyGetter> lazyCall,
			final List<Object> outs) throws Exception {
		final LazyGetterCollector batchCollector = new LazyGetterCollector();
		final DbPropertyDescriptor pkDesc = model.getPrimaryKey();
		final String pkColumn = pkDesc != null ? pkDesc.getFieldName(options).inTable() : null;
		final List<Object> expandedKeys = new ArrayList<>();
		final MongoCursor<Document> cursor = retFind.iterator();
		try (cursor) {
			while (cursor.hasNext()) {
				final Document doc = cursor.next();
				outs.add(createObjectFromFoundDocument(doc, pkColumn, clazz, options, lazyCall, batchCollector,
						expandedKeys));
			}
			runBatchedLazyActions(clazz, lazyCall, batchCollector, expandedKeys);
		}
	}

	/**
	 * Converts one document of a find to an entity.
	 *
	 * @param doc            the document
	 * @param pkColumn       the name of the primary key in the document, {@code null} when the entity has none
	 * @param clazz          the entity class
	 * @param options        the options of the read
	 * @param lazyCall       the deferred loads, completed when the relations are expanded
	 * @param batchCollector the collector of the batched entity-reference loads, completed when the relations
	 *                       are expanded
	 * @param expandedKeys   the primary keys of the entities whose relations are expanded, completed
	 * @return the entity
	 * @throws Exception if the conversion fails
	 */
	private Object createObjectFromFoundDocument(
			final Document doc,
			final String pkColumn,
			final Class<?> clazz,
			final QueryOptions options,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector,
			final List<Object> expandedKeys) throws Exception {
		// Guarded: the JSON rendering is costly and must not run when TRACE is off.
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(" - receive data from DB: {}", TraceMasker.toMaskedJson(doc, clazz, options, false));
		}
		final Object primaryKey = pkColumn != null ? doc.get(pkColumn) : null;
		final Object data;
		if (RelationExpansion.canExpand(clazz, primaryKey)) {
			data = createObjectFromDocument(doc, clazz, options, lazyCall, batchCollector);
			if (primaryKey != null) {
				expandedKeys.add(primaryKey);
			}
		} else {
			// Depth limit reached, or entity already expanded on this chain (cycle): its relation
			// fields are left unloaded.
			data = createObjectFromDocument(doc, clazz, options, new ArrayList<>(), null);
		}
		return data;
	}

	/**
	 * Runs the deferred loads of a read: the batched entity-reference loads (grouped by target entity) are
	 * added to the others, and all run inside the expansion chain of the entities read.
	 *
	 * @param clazz          the entity class
	 * @param lazyCall       the deferred loads, completed then run
	 * @param batchCollector the collector of the batched entity-reference loads
	 * @param expandedKeys   the primary keys of the entities whose relations were expanded
	 * @throws Exception if a deferred load fails
	 */
	private void runBatchedLazyActions(
			final Class<?> clazz,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector,
			final List<Object> expandedKeys) throws Exception {
		// Add batched lazy getters (entity-reference fields grouped by target entity)
		if (!batchCollector.isEmpty()) {
			lazyCall.addAll(batchCollector.buildLazyGetters(this));
		}
		if (!lazyCall.isEmpty()) {
			final List<String> chain = RelationExpansion.enter(clazz, expandedKeys);
			try {
				runLazyActions(lazyCall);
			} finally {
				RelationExpansion.leave(chain);
			}
		}
	}

	/**
	 * Creates a Java object from a MongoDB document using the entity class model and codecs.
	 *
	 * <p>Handles POJO construction, add-on fields (ManyToOne, OneToMany, etc.), and type overrides.
	 *
	 * @param doc            The MongoDB document (or scalar value) to convert
	 * @param type           The target Java type (Class or ParameterizedType)
	 * @param options        Query options affecting field selection and type overrides
	 * @param lazyCall       List to collect deferred entity-reference loading actions
	 * @param batchCollector Collector for batching entity-reference loads across rows
	 * @return The constructed Java object, or null if the document is null
	 * @throws Exception if object construction fails
	 */
	public Object createObjectFromDocument(
			final Object doc,
			final Type type,
			final QueryOptions options,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector) throws Exception {
		if (doc == null) {
			return null;
		}
		// @DataNotRead fields of embedded sub-objects are read only with ReadAllColumn, as at the top level.
		final boolean previousReadAll = MongoCodecFactory
				.setSubObjectReadAllColumn(QueryOptions.readAllColumn(options));
		try {
			return createObjectFromDocumentInternal(doc, type, options, lazyCall, batchCollector);
		} finally {
			MongoCodecFactory.setSubObjectReadAllColumn(previousReadAll);
		}
	}

	private Object createObjectFromDocumentInternal(
			final Object doc,
			final Type type,
			final QueryOptions options,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector) throws Exception {
		// For non-Class types (ParameterizedType: List, Set, Map, etc.) use pre-compiled codec
		if (!(type instanceof final Class<?> clazz)) {
			return MongoCodecFactory.buildReader(TypeInfo.fromType(type)).fromMongo(doc);
		}
		// For scalar types (non-Document), use pre-compiled codec
		if (!(doc instanceof final Document documentModel)) {
			return MongoCodecFactory.buildReader(TypeInfo.ofRaw(clazz)).fromMongo(doc);
		}
		// POJO path: Document → Java object with AddOn/OptionSpecifyType support
		final DbClassModel dbModel = DbClassModel.of(clazz);
		// Same selector as the projection: a field excluded from the transfer is not mapped, and an
		// excluded AddOn field does not trigger the extra queries its resolution would have needed.
		final ReadFieldSelector selector = ReadFieldSelector.of(dbModel, options);

		// Create instance via ClassModel's default constructor
		final Object data = dbModel.getClassModel().newInstance();

		for (final DbPropertyDescriptor desc : dbModel.getAllFields()) {
			if (!selector.isRead(desc, options)) {
				continue;
			}
			if (desc.getAction() == DbFieldAction.ADDON) {
				desc.getAddOn().fillFromDoc(this, documentModel, desc, data, options, lazyCall, batchCollector);
			} else {
				final MongoFieldCodec codec = desc.getCodec();
				if (codec == null) {
					continue;
				}
				final FieldName fieldName = desc.getFieldName(options);

				// OptionSpecifyType override (rare runtime type change): the same rule as the projection.
				final TypeInfo overrideType = ReadFieldSelector.overriddenReadType(desc, options);
				if (overrideType != null) {
					// The value was written by the declared type: the sub-fields it protects are removed before the
					// other class decodes it (that class may declare such a sub-field without protection).
					final Document restricted = documentModel.containsKey(fieldName.inTable())
							? new Document(fieldName.inTable(),
									selector.restrictToDeclaredType(desc, documentModel.get(fieldName.inTable())))
							: documentModel;
					codec.readFromDoc(restricted, fieldName.inTable(), MongoCodecFactory.buildReader(overrideType),
							data);
				} else {
					// Fast path: pre-compiled codec
					codec.readFromDoc(documentModel, fieldName.inTable(), data);
				}
			}
		}
		return data;
	}

	/**
	 * Counts the number of entities matching the specified conditions (QueryOptions variant).
	 *
	 * @param clazz   The class of the entity
	 * @param options Query options including conditions, filters, etc.
	 * @return The number of matching entities
	 * @throws Exception if count operation fails
	 */
	public long count(final Class<?> clazz, final QueryOptions options) throws Exception {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final DbClassModel model = DbClassModel.of(clazz);
		final String deletedFieldName = model.getDeletedFieldName();
		final String collectionName = model.getTableName(options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		try {
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			QueryStatistics.record(collectionName, Operation.COUNT, condition.getFilter(), null,
					statisticSoftDeleteField(options, deletedFieldName));
			statistic.countCountDocuments++;
			if (filters != null) {
				return countIn(collection, filters);
			}
			return countAllIn(collection);
		} catch (final Exception ex) {
			LOGGER.error("Failed to count documents: {}", ex.getMessage(), ex);
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
	}

	/**
	 * Performs a hard (physical) delete of an entity by its unique identifier.
	 *
	 * <p>
	 * <strong>Warning:</strong> This permanently removes the entity from the database,
	 * even when the class has a {@code @DataDeleted} field.
	 * The data cannot be recovered after this operation.
	 * </p>
	 *
	 * <p>
	 * Three facts about the deleted field: a soft-deleted entity is not matched unless the call carries
	 * {@link ForceHardDelete} or {@link AccessDeletedItems}; a class with a {@code @DataAsyncHardDeleted} field
	 * does not lose the document, the call only sets its flags (and its deleted field) unless
	 * {@link ForceHardDelete} is passed; and {@link ForceHardDelete} adds {@link AccessDeletedItems} itself.
	 * </p>
	 *
	 * @param <ID_TYPE> The type of the ID
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier of the entity to delete
	 * @param option    Optional query options
	 * @return Number of entities deleted (typically 0 or 1)
	 * @throws Exception if delete operation fails
	 */
	public <ID_TYPE> long deleteHardById(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteHard(clazz, options.getAllArray());
	}

	/**
	 * Executes pre-delete actions for add-on fields (e.g., cascade deletes, link cleanup).
	 *
	 * @param clazz  The entity class being deleted
	 * @param option Query options used to locate the entities being deleted
	 * @throws Exception if any pre-delete action fails
	 */
	public void actionOnDelete(final Class<?> clazz, final QueryOption... option) throws Exception {
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final DbClassModel model = DbClassModel.of(clazz);
		// Use pre-computed delete action fields
		final List<DbPropertyDescriptor> deleteActionFields = model.getDeleteActionFields();
		List<Object> previousData = null;
		if (model.needsPreviousDataForDelete()) {
			final QueryOptions options = new QueryOptions(option);
			options.add(new AccessDeletedItems());
			options.add(new ReadAllColumn());
			previousData = this.getsRaw(clazz, options);
		}
		for (final DbPropertyDescriptor desc : deleteActionFields) {
			desc.getAddOn().onDelete(this, clazz, desc, previousData, lazyCall);
		}
		runLazyActions(lazyCall);
	}

	/**
	 * Performs a hard (physical) delete of entities matching the specified conditions.
	 *
	 * <p>
	 * <strong>Warning:</strong> This permanently removes matching entities from the database,
	 * even when the class has a {@code @DataDeleted} field.
	 * The data cannot be recovered after this operation.
	 * </p>
	 *
	 * <p>
	 * Three facts about the deleted field: a soft-deleted entity is not matched unless the call carries
	 * {@link ForceHardDelete} or {@link AccessDeletedItems}; a class with a {@code @DataAsyncHardDeleted} field
	 * does not lose the document, the call only sets its flags (and its deleted field) unless
	 * {@link ForceHardDelete} is passed; and {@link ForceHardDelete} adds {@link AccessDeletedItems} itself.
	 * </p>
	 *
	 * <p>
	 * <strong>Required:</strong> You MUST provide conditions via {@link Condition} option.
	 * Attempting to delete without conditions will throw an exception to prevent accidental
	 * deletion of all records.
	 * </p>
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions (required)
	 * @return Number of entities deleted
	 * @throws Exception if delete operation fails or no conditions provided
	 */
	public long deleteHard(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		// ForceHardDelete implies AccessDeletedItems: when physically removing records,
		// we must be able to find already soft-deleted records.
		if (options.exist(ForceHardDelete.class) && !options.exist(AccessDeletedItems.class)) {
			options.add(new AccessDeletedItems());
		}
		final Condition condition = conditionFusionOrEmpty(options, true);
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName(options);
		final String deletedFieldName = model.getDeletedFieldName();
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = matchAllIfNull(condition.getFilter(collectionName, options, deletedFieldName));
		QueryStatistics.record(collectionName, Operation.DELETE, condition.getFilter(), null,
				statisticSoftDeleteField(options, deletedFieldName));

		// If the entity has an @DataAsyncHardDeleted field and ForceHardDelete is not requested,
		// perform an async hard delete (set hardDeleted=true and deleted=true) instead of physical removal.
		final String asyncHardDeletedFieldName = model.getAsyncHardDeletedFieldName();
		if (asyncHardDeletedFieldName != null && !options.exist(ForceHardDelete.class)) {
			final DbPropertyDescriptor updateTsDesc = model.getUpdateTimestamp();
			final Document setFields = new Document(asyncHardDeletedFieldName, true);
			if (deletedFieldName != null) {
				setFields.append(deletedFieldName, true);
			}
			if (updateTsDesc != null) {
				setFields.append(updateTsDesc.getDbFieldName(), Date.from(Instant.now()));
			}
			final Document actions = new Document("$set", setFields);
			statistic.countUpdateMany++;
			final UpdateResult ret = updateManyIn(collection, filters, actions);
			return ret.getModifiedCount();
		}

		actionOnDelete(clazz, option);

		statistic.countDeleteMany++;
		final DeleteResult retFind = deleteManyIn(collection, filters);
		return retFind.getDeletedCount();
	}

	/**
	 * Performs a soft delete of an entity by its unique identifier.
	 *
	 * <p>
	 * Soft delete marks the entity as deleted without physically removing it from the database.
	 * The entity will be excluded from normal queries unless {@code AccessDeletedItems} option is used.
	 * </p>
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity class must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * @param <ID_TYPE> The type of the ID
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier of the entity to soft delete
	 * @param option    Optional query options
	 * @return Number of entities soft deleted (typically 0 or 1)
	 * @throws Exception if soft delete operation fails
	 */
	public <ID_TYPE> long deleteSoftById(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteSoft(clazz, options.getAllArray());
	}

	/**
	 * Performs a soft delete of entities matching the specified conditions.
	 *
	 * <p>
	 * Soft delete marks matching entities as deleted without physically removing them from the database.
	 * These entities will be excluded from normal queries unless {@code AccessDeletedItems} option is used.
	 * </p>
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity class must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * <strong>Required:</strong> You MUST provide conditions via {@link Condition} option.
	 * </p>
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions (required)
	 * @return Number of entities soft deleted
	 * @throws Exception if soft delete operation fails or no conditions provided
	 */
	public long deleteSoft(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName(options);
		final String deletedFieldName = model.getDeletedFieldName();
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = matchAllIfNull(condition.getFilter(collectionName, options, deletedFieldName));
		QueryStatistics.record(collectionName, Operation.DELETE_SOFT, condition.getFilter(), null,
				statisticSoftDeleteField(options, deletedFieldName));
		final DbPropertyDescriptor updateTsDesc = model.getUpdateTimestamp();
		Document actions = null;
		if (updateTsDesc == null) {
			actions = new Document("$set", new Document(deletedFieldName, true));
		} else {
			actions = new Document("$set", new Document(deletedFieldName, true).append(updateTsDesc.getDbFieldName(),
					Date.from(Instant.now())));
		}
		actionOnDelete(clazz, option);
		statistic.countUpdateMany++;
		final UpdateResult ret = updateManyIn(collection, filters, actions);
		return ret.getModifiedCount();
	}

	/**
	 * Clears the soft delete flag of the entity of this identifier, when the call matches it: without option it
	 * does not match a soft-deleted entity, see below.
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity class must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * This overload takes no option, so it cannot carry {@link AccessDeletedItems}: its filter keeps the
	 * entities whose deleted field is false. A soft-deleted entity is not matched and stays deleted. An entity
	 * that is not deleted is matched: its flag is written to false again and its {@code @UpdateTimestamp} date
	 * is refreshed. To restore a soft-deleted entity, use {@link #restoreById(Class, Object, QueryOption...)}
	 * with {@code new AccessDeletedItems()}.
	 * </p>
	 *
	 * @param <ID_TYPE> The type of the ID
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier of the entity to restore
	 * @return the number of documents modified: 0 for a soft-deleted entity, see above
	 * @throws DataAccessException if the class has no deleted field or restore fails
	 */
	public <ID_TYPE> long restoreById(final Class<?> clazz, final ID_TYPE id) throws DataAccessException {
		return restore(clazz, new Condition(getTableIdCondition(clazz, id, new QueryOptions())));
	}

	/**
	 * Restores (un-deletes) a soft-deleted entity by its unique identifier.
	 *
	 * <p>
	 * This method clears the soft delete flag, making the entity visible in normal queries again.
	 * Only works for entities that were previously soft deleted.
	 * </p>
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity class must have a field annotated {@code @DataDeleted}.
	 * </p>
	 *
	 * <p>
	 * The soft-delete filter applies to a restore like to any other write: a document whose deleted field is
	 * {@code true} matches only when the call carries {@link AccessDeletedItems}. Without it, no soft-deleted
	 * document matches, so the call restores nothing and returns 0.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Soft delete a task
	 * db.deleteSoftById(Task.class, taskId);
	 *
	 * // Later, restore it: the option is needed to match the soft-deleted task
	 * long restored = db.restoreById(Task.class, taskId, new AccessDeletedItems());
	 *
	 * // The task is visible in normal queries again
	 * Task task = db.getById(Task.class, taskId);
	 * </pre>
	 *
	 * @param <ID_TYPE> The type of the ID (e.g., ObjectId, UUID, Long)
	 * @param clazz     The class of the entity
	 * @param id        The unique identifier of the entity to restore (e.g., ObjectId)
	 * @param option    Optional query options
	 * @return Number of entities restored (typically 0 or 1)
	 * @throws DataAccessException if the class has no deleted field or restore fails
	 */
	public <ID_TYPE> long restoreById(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return restore(clazz, options.getAllArray());
	}

	/**
	 * Restores (un-deletes) soft-deleted entities matching the specified conditions.
	 *
	 * <p>
	 * This method clears the soft delete flag for matching entities, making them visible
	 * in normal queries again. Only works for entities that were previously soft deleted.
	 * </p>
	 *
	 * <p>
	 * <strong>Requirement:</strong> the entity class must have a field annotated {@code @DataDeleted}.
	 * You MUST provide conditions via {@link Condition} option.
	 * </p>
	 *
	 * <p>
	 * The soft-delete filter applies here like to any other write: a document whose deleted field is
	 * {@code true} matches only when the call carries {@link AccessDeletedItems}. Without it, no soft-deleted
	 * document matches, so the call restores nothing and returns 0.
	 * </p>
	 *
	 * @param clazz  The class of the entity
	 * @param option Query options including conditions (required) and {@link AccessDeletedItems}
	 * @return Number of entities restored
	 * @throws DataAccessException if the class has no deleted field, no conditions provided, or restore fails
	 */
	public long restore(final Class<?> clazz, final QueryOption... option) throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final DbClassModel model;
		try {
			model = DbClassModel.of(clazz);
		} catch (final IntrospectionException e) {
			throw new DataAccessException("Failed to introspect class: " + clazz.getSimpleName(), e);
		}
		final String collectionName = model.getTableName(options);
		final String deletedFieldName = model.getDeletedFieldName();
		if (deletedFieldName == null) {
			throw new DataAccessException("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = matchAllIfNull(condition.getFilter(collectionName, options, deletedFieldName));
		QueryStatistics.record(collectionName, Operation.RESTORE, condition.getFilter(), null,
				statisticSoftDeleteField(options, deletedFieldName));
		// A change like a soft delete: what follows the update date, a delta backup first, has to see it.
		final Document fields = new Document(deletedFieldName, false);
		final DbPropertyDescriptor updateTsDesc = model.getUpdateTimestamp();
		if (updateTsDesc != null) {
			fields.append(updateTsDesc.getDbFieldName(), Date.from(Instant.now()));
		}
		final Document actions = new Document("$set", fields);
		statistic.countUpdateMany++;
		final UpdateResult ret = updateManyIn(collection, filters, actions);
		return ret.getModifiedCount();
	}

	/**
	 * Drops (deletes) the entire collection for the specified entity class.
	 *
	 * <p>
	 * <strong>Warning:</strong> This permanently removes the collection and all its documents.
	 * This operation cannot be undone. Use with extreme caution.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Drop the entire tasks collection
	 * db.drop(Task.class);
	 * </pre>
	 *
	 * @param clazz  The entity class whose collection will be dropped
	 * @param option Optional query options (e.g., table name override)
	 * @throws Exception if drop operation fails
	 */
	public void drop(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = DbClassModel.of(clazz).getTableName(options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		statistic.countDrop++;
		collection.drop();
	}

	/**
	 * Deletes all documents from the collection for the specified entity class.
	 *
	 * <p>
	 * Unlike {@link #drop(Class, QueryOption...)}, this keeps the collection structure
	 * (indexes, schema) but removes all documents. This is a hard delete that cannot be undone.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Remove all the task documents but keep the collection
	 * db.cleanAll(Task.class);
	 * </pre>
	 *
	 * @param clazz  The entity class whose documents will be deleted
	 * @param option Optional query options (e.g., table name override)
	 * @throws Exception if clean operation fails
	 */
	public void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = DbClassModel.of(clazz).getTableName(options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		statistic.countDeleteMany++;
		deleteManyIn(collection, new Document());
	}

	// =======================================================================
	// -- BSON Direct Access API
	// =======================================================================

	/**
	 * Inserts a BSON Document directly into the specified collection.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and inserts a raw BSON document
	 * directly into MongoDB. This is useful for advanced use cases where you need
	 * full control over the document structure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * Document doc = new Document()
	 *     .append("_id", new ObjectId())
	 *     .append("name", "John Doe")
	 *     .append("email", "john@example.com")
	 *     .append("age", 30);
	 *
	 * ObjectId insertedId = db.insertBsonDocument("users", doc);
	 * </pre>
	 *
	 * @param collectionName Name of the collection to insert into
	 * @param document       BSON Document to insert
	 * @return The ObjectId of the inserted document (auto-generated if not provided)
	 * @throws DataAccessException if insertion fails
	 */
	public ObjectId insertBsonDocument(final String collectionName, final Document document)
			throws DataAccessException {
		try {
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			statistic.countInsertOne++;
			final InsertOneResult result = insertOneIn(collection, document);
			final Object insertedId = result.getInsertedId();
			if (insertedId != null && insertedId instanceof org.bson.BsonObjectId) {
				return ((org.bson.BsonObjectId) insertedId).getValue();
			}
			// If no ID was returned, check if the document has one
			final Object docId = document.get("_id");
			if (docId instanceof ObjectId) {
				return (ObjectId) docId;
			}
			throw new DataAccessException("Failed to retrieve inserted document ID");
		} catch (final DataAccessException ex) {
			throw ex;
		} catch (final Exception ex) {
			LOGGER.error("Failed to insert BSON document: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to insert BSON document: " + ex.getMessage());
		}
	}

	/**
	 * Retrieves a single BSON Document from the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and returns a raw BSON document
	 * directly from MongoDB. This is useful for advanced use cases where you need
	 * full control over the document structure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
	 *
	 * // Get document by ID
	 * Document userDoc = db.getBsonDocument("users",
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
	 * @throws DataAccessException if retrieval fails
	 */
	public Document getBsonDocument(final String collectionName, final QueryOption... options)
			throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			final Condition condition = conditionFusionOrEmpty(queryOptions, false);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			QueryStatistics.record(collectionName, Operation.FIND, condition.getFilter(), null, null);
			statistic.countFind++;
			final FindIterable<Document> cursor;
			if (filters != null) {
				cursor = findIn(collection, filters);
			} else {
				cursor = findAllIn(collection);
			}
			try (MongoCursor<Document> iterator = cursor.iterator()) {
				if (iterator.hasNext()) {
					return iterator.next();
				}
			}
			return null;
		} catch (final Exception ex) {
			LOGGER.error("Failed to retrieve BSON document: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to retrieve BSON document: " + ex.getMessage());
		}
	}

	/**
	 * Retrieves multiple BSON Documents from the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and returns raw BSON documents
	 * directly from MongoDB. This is useful for advanced use cases where you need
	 * full control over the document structure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * <pre>
	 * // Get the 10 first users older than 25, ordered by name
	 * List&lt;Document&gt; users = db.getBsonDocuments("users",
	 *     new Condition(Filters.gt("age", 25)),
	 *     new OrderBy(new OrderItem("name", OrderItem.Order.ASC)),
	 *     new Limit(10));
	 * </pre>
	 *
	 * @param collectionName Name of the collection to query
	 * @param options        Query options including conditions, ordering, limits
	 * @return List of matching BSON Documents (empty list if none found)
	 * @throws DataAccessException if retrieval fails
	 */
	public List<Document> getBsonDocuments(final String collectionName, final QueryOption... options)
			throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			final Condition condition = conditionFusionOrEmpty(queryOptions, false);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			final Document sorts = generateSortOrNull(queryOptions);
			QueryStatistics.record(collectionName, Operation.FIND, condition.getFilter(), sorts, null);
			statistic.countFind++;
			FindIterable<Document> cursor;
			if (filters != null) {
				cursor = findIn(collection, filters);
			} else {
				cursor = findAllIn(collection);
			}

			// Apply ordering
			if (sorts != null) {
				cursor = cursor.sort(sorts);
			}

			// Apply limit
			final List<Limit> limits = queryOptions.get(Limit.class);
			if (!limits.isEmpty()) {
				cursor = cursor.limit(Math.toIntExact(limits.get(0).getValue()));
			}

			final List<Document> results = new ArrayList<>();
			try (MongoCursor<Document> iterator = cursor.iterator()) {
				while (iterator.hasNext()) {
					results.add(iterator.next());
				}
			}
			return results;
		} catch (final RuntimeException ex) {
			LOGGER.error("Failed to retrieve BSON documents: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to retrieve BSON documents: " + ex.getMessage());
		}
	}

	/**
	 * Updates BSON Documents in the specified collection matching the given conditions.
	 *
	 * <p>
	 * This method bypasses the object mapping layer and updates documents directly
	 * using MongoDB update operators. This is useful for advanced use cases where you need
	 * full control over the update operation.
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
	 * long count = db.updateBsonDocuments("users", updateOps,
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
	 *                       missing or empty condition is refused unless {@link AllowMassOperation} is given
	 * @return Number of documents modified
	 * @throws DataAccessException if update fails, or if the condition is missing or empty without
	 *                             {@link AllowMassOperation}
	 */
	public long updateBsonDocuments(
			final String collectionName,
			final Document updateDocument,
			final QueryOption... options) throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			// A missing or empty condition is refused, unless AllowMassOperation.
			final Condition condition = conditionFusionOrEmpty(queryOptions, true);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			QueryStatistics.record(collectionName, Operation.UPDATE, condition.getFilter(), null, null);
			statistic.countUpdateMany++;
			if (filters != null) {
				final UpdateResult result = updateManyIn(collection, filters, updateDocument);
				return result.getModifiedCount();
			}
			// No filter: AllowMassOperation was given, update all documents
			final UpdateResult result = updateManyIn(collection, new Document(), updateDocument);
			return result.getModifiedCount();
		} catch (final Exception ex) {
			LOGGER.error("Failed to update BSON documents: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to update BSON documents: " + ex.getMessage());
		}
	}

}
