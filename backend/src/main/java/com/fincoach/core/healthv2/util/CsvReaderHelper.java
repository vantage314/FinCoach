package com.fincoach.core.healthv2.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvReaderHelper {

    public List<CsvRow> readRows(MultipartFile file) throws Exception {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (row == 1) {
                    continue;
                }
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                rows.add(new CsvRow(row, line, columns));
            }
        }
        return rows;
    }

    public record CsvRow(int row, String raw, String[] columns) {}
}
