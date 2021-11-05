package org.telegram.repostcleanerbot.tdlib.entity;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Chat {
    private long id;
    private String title;
    private boolean canSendMessage;
}
