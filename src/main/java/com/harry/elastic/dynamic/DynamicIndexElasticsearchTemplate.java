package com.harry.elastic.dynamic;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchPersistentEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class DynamicIndexElasticsearchTemplate implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(DynamicIndexElasticsearchTemplate.class);
    private ApplicationContext applicationContext;

    protected ElasticsearchTemplate getElasticsearchTemplate() {
        return applicationContext.getBean(ElasticsearchTemplate.class);
    }

    public ElasticsearchTemplate getElasticsearchTemplate(String indexPrefix, Class cls) {
        ElasticsearchTemplate esTemplate = getElasticsearchTemplate();
        setIndex(indexPrefix, cls, esTemplate);
        return esTemplate;
    }

    protected void setIndex(String indexPrefix, Class cls, ElasticsearchTemplate elasticsearchTemplate) {
        ElasticsearchPersistentEntity entity = elasticsearchTemplate.getPersistentEntityFor(cls);
        try {
            Field field = SimpleElasticsearchPersistentEntity.class.getDeclaredField("indexName");
            field.setAccessible(true);
            String indexDefault = field.get(entity).toString();
            if (!StringUtils.isEmpty(indexPrefix)) {
                field.set(entity, indexPrefix + "_" + indexDefault);
            }
        } catch (IllegalAccessException e) {
            logger.error("can not access field: ", e);
        } catch (NoSuchFieldException e) {
            logger.error("no such field: ", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
