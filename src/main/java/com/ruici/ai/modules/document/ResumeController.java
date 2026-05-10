package com.ruici.ai.modules.document;

import com.ruici.ai.common.annotation.RateLimit;
import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.common.exception.ErrorCode;
import com.ruici.ai.common.result.Result;
import com.ruici.ai.modules.document.model.ResumeDetailDTO;
import com.ruici.ai.modules.document.model.ResumeListItemDTO;
import com.ruici.ai.modules.document.model.AnalysisDifficulty;
import com.ruici.ai.modules.document.service.ResumeDeleteService;
import com.ruici.ai.modules.document.service.ResumeHistoryService;
import com.ruici.ai.modules.document.service.ResumeUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 通用文档控制器。
 *
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "通用职业文档上传、分析、导出与删除")
public class ResumeController {

    private final ResumeUploadService uploadService;
    private final ResumeDeleteService deleteService;
    private final ResumeHistoryService historyService;

    /**
     * 上传文档并获取分析结果。
     *
     * @param file 文档文件（支持 PDF、DOCX、DOC、TXT、MD 等）
     * @return 文档分析结果，包含评分和建议
     */
    @PostMapping(value = "/api/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 5)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
    public Result<Map<String, Object>> uploadAndAnalyze(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "profession", required = false) String profession,
        @RequestParam(value = "analysisDifficulty", required = false) AnalysisDifficulty analysisDifficulty
    ) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file, profession, analysisDifficulty);
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if (isDuplicate) {
            return Result.success("检测到相同文档，已返回历史分析结果", result);
        }
        return Result.success(result);
    }

    /**
     * 获取所有文档列表。
     */
    @GetMapping("/api/documents")
    public Result<List<ResumeListItemDTO>> getAllResumes() {
        List<ResumeListItemDTO> resumes = historyService.getAllResumes();
        return Result.success(resumes);
    }

    /**
     * 获取文档详情（包含分析历史）。
     */
    @GetMapping("/api/documents/{id}/detail")
    public Result<ResumeDetailDTO> getResumeDetail(@PathVariable Long id) {
        ResumeDetailDTO detail = historyService.getResumeDetail(id);
        return Result.success(detail);
    }

    /**
     * 预览或下载原始上传文件。
     *
     * <p>安全策略：对可执行文本类型（HTML/SVG/XHTML）强制降级为下载流，
     * 避免浏览器直接执行页面内容或嵌入脚本。</p>
     */
    @GetMapping("/api/documents/{id}/original-file")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
    public ResponseEntity<InputStreamResource> getOriginalFile(
        @PathVariable Long id,
        @RequestParam(value = "disposition", defaultValue = "inline") String disposition
    ) {
        try {
            var result = historyService.getOriginalFileAsStream(id);
            String encodedFilename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);
            String requestedDisposition = "attachment".equalsIgnoreCase(disposition) ? "attachment" : "inline";
            boolean shouldForceAttachment = shouldForceDownload(result.contentType(), requestedDisposition);
            String effectiveDisposition = shouldForceAttachment ? "attachment" : requestedDisposition;
            String safeContentType = shouldForceAttachment
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : result.contentType();
            MediaType mediaType = MediaType.parseMediaType(safeContentType);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    effectiveDisposition + "; filename*=UTF-8''" + encodedFilename)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .contentLength(result.contentLength())
                .body(new InputStreamResource(result.inputStream()));
        } catch (BusinessException e) {
            log.warn("获取原始文档业务异常: documentId={}, code={}, message={}",
                id, e.getCode(), e.getMessage());
            int httpStatus = mapBusinessExceptionToHttpStatus(e);
            return ResponseEntity.status(httpStatus).build();
        } catch (Exception e) {
            log.error("获取原始文档失败: documentId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出文档分析报告为 PDF。
     */
    @GetMapping("/api/documents/{id}/export")
    public ResponseEntity<byte[]> exportAnalysisPdf(@PathVariable Long id) {
        try {
            var result = historyService.exportAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
        } catch (Exception e) {
            log.error("导出文档分析 PDF 失败: documentId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除文档。
     */
    @DeleteMapping("/api/documents/{id}")
    public Result<Void> deleteResume(@PathVariable Long id) {
        deleteService.deleteResume(id);
        return Result.success(null);
    }

    /**
     * 重新分析文档（手动重试）。
     */
    @PostMapping("/api/documents/{id}/reanalyze")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 2)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 2)
    public Result<Void> reanalyze(
        @PathVariable Long id,
        @RequestParam(value = "profession", required = false) String profession,
        @RequestParam(value = "analysisDifficulty", required = false) AnalysisDifficulty analysisDifficulty
    ) {
        uploadService.reanalyze(id, profession, analysisDifficulty);
        return Result.success(null);
    }

    /**
     * 健康检查接口。
     */
    @GetMapping("/api/documents/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
            "status", "UP",
            "service", "Ruici AI Platform - Document Service"
        ));
    }

    /**
     * 对高风险可执行文本类型做安全降级，拦截 HTML / SVG / XHTML 内联预览。
     */
    private boolean shouldForceDownload(String contentType, String disposition) {
        if (!"inline".equalsIgnoreCase(disposition) || contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.contains("text/html")
            || lower.contains("image/svg+xml")
            || lower.contains("application/xhtml+xml")
            || lower.contains("text/xml");
    }

    private int mapBusinessExceptionToHttpStatus(BusinessException e) {
        int code = e.getCode();
        if (code == ErrorCode.RESUME_NOT_FOUND.getCode() || code == ErrorCode.NOT_FOUND.getCode()) {
            return 404;
        }
        if (code == ErrorCode.STORAGE_DOWNLOAD_FAILED.getCode()) {
            return 502;
        }
        return 500;
    }

}
