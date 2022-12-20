package com.dakidev.image_upload.swing_worker;


import com.dakidev.MATCHING_TYPE;
import com.dakidev.image_upload.domain.ImagesUploader;
import com.dakidev.image_upload.domain.ProgressMonitor;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ImagesUploaderWorker extends SwingWorker<Boolean, String> {
    
    private final JProgressBar jProgressBar;
    private final JTextArea consoleTextArea;
    private final int imageIndexColumn;
    private final int matchingIndexColumn;
    private final int headerRow;
    private final MATCHING_TYPE matchingMethod;
    private final String imageExtensions;
    private final String selectedFile;
    private final String selectedImagesFolder;
    private final String imagesSeparator;
    Set<String> messages = new LinkedHashSet<>();
    
    
    public ImagesUploaderWorker(JProgressBar jProgressBar, JTextArea consoleTextArea, int imageIndexColumn, int matchingIndexColumn, int headerRow, MATCHING_TYPE matchingMethod, String imageExtensions, String selectedFile, String selectedImagesFolder, String imagesSeparator) {
        this.jProgressBar = jProgressBar;
        this.consoleTextArea = consoleTextArea;
        this.imageIndexColumn = imageIndexColumn;
        this.matchingIndexColumn = matchingIndexColumn;
        this.headerRow = headerRow;
        this.matchingMethod = matchingMethod;
        this.imageExtensions = imageExtensions;
        this.selectedFile = selectedFile;
        this.selectedImagesFolder = selectedImagesFolder;
        this.imagesSeparator = imagesSeparator;
    
        addPropertyChangeListener(jProgressBar);
    
    }
    
    @Override
    protected void process(List<String> chunks) {
        
        String chunk = chunks.get(chunks.size() - 1);
        
        chunks.stream().filter(s -> s.contains("message-")).forEach(s -> messages.add(s.replace("message-", "")));

        consoleTextArea.setText(String.join("\n", messages));

        if (chunk != null && !chunk.contains("message-"))
            jProgressBar.setString(chunk);
    }
    
    @Override
    protected Boolean doInBackground() {
        
        try {
            ProgressMonitor progressMonitor = new ProgressMonitor() {
                @Override
                public void progressUpdated(double progress) {
            
                    setProgress((int) (progress * 100));
            
                }
                @Override
                public void senMessage(String message) {
                    publish(message);
                }
            };
            
            new ImagesUploader().updateExcelFileImages(selectedFile, selectedImagesFolder, progressMonitor, imageIndexColumn, matchingIndexColumn, imagesSeparator,
                    matchingMethod, imageExtensions, headerRow);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    @Override
    protected void done() {
    
        String title;
        String message = null;
        int messageType;
        
        try {
            boolean success = get();
        
            if (success) {
                if (messages.isEmpty()) {
                    title = "Success";
                    messageType = JOptionPane.INFORMATION_MESSAGE;
                } else {
                    title = "Warning";
                    messageType = JOptionPane.WARNING_MESSAGE;
                    message = "Impossible to load some images, please consult the console for more details";
                }
            } else {
                title = "Error";
                messageType = JOptionPane.ERROR_MESSAGE;
                message = "Please consult the console for more details";
    
            }
        
            JOptionPane.showMessageDialog(jProgressBar.getParent(), message, title, messageType);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            jProgressBar.setValue(0);
            jProgressBar.setString("");
        }
    }
    
    private void addPropertyChangeListener(JProgressBar jProgressBar) {
        addPropertyChangeListener(evt -> {
            
            Integer progress;
            if ("progress".equals(evt.getPropertyName()))
                progress = (Integer) evt.getNewValue();
            else
                progress = 0;
            
            jProgressBar.setValue(progress);
//            jProgressBar.setString("uploading images...");
        });
    }
    
}