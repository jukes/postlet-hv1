
import com.google.gson.Gson;
import com.postlet.HttpFileSender;
import com.postlet.MyObserver;
import com.postlet.UploadResponse;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author hevega
 */
public class PostApplet extends JApplet implements MouseListener, DropTargetListener, MyObserver {

    private int totalBytes, buttonClicked, maxPixels, maxFileSize;
    int sentBytes;
    private Color backgroundColour, columnHeadColourBack, columnHeadColourFore;
    private ArrayList<File> failedFiles, uploadedFiles;
    //private UploadManager1 upMan;
    private File[] files;
    //private TableData tabledata;
    private TableColumn sizeColumn;
    private ImageIcon dropIcon, dropIconUpload, dropIconAdded;
    private JLabel iconLabel;
    // Default error PrintStream!
    private PrintStream out = System.out;
    // Boolean set to false when a javascript method is executed
    private boolean javascriptStatus;
    // Parameters
    private URL endPageURL, helpPageURL, destinationURL, dropImageURL, dropImageUploadURL, dropImageAddedURL;
    private boolean warnMessage, autoUpload, helpButton, failedFileMessage, addButton, removeButton, uploadButton;
    private String language, dropImage, dropImageAdded, dropImageUpload, proxy, fileToRemove;
    private int maxThreads;
    private String[] fileExtensions;
    // URI list flavor (Hack for linux/KDE)
    //private DataFlavor uriListFlavor;
    // JSObject for doing the shit!
    //private JSObject jso;
    private JFileChooser chooser;
    private PostletLabels pLabels;
    //private static final String[] postletJS = {"postletStatus", "postletFinished", "postletFiles", "postletError"};
    // Postlet Version (Mainly for diagnostics and tracking)
    public static final String postletVersion = "1.55";
    private HttpFileSender httpFileSender;
    private Gson gson;
    private String[] emailDomains;
    private String userEmail;

