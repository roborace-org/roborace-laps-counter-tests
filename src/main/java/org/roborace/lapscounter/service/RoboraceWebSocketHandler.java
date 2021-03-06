package org.roborace.lapscounter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.MessageResult;
import org.roborace.lapscounter.domain.ResponseType;
import org.roborace.lapscounter.domain.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Component
public class RoboraceWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private LapsCounterService lapsCounterService;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        try {
            String payload = textMessage.getPayload();
            log.info("handleTextMessage {} {}", session.getRemoteAddress(), payload);
            Message message = JSON.readValue(payload, Message.class);

            MessageResult messageResult = lapsCounterService.handleMessage(message);
            if (messageResult == null) {
                return;
            }

            if (messageResult.getResponseType() == ResponseType.BROADCAST) {
                broadcast(messageResult.getMessages());
            } else if (messageResult.getResponseType() == ResponseType.SINGLE) {
                singleSession(messageResult.getMessages(), session);
            }
        } catch (Exception e) {
            log.error("Exception happen during message handling: {}", e.getMessage(), e);
            if (session.isOpen()) {
                Message message = Message.builder().type(Type.ERROR).message(e.getMessage()).build();
                sendObjectAsJson(message, session);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.info("ConnectionEstablished {}", session.getRemoteAddress());
        sessions.add(session);

        singleSession(lapsCounterService.afterConnectionEstablished(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("ConnectionClosed {}", session.getRemoteAddress());
        sessions.remove(session);
    }

    public List<WebSocketSession> getSessions() {
        return sessions;
    }

    private void singleSession(List<Message> messages, WebSocketSession session) {
        for (Message message : messages) {
            sendObjectAsJson(message, session);
        }
    }

    public void broadcast(List<Message> messages) {
        for (Message message : messages) {
            broadcast(message);
        }
    }

    public void broadcast(Message message) {
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendObjectAsJson(message, session));
    }

    private void sendObjectAsJson(Object message, WebSocketSession session) {
        try {
            sendObjectAsJson(new TextMessage(JSON.writeValueAsString(message)), session);
        } catch (JsonProcessingException e) {
            log.error("Error creating json message for object: {}. Reason: {}", message, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private synchronized void sendObjectAsJson(TextMessage textMessage, WebSocketSession session) {
        try {
            session.sendMessage(textMessage);
        } catch (IOException e) {
            log.error("Error while sending messages to ws client. Reason: {}", e.getMessage(), e);
        }
    }
}
