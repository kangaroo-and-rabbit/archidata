package org.atriasoft.archidata.api;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * {@link StreamingOutput} implementation that streams a range of bytes from a file.
 *
 * <p>Used by {@link DataResource} to serve partial content (HTTP 206) responses for media streaming. The file
 * is opened in {@link #write(OutputStream)} and closed there: when Jersey never writes the body (HEAD request,
 * client gone, exception in a filter), no file descriptor is left open.</p>
 */
public final class MediaStreamer implements StreamingOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaStreamer.class);
	private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	final byte[] buf = new byte[CHUNK_SIZE];
	private long length;
	private final File file;
	private final long from;
	private final RandomAccessFile raf;

	/**
	 * Creates a new media streamer for a byte range of a file, opened only when the body is written.
	 * @param file The file to stream.
	 * @param from The first byte to send.
	 * @param length The number of bytes to stream.
	 * @throws IOException If the specified length or offset is negative.
	 */
	public MediaStreamer(final File file, final long from, final long length) throws IOException {
		if (length < 0 || from < 0) {
			throw new IOException("Wrong range of the file to stream: " + from + " +" + length);
		}
		this.file = file;
		this.from = from;
		this.length = length;
		this.raf = null;
	}

	/**
	 * Creates a new media streamer for the given byte range.
	 * @param length The number of bytes to stream.
	 * @param raf The random access file positioned at the start of the range.
	 * @throws IOException If the specified length is negative.
	 * @deprecated The file stays open when the body is never written (HEAD request, client gone): use
	 *             {@link #MediaStreamer(File, long, long)}.
	 */
	@Deprecated
	public MediaStreamer(final long length, final RandomAccessFile raf) throws IOException {
		// logger.info("request stream of {} data", length / 1024);
		if (length < 0) {
			throw new IOException("Wrong size of the file to stream: " + length);
		}
		this.length = length;
		this.raf = raf;
		this.file = null;
		this.from = 0;
	}

	/** {@inheritDoc} */
	@Override
	public void write(final OutputStream outputStream) {
		try (RandomAccessFile input = this.raf != null ? this.raf : new RandomAccessFile(this.file, "r")) {
			if (this.raf == null) {
				input.seek(this.from);
			}
			while (this.length != 0) {
				final int read = input.read(this.buf, 0,
						this.buf.length > this.length ? (int) this.length : this.buf.length);
				if (read < 0) {
					break;
				}
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
