package org.atriasoft.archidata.converter.mongo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;

/**
 * BSON codec for {@link OffsetDateTime} values.
 * Handles encoding and decoding of {@link OffsetDateTime} to and from BSON DATE_TIME (epoch milliseconds in UTC).
 */
public class OffsetDateTimeCodec implements Codec<OffsetDateTime> {

	/** Default constructor. */
	public OffsetDateTimeCodec() {
		// default constructor
	}

	/**
	 * Encodes an {@link OffsetDateTime} value as a BSON DATE_TIME (epoch milliseconds).
	 * @param writer the BSON writer to write the encoded value to.
	 * @param value the {@link OffsetDateTime} to encode.
	 * @param encoderContext the encoder context.
	 */
	@Override
	public void encode(
			final BsonWriter writer,
			final OffsetDateTime value,
			final org.bson.codecs.EncoderContext encoderContext) {
		Instant instant = value.toInstant(); // convert to UTC
		writer.writeDateTime(instant.toEpochMilli());
	}

	/**
	 * Decodes a BSON DATE_TIME value into an {@link OffsetDateTime} in UTC.
	 * @param reader the BSON reader providing the encoded value.
	 * @param decoderContext the decoder context.
	 * @return the decoded {@link OffsetDateTime} in UTC.
	 * @throws IllegalArgumentException if the current BSON type is not DATE_TIME.
	 */
	@Override
	public OffsetDateTime decode(final BsonReader reader, final org.bson.codecs.DecoderContext decoderContext) {
		final BsonType bsonType = reader.getCurrentBsonType();
		if (bsonType == BsonType.DATE_TIME) {
			long millis = reader.readDateTime();
			return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
		} else {
			throw new IllegalArgumentException("Expected a DATE_TIME but found " + bsonType);
		}
	}

	/**
	 * Returns the class that this codec encodes and decodes.
	 * @return the {@link OffsetDateTime} class.
	 */
	@Override
	public Class<OffsetDateTime> getEncoderClass() {
		return OffsetDateTime.class;
	}
}
