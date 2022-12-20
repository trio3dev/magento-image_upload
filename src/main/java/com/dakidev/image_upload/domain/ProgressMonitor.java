package com.dakidev.image_upload.domain;

public interface ProgressMonitor {
    
    void progressUpdated(double progress);
    
    void senMessage(String message);
    
}