
package com.trustvault;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentPermissionRepository
        extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocument_IdAndUser_Id(
            Long documentId,
            Long userId);

    boolean existsByDocument_IdAndUser_Id(
            Long documentId,
            Long userId);

    void deleteByDocument_IdAndUser_Id(
            Long documentId,
            Long userId);

    void deleteByDocument_Id(Long documentId);
    List<DocumentPermission> findAllByDocument_Id(Long documentId);
    List<DocumentPermission> findAllByDocument_Owner_Id(Long ownerId);
}
