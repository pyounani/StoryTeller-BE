package com.cojac.storyteller.profile.entity;

import com.cojac.storyteller.book.entity.BookEntity;
import com.cojac.storyteller.profile.dto.ProfileDTO;
import com.cojac.storyteller.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private LocalDate birthDate;

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = false)
    private String pinNumber;

    @Column(nullable = false)
    private Integer credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 책 목록 추가
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<BookEntity> books = new ArrayList<>();

    public static final int DEFAULT_FREE_CREDIT = 5;

    @Builder
    public ProfileEntity(Integer id, String name, LocalDate birthDate, String imageUrl, String pinNumber, Integer credit, UserEntity user) {
        this.id = id; // ID 필드 추가
        this.name = name;
        this.birthDate = birthDate;
        this.imageUrl = imageUrl;
        this.pinNumber = pinNumber;
        this.credit = credit;
        this.user = user;
    }

    public void addBook(BookEntity book) {
        books.add(book);
        book.updateProfile(this);
    }

    public void updateProfile(ProfileDTO profileDTO) {
        this.name = profileDTO.getName();
        this.birthDate = profileDTO.getBirthDate();
        this.imageUrl = profileDTO.getImageUrl();
        this.pinNumber = profileDTO.getPinNumber();
    }

    public void deductCredit() {
        this.credit -= 1;
    }

    public void chargeCredit(int amount) {
        this.credit += amount;
    }
}