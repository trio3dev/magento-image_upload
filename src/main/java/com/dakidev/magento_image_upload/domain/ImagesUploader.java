package com.dakidev.magento_image_upload.domain;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ImagesUploader {
    
    public static final int INDEX_OF_IMAGES = 11;
    
    Logger logger = LogManager.getLogger(ImagesUploader.class);
    
    public ImagesUploader() {
    }
    
    public static void main(String[] args) throws Exception {
        
        String baseUrl = "/home/anonyme/workspace/mostaql-zid-585330/product/";
        String filePath = Objects.requireNonNull(ImagesUploader.class.getClassLoader().getResource("dest_zid.xlsx")).toURI().getPath();
        
        new ImagesUploader().updateExcelFileImages(filePath, baseUrl, null);
        
    }
    
    public void updateExcelFileImages(String filePath, String baseUrl, ProgressMonitor progressMonitor) throws IOException {
        
        
        File xlsxFile = new File(filePath);
        
        File destinationFile = createDestinationFile(filePath, xlsxFile);
        
        Map<Integer, String> imagesMap = new HashMap<>();
        
        try (FileInputStream inputStream = new FileInputStream(xlsxFile);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            
            writeWorkbookOnShutdown(destinationFile, imagesMap);
            
            Sheet sheet = workbook.getSheetAt(0);
            
            double rowCount = sheet.getLastRowNum();
            
            for (int i = 1; i <= rowCount; i++) {
                
                updateProgressMonitor(progressMonitor, i / rowCount, null);
                
                Cell imagesCell = sheet.getRow(i).getCell(INDEX_OF_IMAGES);
                
                if (imagesCell != null) {
                    String images = imagesCell.getRichStringCellValue().toString();
                    StringBuilder imagesUploaded = new StringBuilder();
                    
                    for (String s : images.split(",")) {
                        try {
                            imagesUploaded.append(uploadImages(baseUrl, s)).append(",");
                            
                        } catch (Exception e) {
                            imagesUploaded.append(s).append(",");
                            logger.error(e);
                            
                            updateProgressMonitor(progressMonitor, i / rowCount, (i + " : " + images + "  > " + e.getMessage()));
                        }
                    }
                    imagesMap.put(i, imagesUploaded.toString());
                }
            }
            
        } catch (IOException e) {
            logger.error(e);
        }
        
        updateExcelFile(destinationFile, imagesMap);
    }
    
    private void updateExcelFile(File destinationFile, Map<Integer, String> imagesMap) throws IOException {
        
        try (
                FileInputStream inputStream = new FileInputStream(destinationFile);
                Workbook workbook = WorkbookFactory.create(inputStream);
                OutputStream outputStream = new FileOutputStream(destinationFile);) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                
                String uploadedImages = imagesMap.get(i);
                
                if (sheet.getRow(i) != null && uploadedImages != null) {
                    updateImageCell(sheet.getRow(i), uploadedImages.replaceAll(",$", ""));
                }
            }
            
            workbook.write(outputStream);
        }
    }
    
    private File createDestinationFile(String filePath, File xlsxFile) throws IOException {
        File destinationFile;
        try (FileInputStream inputStream = new FileInputStream(xlsxFile);) {
            
            destinationFile = new File(filePath.replaceAll(".xlsx", "_1.xlsx"));
            
            FileUtils.copyInputStreamToFile(inputStream, destinationFile);
        }
        return destinationFile;
    }
    
    private void updateProgressMonitor(ProgressMonitor progressMonitor, double progress, String message) {
        if (progressMonitor != null) progressMonitor.progressUpdated(progress, message);
    }
    
    private void updateImageCell(Row currentRow, String images) {
        Cell imagesCell;
        imagesCell = currentRow.createCell(INDEX_OF_IMAGES);
        imagesCell.setCellValue(images.replaceAll(",$", ""));
    }
    
    private void writeWorkbookOnShutdown(File destinationFile, Map<Integer, String> imagesMap) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                updateExcelFile(destinationFile, imagesMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
    
    private static String uploadImages(String baseUrl, String image) throws Exception {
        
        if (!image.isEmpty() && !image.contains("http")) {
            if (Files.exists(Paths.get(baseUrl + image))) {
                return Uploader.upload(baseUrl + image);
            } else {
                throw new Exception("FileNot Found");
            }
            
        } else
            return image;
    }
    
}
