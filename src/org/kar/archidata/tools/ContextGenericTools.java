package org.kar.archidata.tools;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.kar.archidata.converter.jackson.JacksonModules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ContextGenericTools {

	public static ObjectMapper createObjectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		// Configure Jackson for dates and times
		objectMapper.registerModule(new JavaTimeModule()); // Module for Java 8+ Date and Time API
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// configure the local serialization modules
		objectMapper.registerModule(JacksonModules.getAllModules());
		return objectMapper;
	}

	/**
	 * Add support of Jackson jsr310 for data and time serialization and un-serialization.
	 * @param rc Resource exception model.
	 */
	public static void addJsr310(final ResourceConfig rc) {
		final ObjectMapper objectMapper = createObjectMapper();
		// configure jackson provider for JSON mapper
		final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
		provider.setMapper(objectMapper);
		// Record it on the Resource configuration
		rc.register(provider);

	}
}
