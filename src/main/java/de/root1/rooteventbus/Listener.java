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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Listener {
    
    private final static Logger log = LoggerFactory.getLogger(RootEventBus.class);
    
    private final Object listenerObject;
    private final Method onEventMethod;

    Listener(Object listenerObject, Method onEventMethod) {
        this.listenerObject = listenerObject;
        this.onEventMethod = onEventMethod;
        onEventMethod.setAccessible(true);
    }

    void post(Object event) {
        try {
            onEventMethod.invoke(listenerObject, event);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            log.error("onEventMethod: "+onEventMethod+" listenerObject="+listenerObject+" event="+event, ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.listenerObject);
        hash = 43 * hash + Objects.hashCode(this.onEventMethod);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Listener other = (Listener) obj;
        if (!Objects.equals(this.listenerObject, other.listenerObject)) {
            return false;
        }
        if (!Objects.equals(this.onEventMethod, other.onEventMethod)) {
            return false;
        }
        return true;
    }
    
    
    
}
