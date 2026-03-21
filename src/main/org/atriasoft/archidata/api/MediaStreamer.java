package org.atriasoft.archidata.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * {@link StreamingOutput} implementation that streams a range of bytes from a {@link RandomAccessFile}.
 *
 * <p>Used by {@link DataResource} to serve partial content (HTTP 206) responses for media streaming.</p>
 */
public class MediaStreamer implements StreamingOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaStreamer.class);
	private final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	final byte[] buf = new byte[this.CHUNK_SIZE];
	private long length;
	private final RandomAccessFile raf;

	/**
	 * Creates a new media streamer for the given byte range.
	 * @param length The number of bytes to stream.
	 * @param raf The random access file positioned at the start of the range.
	 * @throws IOException If the specified length is negative.
	 */
	public MediaStreamer(final long length, final RandomAccessFile raf) throws IOException {
		// logger.info("request stream of {} data", length / 1024);
		if (length < 0) {
			throw new IOException("Wrong size of the file to stream: " + length);
		}
		this.length = length;
		this.raf = raf;
	}

	/** {@inheritDoc} */
	@Override
	public void write(final OutputStream outputStream) {
		try {
			while (this.length != 0) {
				final int read = this.raf.read(this.buf, 0,
						this.buf.length > this.length ? (int) this.length : this.buf.length);
				try {
					outputStream.write(this.buf, 0, read);
				} catch (final IOException ex) {
					LOGGER.info("remote close connection");
					break;
				}
				this.length -= read;
			}
		} catch (final IOException ex) {
			throw new InternalServerErrorException(ex);
		} catch (final WebApplicationException ex) {
			throw new InternalServerErrorException(ex);
		} finally {
			try {
				this.raf.close();
			} catch (final IOException ex) {
				LOGGER.error("Failed to close RandomAccessFile: {}", ex.getMessage(), ex);
				throw new InternalServerErrorException(ex);
			}
		}
	}

	/**
	 * Returns the remaining number of bytes to stream.
	 * @return The remaining byte count.
	 */
	public long getLenth() {
		return this.length;
	}
}
