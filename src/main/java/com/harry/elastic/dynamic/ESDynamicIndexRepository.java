package com.harry.elastic.dynamic;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ESDynamicIndexRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(ESDynamicIndexRepository.class);
    @Autowired
    private DynamicIndexElasticsearchTemplate dynamicIndexElasticsearchTemplate;

    private CustomElasticsearchRepositoryFactory getFactory(String indexPrefix, ElasticsearchOperations elasticsearchTemplate) {
        CustomElasticsearchRepositoryFactory elasticFactory = new CustomElasticsearchRepositoryFactory(elasticsearchTemplate, indexPrefix);
        return elasticFactory;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveReturnedClassFromGenericType() {
        ParameterizedType parameterizedType = resolveReturnedClassFromGenericType(getClass());
        return (Class<T>) parameterizedType.getActualTypeArguments()[0];
    }

    private ParameterizedType resolveReturnedClassFromGenericType(Class<?> clazz) {
        Object genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            return (ParameterizedType) genericSuperclass;
        }
        return resolveReturnedClassFromGenericType(clazz.getSuperclass());
    }

    private Class getClazz(Class proxy) {
        Type[] types = proxy.getGenericInterfaces();
        Type t1 = ((ParameterizedType) types[0]).getActualTypeArguments()[0];
        return (Class) t1;
    }


    public T proxyRepository() {
        return getProxyRepository(null);
    }

    public T getProxyRepository(String indexPrefix) {
        Class<T> proxy = resolveReturnedClassFromGenericType();
        if (proxy.getClass().isInstance(ElasticsearchRepository.class)) {
            ElasticsearchOperations esTemplate = dynamicIndexElasticsearchTemplate.getElasticsearchTemplate();
            CustomElasticsearchRepositoryFactory esFactory = getFactory(indexPrefix, esTemplate);
            T proxyRepository = esFactory.getRepository(proxy);
            if (!StringUtils.isEmpty(indexPrefix)) {
                dynamicIndexElasticsearchTemplate.setIndex(indexPrefix, getClazz(proxy), esTemplate);
            }
            return proxyRepository;
        } else {
            throw new RuntimeException("do not support thie proxy class");
        }
    }

}
