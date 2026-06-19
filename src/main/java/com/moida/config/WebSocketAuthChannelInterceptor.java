package com.moida.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        if ((StompCommand.CONNECT.equals(command)
                || StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.SEND.equals(command))
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Authenticated WebSocket ticket is required.");
        }
        return message;
    }
}
