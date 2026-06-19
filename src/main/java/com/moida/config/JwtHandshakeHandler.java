package com.moida.config;

import com.moida.domain.auth.WebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private static final String TICKET_PARAM = "ticket=";

    private final WebSocketTicketService ticketService;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        return ticketService.consume(extractTicket(request));
    }

    private String extractTicket(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (!StringUtils.hasText(query)) return null;
        for (String param : query.split("&")) {
            if (param.startsWith(TICKET_PARAM)) {
                return param.substring(TICKET_PARAM.length());
            }
        }
        return null;
    }
}
