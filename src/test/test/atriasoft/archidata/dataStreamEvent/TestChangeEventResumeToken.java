package test.atriasoft.archidata.dataStreamEvent;

import java.time.Instant;

import org.atriasoft.archidata.dataStreamEvent.ChangeEvent;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.changestream.OperationType;

/** The event of a change is shared by every listener: one of them must not be able to move the resume position. */
class TestChangeEventResumeToken {

	private static BsonDocument token() {
		return new BsonDocument("_data", new BsonString("position"));
	}

	private static ChangeEvent event(final BsonDocument token) {
		return new ChangeEvent(OperationType.INSERT, "collection", "entity", null, null, null, Instant.EPOCH, token);
	}

	@Test
	void theTokenGivenIsCopied() {
		final BsonDocument given = token();
		final ChangeEvent event = event(given);
		given.put("_data", new BsonString("moved"));
		Assertions.assertEquals(token(), event.getResumeToken());
	}

	@Test
	void theTokenReturnedIsACopy() {
		final ChangeEvent event = event(token());
		event.getResumeToken().put("_data", new BsonString("moved"));
		Assertions.assertEquals(token(), event.getResumeToken());
	}

	@Test
	void anEventWithoutTokenIsAccepted() {
		Assertions.assertNull(event(null).getResumeToken());
	}
}
