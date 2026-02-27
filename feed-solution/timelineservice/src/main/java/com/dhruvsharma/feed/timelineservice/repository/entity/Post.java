package com.dhruvsharma.feed.timelineservice.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("posts")
public class Post {
    @Id
    private Long id;
    private String content;
    /** Internal user id (users.id). Resolve display name via GET /api/v1/users/{id}. */
    private Long authorId;
}
