package com.harry.elatic.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "oppf_esb_log-2020.04", type = "_doc")
public class EppfLogEntity {
    @Id
    private Long id;
    private String host;
    private String message;
    private String type;
    @Field(type = FieldType.Date)
    private String date;
    private String time;
    @Field(type = FieldType.Text,fielddata = true)
    private String file;
    @Field(type = FieldType.Text,fielddata = true, name = "class")
    private String clazz;
    private String path;
    private String level;
    private String line;
    private String thread;
    @Field(name = "@timestamp")
    private String currentTime;
}
