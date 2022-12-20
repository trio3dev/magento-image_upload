package com.dakidev.image_upload.domain;

import com.dakidev.image_upload.helper.JsoupHelper;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ImgBBUploader {
    
    static Logger logger = LogManager.getLogger(ImgBBUploader.class);
    
    
    public static final String KEY = "6e3d7587485aeddfcc3dd3ccc8f0ae65";
    public static final String URL = "https://api.imgbb.com/1/upload";
    public static final int MAX_ATTEMPTS = 5;
    public static final int TIMEOUT = 30;
    
    
    public String uploadImage(String image) throws HttpCallException, ImageUploadException {
        
        Map<String, String> params = new HashMap<>();
        params.put("key", KEY);
        params.put("image", image);
        
        Connection.Response httpResponse = JsoupHelper.postCallWithRetry(URL, new HashMap<>(), params, "", TIMEOUT, MAX_ATTEMPTS);
        
        String json = httpResponse.body();
        Response response = new GsonBuilder().create().fromJson(json, Response.class);
        
        if( ! response.success) {
    
            logger.error(image + ": " + json);
            throw new ImageUploadException(response.error.message);
    
        }
    
        return response.data.url;
    }
    
    private String uploadImageByLocalPath(String imagePath) throws Exception {
        
        byte[] imagesBytes = FileUtils.readFileToByteArray(new File(imagePath));
    
        String imageInBase64 = Base64.getEncoder().encodeToString(imagesBytes);
    
        return uploadImage(imageInBase64);
        
    }

    
    static class Response{
        private boolean success;
        private Error error;
        private Data data;
    }
    
    static class Error{
        private int code;
        private String message;
    }
    
    static class Data{
        private String url;
    }
    
}
