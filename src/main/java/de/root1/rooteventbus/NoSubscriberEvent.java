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

/**
 * This event is posted by RootEventBus when there is no subscriber at all for the posted event.
 * 
 */
public final class NoSubscriberEvent {
    
    /** 
     *  The {@link RootEventBus} instance to with the original event was posted to. 
     */
    public final RootEventBus eventBus;

    /** 
     * The original event that could not be delivered to any subscriber. 
     */
    public final Object originalEvent;

    /**
     * @param eventBus reference to use used REB instance
     * @param originalEvent original event for which we do not find a subscriber
     */
    public NoSubscriberEvent(RootEventBus eventBus, Object originalEvent) {
        this.eventBus = eventBus;
        this.originalEvent = originalEvent;
    }

}
