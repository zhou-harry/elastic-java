package com.harry.elastic.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.harry.elastic.entity.EppfLogEntity;
import com.harry.elastic.entity.FileBeatEntity;
import com.harry.elastic.exception.RemoteCallException;
import com.harry.elastic.properties.CoreProperties;
import com.harry.elastic.repository.EppfLogRepository;
import com.harry.elastic.repository.FileBeatRepository;
import com.harry.elastic.service.ElasticService;
import com.harry.elastic.service.PythonService;
import com.harry.elastic.utils.JsonMapperUtil;
import com.harry.elastic.utils.RestUtil;
import com.harry.elastic.view.LogPatternRequest;
import com.harry.elastic.view.LogPatternRsponse;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;

@RestController
@RequestMapping("filebeat")
public class FileBeatController {


    @Autowired
    private ElasticService elasticService;
    @Autowired
    private PythonService pythonService;

    @GetMapping("/findById/{id}")
    public FileBeatEntity findById(@PathVariable("id") String id) {
        FileBeatEntity entity = elasticService.findById(id);
        return entity;
    }

    @GetMapping("/findWithoutPattern")
    public List<FileBeatEntity> findWithoutPattern() {
        List<FileBeatEntity> origin = elasticService.findWithoutPattern();
        return origin;
    }

    @GetMapping("/setPatternNull")
    public String setPatternNull(@RequestBody String[] ids) {
        if(Objects.isNull(ids)){
            return null;
        }
        Arrays.stream(ids).forEach(id->{
            elasticService.setPatternNull(id);
        });
        return "OK";
    }

    /**
     * 根据原始日志匹配日志模式（调用python接口）
     * @return
     */
    @GetMapping("/patternRecognition")
    public List<LogPatternRsponse> patternRecognition() {
        //查询未匹配模式的原始日志
        List<FileBeatEntity> origin = elasticService.findWithoutPattern();
        //调用远程python进行模式匹配
        Map<String, String> types = Maps.newHashMap();
        Map<String, String> msg = Maps.newHashMap();
        origin.stream().forEach(entity->{
            msg.put(entity.getId(),entity.getMessage());
        });
        LogPatternRequest requestBody = LogPatternRequest.builder()
                .num(2)
                .types(types)
                .msg(msg)
                .build();
        List<LogPatternRsponse> patternRsponses = pythonService.matchPattern(requestBody);
        patternRsponses.stream().forEach(pattern->{
            List<String> ids = pattern.getIds();
            //将匹配的日志模式回写到ES中
            ids.stream().forEach(id->{
                elasticService.savePatterns(id,pattern.getName());
            });
        });
        return patternRsponses;
    }




}
