package com.harry.elatic.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bes.es")
public class CoreProperties {

    private String pythonUrl = "http://192.168.1.39:12345";

}
