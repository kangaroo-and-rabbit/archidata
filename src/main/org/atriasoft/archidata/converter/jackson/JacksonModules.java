package org.atriasoft.archidata.converter.jackson;

import java.time.OffsetDateTime;
import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonModules {
	public static SimpleModule getAllModules() {
		final SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new ObjectIdSerializer());
		module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
		module.addSerializer(Date.class, new DateSerializer());
		module.addDeserializer(Date.class, new DateDeserializer());
		module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
		module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
		return module;
	}
}
