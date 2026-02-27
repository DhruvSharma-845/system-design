package com.dhruvsharma.feed.postservice.repository.entity;

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
    /** Internal user id (users.id). Resolved via userservice API at create time. */
    private Long authorId;
}
