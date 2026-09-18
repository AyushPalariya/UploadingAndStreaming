package com.example.VideoManage.Entities;

import jakarta.persistence.*;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video")
@Data
public class VideoUploadEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String uploadId;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String filePath;
        private Integer totalChunks;
        @Enumerated(EnumType.STRING)
        private VideoStatus status;
        @Column(name="created_at",nullable = false)
        private LocalDateTime create;
}

