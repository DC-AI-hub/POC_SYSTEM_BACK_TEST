package demo.backed.UT.FileUploadService;

import demo.backed.BaseServiceTest;
import demo.backed.entity.AttachmentFile;
import demo.backed.repository.AttachmentFileRepository;
import demo.backed.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileUploadService单元测试
 */
@DisplayName("文件上传服务测试")
class FileUploadServiceTest extends BaseServiceTest {

    @Mock
    private AttachmentFileRepository attachmentFileRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    private AttachmentFile testAttachment;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testAttachment = createTestAttachmentFile();
        
        // 设置配置属性
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "maxFileSize", 10485760L); // 10MB
        ReflectionTestUtils.setField(fileUploadService, "allowedExtensions", "pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,gif");
    }

    // ==================== 文件上传测试 ====================

    @Test
    @DisplayName("应该成功上传文件")
    void shouldUploadFileSuccessfully() throws IOException {
        // Given
        String businessType = "EXPENSE_APPLICATION";
        Long businessId = 1L;
        String attachmentType = "EXPENSE_RECEIPT";
        String description = "费用凭证";
        Long uploadUserId = 1L;
        String uploadUserName = "张三";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("invoice.pdf");
        when(multipartFile.getSize()).thenReturn(1024000L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(attachmentFileRepository.save(any(AttachmentFile.class))).thenReturn(testAttachment);

        // When
        AttachmentFile result = fileUploadService.uploadFile(
                multipartFile, businessType, businessId, attachmentType, description, uploadUserId, uploadUserName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBusinessType()).isEqualTo(businessType);
        assertThat(result.getBusinessId()).isEqualTo(businessId);
        assertThat(result.getAttachmentType()).isEqualTo(attachmentType);
        assertThat(result.getOriginalFilename()).isEqualTo("invoice.pdf");
        
        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile, atLeastOnce()).getSize();
        verify(multipartFile).getContentType();
        verify(multipartFile).getInputStream();
        verify(attachmentFileRepository).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("上传空文件应该抛出异常")
    void shouldThrowExceptionWhenUploadEmptyFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(
                multipartFile, "EXPENSE_APPLICATION", 1L, "EXPENSE_RECEIPT", "描述", 1L, "张三"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文件不能为空");
        
        verify(multipartFile).isEmpty();
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("上传文件名为空的文件应该抛出异常")
    void shouldThrowExceptionWhenUploadFileWithEmptyName() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(
                multipartFile, "EXPENSE_APPLICATION", 1L, "EXPENSE_RECEIPT", "描述", 1L, "张三"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文件名不能为空");
        
        verify(multipartFile).isEmpty();
        verify(multipartFile).getOriginalFilename();
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("上传超大文件应该抛出异常")
    void shouldThrowExceptionWhenUploadOversizedFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("large_file.pdf");
        when(multipartFile.getSize()).thenReturn(20971520L); // 20MB > 10MB limit

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(
                multipartFile, "EXPENSE_APPLICATION", 1L, "EXPENSE_RECEIPT", "描述", 1L, "张三"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文件大小超过限制");
        
        verify(multipartFile).isEmpty();
        verify(multipartFile).getOriginalFilename();
        verify(multipartFile).getSize();
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("上传不支持的文件类型应该抛出异常")
    void shouldThrowExceptionWhenUploadUnsupportedFileType() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("malware.exe");
        when(multipartFile.getSize()).thenReturn(1024L);

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(
                multipartFile, "EXPENSE_APPLICATION", 1L, "EXPENSE_RECEIPT", "描述", 1L, "张三"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不支持的文件类型");
        
        verify(multipartFile).isEmpty();
        verify(multipartFile).getOriginalFilename();
        verify(multipartFile).getSize();
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("上传文件名过长的文件应该抛出异常")
    void shouldThrowExceptionWhenUploadFileWithLongName() {
        // Given
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("a");
        }
        String longFilename = sb.toString() + ".pdf"; // 超过255字符
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(longFilename);
        when(multipartFile.getSize()).thenReturn(1024L);

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(
                multipartFile, "EXPENSE_APPLICATION", 1L, "EXPENSE_RECEIPT", "描述", 1L, "张三"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文件名过长");
        
        verify(multipartFile).isEmpty();
        verify(multipartFile).getOriginalFilename();
        verify(multipartFile).getSize();
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    // ==================== 文件下载测试 ====================

    @Test
    @DisplayName("应该成功下载文件")
    void shouldDownloadFileSuccessfully() throws IOException {
        // Given
        Long attachmentId = createTestAttachmentId();
        
        // 创建测试文件
        Path testFile = tempDir.resolve("test_file.pdf");
        Files.write(testFile, "test content".getBytes());
        
        testAttachment.setFilePath(testFile.toString());
        testAttachment.setIsActive(true);
        testAttachment.setDownloadCount(0);
        
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));
        when(attachmentFileRepository.save(any(AttachmentFile.class))).thenReturn(testAttachment);

        // When
        Resource result = fileUploadService.downloadFile(attachmentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
        
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("下载不存在的文件应该抛出异常")
    void shouldThrowExceptionWhenDownloadNonExistentFile() {
        // Given
        Long attachmentId = createTestAttachmentId();
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fileUploadService.downloadFile(attachmentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件不存在");
        
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("下载已删除的文件应该抛出异常")
    void shouldThrowExceptionWhenDownloadDeletedFile() {
        // Given
        Long attachmentId = createTestAttachmentId();
        testAttachment.setIsActive(false);
        
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));

        // When & Then
        assertThatThrownBy(() -> fileUploadService.downloadFile(attachmentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件已被删除");
        
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("下载物理文件不存在的附件应该抛出异常")
    void shouldThrowExceptionWhenDownloadFileWithNonExistentPhysicalFile() {
        // Given
        Long attachmentId = createTestAttachmentId();
        testAttachment.setFilePath("/non/existent/file.pdf");
        testAttachment.setIsActive(true);
        
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));

        // When & Then
        assertThatThrownBy(() -> fileUploadService.downloadFile(attachmentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文件不存在或不可读");
        
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    // ==================== 文件删除测试 ====================

    @Test
    @DisplayName("应该成功删除文件")
    void shouldDeleteFileSuccessfully() {
        // Given
        Long attachmentId = createTestAttachmentId();
        testAttachment.setIsActive(true);
        
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));
        when(attachmentFileRepository.save(any(AttachmentFile.class))).thenReturn(testAttachment);

        // When
        fileUploadService.deleteFile(attachmentId);

        // Then
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository).save(any(AttachmentFile.class));
    }

    @Test
    @DisplayName("删除不存在的文件应该抛出异常")
    void shouldThrowExceptionWhenDeleteNonExistentFile() {
        // Given
        Long attachmentId = createTestAttachmentId();
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fileUploadService.deleteFile(attachmentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件不存在");
        
        verify(attachmentFileRepository).findById(attachmentId);
        verify(attachmentFileRepository, never()).save(any(AttachmentFile.class));
    }

    // ==================== 业务附件查询测试 ====================

    @Test
    @DisplayName("应该成功获取业务附件列表")
    void shouldGetBusinessAttachmentsSuccessfully() {
        // Given
        String businessType = "EXPENSE_APPLICATION";
        Long businessId = 1L;
        List<AttachmentFile> attachments = Arrays.asList(testAttachment);
        
        when(attachmentFileRepository.findByBusinessTypeAndBusinessIdAndIsActiveTrue(businessType, businessId))
                .thenReturn(attachments);

        // When
        List<AttachmentFile> result = fileUploadService.getBusinessAttachments(businessType, businessId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessType()).isEqualTo(businessType);
        assertThat(result.get(0).getBusinessId()).isEqualTo(businessId);
        
        verify(attachmentFileRepository).findByBusinessTypeAndBusinessIdAndIsActiveTrue(businessType, businessId);
    }

    @Test
    @DisplayName("应该成功获取特定类型的业务附件列表")
    void shouldGetBusinessAttachmentsByTypeSuccessfully() {
        // Given
        String businessType = "EXPENSE_APPLICATION";
        Long businessId = 1L;
        String attachmentType = "EXPENSE_RECEIPT";
        List<AttachmentFile> attachments = Arrays.asList(testAttachment);
        
        when(attachmentFileRepository.findByBusinessTypeAndBusinessIdAndAttachmentTypeAndIsActiveTrue(
                businessType, businessId, attachmentType)).thenReturn(attachments);

        // When
        List<AttachmentFile> result = fileUploadService.getBusinessAttachments(businessType, businessId, attachmentType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttachmentType()).isEqualTo(attachmentType);
        
        verify(attachmentFileRepository).findByBusinessTypeAndBusinessIdAndAttachmentTypeAndIsActiveTrue(
                businessType, businessId, attachmentType);
    }

    // ==================== 附件详情查询测试 ====================

    @Test
    @DisplayName("应该成功获取附件详情")
    void shouldGetAttachmentByIdSuccessfully() {
        // Given
        Long attachmentId = createTestAttachmentId();
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));

        // When
        AttachmentFile result = fileUploadService.getAttachmentById(attachmentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testAttachment.getId());
        assertThat(result.getOriginalFilename()).isEqualTo(testAttachment.getOriginalFilename());
        
        verify(attachmentFileRepository).findById(attachmentId);
    }

    @Test
    @DisplayName("获取不存在的附件详情应该抛出异常")
    void shouldThrowExceptionWhenGetNonExistentAttachment() {
        // Given
        Long attachmentId = createTestAttachmentId();
        when(attachmentFileRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fileUploadService.getAttachmentById(attachmentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件不存在");
        
        verify(attachmentFileRepository).findById(attachmentId);
    }

    // ==================== 内容类型获取测试 ====================

    @Test
    @DisplayName("应该根据已有内容类型返回正确的内容类型")
    void shouldGetContentTypeFromExistingContentType() {
        // Given
        testAttachment.setContentType("application/pdf");

        // When
        String result = fileUploadService.getContentType(testAttachment);

        // Then
        assertThat(result).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("应该根据文件扩展名推断内容类型")
    void shouldGetContentTypeFromFileExtension() {
        // Given
        testAttachment.setContentType(null);
        testAttachment.setFileExtension("pdf");

        // When
        String result = fileUploadService.getContentType(testAttachment);

        // Then
        assertThat(result).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("应该为未知文件类型返回默认内容类型")
    void shouldGetDefaultContentTypeForUnknownFileType() {
        // Given
        testAttachment.setContentType(null);
        testAttachment.setFileExtension("unknown");

        // When
        String result = fileUploadService.getContentType(testAttachment);

        // Then
        assertThat(result).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("应该为各种文件扩展名返回正确的内容类型")
    void shouldGetCorrectContentTypeForVariousExtensions() {
        // Given & When & Then
        testAttachment.setContentType(null);
        
        // 测试各种文件类型
        testAttachment.setFileExtension("doc");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("application/msword");
        
        testAttachment.setFileExtension("docx");
        assertThat(fileUploadService.getContentType(testAttachment))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        
        testAttachment.setFileExtension("xls");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("application/vnd.ms-excel");
        
        testAttachment.setFileExtension("xlsx");
        assertThat(fileUploadService.getContentType(testAttachment))
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        testAttachment.setFileExtension("jpg");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("image/jpeg");
        
        testAttachment.setFileExtension("jpeg");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("image/jpeg");
        
        testAttachment.setFileExtension("png");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("image/png");
        
        testAttachment.setFileExtension("gif");
        assertThat(fileUploadService.getContentType(testAttachment)).isEqualTo("image/gif");
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试附件文件实体
     */
    private AttachmentFile createTestAttachmentFile() {
        AttachmentFile attachment = new AttachmentFile();
        attachment.setId(createTestAttachmentId());
        attachment.setBusinessType("EXPENSE_APPLICATION");
        attachment.setBusinessId(1L);
        attachment.setAttachmentType("EXPENSE_RECEIPT");
        attachment.setOriginalFilename("invoice.pdf");
        attachment.setStoredFilename("20250112_143000_12345678.pdf");
        attachment.setFilePath("/uploads/expense_application/2025/01/12/20250112_143000_12345678.pdf");
        attachment.setFileSize(1024000L);
        attachment.setContentType("application/pdf");
        attachment.setFileExtension("pdf");
        attachment.setDescription("费用凭证");
        attachment.setUploadUserId(1L);
        attachment.setUploadUserName("张三");
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setDownloadCount(0);
        attachment.setIsActive(true);
        attachment.setCreatedTime(LocalDateTime.now());
        attachment.setUpdatedTime(LocalDateTime.now());
        return attachment;
    }

    /**
     * 创建测试附件ID
     */
    protected Long createTestAttachmentId() {
        return 1L;
    }
} 