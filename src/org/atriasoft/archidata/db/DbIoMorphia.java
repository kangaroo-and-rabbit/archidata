package org.atriasoft.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.atriasoft.archidata.converter.morphia.SqlTimestampCodec;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.Datastore;
import dev.morphia.Morphia;

public class DbIoMorphia extends DbIo implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(DbIoMorphia.class);
	private MongoClient mongoClient = null;
	private Datastore datastore = null;

	public DbIoMorphia(final DbConfig config) throws IOException {
		super(config);
	}

	public Datastore getDatastore() {
		return this.datastore;
	}

	public MongoClient getClient() {
		return this.mongoClient;
	}

	@Override
	synchronized public void closeImplement() throws IOException {
		this.mongoClient.close();
		this.mongoClient = null;
		this.datastore = null;
	}

	@Override
	synchronized public void openImplement() throws IOException {
		final Class<?>[] classes = this.config.getClasses().toArray(new Class<?>[0]);
		final String dbUrl = this.config.getUrl();
		final String dbName = this.config.getDbName();
		// Connect to MongoDB (simple form):
		// final MongoClient mongoClient = MongoClients.create(dbUrl);
		LOGGER.info("Connect on the DB: {}", dbUrl);
		// Connect to MongoDB (complex form):
		final ConnectionString connectionString = new ConnectionString(dbUrl);
		// Créer un CodecRegistry pour UUID
		//final CodecRegistry uuidCodecRegistry = CodecRegistries.fromCodecs(new UUIDCodec());
		final CodecRegistry SqlTimestampCodecRegistry = CodecRegistries.fromCodecs(new SqlTimestampCodec());
		// Créer un CodecRegistry pour POJOs
		final CodecRegistry pojoCodecRegistry = CodecRegistries
				.fromProviders(PojoCodecProvider.builder().automatic(true).build());
		// Ajouter le CodecRegistry par défaut, le codec UUID et celui pour POJOs
		//final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
		//		MongoClientSettings.getDefaultCodecRegistry(), /*uuidCodecRegistry, */ pojoCodecRegistry);

		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(new org.bson.codecs.UuidCodec(UuidRepresentation.STANDARD)),
				pojoCodecRegistry, SqlTimestampCodecRegistry);
		// Configurer MongoClientSettings
		final MongoClientSettings clientSettings = MongoClientSettings.builder() //
				.applyConnectionString(connectionString)//
				.codecRegistry(codecRegistry) //
				.uuidRepresentation(UuidRepresentation.STANDARD)//
				.build();
		this.mongoClient = MongoClients.create(clientSettings);
		this.datastore = Morphia.createDatastore(this.mongoClient, dbName);
		// Map entities
		this.datastore.getMapper().map(classes);
		// Ensure indexes
		this.datastore.ensureIndexes();
	}
}
