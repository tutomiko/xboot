/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

import java.util.LinkedList;
import java.util.List;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
final class GWXEventSubscription {
	public static GWXEventSubscription createSubscription (final GWXResourceManager rcm, final GWXPath target, final GWXEventAdapter handler){
		final GWXEventSubscription subscription;
		
		subscription = new GWXEventSubscription();
		
		rcm.select(null, target, (targetPath, targetObject) -> {
			final Binding binding;
			
			System.out.println("Registered Subscription to '%s' (%s)".formatted(targetPath, target));
			
			binding = new Binding(targetObject, target.getExtension(), (eventPath, eventId) -> handler.handle(eventPath, targetObject, eventId));
			
			targetObject.addEventHandler(binding.eventId, binding.eventHandler);
			
			subscription.bindings.add(binding);
			
			return true;
		});
		
		return subscription;
	}
	
	
	
	private final List<Binding> bindings = new LinkedList<>();
	
	
	
	GWXEventSubscription (){
		super();
	}
	
	
	
	public void cancel (){
		try {
			for (final var binding : this.bindings) {
				binding.targetNode.removeEventHandler(binding.eventId, binding.eventHandler);
			}
		} finally {
			this.bindings.clear();
		}
	}
	
	
	
	private static final class Binding {
		private final GWXEventHandler eventHandler;
		private final String          eventId;
		private final GWXObject       targetNode;
		
		
		
		private Binding (final GWXObject targetNode, final String eventId, final GWXEventHandler eventHandler){
			super();
			
			this.targetNode = targetNode;
			this.eventId = eventId;
			this.eventHandler = eventHandler;
		}
	}
}