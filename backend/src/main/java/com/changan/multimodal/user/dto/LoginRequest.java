package com.changan.multimodal.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "module不能为空")
    private String module;

    private String ip;
}
