package com.harry.elastic.repository;

import com.harry.elastic.dynamic.ESDynamicIndexRepository;
import org.springframework.stereotype.Component;

@Component
public class FileBeatDynamicRepository extends ESDynamicIndexRepository<FileBeatRepository> {
}
