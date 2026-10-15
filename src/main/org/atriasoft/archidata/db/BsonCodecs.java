package org.atriasoft.archidata.db;

import org.bson.BsonDocument;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoClientSettings;

/** Shared BSON codec registry able to encode UUID values (STANDARD representation), as configured in {@link DbIoMongo}. */
public final class BsonCodecs {
	/** Registry with the STANDARD UUID codec in front of the driver default registry. */
	public static final CodecRegistry UUID_AWARE_REGISTRY = CodecRegistries.fromRegistries(//
			CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)), //
			MongoClientSettings.getDefaultCodecRegistry());

	private BsonCodecs() {
		// Utility class.
	}

	/**
	 * Render a filter as a BsonDocument, supporting UUID values.
	 * @param bson The filter to render.
	 * @return The rendered document.
	 */
	public static BsonDocument toBsonDocument(final Bson bson) {
		return bson.toBsonDocument(BsonDocument.class, UUID_AWARE_REGISTRY);
	}
}
