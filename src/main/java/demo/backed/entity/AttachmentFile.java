package demo.backed.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 附件文件实体
 * 用于存储上传的文件信息
 */
@Entity
@Table(name = "t_poc_attachment_files")
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "附件文件")
public class AttachmentFile extends BaseEntity {

    @Column(name = "business_type", length = 50, nullable = false)
    @ApiModelProperty(value = "业务类型", example = "EXPENSE_APPLICATION")
    private String businessType;

    @Column(name = "business_id", nullable = false)
    @ApiModelProperty(value = "业务ID（如费用申请ID）", example = "1")
    private Long businessId;

    @Column(name = "attachment_type", length = 50)
    @ApiModelProperty(value = "附件类型", example = "BASIC_INFO, SUPERVISOR_REVIEW")
    private String attachmentType;

    @Column(name = "original_filename", length = 255, nullable = false)
    @ApiModelProperty(value = "原始文件名", example = "发票.pdf")
    private String originalFilename;

    @Column(name = "stored_filename", length = 255, nullable = false)
    @ApiModelProperty(value = "存储文件名", example = "20240701_123456_invoice.pdf")
    private String storedFilename;

    @Column(name = "file_path", length = 500, nullable = false)
    @ApiModelProperty(value = "文件存储路径", example = "/uploads/expense/2024/07/01/20240701_123456_invoice.pdf")
    private String filePath;

    @Column(name = "file_size", nullable = false)
    @ApiModelProperty(value = "文件大小（字节）", example = "1024000")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    @ApiModelProperty(value = "文件MIME类型", example = "application/pdf")
    private String contentType;

    @Column(name = "file_extension", length = 20)
    @ApiModelProperty(value = "文件扩展名", example = "pdf")
    private String fileExtension;

    @Column(name = "description", length = 500)
    @ApiModelProperty(value = "文件描述", example = "相关发票凭证")
    private String description;

    @Column(name = "upload_user_id", nullable = false)
    @ApiModelProperty(value = "上传用户ID", example = "1")
    private Long uploadUserId;

    @Column(name = "upload_user_name", length = 50)
    @ApiModelProperty(value = "上传用户名", example = "张三")
    private String uploadUserName;

    @Column(name = "upload_time", nullable = false)
    @ApiModelProperty(value = "上传时间")
    private LocalDateTime uploadTime;

    @Column(name = "download_count")
    @ApiModelProperty(value = "下载次数", example = "5")
    private Integer downloadCount = 0;

    @Column(name = "last_download_time")
    @ApiModelProperty(value = "最后下载时间")
    private LocalDateTime lastDownloadTime;

    @Column(name = "is_active")
    @ApiModelProperty(value = "是否有效", example = "true")
    private Boolean isActive = true;

    /**
     * 业务类型枚举
     */
    public enum BusinessType {
        EXPENSE_APPLICATION("EXPENSE_APPLICATION", "费用申请"),
        TRAVEL_APPLICATION("TRAVEL_APPLICATION", "差旅申请"),
        OTHER("OTHER", "其他");

        private final String code;
        private final String description;

        BusinessType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 附件类型枚举
     */
    public enum AttachmentType {
        BASIC_INFO("BASIC_INFO", "基本信息附件"),
        SUPERVISOR_REVIEW("SUPERVISOR_REVIEW", "主管审批附件"),
        EXPENSE_RECEIPT("EXPENSE_RECEIPT", "费用凭证"),
        OTHER("OTHER", "其他附件");

        private final String code;
        private final String description;

        AttachmentType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取文件大小的人类可读格式
     */
    public String getHumanReadableFileSize() {
        if (fileSize == null || fileSize <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        
        return String.format("%.1f %s",
                fileSize / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * 检查是否为图片文件
     */
    public boolean isImage() {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }

    /**
     * 检查是否为PDF文件
     */
    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    /**
     * 检查是否为文档文件
     */
    public boolean isDocument() {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("application/") &&
                (contentType.contains("word") || contentType.contains("excel") || 
                 contentType.contains("powerpoint") || contentType.contains("pdf"));
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (uploadTime == null) {
            uploadTime = getCreatedTime();
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
} 