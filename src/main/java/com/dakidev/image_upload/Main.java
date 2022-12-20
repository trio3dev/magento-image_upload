package com.dakidev.image_upload;

import com.dakidev.image_upload.domain.ImagesUploader;

public class Main {
    
    public static void main(String[] args) throws Exception {
        
        String baseUrl = "/home/anonyme/Downloads/images/";
        String filePath = "/home/anonyme/Downloads/dest_zid_products_1 (9)_2.xlsx";
        
        new ImagesUploader().updateExcelFileImages(filePath, baseUrl, null, 11, 11, ",", null, null, 1);
        
    }
    
}
