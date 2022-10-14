package com.dakidev.magento_image_upload.domain;

import com.google.gson.GsonBuilder;
import com.dakidev.magento_image_upload.helper.JsoupHelper;
import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Uploader {
    
    
    public static final String KEY = "6e3d7587485aeddfcc3dd3ccc8f0ae65";
    public static final String URL = "https://api.imgbb.com/1/upload";
    
    public static String upload(String filePath) throws IOException {
        
        byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
        
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        
        Map<String, String> params = new HashMap<>();
        params.put("key", KEY);
        params.put("image", encodedString);
        
        Connection.Response response = JsoupHelper.postCall(URL, new HashMap<>(), params, "");
    
        String json = response.parse().body().text();
        Map<String, Object> responseAsMap = new GsonBuilder().create().fromJson(json, Map.class);
        
        return ((Map<String, String>)responseAsMap.get("data")).get("url");
        
        
    }
    
}
