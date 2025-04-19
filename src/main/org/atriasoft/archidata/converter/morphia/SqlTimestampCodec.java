package org.atriasoft.archidata.converter.morphia;

import java.sql.Timestamp;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;

public class SqlTimestampCodec implements Codec<Timestamp> {

	@Override
	public void encode(
			final BsonWriter writer,
			final Timestamp value,
			final org.bson.codecs.EncoderContext encoderContext) {
		writer.writeDateTime(value.getTime());
	}

	@Override
	public Timestamp decode(final BsonReader reader, final org.bson.codecs.DecoderContext decoderContext) {
		final BsonType bsonType = reader.getCurrentBsonType();
		if (bsonType == BsonType.DATE_TIME) {
			return new Timestamp(reader.readDateTime());
		} else {
			throw new IllegalArgumentException("Expected a DATE_TIME but found " + bsonType);
		}
	}

	@Override
	public Class<Timestamp> getEncoderClass() {
		return Timestamp.class;
	}
}
