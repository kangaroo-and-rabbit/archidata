package org.atriasoft.archidata.dataAccess.model.codec;

import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.bson.Document;

/**
 * Pre-compiled codec for a single field of a Java class.
 *
 * <p>Combines property access (lambda getter/setter), type conversion (writer/reader),
 * and field naming into a single object for zero-overhead CRUD operations.
 *
 * <p>One instance per field, created at {@link org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor}
 * construction time and cached for the lifetime of the application.
 *
 * <p>Immutable and thread-safe after construction.
 */
public final class MongoFieldCodec {

	private final PropertyGetter getter;
	private final PropertySetter setter;
	private final String dbFieldName;
	private final MongoTypeWriter writer;
	private final MongoTypeReader reader;
	private final boolean primitive;

	public MongoFieldCodec(
			final PropertyGetter getter,
			final PropertySetter setter,
			final String dbFieldName,
			final MongoTypeWriter writer,
			final MongoTypeReader reader,
			final boolean primitive) {
		this.getter = getter;
		this.setter = setter;
		this.dbFieldName = dbFieldName;
		this.writer = writer;
		this.reader = reader;
		this.primitive = primitive;
	}

	/**
	 * WRITER: Read the field from the Java object and write it to docSet/docUnSet.
	 *
	 * <p>Replaces {@code setValueToDb()} entirely for this field.
	 *
	 * @param parentFieldName optional prefix for sub-object paths (e.g. "address"), may be null
	 * @param instance the Java bean instance
	 * @param docSet the $set document to append to
	 * @param docUnSet the $unset document (may be null for sub-objects/inserts)
	 */
	public void writeToDoc(
			final String parentFieldName,
			final Object instance,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final String fieldKey = composeFieldName(parentFieldName, this.dbFieldName);
		final Object javaValue = getFieldValue(instance);
		if (javaValue == null) {
			if (!this.primitive && docUnSet != null) {
				docUnSet.append(fieldKey, "");
			}
			return;
		}
		docSet.append(fieldKey, this.writer.toMongo(javaValue));
	}

	/**
	 * WRITER with override field name (for AddOns using OptionRenameColumn).
	 *
	 * @param parentFieldName optional prefix for sub-object paths, may be null
	 * @param overrideFieldName the field name to use instead of {@code dbFieldName}
	 * @param instance the Java bean instance
	 * @param docSet the $set document
	 * @param docUnSet the $unset document (may be null)
	 */
	public void writeToDoc(
			final String parentFieldName,
			final String overrideFieldName,
			final Object instance,
			final Document docSet,
			final Document docUnSet) throws Exception {
		final String fieldKey = composeFieldName(parentFieldName, overrideFieldName);
		final Object javaValue = getFieldValue(instance);
		if (javaValue == null) {
			if (!this.primitive && docUnSet != null) {
				docUnSet.append(fieldKey, "");
			}
			return;
		}
		docSet.append(fieldKey, this.writer.toMongo(javaValue));
	}

	/**
	 * READER: Read the field from a Document and set it on the Java object.
	 *
	 * <p>Uses the pre-computed {@code dbFieldName}.
	 *
	 * @param doc the MongoDB document
	 * @param instance the Java bean instance to populate
	 */
	public void readFromDoc(
			final Document doc,
			final Object instance) throws Exception {
		readFromDocInternal(doc, this.dbFieldName, this.reader, instance);
	}

	/**
	 * READER with override field name (for OptionRenameColumn or OptionSpecifyType).
	 *
	 * @param doc the MongoDB document
	 * @param overrideFieldName the field name to read from the document
	 * @param instance the Java bean instance to populate
	 */
	public void readFromDoc(
			final Document doc,
			final String overrideFieldName,
			final Object instance) throws Exception {
		readFromDocInternal(doc, overrideFieldName, this.reader, instance);
	}

	/**
	 * READER with override reader (for OptionSpecifyType runtime type override).
	 *
	 * @param doc the MongoDB document
	 * @param overrideFieldName the field name to read from the document
	 * @param overrideReader the reader to use instead of the pre-compiled one
	 * @param instance the Java bean instance to populate
	 */
	public void readFromDoc(
			final Document doc,
			final String overrideFieldName,
			final MongoTypeReader overrideReader,
			final Object instance) throws Exception {
		readFromDocInternal(doc, overrideFieldName, overrideReader, instance);
	}

	/**
	 * Convert a Java value to its MongoDB form (standalone, without field context).
	 * Useful for AddOns and collection element conversion.
	 *
	 * @param javaValue the Java value (may be null)
	 * @return the MongoDB-compatible value, or null if input is null
	 */
	public Object convertToMongo(final Object javaValue) throws Exception {
		if (javaValue == null) {
			return null;
		}
		return this.writer.toMongo(javaValue);
	}

	// ========== Getters ==========

	public String getDbFieldName() {
		return this.dbFieldName;
	}

	public MongoTypeWriter getWriter() {
		return this.writer;
	}

	public MongoTypeReader getReader() {
		return this.reader;
	}

	// ========== Private ==========

	private Object getFieldValue(final Object instance) throws Exception {
		try {
			return this.getter.get(instance);
		} catch (final Exception e) {
			throw e;
		} catch (final Throwable t) {
			throw new RuntimeException("Failed to read field '" + this.dbFieldName + "'", t);
		}
	}

	private void setFieldValue(final Object instance, final Object value) throws Exception {
		try {
			this.setter.set(instance, value);
		} catch (final Exception e) {
			throw e;
		} catch (final Throwable t) {
			throw new RuntimeException("Failed to write field '" + this.dbFieldName + "'", t);
		}
	}

	private void readFromDocInternal(
			final Document doc,
			final String fieldName,
			final MongoTypeReader readerToUse,
			final Object instance) throws Exception {
		if (!doc.containsKey(fieldName)) {
			if (!this.primitive) {
				setFieldValue(instance, null);
			}
			return;
		}
		final Object mongoValue = doc.get(fieldName);
		if (mongoValue == null) {
			if (!this.primitive) {
				setFieldValue(instance, null);
			}
			return;
		}
		setFieldValue(instance, readerToUse.fromMongo(mongoValue));
	}

	private static String composeFieldName(final String parentFieldName, final String fieldName) {
		if (parentFieldName != null && !parentFieldName.isEmpty()) {
			return parentFieldName + "." + fieldName;
		}
		return fieldName;
	}
}
