package com.ruici.ai.modules.document;

import com.ruici.ai.modules.document.service.ResumeDeleteService;
import com.ruici.ai.modules.document.service.ResumeHistoryService;
import com.ruici.ai.modules.document.service.ResumeUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("文档控制器原文件预览下载测试")
class ResumeControllerTest {

    @Mock
    private ResumeUploadService uploadService;

    @Mock
    private ResumeDeleteService deleteService;

    @Mock
    private ResumeHistoryService historyService;

    @InjectMocks
    private ResumeController controller;

    @Test
    @DisplayName("预览原文件时返回 inline 处置头与原始内容类型")
    void shouldReturnInlineDispositionForPreview() {
        given(historyService.getOriginalFile(1L)).willReturn(
            new ResumeHistoryService.OriginalFileResult(
                new byte[] {1, 2, 3},
                "profile.pdf",
                MediaType.APPLICATION_PDF_VALUE
            )
        );

        ResponseEntity<byte[]> response = controller.getOriginalFile(1L, "inline");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("inline; filename*=UTF-8''profile.pdf");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("下载原文件时返回 attachment 处置头")
    void shouldReturnAttachmentDispositionForDownload() {
        given(historyService.getOriginalFile(2L)).willReturn(
            new ResumeHistoryService.OriginalFileResult(
                new byte[] {4, 5},
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        );

        ResponseEntity<byte[]> response = controller.getOriginalFile(2L, "attachment");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename*=UTF-8''resume.docx");
        assertThat(response.getHeaders().getContentType())
            .hasToString("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    @DisplayName("HTML 原文件请求预览时强制降级为下载")
    void shouldForceAttachmentForHtmlPreview() {
        given(historyService.getOriginalFile(3L)).willReturn(
            new ResumeHistoryService.OriginalFileResult(
                new byte[] {9},
                "unsafe.html",
                MediaType.TEXT_HTML_VALUE
            )
        );

        ResponseEntity<byte[]> response = controller.getOriginalFile(3L, "inline");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename*=UTF-8''unsafe.html");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}
