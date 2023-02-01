package org.kar.archidata.api;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class MediaStreamer implements StreamingOutput {
    private final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    final byte[] buf = new byte[CHUNK_SIZE];
    private long length;
    private RandomAccessFile raf;

    public MediaStreamer(long length, RandomAccessFile raf) throws IOException {
        //System.out.println("request stream of " + length / 1024 + " data");
        if (length<0) {
            throw new IOException("Wrong size of the file to stream: " + length);
        }
        this.length = length;
        this.raf = raf;
    }

    @Override
    public void write(OutputStream outputStream) {
        try {
            while (length != 0) {
                int read = raf.read(buf, 0, buf.length > length ? (int) length : buf.length);
                try {
                    outputStream.write(buf, 0, read);
                } catch (IOException ex) {
                    System.out.println("remote close connection");
                    break;
                }
                length -= read;
            }
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        } catch (WebApplicationException ex) {
            throw new InternalServerErrorException(ex);
        } finally {
            try {
                raf.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new InternalServerErrorException(ex);
            }
        }
    }

    public long getLenth() {
        return length;
    }

}
