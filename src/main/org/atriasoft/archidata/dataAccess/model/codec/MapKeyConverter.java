package org.atriasoft.archidata.dataAccess.model.codec;

/**
 * Converts between Map key objects and their String representation in MongoDB Documents.
 *
 * <p>MongoDB Documents only support String keys, so Map keys must be converted
 * to/from String. A converter is pre-compiled for each key type at codec construction time.
 */
public interface MapKeyConverter {

	/** Convert a String key from a MongoDB Document to the target key type. */
	Object fromString(String key) throws Exception;

	/** Convert a key object to its String representation for MongoDB Document storage. */
	String toString(Object key) throws Exception;
}
