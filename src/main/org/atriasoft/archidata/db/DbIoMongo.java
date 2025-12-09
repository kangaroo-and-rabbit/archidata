package org.atriasoft.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.atriasoft.archidata.converter.morphia.OffsetDateTimeCodec;
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
import com.mongodb.client.MongoDatabase;

public class DbIoMongo extends DbIo implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(DbIoMongo.class);
	private MongoClient mongoClient = null;
	private MongoDatabase dataBase = null;

	public DbIoMongo(final DbConfig config) throws IOException {
		super(config);
	}

	public MongoDatabase getDatabase() {
		return this.dataBase;
	}

	public MongoClient getClient() {
		return this.mongoClient;
	}

	@Override
	synchronized public void closeImplement() throws IOException {
		this.mongoClient.close();
		this.mongoClient = null;
		this.dataBase = null;
	}

	@Override
	synchronized public void openImplement() throws IOException {
		final String dbUrl = this.config.getUrl();
		final String dbName = this.config.getDbName();
		// Connect to MongoDB (complex form):
		final ConnectionString connectionString = new ConnectionString(dbUrl);
		// Créer un CodecRegistry pour UUID
		final CodecRegistry SqlTimestampCodecRegistry = CodecRegistries.fromCodecs(new SqlTimestampCodec());
		final CodecRegistry OffsetDateTimeCodecRegistry = CodecRegistries.fromCodecs(new OffsetDateTimeCodec());
		// Créer un CodecRegistry pour POJOs
		final CodecRegistry pojoCodecRegistry = CodecRegistries
				.fromProviders(PojoCodecProvider.builder().automatic(true).build());
		// Ajouter le CodecRegistry par défaut, le codec UUID et celui pour POJOs
		//final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
		//		MongoClientSettings.getDefaultCodecRegistry(), /*uuidCodecRegistry, */ pojoCodecRegistry);

		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(new org.bson.codecs.UuidCodec(UuidRepresentation.STANDARD)),
				pojoCodecRegistry, SqlTimestampCodecRegistry, OffsetDateTimeCodecRegistry);
		// Configure MongoClientSettings
		final MongoClientSettings clientSettings = MongoClientSettings.builder() //
				.applyConnectionString(connectionString)//
				.codecRegistry(codecRegistry) //
				.uuidRepresentation(UuidRepresentation.STANDARD)//
				.build();
		this.mongoClient = MongoClients.create(clientSettings);
		if (dbName == null) {
			LOGGER.error("Connect on the DB: host:{} port:{}", this.config.getHostname(), this.config.getPort());
		}
		this.dataBase = this.mongoClient.getDatabase(dbName);
	}
}
