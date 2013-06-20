/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.postlet;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author hevega
 */
public class OutputStreamProgress extends OutputStream {

    private final OutputStream outstream;
    private long bytesWritten = 0;
    private final WriteListener writeListener;

    public OutputStreamProgress(OutputStream outstream, WriteListener writeListener) {
        this.outstream = outstream;
        this.writeListener = writeListener;
    }

    @Override
    public void write(int b) throws IOException {
        outstream.write(b);
        bytesWritten++;
        writeListener.registerWrite(bytesWritten);
    }

    @Override
    public void write(byte[] b) throws IOException {
        outstream.write(b);
        bytesWritten += b.length;
        writeListener.registerWrite(bytesWritten);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outstream.write(b, off, len);
        bytesWritten += len;
        writeListener.registerWrite(bytesWritten);
    }

    @Override
    public void flush() throws IOException {
        outstream.flush();
    }

    @Override
    public void close() throws IOException {
        outstream.close();
    }
}
