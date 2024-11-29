package com.papamxzhet.filmio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;
    private String username;

    @Lob
    private String encryptedContent;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "parent_message_id")
    private ChatMessage parentMessage;
}
