package com.dakidev.magento_image_upload.helper;

import org.jsoup.Connection;

import java.io.IOException;
import java.util.Map;

public class JsoupHelper {
    
    
    public static Connection.Response postCall(String url, Map<String, String> headers, Map<String, String> data, String userAgent) throws IOException {
        
        return SSLHelper.getConnection(url)
                .userAgent(userAgent)
                .headers(headers)
                .data(data)
                .method(Connection.Method.POST)
                .ignoreHttpErrors(true)
                . ignoreContentType(true)
                .execute();
    }
    public static Connection.Response getCAll(Map<String, String> cookies, String url, String userAgent) throws IOException {
        
        return SSLHelper.getConnection(url)
                .timeout(30 * 1000)
                .method(Connection.Method.GET)
                .userAgent(userAgent)
                .cookies(cookies)
                .execute();
    }
    
    public static Connection.Response getCAllWithHeader(Map<String, String> cookies, Map<String, String> headers, String url, String userAgent) throws IOException {
        
        return SSLHelper.getConnection(url)
                .timeout(30 * 1000)
                .method(Connection.Method.GET)
                .userAgent(userAgent)
                .cookies(cookies)
                .followRedirects(true)
                .execute();
    }
    
    public static Connection.Response getCAllWithRetry(Map<String, String> cookies, String url, String userAgent, int maxAttempts) throws Exception {
        
        Connection.Response response = null;
        int i = 0;
        boolean success = false;
        
        while (i < maxAttempts) {
            try {
                Thread.sleep(i * 1000L);
                response = SSLHelper.getConnection(url)
                        .timeout(30 * 1000)
                        .method(Connection.Method.GET)
                        .userAgent(userAgent)
                        .cookies(cookies)
                        .execute();
                success = true;
                break;
            } catch (IOException e) {
            }
            i++;
        }
        
        if (success) {
            return response;
        } else {
            throw new Exception("network problem, try again");
        }
        
    }
}
