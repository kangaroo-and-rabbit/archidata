package org.atriasoft.archidata.dataAccess.model.codec;

/**
 * Converts between Map key objects and their String representation in MongoDB Documents.
 *
 * <p>MongoDB Documents only support String keys, so Map keys must be converted
 * to/from String. A converter is pre-compiled for each key type at codec construction time.
 */
public interface MapKeyConverter {

	/**
	 * Convert a String key from a MongoDB Document to the target key type.
	 *
	 * @param key the string key from the MongoDB Document
	 * @return the converted key object
	 * @throws Exception if the conversion fails
	 */
	Object fromString(String key) throws Exception;

	/**
	 * Convert a key object to its String representation for MongoDB Document storage.
	 *
	 * @param key the key object to convert
	 * @return the string representation of the key
	 * @throws Exception if the conversion fails
	 */
	String toString(Object key) throws Exception;
}
