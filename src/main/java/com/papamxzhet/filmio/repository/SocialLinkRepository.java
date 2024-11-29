package com.papamxzhet.filmio.repository;

import com.papamxzhet.filmio.model.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    void deleteByIdAndUserId(Long id, Long userId);

    List<SocialLink> findByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
