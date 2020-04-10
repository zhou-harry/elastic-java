package com.harry.elatic.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogPatternRsponse {

    private String id;
    private String name;
    private Integer count;
    private List<String> ids;
}
