package com.papamxzhet.filmio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papamxzhet.filmio.payload.VkVideoData;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VkVideoService {
    private static final Logger logger = LoggerFactory.getLogger(VkVideoService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vk.api.version:5.131}")
    private String vkApiVersion;

    @Value("${vk.api.base-url:https://api.vk.com/method}")
    private String vkApiBaseUrl;

    public VkVideoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<VkVideoIdentifier> parseVkVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }

        logger.debug("Attempting to parse VK video URL: {}", url);

        Pattern plainIdPattern = Pattern.compile("(?:video)?(-?\\d+)_(\\d+)");
        Matcher plainIdMatcher = plainIdPattern.matcher(url);

        if (plainIdMatcher.find()) {
            String ownerId = plainIdMatcher.group(1);
            String videoId = plainIdMatcher.group(2);
            logger.debug("Matched plain ID format: owner_id={}, video_id={}", ownerId, videoId);
            return Optional.of(new VkVideoIdentifier(ownerId, videoId));
        }

        Pattern directPattern = Pattern.compile("(?:https?://)?(?:www\\.|m\\.)?vk\\.com/(?:video|clips)(-?\\d+)_(\\d+)");
        Matcher directMatcher = directPattern.matcher(url);

        if (directMatcher.find()) {
            String ownerId = directMatcher.group(1);
            String videoId = directMatcher.group(2);
            logger.debug("Matched direct URL format: owner_id={}, video_id={}", ownerId, videoId);
            return Optional.of(new VkVideoIdentifier(ownerId, videoId));
        }

        Pattern paramPattern = Pattern.compile("(?:video|clips)\\?.*?z=(?:video|clips)(-?\\d+)_(\\d+)");
        Matcher paramMatcher = paramPattern.matcher(url);

        if (paramMatcher.find()) {
            String ownerId = paramMatcher.group(1);
            String videoId = paramMatcher.group(2);
            logger.debug("Matched parameter URL format: owner_id={}, video_id={}", ownerId, videoId);
            return Optional.of(new VkVideoIdentifier(ownerId, videoId));
        }

        Pattern extPattern = Pattern.compile("video_ext\\.php\\?.*?oid=(-?\\d+).*?id=(\\d+)");
        Matcher extMatcher = extPattern.matcher(url);

        if (extMatcher.find()) {
            String ownerId = extMatcher.group(1);
            String videoId = extMatcher.group(2);
            logger.debug("Matched video_ext.php format: owner_id={}, video_id={}", ownerId, videoId);
            return Optional.of(new VkVideoIdentifier(ownerId, videoId));
        }

        logger.warn("Could not parse VK video URL: {}", url);
        return Optional.empty();
    }


    public Optional<VkVideoData> getVkVideoInfo(String ownerId, String videoId, String accessToken) {
        try {
            String videos = ownerId + "_" + videoId;
            logger.debug("Requesting video info from VK API for video: {}", videos);

            String url = UriComponentsBuilder.fromHttpUrl(vkApiBaseUrl + "/video.get")
                    .queryParam("access_token", accessToken)
                    .queryParam("videos", videos)
                    .queryParam("extended", 1)
                    .queryParam("v", vkApiVersion)
                    .build()
                    .toUriString();

            logger.debug("Making request to VK API: {}...(token hidden)",
                    url.replaceAll("access_token=[^&]+", "access_token=****"));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (root.has("error")) {
                    JsonNode error = root.get("error");
                    logger.error("VK API error: code={}, message='{}'",
                            error.path("error_code").asText("unknown"),
                            error.path("error_msg").asText("unknown error"));
                    return Optional.empty();
                }

                if (root.has("response") && root.get("response").has("items") && !root.get("response").get("items").isEmpty()) {
                    JsonNode videoNode = root.get("response").get("items").get(0);

                    VkVideoData videoData = new VkVideoData();
                    videoData.setTitle(videoNode.path("title").asText(""));
                    videoData.setDescription(videoNode.path("description").asText(""));

                    String thumbnailUrl = "";
                    if (videoNode.has("image")) {
                        JsonNode images = videoNode.get("image");
                        if (images.isArray() && !images.isEmpty()) {
                            thumbnailUrl = images.get(images.size() - 1).path("url").asText("");
                        }
                    }
                    videoData.setThumbnailUrl(thumbnailUrl);

                    if (videoNode.has("files")) {
                        JsonNode files = videoNode.get("files");
                        String directUrl = null;

                        if (files.has("mp4_1080")) {
                            directUrl = files.get("mp4_1080").asText();
                        } else if (files.has("mp4_720")) {
                            directUrl = files.get("mp4_720").asText();
                        } else if (files.has("mp4_480")) {
                            directUrl = files.get("mp4_480").asText();
                        } else if (files.has("mp4_360")) {
                            directUrl = files.get("mp4_360").asText();
                        } else if (files.has("mp4_240")) {
                            directUrl = files.get("mp4_240").asText();
                        } else if (files.has("external")) {
                            directUrl = files.get("external").asText();
                        } else if (files.has("hls")) {
                            directUrl = files.get("hls").asText();
                        } else if (files.has("dash")) {
                            directUrl = files.get("dash").asText();
                        }

                        if (directUrl != null) {
                            logger.debug("Found direct URL with format: {}",
                                    directUrl.matches(".*mp4_\\d+.*") ? "mp4_" + directUrl.replaceAll(".*mp4_(\\d+).*", "$1") :
                                            directUrl.contains("hls") ? "HLS" :
                                                    directUrl.contains("dash") ? "DASH" :
                                                            directUrl.contains("external") ? "external" : "unknown");
                        } else {
                            logger.warn("No playable video URL found in response");
                        }

                        videoData.setDirectUrl(directUrl);
                    }

                    videoData.setDuration(videoNode.path("duration").asInt(0));

                    String ownerName = "";
                    if (videoNode.has("owner_id")) {
                        int ownerId_ = videoNode.get("owner_id").asInt();

                        if (ownerId_ > 0 && root.get("response").has("profiles")) {
                            JsonNode profiles = root.get("response").get("profiles");

                            for (JsonNode profile : profiles) {
                                if (profile.get("id").asInt() == ownerId_) {
                                    ownerName = profile.path("first_name").asText("") + " " + profile.path("last_name").asText("");
                                    break;
                                }
                            }
                        }
                        else if (ownerId_ < 0 && root.get("response").has("groups")) {
                            int groupId = Math.abs(ownerId_);
                            JsonNode groups = root.get("response").get("groups");

                            for (JsonNode group : groups) {
                                if (group.get("id").asInt() == groupId) {
                                    ownerName = group.path("name").asText("");
                                    break;
                                }
                            }
                        }
                    }

                    videoData.setOwnerName(ownerName);
                    logger.debug("Successfully parsed video data: title='{}', duration={}s",
                            videoData.getTitle(), videoData.getDuration());

                    return Optional.of(videoData);
                } else {
                    logger.warn("No video items found in VK API response");
                }
            } else {
                logger.error("Unexpected HTTP status from VK API: {}", response.getStatusCode());
            }

            return Optional.empty();
        } catch (HttpClientErrorException e) {
            logger.error("Error calling VK API: status={}, response={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error while getting VK video info", e);
            return Optional.empty();
        }
    }

    public record VkVideoIdentifier(String ownerId, String videoId) {

        public VkVideoIdentifier(long ownerId, long videoId) {
            this(String.valueOf(ownerId), String.valueOf(videoId));
        }
    }
}