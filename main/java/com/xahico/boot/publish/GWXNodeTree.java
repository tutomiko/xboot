/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.util.OrderedConsumer;
import java.util.HashMap;
import java.util.Map;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
final class GWXNodeTree {
	private final Map<String, GWXObject> root;
	private final GWXNode                rootNode;
	
	
	
	GWXNodeTree (){
		this(new HashMap<>());
	}
	
	GWXNodeTree (final Map<String, GWXObject> root){
		super();
		
		this.root = root;
		this.rootNode = new GWXNode(GWXProperties.createReflection(null, root));
	}
	
	
	
	public void destroy (){
		this.rootNode.destroy();
	}
	
	public GWXObject lookup (final GWXPath path){
		GWXObject resolveFrom;
		
		resolveFrom = this.lookupRoot(path);
		
		if (null != resolveFrom) 
			return resolveFrom;
		
		resolveFrom = this.lookupRoot(path.root());
		
		if (null == resolveFrom) 
			return null;
		
		if (path.count() == 1) 
			return resolveFrom;
		
		for (var i = 1; i < path.count(); i++) {
			String    lookupName;
			GWXObject lookupNext = null;

			lookupName = path.get(i);

			if (resolveFrom instanceof GWXNode resolveFromNode) {
				final Object propertyObject;

				propertyObject = resolveFromNode.getProperty(lookupName);

				if (propertyObject instanceof GWXObject) {
					lookupNext = ((GWXObject) propertyObject);
				}
			} else if (resolveFrom instanceof GWXNodeCollection resolveFromNodes) {
				lookupNext = resolveFromNodes.lookup(lookupName);
			}

			if (lookupNext == null) {
				return null;
			}

			resolveFrom = lookupNext;
		}
		
		return resolveFrom;
	}
	
	public GWXObject lookup (final String path){
		return this.lookup(GWXPath.create(path));
	}
	
	public GWXObject lookupRoot (final GWXPath path){
		return this.lookupRoot(path.toString());
	}
	
	public GWXObject lookupRoot (final String name){
		return root.get(name);
	}
	
	public GWXObject[] resolve (final GWXPath path, final int resolveCount, final OrderedConsumer<GWXObject> consumer, final GWXPathResolveFallbackHandler resolver){
		GWXObject         resolveFrom;
		final GWXObject[] resolvePath;
		
		if (resolveCount == 0) 
			return new GWXObject[0];
		
		resolveFrom = this.lookupRoot(path);
		
		if (null != resolveFrom) 
			return new GWXObject[]{resolveFrom};
		
		if (resolveCount > path.count()) 
			return resolve(path, path.count(), consumer, resolver);
		
		resolvePath = new GWXObject[resolveCount];
		
		if (resolvePath.length == 0) 
			return resolvePath;
		
		resolveFrom = this.lookupRoot(path.root());
		
		if (null == resolveFrom) {
			resolveFrom = resolver.resolve(path.toString(), path.root());
			
			if (null != resolveFrom) {
				consumer.accept(resolveFrom);
				
				return new GWXObject[]{resolveFrom};
			} else {
				return new GWXObject[0];
			}
		}
		
		resolvePath[0] = resolveFrom;

		if (! consumer.accept(resolveFrom)) {
			return resolvePath;
		}

		for (var i = 1; i < resolveCount; i++) {
			String    lookupName;
			GWXObject lookupNext = null;

			lookupName = path.get(i);

			if (resolveFrom instanceof GWXNode resolveFromNode) {
				final Object propertyObject;

				propertyObject = resolveFromNode.getProperty(lookupName);

				if (propertyObject instanceof GWXObject) {
					lookupNext = ((GWXObject) propertyObject);
				}
			} else if (resolveFrom instanceof GWXNodeCollection resolveFromNodes) {
				lookupNext = resolveFromNodes.lookup(lookupName);
			}

			if (lookupNext == null) {
				lookupNext = resolver.resolve(path.toString(i), lookupName);
				
				if (null == lookupNext) {
					break;
				}
			}

			if (! consumer.accept(lookupNext)) {
				return resolvePath;
			}

			resolvePath[i] = lookupNext;

			resolveFrom = lookupNext;
		}
		
		return resolvePath;
	}
	
	public Map<String, GWXObject> root (){
		return this.root;
	}
	
	public GWXNode rootNode (){
		return this.rootNode;
	}
	
	public void select (final GWXPath path, final GWXSelector consumer){
		this.select(path.withoutExtension(), consumer);
	}
	
	public void select (final String path, final GWXSelector consumer){
		GWXObject selectFrom;
		
		selectFrom = this.lookupRoot(path);
		
		if (null == selectFrom) {
			selectFrom = rootNode;
		}
		
		selectFrom.select(path, "", consumer);
	}
	
	public void traverse (final GWXPath path, final GWXPathWalker consumer){
		GWXObject resolveFrom;
		String    resolvePath;
		
		resolveFrom = this.lookupRoot(path);
		
		if (null != resolveFrom) {
			consumer.walk(path.toString(), resolveFrom);
			
			return;
		}
		
		resolveFrom = lookupRoot(path.root());
		
		if (null != resolveFrom) {
			resolvePath = path.root();
			
			consumer.walk(resolvePath, resolveFrom);
			
			for (var i = 1; i < path.count(); i++) {
				String    lookupName;
				GWXObject lookupNext = null;
				
				lookupName = path.get(i);
				
				if (resolveFrom instanceof GWXNode resolveFromNode) {
					final Object propertyObject;
					
					propertyObject = resolveFromNode.getProperty(lookupName);
					
					if (propertyObject instanceof GWXObject) {
						lookupNext = ((GWXObject) propertyObject);
					}
				} else if (resolveFrom instanceof GWXNodeCollection resolveFromNodes) {
					lookupNext = resolveFromNodes.lookup(lookupName);
				}
				
				if (lookupNext == null) {
					break;
				}
				
				resolvePath += "/";
				resolvePath += lookupName;
				
				if (! consumer.walk(resolvePath, resolveFrom)) {
					break;
				}
				
				resolveFrom = lookupNext;
			}
		}
	}
	
	public void traverse (final String path, final GWXPathWalker consumer){
		traverse(GWXPath.create(path), consumer);
	}
}