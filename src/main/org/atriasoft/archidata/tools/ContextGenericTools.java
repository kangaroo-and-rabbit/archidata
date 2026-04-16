package org.atriasoft.archidata.tools;

import org.atriasoft.archidata.converter.jackson.JacksonModules;
import org.atriasoft.archidata.interceptor.ValidGroupInterceptor;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for configuring Jackson ObjectMapper and JAX-RS resource serialization.
 *
 * <p>Provides a pre-configured ObjectMapper with Java 8+ date/time support and custom archidata modules.</p>
 */
public class ContextGenericTools {
	private ContextGenericTools() {
		// Utility class
	}

	/**
	 * Creates and configures a new Jackson ObjectMapper with archidata custom modules and Java 8+ date/time support.
	 * @return A configured ObjectMapper instance.
	 */
	public static ObjectMapper createObjectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// configure the local serialization modules
		objectMapper.registerModule(JacksonModules.getAllModules());
		// Add java time module at the end to prevent use it in first but in backup
		objectMapper.registerModule(new JavaTimeModule()); // Module for Java 8+ Date and Time API
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
		rc.register(ValidGroupInterceptor.class);
	}
}
