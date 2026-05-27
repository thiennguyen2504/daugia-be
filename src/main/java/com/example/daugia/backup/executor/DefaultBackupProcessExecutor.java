package com.example.daugia.backup.executor;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class DefaultBackupProcessExecutor implements BackupProcessExecutor {

    @Override
    public ProcessResult executeAndStreamOutput(List<String> command, Map<String, String> environment, OutputStream outputStream) {
        return runProcess(command, environment, outputStream, null);
    }

    @Override
    public ProcessResult executeWithInput(List<String> command, Map<String, String> environment, InputStream inputStream) {
        return runProcess(command, environment, null, inputStream);
    }

    private ProcessResult runProcess(List<String> command,
                                     Map<String, String> environment,
                                     OutputStream outputStream,
                                     InputStream inputStream) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (environment != null && !environment.isEmpty()) {
            builder.environment().putAll(environment);
        }

        try {
            Process process = builder.start();
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

            CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(() -> {
                try (InputStream processOut = process.getInputStream()) {
                    if (outputStream != null) {
                        processOut.transferTo(outputStream);
                    } else {
                        processOut.transferTo(OutputStream.nullOutputStream());
                    }
                } catch (IOException ignored) {}
            });

            if (inputStream != null) {
                try (OutputStream processIn = process.getOutputStream()) {
                    inputStream.transferTo(processIn);
                }
            } else {
                process.getOutputStream().close();
            }

            int exitCode = process.waitFor();
            stdoutFuture.join();
            String stderr = stderrFuture.join();
            return new ProcessResult(exitCode, stderr);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ProcessResult(1, ex.getMessage());
        } catch (IOException ex) {
            return new ProcessResult(1, ex.getMessage());
        }
    }

    private CompletableFuture<String> readStreamAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                inputStream.transferTo(buffer);
                return buffer.toString(StandardCharsets.UTF_8);
            } catch (IOException ex) {
                return ex.getMessage();
            }
        });
    }
}
