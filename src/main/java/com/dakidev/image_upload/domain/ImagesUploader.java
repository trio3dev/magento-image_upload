package com.dakidev.image_upload.domain;

import com.dakidev.MATCHING_TYPE;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ImagesUploader {
    
    
    public static final String REGEX_END_OF_TEXT = "$";
    public static final String NEW_FILE_SUFFIX = "_2";
    public static final String ALREADY_UPLOADED_IMAGE_PREFIX = "https://i.ibb";
    public static final int N_THREADS = 5;
    private final UpdateExcelFileService updateExcelFileService = new UpdateExcelFileService();
    public static final String IMAGE_EXTENSIONS_REGEX = "(.+?)(\\.png|\\.jpg|\\.gif|\\.jpeg|\\.webp|\\.bmp)";
    Logger logger = LogManager.getLogger(ImagesUploader.class);
    
    
    public void updateExcelFileImages(String filePath, String imagesFolder, ProgressMonitor progressMonitor, int indexOfImageColumn, int indexOfMatchingColumn, String imagesSeparator, MATCHING_TYPE matchingType, String imageExtensions, int headerRow) throws IOException {
        
        sendMessageMonitor(progressMonitor, "uploading images...");
        
        Map<Integer, String> imagesMap = new HashMap<>();
        
        File destinationFile = createDestinationFile(filePath);
        writeWorkbookOnShutdown(destinationFile, indexOfImageColumn, imagesMap);
        
        ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
        
        List<Callable<Object>> callableList = new ArrayList<>();
        
        try (FileInputStream inputStream = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                
                if (row.getRowNum() <= headerRow)
                    continue;
                
                int rowNum = row.getRowNum();
                
                Cell matchingCell = sheet.getRow(rowNum).getCell(indexOfMatchingColumn);
                Cell imageCell = sheet.getRow(rowNum).getCell(indexOfImageColumn);                
                
                if (matchingCell != null) {
                    String images = getStringValueFromCell(matchingCell);
                    StringBuilder uploadedImagesBuilder = new StringBuilder();
                    
                    if (images == null)
                        continue;
                    
                    String[] imagesArray = getImagesAsArray(imagesFolder, imagesSeparator, images, imageExtensions);
                    
                    callableList.add(() -> {
    
                        updateProgressMonitor(progressMonitor, (double) rowNum / sheet.getLastRowNum());
                        
                        for (String image : imagesArray) {
                            try {
                                if (isImageUploadable(image)) {
                                    
                                    switch (matchingType) {
                                        case FULL:
                                            
                                            uploadedImagesBuilder.append(new ImgBBUploader().uploadImage(image)).append(imagesSeparator);
                                            System.out.println(rowNum);
                                            
                                            break;
                                        case FOLDER:
                                            
                                            String imagePath1 = imagesFolder + "/" + image;
                                            if (isLocalImageExist(imagePath1))
                                                uploadedImagesBuilder.append(new ImgBBUploader().uploadImage(getImageBase64(imagePath1))).append(imagesSeparator);
                                            
                                            else
                                                sendMessageMonitor(progressMonitor, ("message-" + (rowNum + 1) + " : " + imagePath1 + " Image not found"));
                                            break;
                                        case FOLDER_WITH_EXTENSIONS:
                                            
                                            File[] files = getFilesInFolderWithRegexs(image, new File(imagesFolder), imageExtensions.split(","));
                                            for (File file : files) {
                                                uploadedImagesBuilder.append(new ImgBBUploader().uploadImage(getImageBase64(file.getAbsolutePath()))).append(imagesSeparator);
                                            }
                                            
                                            break;
                                        
                                        default:
                                            throw new IllegalArgumentException();
                                    }
                                    
                                } else
                                    uploadedImagesBuilder.append(image).append(imagesSeparator);
                                
                                
                            } catch (Exception e) {
                                uploadedImagesBuilder.append(image).append(imagesSeparator);
                                logger.error(e);
                                
                                sendMessageMonitor(progressMonitor, ("message-" + (rowNum + 1) + " : " + image + " : " + e.getMessage()));
                            }
                        }
                        String uploadedImages = uploadedImagesBuilder.toString().replaceAll(imagesSeparator + REGEX_END_OF_TEXT, "");
                        
                        if (isAtLeastOneImageInCellUploaded(uploadedImages, imagesSeparator))
                            imagesMap.put(rowNum, uploadedImages);
                        else
                            System.out.println();
                        
                        return null;
                    });
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
    
        try {
            executor.invokeAll(callableList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        executor.shutdown();
        
        updateExcelFileService.updateColumnInExcelFile(destinationFile, indexOfImageColumn, imagesMap);
        imagesMap.clear();
    }
    
    private File[] getFilesInFolderWithRegexs(String image, File dir, String[] regexs) {
        
        List<Pattern> patterns = Arrays.stream(regexs)
                .map(s -> Pattern.compile(image + ".*" + s.replace("*", ".*")))
                .collect(Collectors.toList());
        return dir.listFiles((dir1, name) -> {
            for (Pattern rx : patterns) if (rx.matcher(name).matches()) return true;
            return false;
        });
    }
    private static String getStringValueFromCell(Cell cell) {
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getRichStringCellValue().toString();
            case NUMERIC:
                return String.valueOf(new DecimalFormat("###########").format(cell.getNumericCellValue()));
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
    
    private static boolean isAtLeastOneImageInCellUploaded(String images, String imagesSeparator) {
        if (imagesSeparator == null || imagesSeparator.trim().isEmpty())
            return images.contains(ALREADY_UPLOADED_IMAGE_PREFIX);
        
        return Arrays.stream(images.split(imagesSeparator)).anyMatch(s -> s.contains(ALREADY_UPLOADED_IMAGE_PREFIX));
    }
    
    private static boolean isAllImageInCellUploaded(String cellValue, String imagesSeparator) {
        return Arrays.stream(cellValue.split(imagesSeparator)).allMatch(s -> s.contains(ALREADY_UPLOADED_IMAGE_PREFIX));
    }
    
    private static String getImageBase64(String imageLocalPath) throws IOException {
        byte[] imagesBytes = FileUtils.readFileToByteArray(new File(imageLocalPath));
        return Base64.getEncoder().encodeToString(imagesBytes);
    }
    
    private static String[] getImagesAsArray(String imagesFolder, String imagesSeparator, String images, String extensions) {
        
        if (imagesFolder.isEmpty() || !extensions.isEmpty()) {
            
            if (imagesSeparator == null || imagesSeparator.trim().isEmpty())
                return new String[]{images};
            
            return images.split(imagesSeparator);
        } else
            return splitImagesByExtension(images); // local image name can contain imagesSeparator so, I split by image extension
    }
    
    private static boolean isImageAlreadyUploaded(String image) {
        return image.contains(ALREADY_UPLOADED_IMAGE_PREFIX);
    }
    
    private static boolean isImageUploadable(String imageUrl) {
        return !imageUrl.trim().isEmpty() && !isImageAlreadyUploaded(imageUrl);
    }
    
    private File createDestinationFile(String filePath) throws IOException {
        
        File destinationFile;
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            
            destinationFile = new File(filePath.replaceAll(".xlsx", NEW_FILE_SUFFIX + ".xlsx"));
            
            FileUtils.copyInputStreamToFile(inputStream, destinationFile);
        }
        return destinationFile;
    }
    
    private void updateProgressMonitor(ProgressMonitor progressMonitor, double progress) {
        
        if (progressMonitor != null) progressMonitor.progressUpdated(progress);
    }
    
    private void sendMessageMonitor(ProgressMonitor progressMonitor, String message) {
        
        if (progressMonitor != null) progressMonitor.senMessage(message);
    }
    
    private void writeWorkbookOnShutdown(File destinationFile, int indexOfImages, Map<Integer, String> imagesMap) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                updateExcelFileService.updateColumnInExcelFile(destinationFile, indexOfImages, imagesMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
    
    private boolean isLocalImageExist(String imagePath) {
        
        return Files.exists(Paths.get(imagePath));
        
    }
    
    private static String uploadImagesFromWP(String baseUrl, String image) throws Exception {
        
        if (!image.isEmpty()) {
            
            byte[] imageAsBytes = getWPImageAsBytes(baseUrl + image);
            if (imageAsBytes.length != 0) {
                String imageBase64 = Base64.getEncoder().encodeToString(imageAsBytes);
                return new ImgBBUploader().uploadImage(imageBase64);
            } else {
                throw new Exception("FileNot Found");
            }
            
        } else
            return image;
    }
    
    private static byte[] getWPImageAsBytes(String url) throws IOException {
        
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
            return new byte[0];
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(httpURLConnection.getInputStream(), byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
    
    private static String[] splitImagesByExtension(String images) {
        
        Pattern pattern = Pattern.compile(IMAGE_EXTENSIONS_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(images);
        List<String> data = new ArrayList<>();
        while (matcher.find()) {
            data.add(matcher.group().replaceAll("^,", ""));
        }
        
        return data.toArray(new String[0]);
    }
}
