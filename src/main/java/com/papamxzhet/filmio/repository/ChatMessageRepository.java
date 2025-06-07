package com.papamxzhet.filmio.repository;

import com.papamxzhet.filmio.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT DISTINCT m FROM ChatMessage m LEFT JOIN FETCH m.reactions WHERE m.roomId = :roomId")
    Page<ChatMessage> findByRoomIdWithReactions(@Param("roomId") String roomId, Pageable pageable);
}
