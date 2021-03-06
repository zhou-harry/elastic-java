package com.harry.elastic;

import com.harry.elastic.properties.CoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CoreProperties.class)
public class ElaticApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElaticApplication.class, args);
	}

}
