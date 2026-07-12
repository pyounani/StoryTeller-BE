package com.cojac.storyteller.book.entity;

import com.cojac.storyteller.page.entity.PageEntity;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.setting.entity.SettingEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String coverImage;

    @Column(nullable = false)
    private Integer currentPage;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PageEntity> pages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private ProfileEntity profile;

    @Column(nullable = false)
    private boolean isReading;

    @Column(nullable = false)
    private boolean isFavorite;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SettingEntity setting;

    @CreatedDate
    @JoinColumn(name = "created_at")
    private LocalDateTime createdAt; // 생성일

    public int getTotalPageCount() {
        return pages.size();
    }

    public void updateProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    public void updateIsReading(boolean isReading) {
        this.isReading = isReading;
    }

    public void updateIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void updateCurrentPage(Integer currentPage) {
        if (currentPage < 0 || currentPage > getTotalPageCount()) {
            throw new IllegalArgumentException("Invalid page number");
        }
        this.currentPage = currentPage;
    }
}
