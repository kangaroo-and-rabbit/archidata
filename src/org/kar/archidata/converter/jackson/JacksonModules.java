package org.kar.archidata.converter.jackson;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonModules {
	public static SimpleModule getAllModules() {
		final SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new ObjectIdSerializer());
		module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
		return module;
	}
}
