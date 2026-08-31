/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.util.Exceptions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public abstract class GWXObject {
	private volatile Map<String, List<GWXEventHandler>> eventHandlers = null;
	private volatile String                             name = null;
	private GWXObject                                   parent = null;
	
	
	
	GWXObject (){
		super();
	}
	
	
	
	final void addEventHandler (final String eventId, final GWXEventHandler eventHandler){
		Map<String, List<GWXEventHandler>> map = eventHandlers;

		if (map == null) {
			synchronized (this) {
				if (eventHandlers == null) {
					eventHandlers = new ConcurrentHashMap<>();
				}
				
				map = eventHandlers;
			}
		}

		map.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(eventHandler);
	}
	
	protected void cleanup (){
		
	}
	
	final void destroy (){
		this.unlink();
		
		if (null != this.eventHandlers) {
			this.eventHandlers.values().forEach(handlers -> handlers.clear());
			this.eventHandlers.clear();
			this.eventHandlers = null;
		}
		
		this.cleanup();
	}
	
	final void fireEvent (final GWXEvent event){
		final String path;
		
		path = this.path();
		
		if (null != this.eventHandlers) {
			fireEventHandlers(this.eventHandlers.get("**"), path, event);
			
			if (event.source == this) {
				fireEventHandlers(this.eventHandlers.get("*"), path, event);
				
				fireEventHandlers(this.eventHandlers.get(event.id), path, event);
			}
		}
		
		if (null != this.parent) {
			this.parent.fireEvent(event);
		}
	}
	
	private void fireEventHandlers (final List<GWXEventHandler> handlers, final String path, final GWXEvent event){
		if (null != handlers) {
			handlers.forEach((handler) -> {
				try {
					handler.handle(path, event);
				} catch (final Throwable t) {
					Exceptions.ignore(t);
				}
			});
		}
	}
	
	final GWXObject getParent (){
		return this.parent;
	}
	
	final void link (final GWXObject parent){
		assert(null == this.parent);
		
		this.parent = parent;
	}
	
	String name (){
		return this.name;
	}
	
	final void name (final String name){
		this.name = name;
	}
	
	final String path (){
		if (null != this.parent) {
			return (this.parent.path() + "/" + this.name());
		} else {
			return ((null != this.name()) ? this.name() : "");
		}
	}
	
	final void removeEventHandler (final String eventId, final GWXEventHandler eventHandler){
		final List<GWXEventHandler> eventHandlerSet;
		
		if (null != this.eventHandlers) {
			eventHandlerSet = this.eventHandlers.get(eventId);
			
			if (null != eventHandlerSet) {
				if (eventHandlerSet.remove(eventHandler) && eventHandlerSet.isEmpty()) {
					this.eventHandlers.remove(eventId);
				}
			}
		}
	}
	
	final GWXObject root (){
		if (null == this.parent) 
			return this;
		else {
			return this.parent;
		}
	}
	
	abstract void select (final String path, final String from, final GWXSelector consumer);
	
	abstract Object snapshot ();
	
	final void unlink (){
		this.parent = null;
	}
}