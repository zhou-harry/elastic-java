package com.harry.elastic.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.harry.elastic.entity.EppfLogEntity;
import com.harry.elastic.entity.FileBeatEntity;
import com.harry.elastic.repository.FileBeatRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;

@Service
public class ElasticService {

    @Autowired
    private FileBeatRepository repository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * 根据id检索数据
     * @param id
     * @return
     */
    public FileBeatEntity findById(String id){
        return repository.findById(id).get();
    }

    /**
     * 将日志模式存入ES原始日志中
     * @param id
     * @param patternName
     * @return
     */
    public FileBeatEntity savePatterns(String id,String patternName){
        FileBeatEntity logEntity = findById(id);
        List<String> patterns = logEntity.getPatterns();
        if (patterns == null) {
            patterns=Lists.newArrayList();
        }
        patterns.add(patternName);
        logEntity.setPatterns(patterns);
        return repository.save(logEntity);
    }

    /**
     * 将原始日志中的模式信息去掉
     * @param id
     * @return
     */
    public FileBeatEntity setPatternNull(String id){
        FileBeatEntity logEntity = findById(id);
        logEntity.setPatterns(null);
        return repository.save(logEntity);
    }
    /**
     * 查询未匹配模式的原始日志
     * （ES中tags不包含日志模式的数据，日志模式）
     *
     * @return
     */
    public List<FileBeatEntity> findWithoutPattern() {
        BoolQueryBuilder queryBuilder = boolQuery().mustNot(existsQuery("patterns"));
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withSort(fieldSort("@timestamp").order(SortOrder.ASC))
                .withPageable(PageRequest.of(0, 200))
                .build();
        CloseableIterator<FileBeatEntity> stream = elasticsearchOperations.stream(searchQuery, FileBeatEntity.class);

        List<FileBeatEntity> result = Lists.newArrayList();
        while (stream.hasNext()) {
            FileBeatEntity entity = stream.next();
            result.add(entity);
        }
        return result;
    }
}
