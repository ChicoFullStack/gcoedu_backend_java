package com.gcoedu.core.domain.dto.tenant;

/**
 * Retornado logo após o upload do cartão-resposta.
 * O cliente usa o jobId para consultar o progresso via SSE ou polling.
 */
public record OmrUploadResponseDTO(
    String jobId,
    String message,
    String minioObjectKey
) {}
