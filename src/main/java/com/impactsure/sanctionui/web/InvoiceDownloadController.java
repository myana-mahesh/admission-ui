package com.impactsure.sanctionui.web;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceDownloadController {

	@Value("${invoice.storage.base}")
	private String invoiceDir;

    @GetMapping("/download/{admissionId}/{fileName}")
    public ResponseEntity<FileSystemResource> downloadInvoice(
            @PathVariable Long admissionId,
            @PathVariable String fileName) {

        

        File file = new File(invoiceDir+File.separator+admissionId+File.separator+fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(MediaType.APPLICATION_PDF);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
    
 
}
