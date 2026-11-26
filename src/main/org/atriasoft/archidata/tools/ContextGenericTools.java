package org.atriasoft.archidata.tools;

import org.atriasoft.archidata.converter.jackson.ApiReadOnlyDeserializerModifier;
import org.atriasoft.archidata.converter.jackson.JacksonModules;
import org.atriasoft.archidata.interceptor.ValidGroupInterceptor;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonFactory;
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
		// Jackson errors must not quote the faulty token of the body (a password hash, a device code...).
		final JsonFactory factory = JsonFactory.builder()
				.errorReportConfiguration(
						ErrorReportConfiguration.builder().maxErrorTokenLength(0).maxRawContentLength(0).build())
				.build();
		final ObjectMapper objectMapper = new ObjectMapper(factory);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// configure the local serialization modules
		objectMapper.registerModule(JacksonModules.getAllModules());
		// The java time module goes last: the archidata date modules registered above must win over it
		objectMapper.registerModule(new JavaTimeModule()); // Module for Java 8+ Date and Time API
		return objectMapper;
	}

	/**
	 * Registers the JSON setup of an archidata server on the Jersey configuration: the Jackson provider (ISO
	 * dates, archidata modules, Java 8+ date/time) and the {@link ValidGroupInterceptor} that enforces
	 * {@code @ValidGroup}. Without it the validation groups are silently ignored and the date format changes.
	 *
	 * <p>The registered mapper also ignores the {@code @ApiReadOnly} properties of request bodies
	 * ({@link ApiReadOnlyDeserializerModifier}): a client cannot set them. {@link #createObjectMapper()} does
	 * not, so it still reads them back from server responses.</p>
	 * @param rc The Jersey resource configuration of the server.
	 */
	public static void registerArchidataJson(final ResourceConfig rc) {
		final ObjectMapper objectMapper = createObjectMapper();
		objectMapper.registerModule(ApiReadOnlyDeserializerModifier.module());
		// configure jackson provider for JSON mapper
		final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
		provider.setMapper(objectMapper);
		// Record it on the Resource configuration
		rc.register(provider);
		rc.register(ValidGroupInterceptor.class);
	}

	/**
	 * Registers the JSON setup of an archidata server on the Jersey configuration.
	 * @param rc The Jersey resource configuration of the server.
	 * @deprecated misleading name: it registers much more than the JSR-310 date module (the Jackson provider, the
	 *             {@code @ApiReadOnly} filter and the {@code @ValidGroup} interceptor). Use
	 *             {@link #registerArchidataJson(ResourceConfig)}.
	 */
	@Deprecated(since = "0.55.0", forRemoval = true)
	public static void addJsr310(final ResourceConfig rc) {
		registerArchidataJson(rc);
	}
}
