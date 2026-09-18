package com.example.VideoManage.Repository;

import com.example.VideoManage.Entities.VideoUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoUploadRepository extends JpaRepository<VideoUploadEntity,Long> {
    Optional<VideoUploadEntity> findByUploadId(String uploadId);

}
