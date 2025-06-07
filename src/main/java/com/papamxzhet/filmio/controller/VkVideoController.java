package com.papamxzhet.filmio.controller;
import com.papamxzhet.filmio.payload.VkVideoData;
import com.papamxzhet.filmio.payload.VkVideoRequest;
import com.papamxzhet.filmio.payload.VkVideoResponse;
import com.papamxzhet.filmio.service.VkVideoService;
import com.papamxzhet.filmio.service.VkVideoService.VkVideoIdentifier;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

/**
 * Контроллер для работы с видео из ВКонтакте.
 * Была попытка реализации работы с VK video API для обеспечения совместного просмотра видео
 * из ВКонтакте между пользователями в общих комнатах. К сожалению, не получилось полностью
 * реализовать функционал воспроизведения видео и его синхронизации между участниками комнаты
 * из-за ограничений VK API и политик безопасности браузеров.
 * Контроллер предоставляет базовую функциональность для получения метаданных видео
 * из ВКонтакте, но полноценный просмотр остается нереализованным.
 */
@RestController
@RequestMapping("/api/room")
public class VkVideoController {
    private static final Logger logger = LoggerFactory.getLogger(VkVideoController.class);
    private final VkVideoService vkVideoService;

    public VkVideoController(VkVideoService vkVideoService) {
        this.vkVideoService = vkVideoService;
    }

    @PostMapping("/video-info/vk")
    public ResponseEntity<VkVideoResponse> getVkVideoInfo(@Valid @RequestBody VkVideoRequest request) {
        logger.debug("Received VK video info request for URL: {}", request.getVideoUrl());

        if (request.getVideoUrl() == null || request.getVideoUrl().isEmpty()) {
            return ResponseEntity.badRequest().body(new VkVideoResponse("Video URL is required"));
        }

        if (request.getAccessToken() == null || request.getAccessToken().isEmpty()) {
            return ResponseEntity.badRequest().body(new VkVideoResponse("Access token is required"));
        }

        Optional<VkVideoIdentifier> videoIdOpt = vkVideoService.parseVkVideoUrl(request.getVideoUrl());
        if (videoIdOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new VkVideoResponse("Invalid VK video URL. Expected formats include: " +
                            "https://vk.com/video{owner_id}_{video_id}, " +
                            "https://vk.com/video?z=video{owner_id}_{video_id}, " +
                            "or simply {owner_id}_{video_id}"));
        }

        VkVideoIdentifier videoId = videoIdOpt.get();
        logger.debug("Successfully parsed video identifier: owner_id={}, video_id={}",
                videoId.ownerId(), videoId.videoId());

        Optional<VkVideoData> videoDataOpt = vkVideoService.getVkVideoInfo(
                videoId.ownerId(),
                videoId.videoId(),
                request.getAccessToken()
        );

        if (videoDataOpt.isEmpty()) {
            return ResponseEntity.ok(new VkVideoResponse("Failed to retrieve video information from VK API. " +
                    "The video might be private or your access token might not have sufficient permissions."));
        }

        VkVideoData videoData = videoDataOpt.get();
        logger.debug("Retrieved video data: title='{}', duration={}, directUrl='{}'",
                videoData.getTitle(), videoData.getDuration(),
                videoData.getDirectUrl() != null ? "[URL available]" : "[No URL]");

        if (videoData.getDirectUrl() == null || videoData.getDirectUrl().isEmpty()) {
            return ResponseEntity.ok(new VkVideoResponse("Could not retrieve direct URL for this video. " +
                    "It might be protected or require specific permissions."));
        }

        return ResponseEntity.ok(new VkVideoResponse(videoData));
    }
}