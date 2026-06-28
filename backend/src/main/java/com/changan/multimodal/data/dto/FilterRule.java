package com.changan.multimodal.data.dto;

import lombok.Data;

@Data
public class FilterRule {
    private String field;
    private String operator;
    private String value;
}
