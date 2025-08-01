package demo.backed.repository;

import demo.backed.entity.AttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 附件文件Repository
 */
@Repository
public interface AttachmentFileRepository extends JpaRepository<AttachmentFile, Long> {

    /**
     * 根据业务类型和业务ID查询附件列表
     */
    List<AttachmentFile> findByBusinessTypeAndBusinessIdAndIsActiveTrue(String businessType, Long businessId);

    /**
     * 根据业务类型、业务ID和附件类型查询附件列表
     */
    List<AttachmentFile> findByBusinessTypeAndBusinessIdAndAttachmentTypeAndIsActiveTrue(
            String businessType, Long businessId, String attachmentType);

    /**
     * 根据存储文件名查询附件
     */
    Optional<AttachmentFile> findByStoredFilenameAndIsActiveTrue(String storedFilename);

    /**
     * 根据文件路径查询附件
     */
    Optional<AttachmentFile> findByFilePathAndIsActiveTrue(String filePath);

    /**
     * 根据上传用户ID查询附件列表
     */
    List<AttachmentFile> findByUploadUserIdAndIsActiveTrue(Long uploadUserId);

    /**
     * 统计指定业务的附件数量
     */
    @Query("SELECT COUNT(a) FROM AttachmentFile a WHERE a.businessType = :businessType AND a.businessId = :businessId AND a.isActive = true")
    Long countByBusinessTypeAndBusinessId(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 统计指定业务和附件类型的附件数量
     */
    @Query("SELECT COUNT(a) FROM AttachmentFile a WHERE a.businessType = :businessType AND a.businessId = :businessId AND a.attachmentType = :attachmentType AND a.isActive = true")
    Long countByBusinessTypeAndBusinessIdAndAttachmentType(
            @Param("businessType") String businessType, 
            @Param("businessId") Long businessId, 
            @Param("attachmentType") String attachmentType);

    /**
     * 计算指定业务的附件总大小
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM AttachmentFile a WHERE a.businessType = :businessType AND a.businessId = :businessId AND a.isActive = true")
    Long sumFileSizeByBusinessTypeAndBusinessId(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 软删除指定业务的所有附件
     */
    @Query("UPDATE AttachmentFile a SET a.isActive = false WHERE a.businessType = :businessType AND a.businessId = :businessId")
    int softDeleteByBusinessTypeAndBusinessId(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 根据文件扩展名统计附件数量
     */
    @Query("SELECT a.fileExtension, COUNT(a) FROM AttachmentFile a WHERE a.isActive = true GROUP BY a.fileExtension")
    List<Object[]> countByFileExtension();

    /**
     * 查询最近上传的附件（限制数量）
     */
    @Query("SELECT a FROM AttachmentFile a WHERE a.isActive = true ORDER BY a.uploadTime DESC")
    List<AttachmentFile> findRecentUploads(org.springframework.data.domain.Pageable pageable);
} 