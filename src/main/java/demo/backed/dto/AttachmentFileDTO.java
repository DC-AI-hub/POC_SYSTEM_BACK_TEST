package demo.backed.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件文件DTO
 */
@Data
@ApiModel(description = "附件文件信息")
public class AttachmentFileDTO {

    @ApiModelProperty(value = "附件ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "业务类型", example = "EXPENSE_APPLICATION")
    private String businessType;

    @ApiModelProperty(value = "业务ID", example = "1")
    private Long businessId;

    @ApiModelProperty(value = "附件类型", example = "BASIC_INFO")
    private String attachmentType;

    @ApiModelProperty(value = "原始文件名", example = "发票.pdf")
    private String originalFilename;

    @ApiModelProperty(value = "存储文件名", example = "20240701_123456_invoice.pdf")
    private String storedFilename;

    @ApiModelProperty(value = "文件大小（字节）", example = "1024000")
    private Long fileSize;

    @ApiModelProperty(value = "文件大小（人类可读）", example = "1.0 MB")
    private String humanReadableFileSize;

    @ApiModelProperty(value = "文件MIME类型", example = "application/pdf")
    private String contentType;

    @ApiModelProperty(value = "文件扩展名", example = "pdf")
    private String fileExtension;

    @ApiModelProperty(value = "文件描述", example = "相关发票凭证")
    private String description;

    @ApiModelProperty(value = "上传用户ID", example = "1")
    private Long uploadUserId;

    @ApiModelProperty(value = "上传用户名", example = "张三")
    private String uploadUserName;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime uploadTime;

    @ApiModelProperty(value = "下载次数", example = "5")
    private Integer downloadCount;

    @ApiModelProperty(value = "最后下载时间")
    private LocalDateTime lastDownloadTime;

    @ApiModelProperty(value = "是否为图片文件", example = "false")
    private Boolean isImage;

    @ApiModelProperty(value = "是否为PDF文件", example = "true")
    private Boolean isPdf;

    @ApiModelProperty(value = "是否为文档文件", example = "true")
    private Boolean isDocument;

    @ApiModelProperty(value = "是否有效", example = "true")
    private Boolean isActive;

    @ApiModelProperty(value = "下载URL", example = "/api/attachments/1/download")
    private String downloadUrl;

    @ApiModelProperty(value = "预览URL", example = "/api/attachments/1/preview")
    private String previewUrl;
} 