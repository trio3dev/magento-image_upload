package com.dakidev.magento_image_upload.domain;

public interface ProgressMonitor {
 
    void progressUpdated(double progress, String message);
}