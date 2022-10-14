package com.dakidev.magento_image_upload.swing_worker;


import com.dakidev.magento_image_upload.domain. ImagesUploader;

import javax.swing.*;
import java.util.List;

public class ImagesUploaderWorker extends SwingWorker<Boolean, String> {
    
    JProgressBar jProgressBar;
    JTextArea consoleTextArea;
    int max;
    String selectedFile;
    String selectedImagesFolder;
    
    
    public ImagesUploaderWorker(JProgressBar jProgressBar, int max, String selectedFile, String selectedImagesFolder, JTextArea consoleTextArea) {
        this.jProgressBar = jProgressBar;
        this.max = max;
        this.selectedFile = selectedFile;
        this.selectedImagesFolder = selectedImagesFolder;
        this.consoleTextArea = consoleTextArea;
        
        addPropertyChangeListener(evt -> {
            
            Integer progress;
            if ("progress".equals(evt.getPropertyName()))
                progress = (Integer) evt.getNewValue();
            else
                progress = 0;
            
            jProgressBar.setValue(progress);
            jProgressBar.setString("uploading images...");
        });
    }
    
    @Override
    protected void process(List<String> messages) {
        
        String message = messages.get(messages.size() - 1);
        
        if (message != null)
            consoleTextArea.setText(consoleTextArea.getText() + "\n" + message);
        
    }
    
    @Override
    protected Boolean doInBackground() {
        
        try {
             new ImagesUploader().updateExcelFileImages(selectedFile, selectedImagesFolder, (progress, message) -> {
                setProgress((int) (progress * 100));
                publish(message);
            });
             return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    @Override
    protected void done() {
        
        boolean success = consoleTextArea.getText() == null;
        String message;
        int messageType;
        if (success) {
            message = "Success";
            messageType = JOptionPane.INFORMATION_MESSAGE;
        } else {
            message = "Error";
            messageType = JOptionPane.ERROR_MESSAGE;
        }
    
        jProgressBar.setString("");
        jProgressBar.setValue(0);
        
        JOptionPane.showMessageDialog(jProgressBar.getParent(), message, message, messageType);
    }
}