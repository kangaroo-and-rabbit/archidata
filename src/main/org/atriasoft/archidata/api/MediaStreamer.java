package org.atriasoft.archidata.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

public class MediaStreamer implements StreamingOutput {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaStreamer.class);
	private final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	final byte[] buf = new byte[this.CHUNK_SIZE];
	private long length;
	private final RandomAccessFile raf;

	public MediaStreamer(final long length, final RandomAccessFile raf) throws IOException {
		// logger.info("request stream of {} data", length / 1024);
		if (length < 0) {
			throw new IOException("Wrong size of the file to stream: " + length);
		}
		this.length = length;
		this.raf = raf;
	}

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
				ex.printStackTrace();
				throw new InternalServerErrorException(ex);
			}
		}
	}

	public long getLenth() {
		return this.length;
	}
}
