/**
 * Jackson JSON serializers and deserializers for custom types.
 *
 * <p>Handles conversion of {@code ObjectId}, {@code Date}, {@code OffsetDateTime} and BSON
 * {@code Document} for JSON serialization, and the drop of the {@code @ApiReadOnly} properties of a request
 * body.</p>
 */
package org.atriasoft.archidata.converter.jackson;
