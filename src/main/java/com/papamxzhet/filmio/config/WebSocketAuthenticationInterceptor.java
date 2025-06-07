package com.papamxzhet.filmio.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Principal;
import java.util.ArrayList;

@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String token = authToken.substring(7);

                try {
                    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    String username = claims.getSubject();

                    if (username != null) {
                        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
                                username, null, new ArrayList<>());
                        accessor.setUser(principal);

                        SecurityContextHolder.getContext().setAuthentication(principal);

                        System.out.println("WebSocket аутентификация успешна для пользователя: " + username);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при проверке JWT токена в WebSocket: " + e.getMessage());
                }
            } else {
                System.err.println("Authorization заголовок отсутствует или неверный формат");
            }
        }

        return message;
    }
}