package com.harry.elatic.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.harry.elatic.entity.EppfLogEntity;
import com.harry.elatic.exception.RemoteCallException;
import com.harry.elatic.properties.CoreProperties;
import com.harry.elatic.repository.EppfLogRepository;
import com.harry.elatic.utils.JsonMapperUtil;
import com.harry.elatic.utils.RestUtil;
import com.harry.elatic.view.LogPatternRequest;
import com.harry.elatic.view.LogPatternRsponse;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;

@RestController
@RequestMapping("es-index")
public class ElasticIndexController {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    @Autowired
    private EppfLogRepository repository;
    @Autowired
    @Qualifier("remoteRestTemplate")
    private RestTemplate remoteRestTemplate;
    @Autowired
    private CoreProperties properties;

    @GetMapping("/findLosg")
    public List<EppfLogEntity> findLosg() {
        Iterable<EppfLogEntity> entities = repository.findAll();
        return Lists.newArrayList(entities);
    }

    @GetMapping("/findById/{id}")
    public EppfLogEntity findById(@PathVariable("id") String id) {
        Optional<EppfLogEntity> entity = repository.findById(id);
        return entity.get();
    }

    @PostMapping("/updateByIds")
    public String updateById(@RequestBody  String[] ids) {
        if(Objects.isNull(ids)){
            return null;
        }
        Arrays.stream(ids).forEach(id->{
            Optional<EppfLogEntity> entity = repository.findById(id);
            EppfLogEntity logEntity = entity.get();
            logEntity.setTag(null);
            repository.save(logEntity);
        });
        return "OK";
    }

    @GetMapping("/updateTagById/{id}")
    public String updateTagById(@PathVariable("id") String id) {
        repository.updateTagById(null,id);
        return "OK";
    }

    @GetMapping("/getLogsBefor/{currentTime}")
    public List<EppfLogEntity> getLogsBefor(@PathVariable("currentTime") String currentTime) {
        List<EppfLogEntity> entities = repository.findByCurrentTimeBefore(currentTime);
        return entities;
    }

    @GetMapping("/findByTagIsNull")
    public List<EppfLogEntity> findByTagIsNull() {
        List<EppfLogEntity> entities = repository.findByTagNull();
        return entities;
    }


    @GetMapping("queryScroll")
    public List<EppfLogEntity> queryScroll() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                //不分词查询:查询tag带有“服务编码:*_*_*获取到*.*.*.*:*?*=*&*=*_*_*_*&*=*&*=*连接耗时:*,*调用耗时：*”的内容
                .withQuery(matchPhraseQuery("tag", "服务编码:*_*_*获取到*.*.*.*:*?*=*&*=*_*_*_*&*=*&*=*连接耗时:*,*调用耗时：*"))
//                .withQuery(termsQuery("tag.keyword","服务编码:*_*_*获取到*.*.*.*:*?*=*&*=*_*_*_*&*=*&*=*连接耗时:*,*调用耗时：*"))
                .withSort(fieldSort("@timestamp").order(SortOrder.ASC))
                .withFields("id", "message", "tag", "@timestamp")
                .withPageable(PageRequest.of(0, 2))
                .build();

        CloseableIterator<EppfLogEntity> stream = elasticsearchOperations.stream(searchQuery, EppfLogEntity.class);

        List<EppfLogEntity> entities = Lists.newArrayList();
        while (stream.hasNext()) {
            entities.add(stream.next());
        }
        return entities;
    }

    /**
     * 滚动获取ES数据中tag值为null的数据，并发送到Python中进行模式匹配
     *
     * @return
     */
    @GetMapping("/patternRecognition")
    public List<LogPatternRsponse> patternRecognition() {
        //获取ES中tag不存在的日志数据
        BoolQueryBuilder queryBuilder = boolQuery().mustNot(existsQuery("tag"));
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withSort(fieldSort("@timestamp").order(SortOrder.ASC))
                .withFields("id", "message")
                .withPageable(PageRequest.of(0, 200))
                .build();
        CloseableIterator<EppfLogEntity> stream = elasticsearchOperations.stream(searchQuery, EppfLogEntity.class);

        HashMap<String, String> types = Maps.newHashMap();
        HashMap<String, String> msg = Maps.newHashMap();
        while (stream.hasNext()) {
            EppfLogEntity entity = stream.next();
            msg.put(entity.getId(), entity.getMessage());
        }
        LogPatternRequest requestBody = LogPatternRequest.builder()
                .num(2)
                .types(types)
                .msg(msg)
                .build();
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
        List result = (ArrayList)map.get("data");
        if (result == null) {
            throw new RemoteCallException("远程接口调用返回日志模式无效");
        }
        List<LogPatternRsponse> patterns = Lists.newArrayList();
        result.stream().forEach(pattern->{
            LinkedHashMap p = (LinkedHashMap) pattern;
            List ids = (List) p.get("ids");
            patterns.add(LogPatternRsponse.builder()
                    .id(MapUtils.getString(p,"id"))
                    .name(MapUtils.getString(p,"name"))
                    .count(MapUtils.getIntValue(p,"count"))
                    .ids(ids)
                    .build());
            //将匹配的日志模式回写到ES中
            ids.stream().forEach(id->{
                Optional<EppfLogEntity> entity = repository.findById(id.toString());
                EppfLogEntity logEntity = entity.get();
                logEntity.setTag(MapUtils.getString(p,"name"));
                repository.save(logEntity);
            });
        });
        return patterns;
    }

    @GetMapping("getCount")
    public List getCount() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 不过滤任何结果
                .withSourceFilter(new FetchSourceFilter(new String[]{""}, null))
                // 1、添加一个新的聚合，聚合类型为terms，聚合名称为title，聚合字段为class
                .addAggregation(
                        AggregationBuilders.terms("title").field("class.keyword")
                                //嵌套聚合
                                .subAggregation(AggregationBuilders.terms("dateSum").field("date")
                                        .order(BucketOrder.key(true)))
                ).build();
        // 2、查询,需要把结果强转为AggregatedPage类型
        AggregatedPage<EppfLogEntity> aggPage = (AggregatedPage<EppfLogEntity>) this.repository.search(searchQuery);
        // 3、解析
        // 3.1、从结果中取出名为title的那个聚合，
        ParsedStringTerms aggTitle = (ParsedStringTerms) aggPage.getAggregation("title");
        // 3.2、获取桶
        List<? extends Terms.Bucket> buckets = aggTitle.getBuckets();
        // 3.3、遍历
        List<Map> list = Lists.newArrayList();
        for (Terms.Bucket bucket : buckets) {
            //3.5 获取子聚合结果
            Aggregations dataAggregations = bucket.getAggregations();
            ParsedLongTerms dateSum = (ParsedLongTerms) dataAggregations.asMap().get("dateSum");
            dateSum.getBuckets().stream().forEach(subBucket -> {
                HashMap<String, Object> map = Maps.newHashMap();
                // 3.4、获取桶中的key=即错误类型,获取桶中的文档数量
                map.put("错误类型", bucket.getKeyAsString());
                map.put("日期", subBucket.getKeyAsString());
                map.put("错误量", subBucket.getDocCount());
                list.add(map);
            });
        }
        return list;
    }
}
