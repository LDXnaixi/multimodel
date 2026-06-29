package com.changan.multimodal.inference.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class InferenceInput {

    private String inputId;

    private String sampleId;

    /**
     * Internal runner path. Kept for server-side task compatibility; browser clients
     * should submit sampleId instead of learning server filesystem paths.
     */
    private String sourceUri;

    private Map<String, Object> attributes;

    @AssertTrue(message = "sampleId和sourceUri至少提供一个")
    public boolean isInputReferencePresent() {
        return (sampleId != null && !sampleId.isBlank())
                || (sourceUri != null && !sourceUri.isBlank());
    }
}
