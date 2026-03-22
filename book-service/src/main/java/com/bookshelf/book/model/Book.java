package com.bookshelf.book.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "books")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_type", nullable = false, length = 10)
    private BookType bookType;

    @Column(name = "eshop_url", length = 500)
    private String eshopUrl;

    @Column(name = "private_file_key", length = 500)
    private String privateFileKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
