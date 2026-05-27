package com.example.daugia.backup.executor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface BackupProcessExecutor {

    ProcessResult executeAndStreamOutput(List<String> command, Map<String, String> environment, OutputStream outputStream);

    ProcessResult executeWithInput(List<String> command, Map<String, String> environment, InputStream inputStream);
}
