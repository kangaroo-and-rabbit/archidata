package org.atriasoft.archidata.dataAccess.model.codec;

/**
 * Converts a non-null Java value to its MongoDB-compatible representation.
 *
 * <p>Pre-compiled at {@link MongoCodecFactory} construction time for a specific
 * Java type. At runtime, this is a simple lambda call with zero type checking.
 */
@FunctionalInterface
public interface MongoTypeWriter {

	/**
	 * Convert a non-null Java value to its MongoDB Document representation.
	 *
	 * @param javaValue the non-null Java value (already read from the bean)
	 * @return the MongoDB-compatible value (Long, String, Document, List, etc.)
	 * @throws Exception if conversion fails
	 */
	Object toMongo(Object javaValue) throws Exception;
}
