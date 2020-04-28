package com.harry.elastic.controller;

import com.google.common.collect.Maps;
import com.harry.elastic.dynamic.DynamicIndexElasticsearchTemplate;
import com.harry.elastic.entity.FileBeatEntity;
import com.harry.elastic.service.ElasticService;
import com.harry.elastic.service.PythonService;
import com.harry.elastic.view.LogPatternRequest;
import com.harry.elastic.view.LogPatternRsponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
        if (Objects.isNull(ids)) {
            return null;
        }
        Arrays.stream(ids).forEach(id -> {
            elasticService.savePatterns(id, null);
        });
        return "OK";
    }

    @GetMapping("/setPatternTest/{id}")
    public String setPatternTest(@PathVariable("id") String id) {
        elasticService.savePatterns(id, "***___test___***");
        return "OK";
    }

    /**
     * 根据原始日志匹配日志模式（调用python接口）
     *
     * @return
     */
    @GetMapping("/patternRecognition")
    public List<LogPatternRsponse> patternRecognition() {
        //查询未匹配模式的原始日志
        List<FileBeatEntity> origin = elasticService.findWithoutPattern();
        //调用远程python进行模式匹配
        Map<String, String> types = Maps.newHashMap();
        Map<String, String> msg = Maps.newHashMap();
        origin.stream().forEach(entity -> {
            msg.put(entity.getId(), entity.getMessage());
        });
        LogPatternRequest requestBody = LogPatternRequest.builder()
                .num(2)
                .types(types)
                .msg(msg)
                .build();
        List<LogPatternRsponse> patternRsponses = pythonService.matchPattern(requestBody);
        patternRsponses.stream().forEach(pattern -> {
            List<String> ids = pattern.getIds();
            //将匹配的日志模式回写到ES中
            ids.stream().forEach(id -> {
                elasticService.savePatterns(id, pattern.getName());
            });
        });
        return patternRsponses;
    }

    /**
     * 根据日志模式统计
     *
     * @return
     */
    @GetMapping("/patternAggregation")
    public List patternAggregation() {
        return elasticService.patternAggregation();
    }


    @GetMapping("/searchByElasticsearchTemplate")
    public List<FileBeatEntity> searchByElasticsearchTemplate(@RequestParam String id) {
        return elasticService.searchByElasticsearchTemplate(id);
    }

}
