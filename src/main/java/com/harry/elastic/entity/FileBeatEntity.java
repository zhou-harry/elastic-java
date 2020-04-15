package com.harry.elastic.entity;

import com.sun.xml.internal.ws.developer.Serialization;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

@Data
@Document(indexName = "oppf_esb_log-2020.04.15", type = "_doc", createIndex = false)
public class FileBeatEntity implements Serializable{

    @Id
    @Field(type = FieldType.Text)
    private String id;
    @Field(name = "@timestamp")
    private String timestamp;
    @Field(name = "agent")
    private Agent agent;
    private String message;
    private List tags;
    private List patterns;
    @Field(name = "fields")
    private Fields fields;
    @Field(name = "log")
    private Log log;
    /*自定义字段*/
    @Field(type = FieldType.Date)
    private String date;
    private String time;
    @Field(type = FieldType.Text, fielddata = true)
    private String file;
    @Field(type = FieldType.Text, fielddata = true, name = "class")
    private String clazz;
    private String level;
    private String line;
    private String thread;
    @Field(name = "custom_text")
    private String customText;
    @Field(name = "trace_type")
    private String traceType;
    @Field(name = "trace_exception")
    private String traceException;

    @Data
    public class Agent implements Serializable {
        private String id;
        private String type;
        private String name;
        @Field(name = "ephemeral_id")
        private String ephemeralId;
        private String hostname;
        private String version;
    }
    @Data
    public class Fields implements Serializable {
        private String app;
    }

    @Data
    @Serialization
    public class Log implements Serializable {

        private File file;
        @Data
        @Serialization
        public class File implements Serializable {
            private String path;
        }
    }
}