    /**
     * Initializes the applet PostApplet
     */
    @Override
    public void init() {
        // First thing, output the version, for debugging purposes.
        System.out.println("POSTLET VERSION: " + postletVersion);
        String date = "$Date: 2013-05-29 16:26:13 +0000 (Wed, 29 May 2013) $";
        System.out.println(date.substring(7, date.length() - 1));

        // Set the javascript to false, and start listening for clicks
        javascriptStatus = false;
        buttonClicked = 0; // Default of add click.

        getParameters();// Also sets pLabels

        this.httpFileSender = new HttpFileSender(getParameter("destination"));
        this.httpFileSender.addObserver(this);


        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(this);
            getContentPane().setDropTarget(dt);
        } catch (TooManyListenersException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        this.setLF();

        /* Create and display the applet */
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    initComponents();
                }
            });
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        }

        createChooser();
        // Vector of failedFiles
        failedFiles = new ArrayList();
        uploadedFiles = new ArrayList();

        buttonAdd.addMouseListener(this);

        buttonRemove.addMouseListener(this);
        buttonRemove.setEnabled(false);
        buttonUpload.addMouseListener(this);
        buttonUpload.setEnabled(false);
        buttonClearFilesList.setEnabled(false);

        buttonClearFilesList.addMouseListener(this);


        sizeColumn = table.getColumn(pLabels.getLabel(1) /*+ " -KB "*/);
        sizeColumn.setMaxWidth(100);

        progBar.setMinimum(0);
        progBar.setMaximum(100);
        progBar.setValue(0);
        progBar.setStringPainted(true);

        this.panelScrollpaneContainer.remove(this.labelUploadResults);
        this.panelScrollpaneContainer.remove(this.textAreaFinalMessage);

        this.gson = new Gson();

        //Set prompt email if set on parameters
        String promptEmail = getParameter("prompt_email");
        if (promptEmail != null && promptEmail.length() > 0) {
            this.labelEmail.setText(promptEmail);
        }

        //Set email if set
        String paramEmail = getParameter("email_to_files");
        if (paramEmail != null && paramEmail.length() > 0) {
            this.textFieldEmail.setText(paramEmail);
        }

    }

    /**
     * Set look and feel
     */
    private void setLF() {
        //Set the look and feel to users OS LaF.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param message
     */
    protected void errorMessage(String message) {
        out.println("*** " + message + " ***");
    }

    /**
     *
     * @param files
     */
//    private void addUploadFiles(File[] files) {
//        this.httpFileSender.addFiles(files);
//    }
    /**
     *
     * @param name
     */
    public void addFile(String name) {
        File f = new File(name);
        this.httpFileSender.addFile(f);
    }

    private void getParameters() {

        String emailDomainsParam = getParameter("email_domains");

        if (emailDomainsParam != null && emailDomainsParam.length() > 0) {
            this.emailDomains = emailDomainsParam.split(",");
            for (int i = 0; i < emailDomains.length; i++) {
                emailDomains[i] = emailDomains[i].trim();
            }
        }

        /*
         * MAX FILE SIZE
         */
        try {
            maxFileSize = Integer.parseInt(getParameter("maxfilesize"));
        } catch (NullPointerException nullMaxSize) {
            errorMessage("maxfilesize is null");
            maxFileSize = Integer.MAX_VALUE;
        } catch (NumberFormatException nfemaxfilesize) {
            errorMessage("maxfilesize is not a number");
            maxFileSize = Integer.MAX_VALUE;
        }

        /*
         * PROXY
         */
        try {
            proxy = getParameter("proxy");
            if (proxy.equals("") || proxy == null || proxy.toLowerCase().equals("false")) {
                proxy = "";
            }
        } catch (NullPointerException nullProxy) {
            proxy = "";
            errorMessage("proxy is null");
        }

        /*
         * LANGUAGE
         */
        try {
            language = getParameter("language");
            if (language.equals("") || language == null) {
                language = "EN";
            }
        } catch (NullPointerException nullLang) {
            // Default language being set
            language = "EN";
            errorMessage("language is null");
        }
        // This method (getParameters) relies on labels from PostletLabels if
        // there is an error.
        pLabels = new PostletLabels(language, getCodeBase());

        /*
         * DESTINATION
         */
        try {
            destinationURL = new URL(getParameter("destination"));
            // Following line is for testing, and to hard code the applet to postlet.com
            //destinationURL = new URL("http://www.postlet.com/example/javaUpload.php");
        } catch (java.net.MalformedURLException malurlex) {
            // Do something here for badly formed destination, which is ESENTIAL.
            errorMessage("Badly formed destination:###" + getParameter("destination") + "###");
            JOptionPane.showMessageDialog(null, "" + pLabels.getLabel(3), "" + pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
        } catch (java.lang.NullPointerException npe) {
            // Do something here for the missing destination, which is ESENTIAL.
            errorMessage("destination is null");
            JOptionPane.showMessageDialog(null, pLabels.getLabel(4), pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
        }

        /*
         * BACKGROUND
         */
        try {
            Integer bgci = new Integer(getParameter("backgroundcolour"));
            backgroundColour = new Color(bgci.intValue());
        } catch (NumberFormatException numfe) {
            errorMessage("background colour is not a number:###" + getParameter("backgroundcolour") + "###");
        } catch (NullPointerException nullred) {
            errorMessage("background colour is null");
        }

        /*
         * TABLEHEADERFOREGROUND
         */
        try {
            Integer thfi = new Integer(getParameter("tableheadercolour"));
            columnHeadColourFore = new Color(thfi.intValue());
        } catch (NumberFormatException numfe) {
            errorMessage("table header colour is not a number:###" + getParameter("tableheadcolour") + "###");
        } catch (NullPointerException nullred) {
            errorMessage("table header colour is null");
        }

        /*
         * TABLEHEADERBACKGROUND
         */
        try {
            Integer thbi = new Integer(getParameter("tableheaderbackgroundcolour"));
            columnHeadColourBack = new Color(thbi.intValue());
        } catch (NumberFormatException numfe) {
            errorMessage("table header back colour is not a number:###" + getParameter("tableheaderbackgroundcolour") + "###");
        } catch (NullPointerException nullred) {
            errorMessage("table header back colour is null");
        }

        /*
         * FILEEXTENSIONS
         */
        try {
            fileExtensions = getParameter("fileextensions").split(",");
        } catch (NullPointerException nullfileexts) {
            errorMessage("file extensions is null");
        }

        /*
         * WARNINGMESSAGE
         */
        try {
            if (getParameter("warnmessage").toLowerCase().equals("true")) {
                warnMessage = true;
            } else {
                warnMessage = false;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("warnmessage is null");
            warnMessage = false;
        }

        /*
         * AUTOUPLOAD
         */
        try {
            if (getParameter("autoupload").toLowerCase().equals("true")) {
                autoUpload = true;
            } else {
                autoUpload = false;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("autoUpload is null");
            autoUpload = false;
        }

        /*
         * MAXTHREADS
         */
        try {
            Integer maxts = new Integer(getParameter("maxthreads"));
            maxThreads = maxts.intValue();
        } catch (NullPointerException nullmaxthreads) {
            errorMessage("maxthreads is null");
        } catch (NumberFormatException nummaxthreads) {
            errorMessage("maxthread is not a number");
        }

        /*
         * ENDPAGE
         */
        try {
            endPageURL = new URL(getParameter("endpage"));
        } catch (java.net.MalformedURLException malurlex) {
            errorMessage("endpage is badly formed:###" + getParameter("endpage") + "###");
        } catch (java.lang.NullPointerException npe) {
            errorMessage("endpage is null");
        }

        /*
         * HELPPAGE
         */
        try {
            helpPageURL = new URL(getParameter("helppage"));
        } catch (java.net.MalformedURLException malurlex) {
            errorMessage("helppage is badly formed:###" + getParameter("helppage") + "###");
        } catch (java.lang.NullPointerException npe) {
            errorMessage("helppage is null");
        }

        /*
         * HELP BUTTON
         */
        try {
            if (getParameter("helpbutton").toLowerCase().trim().equals("true")) {
                helpButton = true;
            } else {
                helpButton = false;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("helpbutton is null");
            helpButton = false;
        }

        /*
         * ADD BUTTON
         */
        try {
            if (getParameter("addbutton").toLowerCase().trim().equals("false")) {
                addButton = false;
            } else {
                addButton = true;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("addbutton is null");
            addButton = true;
        }

        /*
         * REMOVE BUTTON
         */
        try {
            if (getParameter("removebutton").toLowerCase().trim().equals("false")) {
                removeButton = false;
            } else {
                removeButton = true;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("removebutton is null");
            removeButton = true;
        }

        /*
         * UPLOAD BUTTON
         */
        try {
            if (getParameter("uploadbutton").toLowerCase().trim().equals("false")) {
                uploadButton = false;
            } else {
                uploadButton = true;
            }
        } catch (NullPointerException nullwarnmessage) {
            errorMessage("uploadbutton is null");
            uploadButton = true;
        }

        /*
         * REPLACE TABLE WITH "DROP" IMAGE
         */
        try {
            dropImage = getParameter("dropimage");
            if (dropImage != null) {
                dropImageURL = new URL(dropImage);
            }
        } catch (MalformedURLException urlexception) {
            try {
                URL codeBase = getCodeBase();
                dropImageURL = new URL(codeBase.getProtocol() + "://" + codeBase.getHost() + codeBase.getPath() + dropImage);
            } catch (MalformedURLException urlexception2) {
                errorMessage("dropimage is not a valid reference");
            }
        }
        /*
         * REPLACE TABLE WITH "DROP" IMAGE (UPLOAD IMAGE)
         */
        try {
            dropImageUpload = getParameter("dropimageupload");
            if (dropImageUpload != null) {
                dropImageUploadURL = new URL(dropImageUpload);
            }
        } catch (MalformedURLException urlexception) {
            try {
                URL codeBase = getCodeBase();
                dropImageUploadURL = new URL(codeBase.getProtocol() + "://" + codeBase.getHost() + codeBase.getPath() + dropImageUpload);
            } catch (MalformedURLException urlexception2) {
                errorMessage("dropimageupload is not a valid reference");
            }
        }
        /*
         * REPLACE TABLE WITH "DROP" IMAGE (ADDED IMAGE)
         */
        try {
            dropImageAdded = getParameter("dropimageadded");
            if (dropImageAdded != null) {
                dropImageAddedURL = new URL(dropImageAdded);
            }
        } catch (MalformedURLException urlexception) {
            try {
                URL codeBase = getCodeBase();
                dropImageAddedURL = new URL(codeBase.getProtocol() + "://" + codeBase.getHost() + codeBase.getPath() + dropImageAdded);
            } catch (MalformedURLException urlexception2) {
                errorMessage("dropimageupload is not a valid reference");
            }
        }

        /*
         * FAILED FILES WARNING
         */
        // This should be set to false if failed files are being handled in
        // javascript
        try {
            if (getParameter("failedfilesmessage").toLowerCase().trim().equals("true")) {
                failedFileMessage = true;
            } else {
                failedFileMessage = false;
            }
        } catch (NullPointerException nullfailedfilemessage) {
            errorMessage("failedfilemessage is null");
            failedFileMessage = false;
        }

        /*
         * MAX PIXELS FOR AN UPLOADED IMAGE
         */
        // This supports PNG, GIF and JPEG images only. All other images will
        // not be resized
        try {
            Integer maxps = new Integer(getParameter("maxpixels"));
            maxPixels = maxps.intValue();
        } catch (NullPointerException nullmaxpixels) {
            errorMessage("maxpixels is null");
        } catch (NumberFormatException nummaxpixels) {
            errorMessage("maxpixels is not a number");
        }
    }

    /**
     *
     */
    public void requestEmail() {
        boolean inValid = true;
        while (inValid) {
            String email = JOptionPane.showInputDialog(this, "Please enter your email");
            if (isValidEmailAddress(email)) {

                this.userEmail = email;

                String emailDomain = email.split("@")[1];

                if (!Arrays.asList(this.emailDomains).contains(emailDomain)) {

                    showErrorMessage("Ivalid email", "Please provide a valid " + this.emailDomains[0] + " email");
                } else {
                    inValid = false;
                }
            } else {
                showErrorMessage("Ivalid email", "Please enter a valid email address");
            }
        }
    }

    /**
     * This method is called from within the init() method to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panelTableContainer = new javax.swing.JPanel();
        panelScrollpaneContainer = new javax.swing.JPanel();
        scrollPaneTableContainer = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        labelUploadResults = new javax.swing.JLabel();
        textAreaFinalMessage = new javax.swing.JTextArea();
        panelToolbarContainer = new javax.swing.JPanel();
        buttonAdd = new javax.swing.JButton();
        buttonRemove = new javax.swing.JButton();
        buttonUpload = new javax.swing.JButton();
        buttonClearFilesList = new javax.swing.JButton();
        panelProgressbarContainer = new javax.swing.JPanel();
        labelTotalFiles = new javax.swing.JLabel();
        labelFilesSize = new javax.swing.JLabel();
        progBar = new javax.swing.JProgressBar();
        progCompletion = new javax.swing.JLabel();
        labelBytesSent = new javax.swing.JLabel();
        panelEmailContainer = new javax.swing.JPanel();
        labelEmail = new javax.swing.JLabel();
        textFieldEmail = new javax.swing.JTextField();
        labelSpacer = new javax.swing.JLabel();

        setBackground(new java.awt.Color(0, 204, 204));
        setPreferredSize(new java.awt.Dimension(550, 401));

        panelTableContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelTableContainer.setLayout(new java.awt.BorderLayout());

        panelScrollpaneContainer.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        panelScrollpaneContainer.setLayout(new java.awt.BorderLayout());

        table.setModel(this.httpFileSender);
        table.setMinimumSize(new java.awt.Dimension(32767, 32767));
        scrollPaneTableContainer.setViewportView(table);

        panelScrollpaneContainer.add(scrollPaneTableContainer, java.awt.BorderLayout.CENTER);

        labelUploadResults.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        labelUploadResults.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelUploadResults.setText("Upload Results");
        labelUploadResults.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        labelUploadResults.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labelUploadResults.setPreferredSize(new java.awt.Dimension(1920, 25));
        panelScrollpaneContainer.add(labelUploadResults, java.awt.BorderLayout.PAGE_START);

        textAreaFinalMessage.setEditable(false);
        textAreaFinalMessage.setColumns(20);
        textAreaFinalMessage.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        textAreaFinalMessage.setForeground(new java.awt.Color(0, 153, 51));
        textAreaFinalMessage.setLineWrap(true);
        textAreaFinalMessage.setRows(2);
        textAreaFinalMessage.setText("Your # files are now uploaded into the staging area and will be processed as soon as possible. You will receive an email when they are finished, processing generally within 5 - 15 minutes.");
        textAreaFinalMessage.setWrapStyleWord(true);
        textAreaFinalMessage.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED, new java.awt.Color(204, 204, 204), new java.awt.Color(204, 204, 204)));
        textAreaFinalMessage.setDisabledTextColor(new java.awt.Color(0, 153, 0));
        textAreaFinalMessage.setEnabled(false);
        panelScrollpaneContainer.add(textAreaFinalMessage, java.awt.BorderLayout.PAGE_END);

        panelTableContainer.add(panelScrollpaneContainer, java.awt.BorderLayout.CENTER);

        panelToolbarContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelToolbarContainer.setLayout(new java.awt.GridLayout(4, 1, 3, 8));

        buttonAdd.setText("Browse CSV Files");
        buttonAdd.setBorder(null);
        buttonAdd.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAdd.setMaximumSize(new java.awt.Dimension(91, 35));
        buttonAdd.setMinimumSize(new java.awt.Dimension(91, 35));
        buttonAdd.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        panelToolbarContainer.add(buttonAdd);

        buttonRemove.setText(" Remove Selected Files ");
        buttonRemove.setBorder(null);
        buttonRemove.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRemove.setMaximumSize(new java.awt.Dimension(93, 35));
        buttonRemove.setMinimumSize(new java.awt.Dimension(93, 35));
        buttonRemove.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        panelToolbarContainer.add(buttonRemove);

        buttonUpload.setText(" Upload ");
        buttonUpload.setBorder(null);
        buttonUpload.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonUpload.setMaximumSize(new java.awt.Dimension(43, 35));
        buttonUpload.setMinimumSize(new java.awt.Dimension(43, 35));
        buttonUpload.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        panelToolbarContainer.add(buttonUpload);

        buttonClearFilesList.setText("Clear Files List");
        panelToolbarContainer.add(buttonClearFilesList);

        panelTableContainer.add(panelToolbarContainer, java.awt.BorderLayout.EAST);

        getContentPane().add(panelTableContainer, java.awt.BorderLayout.CENTER);

        panelProgressbarContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelProgressbarContainer.setLayout(new java.awt.GridBagLayout());

        labelTotalFiles.setText("Total files: 0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        panelProgressbarContainer.add(labelTotalFiles, gridBagConstraints);

        labelFilesSize.setText("Files Size: 0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.5;
        panelProgressbarContainer.add(labelFilesSize, gridBagConstraints);

        progBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 1;
        panelProgressbarContainer.add(progBar, gridBagConstraints);

        progCompletion.setText("Upload Progress:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panelProgressbarContainer.add(progCompletion, gridBagConstraints);

        labelBytesSent.setText("Bytes Sent: 0 of 0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.weightx = 0.5;
        panelProgressbarContainer.add(labelBytesSent, gridBagConstraints);

        getContentPane().add(panelProgressbarContainer, java.awt.BorderLayout.PAGE_END);

        panelEmailContainer.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelEmailContainer.setLayout(new java.awt.BorderLayout(2, 5));

        labelEmail.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        labelEmail.setText("Email:");
        panelEmailContainer.add(labelEmail, java.awt.BorderLayout.WEST);

        textFieldEmail.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        textFieldEmail.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                textFieldEmailFocusLost(evt);
            }
        });
        panelEmailContainer.add(textFieldEmail, java.awt.BorderLayout.CENTER);

        labelSpacer.setText("                                                                                                                                          ");
        panelEmailContainer.add(labelSpacer, java.awt.BorderLayout.EAST);

        getContentPane().add(panelEmailContainer, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void textFieldEmailFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textFieldEmailFocusLost
        // TODO add your handling code here:        
    }//GEN-LAST:event_textFieldEmailFocusLost

    /**
     *
     * @return
     */
    public boolean validateMail() {
        String email = this.textFieldEmail.getText();
        if (isValidEmailAddress(email)) {

            this.userEmail = email;

            String emailDomain = email.split("@")[1];

            if (!Arrays.asList(this.emailDomains).contains(emailDomain)) {
                showErrorMessage("Ivalid email", "Please provide a valid " + this.emailDomains[0] + " email");
                return false;
            } else {
                //inValid = false;
                return true;
            }
        } else {
            showErrorMessage("Ivalid email", "Please enter a valid email address");
            return false;
        }

    }

    public boolean isValidEmailAddress(String email) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(".+@.+\\.[a-z]+");
        java.util.regex.Matcher m = p.matcher(email);
        boolean matchFound = m.matches();
        return matchFound;
    }

    /**
     *
     * @param title
     * @param msg
     */
    public void showErrorMessage(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAdd;
    private javax.swing.JButton buttonClearFilesList;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JButton buttonUpload;
    private javax.swing.JLabel labelBytesSent;
    private javax.swing.JLabel labelEmail;
    private javax.swing.JLabel labelFilesSize;
    private javax.swing.JLabel labelSpacer;
    private javax.swing.JLabel labelTotalFiles;
    private javax.swing.JLabel labelUploadResults;
    private javax.swing.JPanel panelEmailContainer;
    private javax.swing.JPanel panelProgressbarContainer;
    private javax.swing.JPanel panelScrollpaneContainer;
    private javax.swing.JPanel panelTableContainer;
    private javax.swing.JPanel panelToolbarContainer;
    private javax.swing.JProgressBar progBar;
    private javax.swing.JLabel progCompletion;
    private javax.swing.JScrollPane scrollPaneTableContainer;
    private javax.swing.JTable table;
    private javax.swing.JTextArea textAreaFinalMessage;
    private javax.swing.JTextField textFieldEmail;
    // End of variables declaration//GEN-END:variables

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == buttonAdd && buttonAdd.isEnabled()) {
            //addClick();

            //chooser.setLocation(e.getLocationOnScreen());
            //System.out.println("e.getLocationOnScreen: "+e.getLocationOnScreen().toString());
            this.buttonAddClicked();
        }
        if (e.getSource() == buttonUpload && buttonUpload.isEnabled()) {
            //uploadClick();
            this.buttonUpladClicked();
        }
        if (e.getSource() == buttonRemove && buttonRemove.isEnabled()) {
            //msgDiag("Remove2!!");
            this.buttonRemoveClicked();
            //removeClick();
        }
        if (e.getSource() == buttonClearFilesList && buttonClearFilesList.isEnabled()) {
            this.buttonClearFilesListClicked();
        }


//        if (e.getSource() == help && help.isEnabled()) {
//            helpClick();
//        }
    }

    /**
     * Button add was clicked
     */
    public void buttonAddClicked() {

        if (!this.validateMail()) {
            return;
        }

        int returnVal = chooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] tempFiles = chooser.getSelectedFiles();

        //this.addUploadFiles(tempFiles);
        this.onFilesAdded(tempFiles);

    }

    /**
     *
     * @param tempFiles
     */
    private void onFilesAdded(File[] tempFiles) {

        this.httpFileSender.addFiles(tempFiles);

        if (this.httpFileSender.getFiles().size() > 0) {
            tableUpdate();
            this.buttonUpload.setEnabled(true);
            this.buttonRemove.setEnabled(true);
            this.buttonClearFilesList.setEnabled(true);

            if (autoUpload) {
                //uploadClick();
                this.buttonUpladClicked();
            }
        }
    }

    /**
     * Button upload was clicked
     */
    private void buttonUpladClicked() {

        if (!this.validateMail()) {
            return;
        }

        int filesToSend = this.httpFileSender.getFiles().size();

        if (filesToSend == 0) {
            return;
        }

        this.buttonAdd.setEnabled(false);
        this.buttonUpload.setEnabled(false);
        this.buttonRemove.setEnabled(false);

        sentBytes = 0;

        this.httpFileSender.setEmail(this.userEmail);

        new Thread(httpFileSender).start();
    }

    /**
     *
     */
    public void buttonRemoveClicked() {

        int[] selected = this.table.getSelectedRows();
        if (selected.length == 0) {
            return;
        }

//        System.out.println("SELECTED: " + gson.toJson(selected)+". File: "+this.httpFileSender.getFiles().get(selected[0]).getName() );

        this.httpFileSender.removeFilesAt(selected);

        this.tableUpdate();

        if (this.httpFileSender.getFiles().isEmpty()) {
            this.buttonUpload.setEnabled(false);
            this.buttonRemove.setEnabled(false);
            this.buttonClearFilesList.setEnabled(false);
            this.progBar.setValue(0);
        }
    }

    /**
     *
     */
    private void buttonClearFilesListClicked() {
        this.startOver();
    }

    /**
     * Start with a new empty table and all numbers in zero
     */
    private void startOver() {
        this.resetTable();
        this.removeResultLabel();
        this.removeFinalMessage();
        this.buttonClearFilesList.setEnabled(false);
        this.buttonUpload.setEnabled(false);
        this.buttonRemove.setEnabled(false);
    }

    /**
     * Create chooser
     */
    private void createChooser() {

        chooser = new JFileChooser("D:\\21GNET\\SQL");

        progBar.setValue(0);
        if (fileExtensions != null) {

            UploaderFileFilter filter = new UploaderFileFilter();
            for (int i = 1; i < fileExtensions.length; i++) {
                filter.addExtension(fileExtensions[i]);
            }
            filter.setDescription(fileExtensions[0]);

            chooser.addChoosableFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(filter);
        } else {
            chooser.setFileFilter(chooser.getAcceptAllFileFilter());
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(pLabels.getLabel(14));

    }

    @Override
    public void mousePressed(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {

        if (!this.validateMail()) {
            return;
        }

        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable transferable = dtde.getTransferable();
        try {
            List<File> dropppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

            if (!dropppedFiles.isEmpty()) {

                if (!this.buttonAdd.isEnabled()) {
                    this.startOver();
                }
                //this.httpFileSender.addFiles( dropppedFiles.toArray(new File[]{}) );
                this.onFilesAdded(dropppedFiles.toArray(new File[]{}));
            }

        } catch (UnsupportedFlavorException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PostApplet.class.getName()).log(Level.SEVERE, null, ex);
        }

        dtde.dropComplete(true);
    }

    private void tableUpdate() {
        totalBytes = 0;

        //Heiner overwrite with actual files to send
        files = this.httpFileSender.getFiles().toArray(new File[]{});
        for (int i = 0; i < files.length; i++) {
            totalBytes += (int) files[i].length();
        }

//        int i = 0;
//        // FIXME - THIS SEEMS SILLY!********************************************
//        String[][] rowData = new String[files.length][2];
//        while (i < files.length) {
//            rowData[i][0] = files[i].getName();
//            rowData[i][1] = "" + files[i].length();
//            i++;
//        }

        // *********************************************************************
        //tabledata.formatTable(rowData, i);


        //sizeColumn.setMaxWidth(100);
        //sizeColumn.setMinWidth(100);

        this.labelFilesSize.setText("Files Size: " + this.httpFileSender.formatFileSize(totalBytes));
        this.labelTotalFiles.setText("Total files: " + files.length);
        this.labelBytesSent.setText("Bytes Sent: 0 of " + totalBytes);

        progBar.setMaximum(totalBytes);

        //repaint();


    }

    /**
     *
     */
    public void addClick() {
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] tempFiles = chooser.getSelectedFiles();
            ArrayList<File> filesForUpload = new ArrayList();
            for (int i = 0; i < tempFiles.length; i++) {
                if (tempFiles[i].isDirectory()) {
                    File[] subDirFiles = tempFiles[i].listFiles();
                    for (int j = 0; j < subDirFiles.length; j++) {
                        if (subDirFiles[j].isFile()) {
                            if (subDirFiles[j].length() < maxFileSize) {
                                filesForUpload.add(subDirFiles[j]);
                            } else {
                                fileTooBig(subDirFiles[j]);
                            }
                        }
                    }

                } else {
                    if (tempFiles[i].length() < maxFileSize) {
                        filesForUpload.add(tempFiles[i]);
                    } else {
                        fileTooBig(tempFiles[i]);
                    }
                }
            }
            if (files == null) {
                files = new File[0];
            }
            tempFiles = new File[filesForUpload.size() + files.length];
            System.arraycopy(files, 0, tempFiles, 0, files.length);
            for (int i = 0; i < filesForUpload.size(); i++) {
                tempFiles[i + files.length] = filesForUpload.get(i);
            }
            files = tempFiles;
            tableUpdate();
        }
        if (files != null && files.length > 0) {
            buttonUpload.setEnabled(true);
            buttonRemove.setEnabled(true);
            if (dropImageURL != null && dropImageAddedURL != null) {
                iconLabel.setIcon(dropIconAdded);
                repaint();
            }
        }

        if (files != null && autoUpload) {
            //uploadClick();
            this.buttonUpladClicked();
        }

        createChooser();// Not sure if this is necesary. FIXME

    }

    /**
     * Upload button clicked
     */
    public void uploadClick() {
        if (files != null && files.length > 0) {
            if (warnMessage) {
                JOptionPane.showMessageDialog(null, pLabels.getLabel(11), pLabels.getLabel(12), JOptionPane.INFORMATION_MESSAGE);
            }
            buttonAdd.setEnabled(false);
            buttonRemove.setEnabled(false);
            //help.setEnabled(false);
            buttonUpload.setEnabled(false);
            if (dropImageURL != null && dropImageUploadURL != null) {
                iconLabel.setIcon(dropIconUpload);
                repaint();
            }
            sentBytes = 0;
            progBar.setMaximum(totalBytes);
            progBar.setMinimum(0);
//            try {
//                upMan = new UploadManager1(files, this, destinationURL, maxThreads);
//            } catch (java.lang.NullPointerException npered) {
//                upMan = new UploadManager1(files, this, destinationURL);
//            }
//            upMan.start();
        }
    }

    /**
     * Button remove clicked
     */
    private void removeClick() {

        msgDiag("Sel cols: " + table.getSelectedColumnCount());

        if (table.getSelectedRowCount() > 0) {
            File[] fileTemp = new File[files.length - table.getSelectedRowCount()];
            int[] selectedRows = table.getSelectedRows();
            Arrays.sort(selectedRows);
            int k = 0;
            for (int i = 0; i < files.length; i++) {
                if (Arrays.binarySearch(selectedRows, i) < 0) {
                    fileTemp[k] = files[i];
                    k++;
                }
            }
            files = fileTemp;
            tableUpdate();
        }
        if (files.length == 0) {
            buttonUpload.setEnabled(false);
            buttonRemove.setEnabled(false);
        }
    }

    /**
     *
     * @param a
     */
    protected synchronized void setProgress(int a) {

        if (totalBytes > 0) {
            sentBytes += a;
//
//            progBar.setValue(sentBytes);
//
//            //labelBytesSent.setText("Bytes Sent: " + sentBytes + "/" + totalBytes);
//
//            if ((sentBytes * 100) / totalBytes > percentComplete) {
//                percentComplete = (sentBytes * 100) / totalBytes;
//                try {
//                    jso.eval("try{" + postletJS[0] + "(" + percentComplete + ");}catch(e){;}");
//                } catch (netscape.javascript.JSException | NullPointerException ex) {
//                    errorMessage("Unable to send status to Javascript");
//                    msgDiag(ex.getClass().getName() + ": " + ex.getMessage());
//                }
//            }
            if (sentBytes >= totalBytes) {

                if (sentBytes == totalBytes) {
                    // Upload is complete. Check for failed files.
                    if (failedFiles.size() > 0 && failedFileMessage) {
                        // There is at least one failed file. Show an error message
                        String failedFilesString = "\r\n";
                        for (int i = 0; i < failedFiles.size(); i++) {
                            File tempFile = (File) failedFiles.get(i);
                            failedFilesString += tempFile.getName() + "\r\n";
                        }
                        JOptionPane.showMessageDialog(null, pLabels.getLabel(16) + ":" + failedFilesString, pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
                    }
                    progCompletion.setText(pLabels.getLabel(2));

                    if (endPageURL != null) {
                        errorMessage("Changing browser page");
                        getAppletContext().showDocument(endPageURL);
                    } else {
//                        try {
//                            // Just ignore this error, as it is most likely from the endpage
//                            // not being set.
//                            // Attempt at calling Javascript after upload is complete.
//                            //errorMessage("Executing: " + postletJS[1] + "();");
//                            //jso.eval("try{" + postletJS[1] + "();}catch(e){;}");
//                        } catch (netscape.javascript.JSException jse) {
//                            // Not to worry, just means the end page and a postletFinished
//                            // method aren't set. Just finish, and let the web page user
//                            // exit the page
//                            //errorMessage("postletFinished, and End page unset");
//                        } catch (NullPointerException ex) {
//                            //errorMessage("postletFinished, and End page unset, and JS not executed");
//                            //msgDiag(ex.getClass().getName() + ": " + ex.getMessage());
//                        }
                    }
                    JOptionPane.showMessageDialog(null, "Sent all", "Sent all", JOptionPane.ERROR_MESSAGE);
                }

                //if (sentBytes > totalBytes) {
                //    JOptionPane.showMessageDialog(null, "sentBytes > totalBytes!!", "sentBytes > totalBytes!!", JOptionPane.ERROR_MESSAGE);
                //}

                // Reset the applet
                totalBytes = 0;
//                percentComplete = 0;
                progBar.setValue(0);
                progBar.repaint();
                files = new File[0];
                tableUpdate();
                buttonAdd.setEnabled(true);
                //help.setEnabled(true);
                if (dropImageURL != null && dropImageUploadURL != null) {
                    iconLabel.setIcon(dropIcon);
                }
                failedFiles.clear();
                uploadedFiles.clear();
                repaint();

            } else {
                //errorMessage("Still not sent all");
            }
        }
    }

    /**
     *
     * @param txt
     */
    private void msgDiag(String txt) {
        JOptionPane.showMessageDialog(null, txt, txt, JOptionPane.INFORMATION_MESSAGE);
    }

    public void fileTooBig(File f) {
        errorMessage("file too big: " + f.getName() + " - " + f.length());
        if (warnMessage) {
            JOptionPane.showMessageDialog(null, "" + pLabels.getLabel(1) + " - " + f.getName(), "" + pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
        }
        addFailedFile(f);
//        try {
//            jso.eval("try{" + postletJS[3] + "(0,'" + f.getName().replace("'", "`") + "');}catch(e){;}");
//        } catch (netscape.javascript.JSException | NullPointerException jsepf) {
//            errorMessage("Unable to send info about 'file too big'");
//        }
    }

    public void fileNotAllowed(File f) {
        errorMessage("file not allowed: " + f.getName());
        addFailedFile(f);
//        try {
//            jso.eval("try{" + postletJS[3] + "(1,'" + f.getName().replace("'", "`") + "');}catch(e){;}");
//        } catch (netscape.javascript.JSException jsepf) {
//            errorMessage("Unable to send info about 'file not allowed'");
//        }
    }

    public void fileNotFound(File f) {
        errorMessage("file not found: " + f.getName());
        addFailedFile(f);
//        try {
//            jso.eval("try{" + postletJS[3] + "(2,'" + f.getName().replace("'", "`") + "');}catch(e){;}");
//        } catch (netscape.javascript.JSException | NullPointerException jsepf) {
//            errorMessage("Unable to send info about 'file not found'");
//        }
    }

    public void fileUploadFailed(File f) {
        errorMessage("file upload failed: " + f.getName());
        addFailedFile(f);
//        try {
//            jso.eval("try{" + postletJS[3] + "(3,'" + f.getName().replace("'", "`") + "');}catch(e){;}");
//        } catch (netscape.javascript.JSException | NullPointerException jsepf) {
//            errorMessage("Unable to send info about 'file upload failed'");
//        }
    }

    public void helpClick() {
        // Open a web page in another frame/window
        // Unless specified as a parameter, this will be a help page
        // on the postlet website.

        try {
            getAppletContext().showDocument(helpPageURL, "_blank");
        } catch (NullPointerException nohelppage) {
            // Show a popup with help instead!
            try {
                getAppletContext().showDocument(new URL("http://www.postlet.com/help/"), "_blank");
            } catch (MalformedURLException mfue) {
            }// Hard coded URL, no need for catch
        }

    }

//    public String getCookie() {
//
//        // Method reads the cookie in from the Browser using the LiveConnect object.
//        // May also add an option to set the cookie using an applet parameter FIXME!
////        try {
////            String cookie = (String) jso.eval("try{document.cookie;}catch(e){;}");
////            errorMessage("Cookie is:###" + cookie + "###");
////            return cookie;
////        } catch (Exception e) {
////            errorMessage("Failed to get cookie");
////            return "";
////        }
//    }
    /**
     * Cancel all upload of files.
     */
//    public void cancelUpload() {
//        upMan.cancelUpload();
//        errorMessage("Canceled upload");
//        if (totalBytes > 0) {
//            setProgress(totalBytes + 1);
//        }
//    }
    /**
     * This method has been altered due to IE (and Safari) being shite (it did
     * return an array - oh well, backwards stepping).
     */
    public String getFailedFiles() {
        if (failedFiles.size() > 0) {
            String failedFilesString = "";
            // Return a "/" delimited string (as "/" is not a legal character).
            for (int i = 0; i < failedFiles.size(); i++) {
                File tempFile = failedFiles.get(i);
                failedFilesString += tempFile.getName() + "/";
            }
            return failedFilesString.replace("'", "`");
            /*
             * String [] arrayFailedFiles = new String[failedFiles.size()]; for
             * (int i=0; i<failedFiles.size(); i++){ File tempFile =
             * (File)failedFiles.elementAt(i); arrayFailedFiles[i] =
             * tempFile.getName(); } return arrayFailedFiles;
             */
        }
        return null;
    }

    /**
     * This method returns all the files that have been added to Postlet
     */
    public String getFiles() {
        String fileString = "" + files.length;
        for (int i = 0; i < files.length; i++) {
            fileString += "/" + files[i].getName();
        }
        return fileString.replace("'", "`");
    }

    public String getUploadedFiles() {
        if (uploadedFiles.size() > 0) {
            String uploadedFilesString = "";
            // Return a "/" delimited string (as "/" is not a legal character).
            for (int i = 0; i < uploadedFiles.size(); i++) {
                File tempFile = uploadedFiles.get(i);
                uploadedFilesString += tempFile.getName() + "/";
            }
            return uploadedFilesString.replace("'", "`");
            /*
             * String [] arrayUploadedFiles = new String[uploadedFiles.size()];
             * for (int i=0; i<uploadedFiles.size(); i++){ File tempFile =
             * (File)uploadedFiles.elementAt(i); arrayUploadedFiles[i] =
             * tempFile.getName(); } return arrayUploadedFiles;
             */
        }
        return null;
    }

    public void changedDestination(String destination) {
        // Change the destination before upload.
        try {
            destinationURL = new URL(destination);
        } catch (java.net.MalformedURLException malurlex) {
            // Do something here for badly formed destination, which is ESENTIAL.
            errorMessage("Badly formed destination:###" + destination + "###");
            JOptionPane.showMessageDialog(null, "" + pLabels.getLabel(3), "" + pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
        } catch (java.lang.NullPointerException npe) {
            // Do something here for the missing destination, which is ESENTIAL.
            errorMessage("destination is null");
            JOptionPane.showMessageDialog(null, pLabels.getLabel(4), pLabels.getLabel(5), JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void doRemoveFile(String number) {
        try {
            int fileNumber = Integer.parseInt(number);
            if (files.length > fileNumber && fileNumber > -1) {
                File[] fileTemp = new File[files.length - 1];
                int j = 0;
                for (int i = 0; i < files.length; i++) {
                    if (i != fileNumber) {
                        fileTemp[j] = files[i];
                        j++;
                    }
                }
                files = fileTemp;
                tableUpdate();
                if (files.length == 0) {
                    buttonUpload.setEnabled(false);
                    buttonRemove.setEnabled(false);
                }
            }
//            try {
//                jso.eval("try{" + postletJS[2] + "('" + getFiles() + "');}catch(e){;}");
//            } catch (netscape.javascript.JSException | NullPointerException jsepf) {
//                errorMessage("Unable to send info about files added");
//            }
        } catch (NumberFormatException nfe) {
            errorMessage("removeFile not a number");
        }
    }

    public void postletAdd() {

        // Set a variable so that the listening thread can call the add click method
        buttonClicked = 0;
        javascriptStatus = true;
    }

    public void removeFile(String number) {
        // As above
        buttonClicked = 3;
        fileToRemove = number;
        javascriptStatus = true;
    }

    // Adds a file that HASN'T uploaded to an array. Once uploading is complete,
    // these can be listed with a popup box.
    public void addFailedFile(File f) {
        failedFiles.add(f);
    }

    // Adds a file that HAS uploaded to an array. These are passed along with
    // failed files to a javascript method.
    public void addUploadedFile(File f) {

        System.out.println("f nul?" + (f == null));
        System.out.println("uploadedFiles nul?" + (uploadedFiles == null));

        uploadedFiles.add(f);
    }

    public String getFileToRemove() {
        return fileToRemove;
    }

    public boolean isUploadEnabled() {

        return buttonUpload.isEnabled();
    }

    public boolean getJavascriptStatus() {
        return javascriptStatus;
    }

    public void setJavascriptStatus(boolean javascriptStatus) {
        this.javascriptStatus = javascriptStatus;
    }

    public int getButtonClicked() {
        return buttonClicked;
    }

    public void setButtonClicked(int buttonClicked) {
        this.buttonClicked = buttonClicked;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(int maxPixels) {
        this.maxPixels = maxPixels;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void update(Object arg) {

        if (arg instanceof Long) {

            int writtenBytes = ((Long) arg).intValue();
            //double inc = 100/(totalBytes/writtenBytes);
            sentBytes = writtenBytes;
            //System.out.println("--update: Wrt:" + writtenBytes + " Tot:" + totalBytes + " Snt:" + sentBytes);
            this.labelBytesSent.setText("Bytes Sent: " + sentBytes + " of " + totalBytes);
            this.progBar.setValue(sentBytes);
        } else if (arg instanceof String) {
            System.out.println("RESP: " + arg);
            UploadResponse response = gson.fromJson((String) arg, UploadResponse.class);
            this.labelBytesSent.setText("Bytes Sent: " + totalBytes + " of " + totalBytes);
            this.updateTable(response);
        }
    }

    /**
     * Clean and rest the table
     */
    private void resetTable() {

        this.httpFileSender.clearFiles();
        this.table.setModel(this.httpFileSender);
        this.buttonAdd.setEnabled(true);
        this.progBar.setValue(0);
        this.labelTotalFiles.setText("Total files: 0");
        this.labelFilesSize.setText("Files Size: 0");
        this.labelBytesSent.setText("Bytes Sent: 0 of 0");
    }

    /**
     *
     * @param response
     */
    private void updateTable(final UploadResponse response) {

        final int totalFiles = httpFileSender.getFailedLocalFiles().size() + response.getSuccessCount() + response.getFailedCount();

        final ArrayList<String> allFiles = new ArrayList<String>(this.httpFileSender.getFailedLocalFiles());
        allFiles.addAll(Arrays.asList(response.getFailed()));
        //final ArrayList<String> allFiles = new ArrayList(Arrays.asList(response.getFailed()));
        allFiles.addAll(Arrays.asList(response.getSuccess()));
        
        final ArrayList<String> statuses = new ArrayList(this.httpFileSender.getFailedLocalFilesErrors());
        statuses.addAll(Arrays.asList(response.getErrors()));
        //final ArrayList<String> statuses = new ArrayList(Arrays.asList(response.getErrors()));
        statuses.addAll(Arrays.asList(response.getSuccess()));

        TableModel model;
        model = new AbstractTableModel() {
            String[] cols = new String[]{"Filename", "Status"};

            @Override
            public int getRowCount() {
                return totalFiles;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return this.cols[columnIndex];
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
                
                int totalFailed = httpFileSender.getFailedLocalFiles().size() + response.getFailedCount();
                
                if (rowIndex < totalFailed) {
                    if (columnIndex == 0) {
                        String text = "<html><font color=red><b>" + allFiles.get(rowIndex) + "</b></font></html>";
                        return text;
                    } else {
                        return "<html><font color=red><b>" + statuses.get(rowIndex) + "</b></font></html>";
                    }
                } else {
                    if (columnIndex == 0) {
                        return "<html><font color=green >" + allFiles.get(rowIndex) + "</font></html>";
                    } else {
                        return "<html><font color=green>Upload successful</font></html>";
                    }
                }
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void addTableModelListener(TableModelListener l) {
            }

            @Override
            public void removeTableModelListener(TableModelListener l) {
            }
        };

        this.showResultLabel();

        if (response.getSuccessCount() > 0) {
            this.textAreaFinalMessage.setText(response.getFinalMessage());
            this.showFinalMessage();
        }


        this.table.setModel(model);


    }

    private void showResultLabel() {

        this.panelScrollpaneContainer.add(this.labelUploadResults, BorderLayout.PAGE_START);

        this.panelScrollpaneContainer.revalidate();
        this.panelScrollpaneContainer.repaint();

    }

    private void removeResultLabel() {

        this.panelScrollpaneContainer.remove(this.labelUploadResults);
        this.panelScrollpaneContainer.revalidate();
        this.panelScrollpaneContainer.repaint();

    }

    private void showFinalMessage() {
        this.panelScrollpaneContainer.add(this.textAreaFinalMessage, BorderLayout.PAGE_END);

        this.panelScrollpaneContainer.revalidate();
        this.panelScrollpaneContainer.repaint();

    }

    private void removeFinalMessage() {
        this.panelScrollpaneContainer.remove(this.textAreaFinalMessage);
        this.panelScrollpaneContainer.revalidate();
        this.panelScrollpaneContainer.repaint();
    }
}
