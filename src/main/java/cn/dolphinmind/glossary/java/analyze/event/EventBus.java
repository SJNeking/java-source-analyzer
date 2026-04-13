package cn.dolphinmind.glossary.java.analyze.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 轻量事件总线
 * 
 * 用于发布/订阅分析事件。
 * 线程安全，基于内存实现，适合单体架构。
 */
public class EventBus {

    private static final Logger logger = Logger.getLogger(EventBus.class.getName());
    
    private static final EventBus instance = new EventBus();
    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    public interface EventListener {
        void onEvent(AnalysisEvent event);
    }

    public static EventBus getInstance() {
        return instance;
    }

    private EventBus() {}

    /**
     * 注册监听器
     */
    public void register(EventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.info("EventListener registered: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * 注销监听器
     */
    public void unregister(EventListener listener) {
        listeners.remove(listener);
    }

    /**
     * 发布事件
     */
    public void publish(AnalysisEvent event) {
        logger.info("Publishing event: " + event);
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                logger.warning("EventListener failed: " + e.getMessage());
            }
        }
    }
}
