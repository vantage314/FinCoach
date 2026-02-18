package com.fincoach.core.healthv2.service.admin.impl;

import com.fincoach.core.healthv2.service.admin.CrawlerRunRequest;
import com.fincoach.core.healthv2.service.admin.CrawlerRunResult;
import com.fincoach.core.healthv2.service.admin.CrawlerRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
public class DefaultCrawlerRunner implements CrawlerRunner {

    @Override
    public CrawlerRunResult run(CrawlerRunRequest request, Consumer<String> lineConsumer, AtomicBoolean stopSignal) {
        CrawlerRunResult result = new CrawlerRunResult();
        if (request == null || request.getScriptPath() == null || request.getScriptPath().isBlank()) {
            result.setSuccess(false);
            result.setError("script path missing");
            return result;
        }

        File script = Path.of(request.getScriptPath()).toFile();
        if (!script.exists()) {
            result.setSuccess(false);
            result.setError("script not found: " + script.getAbsolutePath());
            return result;
        }

        List<String> command = new ArrayList<>();
        String scriptPath = script.getAbsolutePath();
        if (scriptPath.toLowerCase().endsWith(".ps1")) {
            command.add("powershell");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(scriptPath);
        } else {
            command.add("python");
            command.add(scriptPath);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        int lines = 0;
        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stopSignal != null && stopSignal.get()) {
                        process.destroy();
                        break;
                    }
                    if (!line.isBlank()) {
                        lineConsumer.accept(line);
                        lines++;
                    }
                }
            }
            int exitCode = process.waitFor();
            if (stopSignal != null && stopSignal.get()) {
                result.setSuccess(false);
                result.setError("stopped");
            } else if (exitCode != 0) {
                result.setSuccess(false);
                result.setError("process exit " + exitCode);
            } else {
                result.setSuccess(true);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        result.setLines(lines);
        return result;
    }
}
