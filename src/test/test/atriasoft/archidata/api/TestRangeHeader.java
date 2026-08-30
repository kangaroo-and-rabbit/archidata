package test.atriasoft.archidata.api;

import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.api.DataResource.ByteRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** What a browser asks for with a "Range" header, and what is answered to it.
 *
 * <p>
 * A player reads the index of a Matroska by asking for the last bytes of the file, and it can not
 * seek in a media until it has that index: a header form that is not understood here is a media
 * that plays from the start and never jumps. */
public class TestRangeHeader {
	private static final long FILE_LENGTH = 10_000;

	@Test
	public void noHeaderMeansTheWholeFile() {
		Assertions.assertNull(DataResource.parseRange(null, FILE_LENGTH));
	}

	@Test
	public void aRangeWithBothEndsIsTakenAsItIs() {
		final ByteRange range = DataResource.parseRange("bytes=100-199", FILE_LENGTH);
		Assertions.assertEquals(100, range.from());
		Assertions.assertEquals(199, range.to());
	}

	@Test
	public void anOpenEndedRangeRunsToTheEndOfTheFile() {
		final ByteRange range = DataResource.parseRange("bytes=9000-", FILE_LENGTH);
		Assertions.assertEquals(9000, range.from());
		Assertions.assertEquals(FILE_LENGTH - 1, range.to());
	}

	@Test
	public void theLastBytesOfTheFileAreTheIndexOfAMedia() {
		final ByteRange range = DataResource.parseRange("bytes=-1024", FILE_LENGTH);
		Assertions.assertEquals(FILE_LENGTH - 1024, range.from());
		Assertions.assertEquals(FILE_LENGTH - 1, range.to());
	}

	@Test
	public void askingForMoreThanTheFileHoldsGivesTheWholeOfIt() {
		final ByteRange range = DataResource.parseRange("bytes=-50000", FILE_LENGTH);
		Assertions.assertEquals(0, range.from());
		Assertions.assertEquals(FILE_LENGTH - 1, range.to());
	}

	@Test
	public void anEndPastTheFileStopsAtItsLastByte() {
		final ByteRange range = DataResource.parseRange("bytes=9000-99999", FILE_LENGTH);
		Assertions.assertEquals(9000, range.from());
		Assertions.assertEquals(FILE_LENGTH - 1, range.to());
	}

	@Test
	public void onlyTheFirstOfSeveralRangesIsAnswered() {
		final ByteRange range = DataResource.parseRange("bytes=0-99, 200-299", FILE_LENGTH);
		Assertions.assertEquals(0, range.from());
		Assertions.assertEquals(99, range.to());
	}

	@Test
	public void aRangeThatStartsPastTheEndIsRefused() {
		Assertions.assertSame(DataResource.RANGE_OUT_OF_FILE, DataResource.parseRange("bytes=20000-", FILE_LENGTH));
	}

	@Test
	public void aBackwardsRangeIsRefused() {
		Assertions.assertSame(DataResource.RANGE_OUT_OF_FILE, DataResource.parseRange("bytes=500-100", FILE_LENGTH));
	}

	@Test
	public void aHeaderThatCanNotBeReadIsAnsweredWithTheWholeFile() {
		Assertions.assertNull(DataResource.parseRange("bytes=abc-def", FILE_LENGTH));
		Assertions.assertNull(DataResource.parseRange("items=0-10", FILE_LENGTH));
		Assertions.assertNull(DataResource.parseRange("bytes=", FILE_LENGTH));
	}

	@Test
	public void anEmptyFileHasNoRangeToGive() {
		Assertions.assertNull(DataResource.parseRange("bytes=0-10", 0));
	}
}
