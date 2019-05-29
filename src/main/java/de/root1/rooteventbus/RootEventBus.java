/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of RootEventBus (REB).
 *
 *   REB is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   REB is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with REB.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.rooteventbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Event Bus implementation, inspired by greenrobot (http://greenrobot.de)
 */
public class RootEventBus {

    private final static Logger log = LoggerFactory.getLogger(RootEventBus.class);

    private final Map<Class, List<Listener>> listeners = new HashMap<>();
    private final Map<Class, List<Listener>> backgroundThreadListeners = new HashMap<>();
    private final Map<Class, List<Listener>> asyncBackgroundThreadListeners = new HashMap<>();
    private final Map<Class<?>, Object> stickyEvents = new HashMap<>();

    private final ExecutorService backgroundThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {

        AtomicInteger i = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "EventBus#BackgroundThreadPool(" + i.getAndIncrement() + ")");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final RootEventBus defaultInstance = new RootEventBus();

    /**
     * Returns default singleton instance
     */
    public static RootEventBus getDefault() {
        return defaultInstance;
    }

    /**
     * Post an event
     * @param event event object
     * @param sticky sticky-flag
     * @see RootEventBus#getStickyEvent(java.lang.Class) 
     */
    private void post(Object event, boolean sticky) {

        Class<? extends Object> eventType = event.getClass();
        boolean subscriberFound = false;

        if (sticky) {
            synchronized (stickyEvents) {
                stickyEvents.put(eventType, event);
            }
        }
        List<Listener> listener = listeners.get(eventType);
        if (listener != null) {
            subscriberFound = listener.size() > 0;
            // crazy shit from Java8 ;-)
            listener.stream().forEach((l) -> {
                l.post(event);
            });
        }

        List<Listener> backgroundListener = backgroundThreadListeners.get(eventType);
        if (backgroundListener != null) {
            subscriberFound = backgroundListener.size() > 0;
            backgroundThreadPool.execute(() -> {
                backgroundListener.stream().forEach((l) -> {
                    l.post(event);
                });
            });
        }

        List<Listener> asyncListener = asyncBackgroundThreadListeners.get(eventType);
        if (asyncListener != null) {
            subscriberFound = asyncListener.size() > 0;
            asyncListener.stream().forEach((l) -> {
                backgroundThreadPool.execute(() -> {
                    l.post(event);
                });
            });
        }

        if (!subscriberFound && event.getClass() != NoSubscriberEvent.class) {
            post(new NoSubscriberEvent(this, event));
        }
    }

    /**
     * Post an event
     * @param event event object
     */
    public void post(Object event) {
        post(event, false);
    }

    /**
     * Post an event as an sticky event
     * @param event event object
     * @see RootEventBus#getStickyEvent(java.lang.Class) 
     */
    public void postSticky(Object event) {
        post(event, true);
    }

    /**
     * Unregister a subscriber
     * @param subscriber subscriber to unregister
     */
    public void unregister(Object subscriber) {
        List<SubscriberMethod> subscriberMethods = SubscriberMethod.getSubscriberMethods(subscriber);
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            Class eventType = subscriberMethod.getEventType();
            Method method = subscriberMethod.getMethod();
            String methodName = subscriberMethod.getMethod().getName();

            /*
             * Check if method is a "onEvent..." method and set the target listener map
             */
            Map<Class, List<Listener>> targetMap = null;
            switch (methodName) {
                case "onEvent":
                    log.debug("Found 'onEvent' for {} in {}", eventType, subscriber.getClass());
                    targetMap = listeners;
                    break;
                case "onEventBackgroundThread":
                    log.debug("Found 'onEventBackgroundThread' for {} in {}", eventType, subscriber.getClass());
                    targetMap = backgroundThreadListeners;
                    break;
                case "onEventAsync":
                    log.debug("Found 'onEventAsync' for {} in {}", eventType, subscriber.getClass());
                    targetMap = asyncBackgroundThreadListeners;
                    break;
            }
            // if target listener map is set, method is a onEvent method, otherwise skip
            if (targetMap != null) {
                Listener l = new Listener(subscriber, method);

                List<Listener> listenerList = targetMap.get(eventType);
                listenerList.remove(l);
            }
        }
    }

    /**
     * Register an object as an sticky event receiver
     * @param subscriber subscriber object
     * @see RootEventBus#getStickyEvent(java.lang.Class) 
     */
    public void registerSticky(Object subscriber) {
        register(subscriber, true);
    }

    /**
     * Register an object as an event receiver
     * @param subscriber subscriber object
     */
    public void register(Object subscriber) {
        register(subscriber, false);
    }

    /**
     * Register an object as an receiver
     * @param subscriber subscriber object
     * @param sticky register as sticky receiver as well. 
     * @see RootEventBus#getStickyEvent(java.lang.Class) 
     */
    private void register(Object subscriber, boolean sticky) {

        List<SubscriberMethod> subscriberMethods = SubscriberMethod.getSubscriberMethods(subscriber);

        for (SubscriberMethod subscriberMethod : subscriberMethods) {

            Class eventType = subscriberMethod.getEventType();
            Method method = subscriberMethod.getMethod();
            String methodName = subscriberMethod.getMethod().getName();

            /*
             * Check if method is a "onEvent..." method and set the target listener map
             */
            Map<Class, List<Listener>> targetMap = null;
            switch (methodName) {
                case "onEvent":
                    log.debug("Found 'onEvent' for {} in {}", eventType, subscriber.getClass());
                    targetMap = listeners;
                    break;
                case "onEventBackgroundThread":
                    log.debug("Found 'onEventBackgroundThread' for {} in {}", eventType, subscriber.getClass());
                    targetMap = backgroundThreadListeners;
                    break;
                case "onEventAsync":
                    log.debug("Found 'onEventAsync' for {} in {}", eventType, subscriber.getClass());
                    targetMap = asyncBackgroundThreadListeners;
                    break;
            }
            // if target listener map is set, method is a onEvent method, otherwise skip
            if (targetMap != null) {
                log.trace("eventType: {}", eventType);

                Listener l = new Listener(subscriber, method);

                List<Listener> listenerList = targetMap.get(eventType);
                if (listenerList == null) {
                    listenerList = new ArrayList<>();
                    targetMap.put(eventType, listenerList);
                }
                listenerList.add(l);

                if (sticky) {
                    synchronized (stickyEvents) {
                        Object event = stickyEvents.get(eventType);
                        if (event != null) {
                            l.post(event);
                        }
                    }
                }
            }

        }

    }

    /**
     * Returns last sticky event for given event type
     *
     * @param <T> type of event
     * @param eventType event class
     * @return event instance
     */
    public <T> T getStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.get(eventType));
        }
    }

    /**
     * Just for testing
     * @param args 
     */
    public static void main(String[] args) {

        Object o = new Object() {

            public void onEventAsync(NoSubscriberEvent event) {
                System.out.println("Do nothing: " + event.toString());
            }

        };

        RootEventBus.getDefault().postSticky(new NoSubscriberEvent(defaultInstance, null));
        RootEventBus.getDefault().registerSticky(o);
        RootEventBus.getDefault().post(new NoSubscriberEvent(defaultInstance, null));
        RootEventBus.getDefault().unregister(o);
        RootEventBus.getDefault().post(new NoSubscriberEvent(defaultInstance, null));
    }

}
