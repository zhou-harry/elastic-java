package com.harry.elatic.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.*;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Map;

public class RestUtil<T> {

    public static class RestUtilBuilder<T> {

        private final RestTemplate restTemplate;
        private String url;
        private HttpMethod method;
        private Class<T> responseType;
        private Object body;
        private Map<String, String> header;

        private String mimeType;

        public RestUtilBuilder(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        public ResponseEntity<T> exchange() {
            return new RestUtil().exchange(
                    this.restTemplate,
                    this.url,
                    this.method,
                    this.responseType,
                    this.body,
                    this.header,
                    this.mimeType
            );
        }

        public RestUtilBuilder url(String url) {
            this.url = url;
            return this;
        }

        public RestUtilBuilder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public RestUtilBuilder responseType(Class<T> responseType) {
            this.responseType = responseType;
            return this;
        }

        public RestUtilBuilder body(Object body) {
            this.body = body;
            return this;
        }

        public RestUtilBuilder header(Map<String, String> header) {
            this.header = header;
            return this;
        }

        public RestUtilBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }
    }

    public static RestUtil.RestUtilBuilder builder(RestTemplate restTemplate) {
        return new RestUtil.RestUtilBuilder(restTemplate);
    }

    /**
     * 发送/获取 服务端数据 (主要用于解决发送put,delete方法无返回值问题).
     *
     * @param url          绝对地址
     * @param method       请求方式
     * @param responseType 返回类型
     * @param body         请求体
     * @param <T>          返回类型
     * @param header       请求头
     * @param mType        请求媒体类型
     * @return
     */
    public <T> ResponseEntity<T> exchange(RestTemplate restTemplate, String url, HttpMethod method, Class<T> responseType, Object body, Map<String, String> header, String mType) {
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        MimeType mimeType = MimeTypeUtils.parseMimeType(null == mType ? MediaType.APPLICATION_JSON_VALUE : mType);
        MediaType mediaType = new MediaType(mimeType.getType(), mimeType.getSubtype(), Charset.forName("UTF-8"));
        // 媒体类型
        headers.setContentType(mediaType);
        if (MapUtils.isNotEmpty(header)) {
            header.keySet().forEach(key -> {
                headers.add(key, header.get(key));
            });
        }
        //提供json转化功能
        ObjectMapper mapper = new ObjectMapper();
        HttpEntity<Object> entity = null;
        if (body != null) {
            if (body instanceof MultiValueMap) {
                entity = new HttpEntity<>(body, headers);
            } else if (body instanceof Map) {
                Map<String, Object> requestBody = (Map<String, Object>) body;
                try {
                    if (!requestBody.isEmpty()) {
                        entity = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                entity = new HttpEntity(body, headers);
            }
        } else {
            entity = new HttpEntity(null, headers);
        }
        // 发送请求
        return restTemplate.exchange(url, method, entity, responseType);
    }
}
