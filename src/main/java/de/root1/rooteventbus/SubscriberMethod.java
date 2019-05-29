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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
/* package private */ class SubscriberMethod {
    
    private final static Logger log = LoggerFactory.getLogger(SubscriberMethod.class);
    private final Class eventType;
    private final Method method;
    private final Object subscriber;

    private SubscriberMethod(Object subscriber, Method method, Class eventType) {
        this.subscriber = subscriber;
        this.method = method;
        this.eventType = eventType;
    }

    public Class getEventType() {
        return eventType;
    }

    public Method getMethod() {
        return method;
    }

    public Object getSubscriber() {
        return subscriber;
    }
    
    public static List<SubscriberMethod> getSubscriberMethods(Object subscriber) {
        
        List<SubscriberMethod> list = new ArrayList<>();
        
        Method[] subscriberMethods = subscriber.getClass().getDeclaredMethods();
        for (Method subscriberMethod : subscriberMethods) {
            
            /*
             * Use methodname + returntype + parameter length as indicator for "onEvent..." methods
             */
            String methodName = subscriberMethod.getName();
            boolean isVoidMethod = subscriberMethod.getReturnType().equals(void.class);
            Class<?>[] parameterTypes = subscriberMethod.getParameterTypes();
            
            log.trace("methodName: {} isVoidMethod: ",methodName, isVoidMethod);
            
            if (isVoidMethod && parameterTypes.length==1) {
            
                Class eventType = parameterTypes[0];
                /*
                 * Check if method is a "onEvent..." method and set the target listener map
                 */
                String subscriberMethodName = null;
                
                switch (methodName) {
                    case "onEvent":
                        log.debug("Found 'onEvent' for {} in {}", eventType, subscriber.getClass());
                        subscriberMethodName = methodName;
                        break;
                    case "onEventBackgroundThread":
                        log.debug("Found 'onEventBackgroundThread' for {} in {}", eventType, subscriber.getClass());
                        subscriberMethodName = methodName;
                        break;
                    case "onEventAsync":
                        log.debug("Found 'onEventAsync' for {} in {}", eventType, subscriber.getClass());
                        subscriberMethodName = methodName;
                        break;
                }
                
                if (subscriberMethodName!=null) {
                    list.add(new SubscriberMethod(subscriber, subscriberMethod, eventType));
                }
                
            }
        }
        
        
        return list;
    }

    
}
