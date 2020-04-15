package com.harry.elastic.repository;

import com.harry.elastic.entity.FileBeatEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FileBeatRepository extends ElasticsearchRepository<FileBeatEntity, String> {
}
