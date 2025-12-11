package com.impactsure.sanctionui.dto;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultipleUploadRequest {
    private List<UploadRequest> files;
    private List<InstallmentUpsertRequest> installments;
}
