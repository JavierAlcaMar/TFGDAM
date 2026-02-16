package com.sara.tfgdam.service;

import java.nio.file.Path;

public interface ExtractorClient {

    String extractRAs(Path filePath, String originalFilename);
}
