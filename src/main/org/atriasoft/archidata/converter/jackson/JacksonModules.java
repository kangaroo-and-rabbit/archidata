package org.atriasoft.archidata.converter.jackson;

import java.time.OffsetDateTime;
import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Utility class providing Jackson module configuration for custom serializers and deserializers.
 * Registers converters for {@link ObjectId}, {@link Date}, {@link OffsetDateTime}, and {@link Document}.
 */
public class JacksonModules {
	private JacksonModules() {
		// Utility class
	}

	/**
	 * Creates a Jackson {@link SimpleModule} with all custom serializers and deserializers registered.
	 * This includes converters for {@link ObjectId}, {@link Date}, {@link OffsetDateTime}, and {@link Document}.
	 * @return a configured {@link SimpleModule} containing all custom converters.
	 */
	public static SimpleModule getAllModules() {
		final SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new ObjectIdSerializer());
		module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
		module.addSerializer(Date.class, new DateSerializer());
		module.addDeserializer(Date.class, new DateDeserializer());
		module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
		module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
		module.addSerializer(Document.class, new DocumentSerializer());
		module.addDeserializer(Document.class, new DocumentDeserializer());
		return module;
	}
}
