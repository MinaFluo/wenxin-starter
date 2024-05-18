package com.gearwenxin.core;

import com.gearwenxin.entity.Message;
import com.gearwenxin.entity.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gearwenxin.common.Constant.MAX_TOTAL_LENGTH;
import static com.gearwenxin.common.WenXinUtils.assertNotBlank;
import static com.gearwenxin.common.WenXinUtils.assertNotNull;

public class MessageHistoryManager {

    private static final Logger log = LoggerFactory.getLogger(MessageHistoryManager.class);

    private MessageHistoryManager() {
    }

    private static final Integer DEFAULT_MESSAGE_MAP_KRY_SIZE = 1024;

    private volatile int CURRENT_MAP = 0;

    /**
     * 历史消息记录, 扩容时会来回切换
     */
    private static final Map<String, Deque<Message>> chatMessageHistoryMap0;


    private static volatile MessageHistoryManager messageHistoryManager;

    static {
        chatMessageHistoryMap0 = new ConcurrentHashMap<>(DEFAULT_MESSAGE_MAP_KRY_SIZE);
    }

    public static MessageHistoryManager getInstance() {
        if (messageHistoryManager == null) {
            synchronized (MessageHistoryManager.class) {
                if (messageHistoryManager == null) {
                    messageHistoryManager = new MessageHistoryManager();
                }
            }
        }
        return messageHistoryManager;
    }

    public Map<String, Deque<Message>> getChatMessageHistoryMap() {
        return chatMessageHistoryMap0;
    }

    public synchronized void setChatMessageHistoryMap(Map<String, Deque<Message>> map) {
        if (CURRENT_MAP == 0) {
            chatMessageHistoryMap0.clear();
            chatMessageHistoryMap0.putAll(map);
        }
    }

    public Deque<Message> getMessageHistory(String msgUid) {
        return getChatMessageHistoryMap().get(msgUid);
    }

    /**
     * 向历史消息中添加消息
     *
     * @param originalHistory 历史消息队列
     * @param message         需添加的Message
     */
    public static void addMessage(Deque<Message> originalHistory, Message message) {
        assertNotNull(originalHistory, "messagesHistory is null");
        assertNotNull(message, "message is null");
        assertNotBlank(message.getContent(), "message.content is null or blank");

        // 复制原始历史记录，避免直接修改原始历史记录
        Deque<Message> updatedHistory = new LinkedList<>(originalHistory);

        // 验证消息规则
        validateMessageRule(updatedHistory, message);

        // 将新消息添加到历史记录中
        updatedHistory.offer(message);

        if (message.getRole() == Role.assistant) {
            syncHistories(originalHistory, updatedHistory);
            return;
        }

        // 处理超出长度的情况
        handleExceedingLength(updatedHistory);

        // 同步历史记录
        syncHistories(originalHistory, updatedHistory);
    }

    public static void validateMessageRule(Deque<Message> history, Message message) {
        if (!history.isEmpty()) {
            Message lastMessage = history.peekLast();
            if (lastMessage != null) {
                // 如果当前是奇数位message，要求role值为user或function
                if (history.size() % 2 != 0) {
                    if (message.getRole() != Role.user && message.getRole() != Role.function) {
                        // 删除最后一条消息
                        Message polledMessage = history.pollLast();
                        log.debug("remove message: {}. Odd Position role is not user or function", polledMessage);
                        validateMessageRule(history, message);
                    }
                } else {
                    // 如果当前是偶数位message，要求role值为assistant
                    if (message.getRole() != Role.assistant) {
                        // 删除最后一条消息
                        Message polledMessage = history.pollLast();
                        log.debug("remove message: {}. Even position role is not assistant", polledMessage);
                        validateMessageRule(history, message);
                    }
                }
                // 第一个message的role不能是function
                if (history.size() == 1 && message.getRole() == Role.function) {
                    // 删除最后一条消息
                    Message polledMessage = history.pollLast();
                    log.debug("remove message: {}. first role is function", polledMessage);
                    validateMessageRule(history, message);
                }

                // 移除连续的相同role的user messages
                if (lastMessage.getRole() == Role.user && message.getRole() == Role.user) {
                    Message polledMessage = history.pollLast();
                    log.debug("remove message: {}. Same role message", polledMessage);
                    validateMessageRule(history, message);
                }
            }
        }
    }

    public static void validateMessageRule(Deque<Message> history) {
        if (history != null && !history.isEmpty()) {
            Message message = history.pollLast();
            validateMessageRule(history, message);
        }
    }

    private static void syncHistories(Deque<Message> original, Deque<Message> updated) {
//        if (updated.size() <= original.size()) {
        original.clear();
        original.addAll(updated);
//        }
    }

    private static void handleExceedingLength(Deque<Message> updatedHistory) {
        int totalLength = updatedHistory.stream()
                .filter(msg -> msg.getRole() == Role.user)
                .mapToInt(msg -> msg.getContent().length())
                .sum();

        while (totalLength > MAX_TOTAL_LENGTH && updatedHistory.size() > 2) {
            Message firstMessage = updatedHistory.poll();
            Message secondMessage = updatedHistory.poll();
            if (firstMessage != null && secondMessage != null) {
                totalLength -= (firstMessage.getContent().length() + secondMessage.getContent().length());
            } else if (secondMessage != null) {
                updatedHistory.addFirst(secondMessage);
                totalLength -= secondMessage.getContent().length();
            }
        }
    }

}
