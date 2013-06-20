/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.postlet;

/**
 *
 * @author hevega
 */
public class UploadResponse {
    private int successCount;
    private int failedCount;
    private long bytesCopied;
    private String finalMessage;
    private String[] success;
    private String[] failed;
    private String[] errors;

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public long getBytesCopied() {
        return bytesCopied;
    }

    public void setBytesCopied(long bytesCopied) {
        this.bytesCopied = bytesCopied;
    }

    public String[] getSuccess() {
        return success;
    }

    public void setSuccess(String[] success) {
        this.success = success;
    }

    public String[] getFailed() {
        return failed;
    }

    public void setFailed(String[] failed) {
        this.failed = failed;
    }

    public String[] getErrors() {
        return errors;
    }

    public void setErrors(String[] errors) {
        this.errors = errors;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }
    
    
}
