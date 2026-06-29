package com.changan.multimodal.inference.service;

import com.changan.multimodal.inference.dto.InferenceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LocalModelProcessRunner {

    private final ObjectMapper objectMapper;

    @Value("${app.model-runner.python-command:python}")
    private String pythonCommand;

    @Value("${app.model-runner.script-path:./model-runners/demo_model_runner.py}")
    private String scriptPath;

    @Value("${app.model-runner.timeout-ms:8000}")
    private long timeoutMs;

    public Optional<JsonNode> run(InferenceRequest request, String runtimeCommand) {
        List<String> command = resolveCommand(runtimeCommand);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(objectMapper.writeValueAsString(request));
            }
            boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return Optional.empty();
            }
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            if (process.exitValue() != 0 || output.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(output.toString()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Duration timeout() {
        return Duration.ofMillis(timeoutMs);
    }

    private List<String> resolveCommand(String runtimeCommand) {
        if (runtimeCommand != null && !runtimeCommand.isBlank()) {
            return splitCommand(runtimeCommand);
        }
        return List.of(pythonCommand, scriptPath);
    }

    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char c : command.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }
}

