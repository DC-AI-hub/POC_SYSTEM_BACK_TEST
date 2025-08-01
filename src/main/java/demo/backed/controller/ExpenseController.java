package demo.backed.controller;

import demo.backed.dto.*;
import demo.backed.entity.AttachmentFile;
import demo.backed.service.ExpenseApplicationService;
import demo.backed.service.FileUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 费用申请控制器
 * 提供费用申请的CRUD操作和审批流程管理
 */
@RestController
@RequestMapping("/api/expense")
@Api(tags = "费用申请管理")
@Slf4j
@Validated
public class ExpenseController {
    
    @Autowired
    private ExpenseApplicationService applicationService;
    
    @Autowired
    private FileUploadService fileUploadService;
    
    /**
     * 创建费用申请
     */
    @PostMapping("/applications")
    @ApiOperation("创建费用申请")
    public ApiResponse<ExpenseApplicationDTO> createApplication(
            @Valid @RequestBody CreateExpenseApplicationDTO dto) {
        try {
            log.info("创建费用申请，申请人ID: {}", dto.getApplicantId());
            ExpenseApplicationDTO result = applicationService.createApplication(dto);
            return ApiResponse.success("费用申请创建成功", result);
        } catch (Exception e) {
            log.error("创建费用申请失败", e);
            return ApiResponse.error("创建费用申请失败: " + e.getMessage());
        }
    }
    
