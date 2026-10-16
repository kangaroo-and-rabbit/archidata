package org.atriasoft.archidata.converter.jackson;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Drops the {@link ApiReadOnly} properties of a request body (protection against mass assignment).
 *
 * <p>The value a client sends for an {@code @ApiReadOnly} property (on the field, the getter or the
 * setter) is silently ignored, as with {@code @JsonProperty(access = READ_ONLY)}: the property stays at
 * the value the constructor gives it. No error is raised, because the fronts often send back the whole
 * object they read (with its id, its timestamps...).
 *
 * <p>Only the JSON provider of the server registers it ({@code ContextGenericTools.addJsr310}). The
 * mappers that read data produced by a server ({@code RESTApiRequest}, backups...) must not: they would
 * lose the read-only fields of the responses. Properties set through a constructor (records,
 * {@code @JsonCreator}) are left untouched.
 */
public class ApiReadOnlyDeserializerModifier extends BeanDeserializerModifier {
	private static final long serialVersionUID = 1L;

	/** Name of the module returned by {@link #module()}. */
	public static final String MODULE_NAME = "archidata-api-read-only-input";

	/** Creates the modifier. */
	public ApiReadOnlyDeserializerModifier() {
		// default constructor
	}

	/**
	 * Creates a Jackson module that registers this modifier.
	 *
	 * @return a module to register on the server-side request body mapper only
	 */
	public static SimpleModule module() {
		final SimpleModule module = new SimpleModule(MODULE_NAME);
		module.setDeserializerModifier(new ApiReadOnlyDeserializerModifier());
		return module;
	}

	@Override
	public BeanDeserializerBuilder updateBuilder(
			final DeserializationConfig config,
			final BeanDescription beanDesc,
			final BeanDeserializerBuilder builder) {
		final List<String> readOnlyNames = new ArrayList<>();
		for (final BeanPropertyDefinition definition : beanDesc.findProperties()) {
			if (isApiReadOnly(definition)) {
				readOnlyNames.add(definition.getName());
			}
		}
		for (final String name : readOnlyNames) {
			final SettableBeanProperty property = builder.findProperty(PropertyName.construct(name));
			if (property == null || property instanceof CreatorProperty) {
				continue;
			}
			builder.removeProperty(PropertyName.construct(name));
			builder.addIgnorable(name);
		}
		return builder;
	}

	private static boolean isApiReadOnly(final BeanPropertyDefinition definition) {
		return hasApiReadOnly(definition.getField()) || hasApiReadOnly(definition.getGetter())
				|| hasApiReadOnly(definition.getSetter());
	}

	private static boolean hasApiReadOnly(final AnnotatedMember member) {
		return member != null && member.hasAnnotation(ApiReadOnly.class);
	}
}
