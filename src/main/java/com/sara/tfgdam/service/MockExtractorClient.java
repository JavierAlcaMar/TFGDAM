package com.sara.tfgdam.service;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class MockExtractorClient implements ExtractorClient {

    @Override
    public String extractRAs(Path filePath, String originalFilename) {
        String safeFilename = originalFilename.replace("\"", "");
        return "{\"source\":\"mock-extractor\",\"filename\":\"" + safeFilename +
                "\",\"detectedRas\":[{\"code\":\"RA1\",\"name\":\"Resultado importado 1\",\"weightPercent\":50.00}," +
                "{\"code\":\"RA2\",\"name\":\"Resultado importado 2\",\"weightPercent\":50.00}]," +
                "\"filePath\":\"" + filePath.toAbsolutePath().toString().replace("\\", "\\\\") + "\"}";
    }
}
