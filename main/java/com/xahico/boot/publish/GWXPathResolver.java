/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.util.OrderedConsumer;

/**
 * Multi-layered path resolver.
 * 
 * Supports multilayered shadow path resolution, 
 * e.g. local/global/global/local/global.
 * 
 * @author Tuomas Kontiainen
**/
public final class GWXPathResolver {
	private final GWXNodeTree fallback;
	private final GWXNodeTree maintail;
	
	
	
	GWXPathResolver (final GWXNodeTree maintail, final GWXNodeTree fallback){
		super();
		
		this.maintail = maintail;
		this.fallback = fallback;
	}
	
	
	
	public GWXObject absolute (final GWXPath path){
		GWXObject resolved;
		
		resolved = this.maintail.lookupRoot(path);
		
		if (null == resolved) {
			resolved = this.fallback.lookupRoot(path);
		}
		
		return resolved;
	}
	
	public GWXObject absolute (final String path){
		return this.absolute(GWXPath.create(path));
	}
	
	
	
	public void traverse (final GWXPath path, final OrderedConsumer<GWXObject> consumer){
		GWXObject resolved;
		GWXObject resolveFrom;
		
		resolved = this.absolute(path);
		
		if (null != resolved) {
			consumer.accept(resolved);
			
			return;
		}
		
		resolved = this.absolute(path.root());

		if (null == resolved) {
			return;
		}
		
		if (path.count() == 1) {
			consumer.accept(resolved);
			
			return;
		}
		
		resolveFrom = resolved;
		
		for (var i = 1; i < path.count(); i++) {
			final String name;
			final Object next;
			
			name = path.get(i);
			
			if (resolveFrom instanceof GWXNode resolveFromNode) {
				next = resolveFromNode.getProperty(name);
				
				if (!(next instanceof GWXObject)) {
					break;
				}
			} else if (resolveFrom instanceof GWXNodeCollection resolveFromNodes) {
				next = resolveFromNodes.lookup(name);
			} else {
				break;
			}
			
			if (null != next) {
				resolved = (GWXObject)(next);
			} else {
				resolved = this.absolute(path.toString(i));
				
				if (null == resolved) {
					break;
				}
			}
			
			if (! consumer.accept(resolved)) {
				break;
			}
			
			resolveFrom = resolved;
		}
	}
}