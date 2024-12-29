package org.kar.archidata.tools;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ContextGenericTools {

	/**
	 * Add support of Jackson jsr310 for data and time serialization and un-serialization.
	 * @param rc Resource exception model.
	 */
	public static void addJsr310(final ResourceConfig rc) {
		// Configure Jackson for dates and times
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule()); // Module for Java 8+ Date and Time API
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// configure jackson provider for JSON mapper
		final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
		provider.setMapper(objectMapper);

		// Record it on the Resource configuration
		rc.register(provider);

	}
}
