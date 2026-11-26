/**
 * Per-field codecs of the model.
 *
 * <p>{@code MongoCodecFactory} builds one {@code MongoFieldCodec} per field of an entity, which copies the
 * value between the Java object and the MongoDB document ({@code MongoTypeWriter} and
 * {@code MongoTypeReader} do the type conversion). These are archidata's own classes, not codecs registered
 * in the MongoDB driver.</p>
 */
package org.atriasoft.archidata.dataAccess.model.codec;
