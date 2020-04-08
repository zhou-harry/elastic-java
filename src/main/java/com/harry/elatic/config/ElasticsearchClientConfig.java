package com.harry.elatic.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchEntityMapper;
import org.springframework.data.elasticsearch.core.EntityMapper;

import java.time.Duration;

@Configuration
public class ElasticsearchClientConfig extends AbstractElasticsearchConfiguration {

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("192.168.1.184:9200", "192.168.1.184:9201", "192.168.1.184:9202")
                .withBasicAuth("elastic","qwe123")
                //Default is 10 sec.
                .withConnectTimeout(Duration.ofSeconds(5))
                //Default is 5 sec.
                .withSocketTimeout(Duration.ofSeconds(3))
                .build();
        return RestClients.create(clientConfiguration).rest();
    }

    // use the ElasticsearchEntityMapper
    @Bean
    @Override
    public EntityMapper entityMapper() {
        ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(
                elasticsearchMappingContext(),new DefaultConversionService()
        );
        entityMapper.setConversions(elasticsearchCustomConversions());

        return entityMapper;
    }

}
