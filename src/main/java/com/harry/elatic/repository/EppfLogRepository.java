package com.harry.elatic.repository;

import com.harry.elatic.entity.EppfLogEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EppfLogRepository extends ElasticsearchRepository<EppfLogEntity, String> {

    List<EppfLogEntity> findByCurrentTimeBefore(String currentTime);

}
