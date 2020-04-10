package com.harry.elatic.repository;

import com.harry.elatic.entity.EppfLogEntity;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EppfLogRepository extends ElasticsearchRepository<EppfLogEntity, String> {

    List<EppfLogEntity> findByCurrentTimeBefore(String currentTime);

    List<EppfLogEntity> findByTagNull();

    @Transactional
    @Query("UPDATE EppfLogEntity SET tag=:tag WHERE id=:id")
    void updateTagById(String tag, String id);

}
