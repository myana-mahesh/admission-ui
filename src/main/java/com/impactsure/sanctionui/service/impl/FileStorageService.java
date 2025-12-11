package com.impactsure.sanctionui.service.impl;



import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

@Value("${upload.base-dir}") // relative to app working dir
private String baseDir;

public StoredFile store(Long admissionId, MultipartFile file) throws IOException {
 String day = LocalDate.now().toString(); // yyyy-MM-dd
 String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
 Path dir = Paths.get(baseDir, String.valueOf(admissionId), day);
 Files.createDirectories(dir);
 Path dest = dir.resolve(safeName);
 Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

 // public URL served by Spring static handler (see WebMvcConfig below)
 String url = "/uploads/" + admissionId + "/" + day + "/" + safeName;
 return new StoredFile(dest, url);
}

public record StoredFile(Path path, String url) {}
}
