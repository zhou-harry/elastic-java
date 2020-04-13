package com.harry.elastic.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogPatternRequest {

    private Integer num;
    private Map<String, String> types;
    private Map<String, String> msg;
}
