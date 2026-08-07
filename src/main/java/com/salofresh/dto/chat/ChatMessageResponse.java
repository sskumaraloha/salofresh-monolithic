package com.salofresh.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String message;
    private Instant sentAt;
    private Instant readAt;

    /**
     * Whether the requesting user is the sender of this message. Computed relative to the
     * requester in the service layer since MapStruct alone has no access to request context.
     */
    private boolean mine;
}
