package demo.backed.service;

import demo.backed.entity.AttachmentFile;
import demo.backed.repository.AttachmentFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务
 */
@Service
@Slf4j
@Transactional
public class FileUploadService {

    @Autowired
    private AttachmentFileRepository attachmentFileRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:10485760}") // 10MB
    private long maxFileSize;

    @Value("${app.upload.allowed-extensions:pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,gif}")
    private String allowedExtensions;

    /**
     * 上传文件
     */
    public AttachmentFile uploadFile(MultipartFile file, String businessType, Long businessId, 
                                   String attachmentType, String description, Long uploadUserId, String uploadUserName) {
        try {
            // 验证文件
            validateFile(file);

            // 创建存储目录
            Path uploadPath = createUploadPath(businessType);

            // 生成存储文件名
            String storedFilename = generateStoredFilename(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(storedFilename);

            // 确保目录存在
            Files.createDirectories(uploadPath);

            // 保存文件
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 创建附件记录
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown_file";
            }
            
            AttachmentFile attachment = new AttachmentFile();
            attachment.setBusinessType(businessType);
            attachment.setBusinessId(businessId);
            attachment.setAttachmentType(attachmentType);
            attachment.setOriginalFilename(originalFilename);
            attachment.setStoredFilename(storedFilename);
            attachment.setFilePath(filePath.toString());
            attachment.setFileSize(file.getSize());
            attachment.setContentType(file.getContentType());
            attachment.setFileExtension(getFileExtension(originalFilename));
            attachment.setDescription(description);
            attachment.setUploadUserId(uploadUserId);
            attachment.setUploadUserName(uploadUserName);
            attachment.setUploadTime(LocalDateTime.now());

            AttachmentFile savedAttachment = attachmentFileRepository.save(attachment);

            log.info("文件上传成功: {} -> {}", originalFilename, storedFilename);
            return savedAttachment;

        } catch (IOException e) {
            String originalFilename = file.getOriginalFilename();
            log.error("文件上传失败: {}", originalFilename != null ? originalFilename : "unknown_file", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    public Resource downloadFile(Long attachmentId) {
        try {
            AttachmentFile attachment = attachmentFileRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("附件不存在"));

            if (!attachment.getIsActive()) {
                throw new RuntimeException("附件已被删除");
            }

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // 更新下载统计
                attachment.setDownloadCount(attachment.getDownloadCount() + 1);
                attachment.setLastDownloadTime(LocalDateTime.now());
                attachmentFileRepository.save(attachment);

                log.info("下载文件: {} (ID: {})", attachment.getOriginalFilename(), attachmentId);
                return resource;
            } else {
                log.error("文件不存在或不可读: {}", attachment.getFilePath());
                throw new RuntimeException("文件不存在或不可读");
            }

        } catch (MalformedURLException e) {
            log.error("文件路径错误，附件ID: {}", attachmentId, e);
            throw new RuntimeException("文件路径错误");
        }
    }

    /**
     * 删除文件（软删除）
     */
    public void deleteFile(Long attachmentId) {
        AttachmentFile attachment = attachmentFileRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("附件不存在"));

        attachment.setIsActive(false);
        attachmentFileRepository.save(attachment);

        log.info("软删除附件: {} (ID: {})", attachment.getOriginalFilename(), attachmentId);
    }



    /**
     * 获取业务附件列表
     */
    public List<AttachmentFile> getBusinessAttachments(String businessType, Long businessId) {
        return attachmentFileRepository.findByBusinessTypeAndBusinessIdAndIsActiveTrue(businessType, businessId);
    }

    /**
     * 获取特定类型的业务附件列表
     */
    public List<AttachmentFile> getBusinessAttachments(String businessType, Long businessId, String attachmentType) {
        return attachmentFileRepository.findByBusinessTypeAndBusinessIdAndAttachmentTypeAndIsActiveTrue(
                businessType, businessId, attachmentType);
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件名是否为空
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException(String.format("文件大小超过限制（最大 %d MB）", maxFileSize / 1024 / 1024));
        }

        // 检查文件扩展名（使用已经验证的文件名）
        String extension = getFileExtension(originalFilename);
        List<String> allowedExtensionList = Arrays.asList(allowedExtensions.split(","));
        if (!allowedExtensionList.contains(extension.toLowerCase())) {
            throw new RuntimeException(String.format("不支持的文件类型，支持的类型: %s", allowedExtensions));
        }

        // 检查文件名长度（此时已经确保文件名不为null）
        if (originalFilename.length() > 255) {
            throw new RuntimeException("文件名过长，请重命名后上传");
        }
    }

    /**
     * 创建上传路径
     */
    private Path createUploadPath(String businessType) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return Paths.get(uploadDir, businessType.toLowerCase(), datePath);
    }

    /**
     * 生成存储文件名
     */
    private String generateStoredFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "unknown_file";
        }
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 获取附件详情
     */
    public AttachmentFile getAttachmentById(Long attachmentId) {
        return attachmentFileRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("附件不存在"));
    }

    /**
     * 获取文件内容类型用于下载
     */
    public String getContentType(AttachmentFile attachment) {
        String contentType = attachment.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            // 根据文件扩展名推断内容类型
            String extension = attachment.getFileExtension().toLowerCase();
            switch (extension) {
                case "pdf":
                    return "application/pdf";
                case "doc":
                    return "application/msword";
                case "docx":
                    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls":
                    return "application/vnd.ms-excel";
                case "xlsx":
                    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                default:
                    return "application/octet-stream";
            }
        }
        return contentType;
    }


} 