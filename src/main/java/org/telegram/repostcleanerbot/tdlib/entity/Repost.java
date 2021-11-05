package org.telegram.repostcleanerbot.tdlib.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repost {
    private long id;
    private long repostedBy;
    private Chat repostedFrom;
    private Chat repostedIn;
    private Date repostedAt;
}
