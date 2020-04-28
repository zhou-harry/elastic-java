package com.harry.elastic.dynamic;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchPartQuery;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchQueryMethod;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchStringQuery;
import org.springframework.data.elasticsearch.repository.support.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class CustomElasticsearchRepositoryFactory extends RepositoryFactorySupport {
    private static final Logger logger = LoggerFactory.getLogger(CustomElasticsearchRepositoryFactory.class);
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchEntityInformationCreator entityInformationCreator;
    private String indexPrefix;

    public CustomElasticsearchRepositoryFactory(ElasticsearchOperations elasticsearchOperations, String indexPrefix) {
        Assert.notNull(elasticsearchOperations, "ElasticsearchOperations must not be null!");
        this.elasticsearchOperations = elasticsearchOperations;
        this.indexPrefix = indexPrefix;
        this.entityInformationCreator = new ElasticsearchEntityInformationCreatorImpl(elasticsearchOperations.getElasticsearchConverter().getMappingContext());
    }

    private void setEntityInformationIndexName(MappingElasticsearchEntityInformation entityInformation) {
        try {
            Field field = MappingElasticsearchEntityInformation.class.getDeclaredField("indexName");
            field.setAccessible(true);
            String indexDefault = field.get(entityInformation).toString();
            if (!StringUtils.isEmpty(this.indexPrefix)) {
                field.set(entityInformation, this.indexPrefix + "-" + indexDefault);
            }
        } catch (IllegalAccessException e) {
            logger.error("can not access field: ", e);
        } catch (NoSuchFieldException e) {
            logger.error("no such field: ", e);
        }
    }

    public <T, ID> ElasticsearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        ElasticsearchEntityInformation entityInformation = this.entityInformationCreator.getEntityInformation(domainClass);
        if (!StringUtils.isEmpty(this.indexPrefix)) {
            setEntityInformationIndexName((MappingElasticsearchEntityInformation) entityInformation);
        }
        return entityInformation;

    }

    protected Object getTargetRepository(RepositoryInformation metadata) {
        return this.getTargetRepositoryViaReflection(metadata, new Object[]{this.getEntityInformation(metadata.getDomainType()), this.elasticsearchOperations});
    }

    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        if (isQueryDslRepository(metadata.getRepositoryInterface())) {
            throw new IllegalArgumentException("QueryDsl Support has not been implemented yet.");
        } else {
            return SimpleElasticsearchRepository.class;
        }
    }

    private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
        return QuerydslUtils.QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
    }

    private class ElasticsearchQueryLookupStrategy implements QueryLookupStrategy {
        private ElasticsearchQueryLookupStrategy() {
        }

        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
            ElasticsearchQueryMethod queryMethod = new ElasticsearchQueryMethod(method, metadata, factory, CustomElasticsearchRepositoryFactory.this.elasticsearchOperations.getElasticsearchConverter().getMappingContext());
            String namedQueryName = queryMethod.getNamedQueryName();
            if (namedQueries.hasQuery(namedQueryName)) {
                String namedQuery = namedQueries.getQuery(namedQueryName);
                return new ElasticsearchStringQuery(queryMethod, CustomElasticsearchRepositoryFactory.this.elasticsearchOperations, namedQuery);
            } else {
                return (RepositoryQuery)(queryMethod.hasAnnotatedQuery() ? new ElasticsearchStringQuery(queryMethod, CustomElasticsearchRepositoryFactory.this.elasticsearchOperations, queryMethod.getAnnotatedQuery()) : new ElasticsearchPartQuery(queryMethod, CustomElasticsearchRepositoryFactory.this.elasticsearchOperations));
            }
        }
    }

    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
        return Optional.of(new CustomElasticsearchRepositoryFactory.ElasticsearchQueryLookupStrategy());
    }
}
