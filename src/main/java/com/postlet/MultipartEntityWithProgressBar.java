/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.postlet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

/**
 *
 * @author hevega
 */
public class MultipartEntityWithProgressBar extends MultipartEntity {

    private OutputStreamProgress outstream;
    private WriteListener writeListener;

    public MultipartEntityWithProgressBar(WriteListener writeListener) {
        super();
        this.writeListener = writeListener;
    }

    public MultipartEntityWithProgressBar(HttpMultipartMode mode, WriteListener writeListener) {
        super(mode);
        this.writeListener = writeListener;
    }

    public MultipartEntityWithProgressBar(HttpMultipartMode mode, String boundary, Charset charset, WriteListener writeListener) {
        super(mode, boundary, charset);
        this.writeListener = writeListener;
    }
    
    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        this.outstream = new OutputStreamProgress(outstream, writeListener);
        super.writeTo(this.outstream);
    }    
    
    // Left in for clarity to show where I took from kilaka's answer
//  /**
//   * Progress: 0-100
//   */
//  public int getProgress() {
//      if (outstream == null) {
//          return 0;
//      }
//      long contentLength = getContentLength();
//      if (contentLength <= 0) { // Prevent division by zero and negative values
//          return 0;
//      }
//      long writtenLength = outstream.getWrittenLength();
//      return (int) (100*writtenLength/contentLength);
//  }
}