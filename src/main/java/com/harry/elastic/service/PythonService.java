package com.harry.elastic.service;

import com.google.common.collect.Lists;
import com.harry.elastic.entity.FileBeatEntity;
import com.harry.elastic.exception.RemoteCallException;
import com.harry.elastic.properties.CoreProperties;
import com.harry.elastic.utils.JsonMapperUtil;
import com.harry.elastic.utils.RestUtil;
import com.harry.elastic.view.LogPatternRequest;
import com.harry.elastic.view.LogPatternRsponse;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PythonService {

    @Autowired
    private CoreProperties properties;
    @Autowired
    @Qualifier("remoteRestTemplate")
    private RestTemplate remoteRestTemplate;

    /**
     * 输入原始日志获取日志模式
     * @param requestBody
     * @return
     */
    public List<LogPatternRsponse> matchPattern(LogPatternRequest requestBody){
        //调用Python远程rest接口
        String url = properties.getPythonUrl() + "/LogTemplate";
        ResponseEntity<String> responseEntity = RestUtil.builder(remoteRestTemplate)
                .url(url)
                .method(HttpMethod.POST)
                .body(requestBody)
                .responseType(String.class)
                .exchange();
        if (HttpStatus.OK != responseEntity.getStatusCode()) {
            throw new RemoteCallException("远程接口调用异常，状态码：" + responseEntity.getStatusCode());
        }
        List<Map<String, Object>> maps = JsonMapperUtil.fromJsonForMapList(responseEntity.getBody());

        if (maps.size() != 1) {
            throw new RemoteCallException("远程接口调用返回结果无效");
        }
        Map<String, Object> map = maps.get(0);
        List<Map> result = (ArrayList)map.get("data");
        if (result == null) {
            throw new RemoteCallException("远程接口调用返回日志模式无效");
        }

        List<LogPatternRsponse> patterns = Lists.newArrayList();
        result.stream().forEach(pattern->{
            List ids = (List) pattern.get("ids");
            patterns.add(LogPatternRsponse.builder()
                    .id(MapUtils.getString(pattern,"id"))
                    .name(MapUtils.getString(pattern,"name"))
                    .count(MapUtils.getIntValue(pattern,"count"))
                    .ids(ids)
                    .build());
        });
        return patterns;
    }
}
