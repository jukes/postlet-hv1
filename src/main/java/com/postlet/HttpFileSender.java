/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.postlet;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author hevega
 */
public class HttpFileSender extends AbstractTableModel implements Runnable, WriteListener {

    private String url;
    private ArrayList<File> files;
    private String response;
    private boolean changed;
    private ArrayList<MyObserver> observers;
    private String email;
    private ArrayList<String> failedLocalFiles;
    private ArrayList<String> failedLocalFilesErrors;

    public HttpFileSender(String url) {
        this.url = url;
        this.files = new ArrayList();
        this.observers = new ArrayList();
        this.failedLocalFiles = new ArrayList<String>();
        this.failedLocalFilesErrors = new ArrayList<String>();
    }

    public HttpFileSender(String url, File[] files) {
        this.url = url;
        this.files = new ArrayList(files.length);
        this.addFiles(files);
    }

    /**
     * Add files to send
     *
     * @param files
     */
    final public void addFiles(File[] files) {
        for (File file : files) {
            if (!repeatedFile(file) && !file.isDirectory()) {
                this.files.add(file);
            }
        }
        this.fireTableDataChanged();
    }

    /**
     *
     * @param file
     */
    public void addFile(File file) {
        if (!repeatedFile(file) && !file.isDirectory()) {
            this.files.add(file);
        }
        this.fireTableDataChanged();
    }

    /**
     * Clear files to send
     */
    public void clearFiles() {
        this.files.clear();
        this.failedLocalFiles.clear();
        this.failedLocalFilesErrors.clear();
        this.fireTableDataChanged();
    }

    /**
     * Avoid adding repeated files
     *
     * @param f
     * @return
     */
    private boolean repeatedFile(File f) {
        String name = f.getAbsolutePath();
        for (File file : files) {
            if (name.equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format file size
     *
     * @param i
     * @return
     */
    public String fileSize(int i) {
        long sizeBytes = files.get(i).length();
        return formatFileSize(sizeBytes);
    }

    public String formatFileSize(long sizeBytes) {

        double sizeKB = sizeBytes / 1024;
        double sizeMB = sizeBytes / 1048576;
        DecimalFormat df = new DecimalFormat("#.##");
        String sizeF;
        if (sizeMB >= 1.00) {
            sizeF = df.format(sizeMB) + "MB";
        } else if (sizeKB >= 1.00) {
            sizeF = df.format(sizeKB) + "KB";
        } else {
            sizeF = sizeBytes + " Bytes";
        }

        return sizeF;
    }

    public static void main(String[] args) {
        File f = new File("D:/21GNET/maven-projects/postlet-hv1/src/main/java/com/postlet/HttpFileSender.java");
        System.out.println("Name: " + f.getAbsolutePath());
    }

    @Override
    public void run() {

        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost post = new HttpPost(url);
        MultipartEntityWithProgressBar entity = new MultipartEntityWithProgressBar(HttpMultipartMode.BROWSER_COMPATIBLE, this);
        try {
            entity.addPart("email_to_files", new StringBody(this.email));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HttpFileSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (File file : files) {

            if (!file.exists()) {
                this.failedLocalFiles.add(file.getName());
                this.failedLocalFilesErrors.add("The local file doesn't exist");
            }
            else{
                entity.addPart("files[]", new FileBody(file));
            }
        }

        post.setEntity(entity);
        try {
            this.response = EntityUtils.toString(client.execute(post).getEntity(), "UTF-8");

            //System.out.println("RESP:: " + this.response);

            this.setChanged();
            this.notifyObservers(this.response);

            //this.clearFiles();

        } catch (IOException ex) {
            Logger.getLogger(HttpFileSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void registerWrite(long amountOfBytesWritten) {
        //System.out.println("--written: " + amountOfBytesWritten);
        this.setChanged();
        this.notifyObservers(amountOfBytesWritten);

    }

    public void removeFileAt(int i) {
        File f = this.files.get(i);
        System.out.println("Remove: " + f.getName());
        this.files.remove(f);
        this.fireTableDataChanged();
    }

    /**
     *
     * @param indexes
     */
    public void removeFilesAt(int[] indexes) {
        int j = 0;
        for (int i = 0; i < indexes.length; i++) {
            this.files.remove(indexes[i] - j);
            j++;
        }

        this.fireTableDataChanged();
    }

    /**
     * Remove files in list
     *
     * @param fileList
     */
    public void removeFiles(File[] fileList) {
        for (int i = 0; i < fileList.length; i++) {
            this.files.remove(fileList[i]);
        }
        this.fireTableDataChanged();
    }

    public void addObserver(MyObserver o) {
        this.observers.add(o);
    }

    private String fileList() {
        String l = "";
        for (int i = 0; i < this.files.size(); i++) {
            l += this.files.get(i).getName() + "\n";
        }
        return l;
    }

    /**
     *
     * @param o
     */
    public void notifyObservers(Object o) {

        for (MyObserver observer : observers) {
            observer.update(o);
        }

        this.changed = false;
    }

    @Override
    public int getRowCount() {
        return this.files.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Filename" : "File Size";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columnIndex == 0 ? this.files.get(rowIndex).getName() : this.fileSize(rowIndex);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged() {
        this.changed = true;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<String> getFailedLocalFiles() {
        return failedLocalFiles;
    }

    public void setFailedLocalFiles(ArrayList<String> failedLocalFiles) {
        this.failedLocalFiles = failedLocalFiles;
    }

    public ArrayList<String> getFailedLocalFilesErrors() {
        return failedLocalFilesErrors;
    }

    public void setFailedLocalFilesErrors(ArrayList<String> failedLocalFilesErrors) {
        this.failedLocalFilesErrors = failedLocalFilesErrors;
    }
}
