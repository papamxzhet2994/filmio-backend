package com.papamxzhet.filmio.repository;

import com.papamxzhet.filmio.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByTimestamp(String roomId);

    Page<ChatMessage> findByRoomId(String roomId, Pageable pageable);
}
