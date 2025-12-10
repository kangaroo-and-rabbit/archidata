package org.atriasoft.archidata.converter.mongo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;

public class OffsetDateTimeCodec implements Codec<OffsetDateTime> {

	@Override
	public void encode(
			final BsonWriter writer,
			final OffsetDateTime value,
			final org.bson.codecs.EncoderContext encoderContext) {
		Instant instant = value.toInstant(); // convert to UTC
		writer.writeDateTime(instant.toEpochMilli());
	}

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

	@Override
	public Class<OffsetDateTime> getEncoderClass() {
		return OffsetDateTime.class;
	}
}
