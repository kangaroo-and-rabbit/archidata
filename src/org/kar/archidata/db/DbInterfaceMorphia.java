package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;

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

public class DbInterfaceMorphia extends DbInterface implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DbInterfaceMorphia.class);
	private final MongoClient mongoClient;
	private final Datastore datastore;
	
	public DbInterfaceMorphia(final String dbUrl, final String dbName, final Class<?>... classes) {
		// Connect to MongoDB (simple form):
		// final MongoClient mongoClient = MongoClients.create(dbUrl);
		
		// Connect to MongoDB (complex form):
		final ConnectionString connectionString = new ConnectionString(dbUrl);
		// Créer un CodecRegistry pour UUID
		//final CodecRegistry uuidCodecRegistry = CodecRegistries.fromCodecs(new UUIDCodec());
		// Créer un CodecRegistry pour POJOs
		final CodecRegistry pojoCodecRegistry = CodecRegistries
				.fromProviders(PojoCodecProvider.builder().automatic(true).build());
		// Ajouter le CodecRegistry par défaut, le codec UUID et celui pour POJOs
		//final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
		//		MongoClientSettings.getDefaultCodecRegistry(), /*uuidCodecRegistry, */ pojoCodecRegistry);
		
		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(new org.bson.codecs.UuidCodec(UuidRepresentation.STANDARD)),
				pojoCodecRegistry);
		// Configurer MongoClientSettings
		final MongoClientSettings clientSettings = MongoClientSettings.builder() //
				.applyConnectionString(connectionString)//
				.codecRegistry(codecRegistry) //
				.uuidRepresentation(UuidRepresentation.STANDARD)//
				.build();
		this.mongoClient = MongoClients.create(clientSettings);
		this.datastore = Morphia.createDatastore(this.mongoClient, "karusic");
		// Map entities
		this.datastore.getMapper().map(classes);
		// Ensure indexes
		this.datastore.ensureIndexes();
	}
	
	public Datastore getDatastore() {
		return this.datastore;
	}
	
	@Override
	public void close() throws IOException {
		this.mongoClient.close();

	}
}
