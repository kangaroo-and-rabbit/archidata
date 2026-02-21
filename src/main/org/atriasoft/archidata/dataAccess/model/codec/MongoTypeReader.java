package org.atriasoft.archidata.dataAccess.model.codec;

/**
 * Converts a non-null MongoDB value to the target Java type.
 *
 * <p>Pre-compiled at {@link MongoCodecFactory} construction time for a specific
 * Java type. At runtime, this is a simple lambda call with zero type checking.
 */
@FunctionalInterface
public interface MongoTypeReader {

	/**
	 * Convert a non-null MongoDB value to the Java value.
	 *
	 * @param mongoValue the non-null value from the MongoDB Document
	 * @return the Java value ready to be set on the bean
	 * @throws Exception if conversion fails
	 */
	Object fromMongo(Object mongoValue) throws Exception;
}
