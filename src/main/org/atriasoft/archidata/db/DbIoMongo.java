package org.atriasoft.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atriasoft.archidata.converter.mongo.OffsetDateTimeCodec;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;

/**
 * MongoDB-specific implementation of {@link DbIo} that manages a shared
 * {@link MongoClient} and its associated {@link MongoDatabase}.
 *
 * <p>
 * The {@link MongoClient} is cached per connection URL and shared across all
 * {@code DbIoMongo} instances pointing at the same server. This matches the
 * MongoDB Java driver guidance ("a single MongoClient instance is sufficient
 * for an entire application") and avoids the per-request topology discovery
 * overhead that occurred when each {@code DbIoMongo} owned its own client.
 * </p>
 *
 * <p>
 * Reconnection after a MongoDB outage is handled natively by the driver: the
 * cluster monitor detects state changes, the pool evicts dead connections, and
 * {@code retryWrites}/{@code retryReads} retry in-flight operations once the
 * primary becomes available again. State transitions are logged via a
 * {@link ClusterListener}.
 * </p>
 *
 * <p>
 * Use {@link #forceReconnect()} to drop the cached clients (admin lever for
 * the rare case where the driver fails to recover).
 * </p>
 */
public class DbIoMongo extends DbIo implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbIoMongo.class);

	/** Shared {@link MongoClient} pool keyed by the resolved connection URL. */
	private static final ConcurrentHashMap<String, MongoClient> CLIENT_CACHE = new ConcurrentHashMap<>();
	private static final AtomicBoolean SHUTDOWN_HOOK_INSTALLED = new AtomicBoolean(false);

	private MongoClient mongoClient = null;
	private MongoDatabase dataBase = null;

	/**
	 * Constructs a DbIoMongo with the given database configuration.
	 *
	 * @param config the database configuration
	 * @throws IOException if initialization fails
	 */
	public DbIoMongo(final DbConfig config) throws IOException {
		super(config);
	}

	/**
	 * Returns the MongoDB database instance.
	 *
	 * @return the {@link MongoDatabase} instance
	 */
	public MongoDatabase getDatabase() {
		return this.dataBase;
	}

	/**
	 * Returns the underlying MongoDB client.
	 *
	 * @return the {@link MongoClient} instance
	 */
	public MongoClient getClient() {
		return this.mongoClient;
	}

	/**
	 * Releases the local references to the shared {@link MongoClient}.
	 *
	 * <p>
	 * The shared client itself is NOT closed here — it is kept alive for reuse
	 * by subsequent {@code DbIoMongo} instances. Use {@link #shutdownAll()} or
	 * {@link #forceReconnect()} to actually close the cached clients.
	 * </p>
	 */
	@Override
	synchronized public void closeImplement() throws IOException {
		this.mongoClient = null;
		this.dataBase = null;
	}

	/**
	 * Opens a MongoDB connection by reusing the shared {@link MongoClient} for
	 * the configured URL, or creating it on first use.
	 *
	 * @throws IOException if the connection cannot be established
	 */
	@Override
	synchronized public void openImplement() throws IOException {
		final String dbUrl = this.config.getUrl();
		final String dbName = this.config.getDbName();
		this.mongoClient = CLIENT_CACHE.computeIfAbsent(dbUrl, url -> createSharedClient(this.config));
		if (dbName == null) {
			LOGGER.error("Connect on the DB: host:{} port:{}", this.config.getHostname(), this.config.getPort());
		}
		this.dataBase = this.mongoClient.getDatabase(dbName);
		installShutdownHookOnce();
	}

	private static MongoClient createSharedClient(final DbConfig config) {
		final ConnectionString connectionString = new ConnectionString(config.getUrl());
		final CodecRegistry offsetDateTimeCodecRegistry = CodecRegistries.fromCodecs(new OffsetDateTimeCodec());
		final CodecRegistry pojoCodecRegistry = CodecRegistries
				.fromProviders(PojoCodecProvider.builder().automatic(true).build());
		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(//
				MongoClientSettings.getDefaultCodecRegistry(), //
				CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)), //
				pojoCodecRegistry, //
				offsetDateTimeCodecRegistry);
		final MongoClientSettings clientSettings = MongoClientSettings.builder() //
				.applyConnectionString(connectionString) //
				.codecRegistry(codecRegistry) //
				.uuidRepresentation(UuidRepresentation.STANDARD) //
				.applyToConnectionPoolSettings(builder -> builder //
						.maxSize(200) //
						.minSize(10) //
						.maxWaitTime(5000, TimeUnit.MILLISECONDS) //
						.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS) //
						.maxConnectionLifeTime(300000, TimeUnit.MILLISECONDS)) //
				.applyToClusterSettings(builder -> builder.addClusterListener(new ClusterStateLogger())) //
				.build();
		LOGGER.info("Creating shared MongoClient for host:{} port:{} dbName:{}", config.getHostname(), config.getPort(),
				config.getDbName());
		return MongoClients.create(clientSettings);
	}

	private static void installShutdownHookOnce() {
		if (SHUTDOWN_HOOK_INSTALLED.compareAndSet(false, true)) {
			Runtime.getRuntime().addShutdownHook(new Thread(DbIoMongo::shutdownAll, "DbIoMongo-shutdown"));
		}
	}

	/**
	 * Closes every cached {@link MongoClient}. Intended for JVM shutdown only.
	 */
	public static void shutdownAll() {
		final List<MongoClient> toClose = new ArrayList<>(CLIENT_CACHE.values());
		CLIENT_CACHE.clear();
		for (final MongoClient client : toClose) {
			try {
				client.close();
			} catch (final Exception e) {
				LOGGER.warn("Error closing shared MongoClient: {}", e.getMessage());
			}
		}
	}

	/**
	 * Drops every cached {@link MongoClient} so the next {@code openImplement()}
	 * recreates a fresh one. Use as an admin lever when the driver fails to
	 * recover from a MongoDB outage on its own.
	 */
	public static synchronized void forceReconnect() {
		LOGGER.warn("Force-reconnect requested: dropping {} cached MongoClient(s)", CLIENT_CACHE.size());
		shutdownAll();
	}

	/**
	 * Returns the number of currently cached shared {@link MongoClient} instances.
	 *
	 * @return the count of live shared clients (typically 1 in single-DB setups)
	 */
	public static int getSharedClientCount() {
		return CLIENT_CACHE.size();
	}

	/**
	 * Returns a short human-readable description of every cached cluster, for
	 * health-check / heartbeat logs.
	 */
	public static String describeSharedClusters() {
		if (CLIENT_CACHE.isEmpty()) {
			return "no-shared-client";
		}
		final StringBuilder sb = new StringBuilder();
		for (final Map.Entry<String, MongoClient> entry : CLIENT_CACHE.entrySet()) {
			final ClusterDescription desc = entry.getValue().getClusterDescription();
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append(desc.getType()).append('(').append(desc.getServerDescriptions().size()).append(" servers)");
		}
		return sb.toString();
	}

	/**
	 * Cluster listener that logs every state transition. Lets operators see in
	 * the application logs when MongoDB drops and comes back up, instead of
	 * having to dig in driver-internal monitor traces.
	 */
	private static final class ClusterStateLogger implements ClusterListener {
		@Override
		public void clusterDescriptionChanged(final ClusterDescriptionChangedEvent event) {
			final ClusterType prev = event.getPreviousDescription().getType();
			final ClusterType next = event.getNewDescription().getType();
			if (prev == next) {
				return;
			}
			if (next == ClusterType.UNKNOWN) {
				LOGGER.warn("MongoDB cluster lost ({} -> {}): {}", prev, next,
						event.getNewDescription().getShortDescription());
			} else {
				LOGGER.info("MongoDB cluster up ({} -> {}): {}", prev, next,
						event.getNewDescription().getShortDescription());
			}
		}
	}
}
