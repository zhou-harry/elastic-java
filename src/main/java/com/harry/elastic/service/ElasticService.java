package com.harry.elastic.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.harry.elastic.dynamic.DynamicIndexElasticsearchTemplate;
import com.harry.elastic.entity.FileBeatEntity;
import com.harry.elastic.repository.FileBeatDynamicRepository;
import com.harry.elastic.repository.FileBeatRepository;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

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
    @Autowired
    private DynamicIndexElasticsearchTemplate dynamicIndexElasticsearchTemplate;
    @Autowired
    private FileBeatDynamicRepository dynamicRepository;

    /**
     * 根据id检索数据
     *
     * @param id
     * @return
     */
    public FileBeatEntity findById(String id) {
        FileBeatRepository proxyRepository = dynamicRepository.getProxyRepository("bes");
        return proxyRepository.findById(id).get();
    }

    /**
     * 更新原始日志中的模式信息
     *
     * @param id
     * @return
     */
    public FileBeatEntity savePatterns(String id, String pattern) {
        FileBeatEntity logEntity = findById(id);
        if (logEntity == null) {
            return null;
        }
        if (pattern == null) {
            logEntity.setPatterns(null);
        } else {
            List<String> patterns = logEntity.getPatterns();
            if (patterns == null) {
                patterns = Lists.newArrayList();
            }
            patterns.add(pattern);
            logEntity.setPatterns(patterns);
        }
        FileBeatRepository proxyRepository = dynamicRepository.getProxyRepository("bes");
        return proxyRepository.save(logEntity);
    }

    /**
     * 动态索引模板
     *
     * @return
     */
    public List<FileBeatEntity> searchByElasticsearchTemplate(String id) {

        BoolQueryBuilder boolQuery = boolQuery()
                .must(QueryBuilders.termsQuery("_id", id));

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .build();
        List<FileBeatEntity> list = dynamicIndexElasticsearchTemplate
                .getElasticsearchTemplate("bes", FileBeatEntity.class)
                .queryForList(searchQuery, FileBeatEntity.class);
        return list;
    }

    /**
     * 查询未匹配模式的原始日志
     * （ES中tags不包含日志模式的数据，日志模式）
     *
     * @return
     */
    public List<FileBeatEntity> findWithoutPattern() {
//        BoolQueryBuilder queryBuilder = boolQuery().mustNot(matchQuery("patterns.id","esb"));
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

    public List<FileBeatEntity> findWithPattern(String pattern) {
        BoolQueryBuilder queryBuilder = boolQuery().mustNot(matchQuery("patterns", pattern));

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withSort(fieldSort("@timestamp").order(SortOrder.ASC))
                .withPageable(PageRequest.of(0, 200))
                .build();
//        Page<FileBeatEntity> page = elasticsearchOperations.queryForPage(searchQuery, FileBeatEntity.class);
        CloseableIterator<FileBeatEntity> stream = elasticsearchOperations.stream(searchQuery, FileBeatEntity.class);
        List<FileBeatEntity> result = Lists.newArrayList();
        while (stream.hasNext()) {
            FileBeatEntity entity = stream.next();
            result.add(entity);
        }
        return result;
    }

    /**
     * 根据日志模式统计
     *
     * @return
     */
    public List patternAggregation() {
        String termsTitle = "patterns";
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 不过滤任何结果
                .withSourceFilter(new FetchSourceFilter(new String[]{""}, null))
                // 1、添加一个新的聚合，聚合类型为terms，聚合名称为title，聚合字段为class
                .addAggregation(
                        AggregationBuilders.terms(termsTitle).field("patterns.keyword")
                ).build();
        // 2、查询,需要把结果强转为AggregatedPage类型
        AggregatedPage<FileBeatEntity> aggPage = (AggregatedPage<FileBeatEntity>) this.repository.search(searchQuery);
        // 3、解析
        // 3.1、从结果中取出名为pattern的那个聚合，
        ParsedStringTerms aggPattern = (ParsedStringTerms) aggPage.getAggregation(termsTitle);
        // 3.2、获取桶
        List<? extends Terms.Bucket> buckets = aggPattern.getBuckets();
        // 3.3、遍历
        List<Map> list = Lists.newArrayList();
        for (Terms.Bucket bucket : buckets) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("日志模式", bucket.getKeyAsString());
            map.put("size", bucket.getDocCount());
            list.add(map);
        }
        return list;
    }

}
