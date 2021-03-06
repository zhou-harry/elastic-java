package com.harry.elastic.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "oppf_esb_log-2020.04.13", type = "_doc", createIndex = false)
public class EppfLogEntity {
    @Id
    @Field(type = FieldType.Text)
    private String id;
    private String host;
    private String message;
    private String type;
    @Field(type = FieldType.Date)
    private String date;
    private String time;
    @Field(type = FieldType.Text, fielddata = true)
    private String file;
    @Field(type = FieldType.Text, fielddata = true, name = "class")
    private String clazz;
    private String path;
    private String level;
    private String line;
    private String thread;
    @Field(name = "@timestamp")
    private String currentTime;
    @Field(name = "custom_text")
    private String customText;
    @Field(name = "trace_exception")
    private String traceException;
    //日志模式标签
    private String tag;
}
