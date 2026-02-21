package org.atriasoft.archidata.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.bean.accessor.LambdaAccessorFactory;
import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.atriasoft.archidata.bean.accessor.TypedPropertyGetter;
import org.atriasoft.archidata.bean.accessor.TypedPropertySetter;
import org.atriasoft.archidata.bean.annotation.UnifiedAnnotationLookup;
import org.atriasoft.archidata.bean.exception.IntrospectionException;

/**
 * Immutable descriptor for a single property of a Java class.
 *
 * <p>A property unifies a field and its corresponding getter/setter methods (if any).
 * Annotations are merged from all sources (field, getter, setter, and their interface
 * declarations) into a single cache for O(1) lookup.
 *
 * <p>Access to property values is done through pre-built lambda accessors for
 * near-native performance.
 */
public final class PropertyDescriptor {

	private final String name;
	private final TypeInfo typeInfo;
	private final Field field;
	private final Method getter;
	private final Method setter;
	private final PropertyGetter lambdaGetter;
	private final PropertySetter lambdaSetter;
	private final boolean constructorSettable;
	private final boolean readOnly;
	private final Map<Class<? extends Annotation>, Annotation> annotationCache;

	PropertyDescriptor(
			final String name,
			final TypeInfo typeInfo,
			final Field field,
			final Method getter,
			final Method setter,
			final PropertyGetter lambdaGetter,
			final PropertySetter lambdaSetter,
			final boolean constructorSettable,
			final boolean readOnly) {
		this.name = name;
		this.typeInfo = typeInfo;
		this.field = field;
		this.getter = getter;
		this.setter = setter;
		this.lambdaGetter = lambdaGetter;
		this.lambdaSetter = lambdaSetter;
		this.constructorSettable = constructorSettable;
		this.readOnly = readOnly;
		this.annotationCache = UnifiedAnnotationLookup.buildAnnotationCache(field, getter, setter);
	}

	/** The canonical property name (e.g. "firstName" from getFirstName() or field firstName). */
	public String getName() {
		return this.name;
	}

	/** Full type information including generics. */
	public TypeInfo getTypeInfo() {
		return this.typeInfo;
	}

	/** The raw type shortcut (equivalent to getTypeInfo().rawType()). */
	public Class<?> getType() {
		return this.typeInfo.rawType();
	}

	/** The element/sub type shortcut (equivalent to getTypeInfo().elementType()). May be null. */
	public Class<?> getElementType() {
		return this.typeInfo.elementType();
	}

	/** The underlying Field, or null if this property is method-only. */
	public Field getField() {
		return this.field;
	}

	/** The getter Method, or null if no getter exists. */
	public Method getGetter() {
		return this.getter;
	}

	/** The setter Method, or null if no setter exists. */
	public Method getSetter() {
		return this.setter;
	}

	/** Whether this property can be set via a constructor parameter. */
	public boolean isConstructorSettable() {
		return this.constructorSettable;
	}

	/** Whether this property is read-only (final field with no setter). */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/** Whether a value can be read from this property. */
	public boolean canRead() {
		return this.lambdaGetter != null;
	}

	/** Whether a value can be written to this property. */
	public boolean canWrite() {
		return this.lambdaSetter != null || this.constructorSettable;
	}

	// ========== Annotation API ==========