    /**
     * 提交费用申请审批
     */
    @PostMapping("/applications/{id}/submit")
    @ApiOperation("提交费用申请审批")
    public ApiResponse<ExpenseApplicationDTO> submitApplication(@PathVariable Long id) {
        try {
            log.info("提交费用申请审批，申请ID: {}", id);
            ExpenseApplicationDTO result = applicationService.submitForApproval(id);
            return ApiResponse.success("申请已提交，等待审批", result);
        } catch (Exception e) {
            log.error("提交费用申请失败，申请ID: {}", id, e);
            return ApiResponse.error("提交申请失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取费用申请列表
     */
    @GetMapping("/applications")
    @ApiOperation("获取费用申请列表")
    public ApiResponse<Page<ExpenseApplicationDTO>> getApplications(
            ExpenseQueryDTO queryDto,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            log.debug("查询费用申请列表，查询条件: {}", queryDto);
            Page<ExpenseApplicationDTO> result = applicationService.findApplications(queryDto, pageable);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询费用申请列表失败", e);
            return ApiResponse.error("查询申请列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取费用申请详情
     */
    @GetMapping("/applications/{id}")
    @ApiOperation("获取费用申请详情")
    public ApiResponse<ExpenseApplicationDTO> getApplication(@PathVariable Long id) {
        try {
            log.debug("获取费用申请详情，申请ID: {}", id);
            ExpenseApplicationDTO result = applicationService.getApplicationDetail(id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取费用申请详情失败，申请ID: {}", id, e);
            return ApiResponse.error("获取申请详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新费用申请
     */
    @PutMapping("/applications/{id}")
    @ApiOperation("更新费用申请")
    public ApiResponse<ExpenseApplicationDTO> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody CreateExpenseApplicationDTO dto) {
        try {
            log.info("更新费用申请，申请ID: {}", id);
            ExpenseApplicationDTO result = applicationService.updateApplication(id, dto);
            return ApiResponse.success("申请更新成功", result);
        } catch (Exception e) {
            log.error("更新费用申请失败，申请ID: {}", id, e);
            return ApiResponse.error("更新申请失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除费用申请
     */
    @DeleteMapping("/applications/{id}")
    @ApiOperation("删除费用申请")
    public ApiResponse<Void> deleteApplication(@PathVariable Long id) {
        try {
            log.info("删除费用申请，申请ID: {}", id);
            applicationService.deleteApplication(id);
            return ApiResponse.<Void>success("申请删除成功", null);
        } catch (Exception e) {
            log.error("删除费用申请失败，申请ID: {}", id, e);
            return ApiResponse.error("删除申请失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传申请附件
     */
    @PostMapping("/applications/{id}/attachments")
    @ApiOperation("上传申请附件")
    public ApiResponse<AttachmentFileDTO> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "BASIC_INFO") String attachmentType,
            @RequestParam(required = false) String description) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.error("请选择要上传的文件");
            }

            // 从认证上下文中获取当前用户信息
            // 当前使用模拟数据，实际应用中应该从JWT Token或Session中获取
            Long currentUserId = 1L;
            String currentUserName = "测试用户";

            // 上传文件
            AttachmentFile attachment = fileUploadService.uploadFile(
                    file,
                    AttachmentFile.BusinessType.EXPENSE_APPLICATION.getCode(),
                    id,
                    attachmentType,
                    description,
                    currentUserId,
                    currentUserName
            );

            // 转换为DTO
            AttachmentFileDTO dto = convertToAttachmentDTO(attachment);

            log.info("上传附件成功，申请ID: {}, 文件名: {}", id, file.getOriginalFilename());
            return ApiResponse.success("附件上传成功", dto);
        } catch (Exception e) {
            log.error("上传附件失败，申请ID: {}", id, e);
            return ApiResponse.error("附件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取申请附件列表
     */
    @GetMapping("/applications/{id}/attachments")
    @ApiOperation("获取申请附件列表")
    public ApiResponse<List<AttachmentFileDTO>> getApplicationAttachments(
            @PathVariable Long id,
            @RequestParam(required = false) String attachmentType) {
        try {
            List<AttachmentFile> attachments;
            if (attachmentType != null && !attachmentType.isEmpty()) {
                attachments = fileUploadService.getBusinessAttachments(
                        AttachmentFile.BusinessType.EXPENSE_APPLICATION.getCode(), id, attachmentType);
            } else {
                attachments = fileUploadService.getBusinessAttachments(
                        AttachmentFile.BusinessType.EXPENSE_APPLICATION.getCode(), id);
            }

            List<AttachmentFileDTO> dtos = attachments.stream()
                    .map(this::convertToAttachmentDTO)
                    .collect(Collectors.toList());

            return ApiResponse.success(dtos);
        } catch (Exception e) {
            log.error("获取申请附件列表失败，申请ID: {}", id, e);
            return ApiResponse.error("获取附件列表失败: " + e.getMessage());
        }
    }

    /**
     * 下载附件
     */
    @GetMapping("/attachments/{attachmentId}/download")
    @ApiOperation("下载附件")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            AttachmentFile attachment = fileUploadService.getAttachmentById(attachmentId);
            Resource resource = fileUploadService.downloadFile(attachmentId);

            String contentType = fileUploadService.getContentType(attachment);
            String encodedFilename = encodeFilename(attachment.getOriginalFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + encodedFilename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("下载附件失败，附件ID: {}", attachmentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除附件
     */
    @DeleteMapping("/attachments/{attachmentId}")
    @ApiOperation("删除附件")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            fileUploadService.deleteFile(attachmentId);
            log.info("删除附件成功，附件ID: {}", attachmentId);
            return ApiResponse.success("附件删除成功", null);
        } catch (Exception e) {
            log.error("删除附件失败，附件ID: {}", attachmentId, e);
            return ApiResponse.error("删除附件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取费用科目列表
     */
    @GetMapping("/categories")
    @ApiOperation("获取费用科目列表")
    public ApiResponse<Map<String, Object>> getExpenseCategories() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 定义费用科目列表
            String[] categories = {
                "交通费", "住宿费", "餐饮费", "办公用品", 
                "通讯费", "培训费", "会议费", "差旅费", 
                "设备采购", "维护费", "咨询费", "其他"
            };
            
            response.put("categories", categories);
            response.put("count", categories.length);
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取费用科目列表失败", e);
            return ApiResponse.error("获取费用科目失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取报销统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取费用申请统计信息")
    public ApiResponse<Map<String, Object>> getExpenseStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long departmentId) {
        try {
            // 实现统计查询
            // 当前返回模拟数据，后续可以基于实际数据库数据统计
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalApplications", 25);
            statistics.put("pendingApplications", 8);
            statistics.put("approvedApplications", 15);
            statistics.put("rejectedApplications", 2);
            statistics.put("totalAmount", 45600.00);
            statistics.put("averageAmount", 1824.00);
            
            log.debug("获取费用申请统计信息，参数: startDate={}, endDate={}, departmentId={}", 
                     startDate, endDate, departmentId);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取申请人的历史申请
     */
    @GetMapping("/applications/history/{applicantId}")
    @ApiOperation("获取申请人历史申请")
    public ApiResponse<Page<ExpenseApplicationDTO>> getApplicantHistory(
            @PathVariable Long applicantId,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            ExpenseQueryDTO queryDto = new ExpenseQueryDTO();
            queryDto.setApplicantId(applicantId);
            
            Page<ExpenseApplicationDTO> result = applicationService.findApplications(queryDto, pageable);
            
            log.debug("获取申请人历史申请，申请人ID: {}, 结果数量: {}", applicantId, result.getTotalElements());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取申请人历史申请失败，申请人ID: {}", applicantId, e);
            return ApiResponse.error("获取历史申请失败: " + e.getMessage());
        }
    }

    /**
     * 转换AttachmentFile为AttachmentFileDTO
     */
    private AttachmentFileDTO convertToAttachmentDTO(AttachmentFile attachment) {
        AttachmentFileDTO dto = new AttachmentFileDTO();
        dto.setId(attachment.getId());
        dto.setBusinessType(attachment.getBusinessType());
        dto.setBusinessId(attachment.getBusinessId());
        dto.setAttachmentType(attachment.getAttachmentType());
        dto.setOriginalFilename(attachment.getOriginalFilename());
        dto.setStoredFilename(attachment.getStoredFilename());
        dto.setFileSize(attachment.getFileSize());
        dto.setHumanReadableFileSize(attachment.getHumanReadableFileSize());
        dto.setContentType(attachment.getContentType());
        dto.setFileExtension(attachment.getFileExtension());
        dto.setDescription(attachment.getDescription());
        dto.setUploadUserId(attachment.getUploadUserId());
        dto.setUploadUserName(attachment.getUploadUserName());
        dto.setUploadTime(attachment.getUploadTime());
        dto.setDownloadCount(attachment.getDownloadCount());
        dto.setLastDownloadTime(attachment.getLastDownloadTime());
        dto.setIsImage(attachment.isImage());
        dto.setIsPdf(attachment.isPdf());
        dto.setIsDocument(attachment.isDocument());
        dto.setIsActive(attachment.getIsActive());
        dto.setDownloadUrl("/api/expense/attachments/" + attachment.getId() + "/download");
        dto.setPreviewUrl("/api/expense/attachments/" + attachment.getId() + "/preview");
        return dto;
    }

    /**
     * 编码文件名用于下载响应
     */
    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            log.warn("文件名编码失败: {}", filename, e);
            return filename;
        }
    }
} 