	/**
	 * Get a specific annotation, looking across field, getter, setter, and interfaces.
	 *
	 * @return the annotation instance, or null if not found anywhere
	 */
	public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
		return UnifiedAnnotationLookup.get(this.annotationCache, annotationClass);
	}

	/**
	 * Check if an annotation is present on field, getter, setter, or their interfaces.
	 */
	public <A extends Annotation> boolean hasAnnotation(final Class<A> annotationClass) {
		return UnifiedAnnotationLookup.has(this.annotationCache, annotationClass);
	}

	/**
	 * Get ALL instances of a repeatable annotation across all sources.
	 * Unlike {@link #getAnnotation}, this collects from every source rather than
	 * returning only the first match.
	 */
	public <A extends Annotation> List<A> getAnnotations(final Class<A> annotationClass) {
		return UnifiedAnnotationLookup.getAll(this.field, this.getter, this.setter, annotationClass);
	}

	/**
	 * Get all annotations (unmodifiable map).
	 */
	public Map<Class<? extends Annotation>, Annotation> getAllAnnotations() {
		return this.annotationCache;
	}

	// ========== Value access ==========

	/**
	 * Read the property value from an object instance using the pre-built lambda accessor.
	 *
	 * @throws IntrospectionException if no getter is available or if the read fails
	 */
	public Object getValue(final Object instance) throws IntrospectionException {
		if (this.lambdaGetter == null) {
			throw new IntrospectionException("Property '" + this.name + "' has no getter");
		}
		try {
			return this.lambdaGetter.get(instance);
		} catch (final Throwable e) {
			throw new IntrospectionException("Failed to read property '" + this.name + "'", e);
		}
	}

	/**
	 * Write a value to the property on an object instance using the pre-built lambda accessor.
	 *
	 * @throws IntrospectionException if no setter is available or if the write fails
	 */
	public void setValue(final Object instance, final Object value) throws IntrospectionException {
		if (this.lambdaSetter == null) {
			throw new IntrospectionException("Property '" + this.name + "' has no setter (readOnly=" + this.readOnly + ")");
		}
		try {
			this.lambdaSetter.set(instance, value);
		} catch (final Throwable e) {
			throw new IntrospectionException("Failed to write property '" + this.name + "'", e);
		}
	}

	// ========== Typed access ==========

	/**
	 * Get the raw (untyped) PropertyGetter lambda. Useful for callers that need
	 * direct lambda access without the getValue() exception wrapping.
	 *
	 * @return the pre-built getter, or null if no getter exists
	 */
	public PropertyGetter getRawGetter() {
		return this.lambdaGetter;
	}

	/**
	 * Get the raw (untyped) PropertySetter lambda. Useful for callers that need
	 * direct lambda access without the setValue() exception wrapping.
	 *
	 * @return the pre-built setter, or null if no setter exists
	 */
	public PropertySetter getRawSetter() {
		return this.lambdaSetter;
	}

	/**
	 * Create a typed getter for this property. The returned getter preserves
	 * generic type information and avoids boxing for reference types.
	 *
	 * <p>Note: this creates a new accessor each time; callers should cache the result.
	 *
	 * @param <T> the bean type
	 * @param <V> the property value type
	 * @param beanType the bean class
	 * @param valueType the property value class
	 * @return a typed property getter
	 * @throws IntrospectionException if no getter is available
	 */
	@SuppressWarnings("unchecked")
	public <T, V> TypedPropertyGetter<T, V> createTypedGetter(
			final Class<T> beanType,
			final Class<V> valueType) throws IntrospectionException {
		if (this.getter != null) {
			return LambdaAccessorFactory.createTypedGetter(this.getter, beanType, valueType);
		}
		if (this.field != null) {
			return LambdaAccessorFactory.createTypedFieldGetter(this.field, beanType, valueType);
		}
		throw new IntrospectionException("Property '" + this.name + "' has no getter or field");
	}

	/**
	 * Create a typed setter for this property. The returned setter preserves
	 * generic type information and avoids boxing for reference types.
	 *
	 * <p>Note: this creates a new accessor each time; callers should cache the result.
	 *
	 * @param <T> the bean type
	 * @param <V> the property value type
	 * @param beanType the bean class
	 * @param valueType the property value class
	 * @return a typed property setter
	 * @throws IntrospectionException if no setter is available
	 */
	@SuppressWarnings("unchecked")
	public <T, V> TypedPropertySetter<T, V> createTypedSetter(
			final Class<T> beanType,
			final Class<V> valueType) throws IntrospectionException {
		if (this.setter != null) {
			return LambdaAccessorFactory.createTypedSetter(this.setter, beanType, valueType);
		}
		if (this.field != null) {
			final TypedPropertySetter<T, V> result = LambdaAccessorFactory.createTypedFieldSetter(this.field, beanType, valueType);
			if (result == null) {
				throw new IntrospectionException("Property '" + this.name + "' is final (no setter)");
			}
			return result;
		}
		throw new IntrospectionException("Property '" + this.name + "' has no setter or field");
	}

	@Override
	public String toString() {
		return "PropertyDescriptor{name='" + this.name + "', type=" + this.typeInfo.rawType().getSimpleName()
				+ ", canRead=" + canRead() + ", canWrite=" + canWrite() + "}";
	}

	// ========== Builder (package-private, used by ClassModel) ==========

	static Builder builder(final String name) {
		return new Builder(name);
	}

	static final class Builder {
		private final String name;
		private TypeInfo typeInfo;
		private Field field;
		private Method getter;
		private Method setter;
		private PropertyGetter lambdaGetter;
		private PropertySetter lambdaSetter;
		private boolean constructorSettable;
		private boolean readOnly;

		Builder(final String name) {
			this.name = name;
		}

		TypeInfo getTypeInfo() {
			return this.typeInfo;
		}

		Builder typeInfo(final TypeInfo typeInfo) {
			this.typeInfo = typeInfo;
			return this;
		}

		Builder field(final Field field) {
			this.field = field;
			return this;
		}

		Builder getter(final Method getter) {
			this.getter = getter;
			return this;
		}

		Builder setter(final Method setter) {
			this.setter = setter;
			return this;
		}

		Builder lambdaGetter(final PropertyGetter lambdaGetter) {
			this.lambdaGetter = lambdaGetter;
			return this;
		}

		Builder lambdaSetter(final PropertySetter lambdaSetter) {
			this.lambdaSetter = lambdaSetter;
			return this;
		}

		Builder constructorSettable(final boolean constructorSettable) {
			this.constructorSettable = constructorSettable;
			return this;
		}

		Builder readOnly(final boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		PropertyDescriptor build() {
			return new PropertyDescriptor(
					this.name,
					this.typeInfo,
					this.field,
					this.getter,
					this.setter,
					this.lambdaGetter,
					this.lambdaSetter,
					this.constructorSettable,
					this.readOnly);
		}
	}
}
