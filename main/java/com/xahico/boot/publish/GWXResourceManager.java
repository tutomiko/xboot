/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.reflection.Reflection;
import com.xahico.boot.util.Filter;
import com.xahico.boot.util.OrderedConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public final class GWXResourceManager {
	private static final GWXNodeTree globals = new GWXNodeTree();
	
	
	
	private static Set<Class<? extends GWXObject>> collectResourcesClassesFor (final Class<? extends GWXSession> serviceClass){
		final Set<Class<? extends GWXObject>> collection;
		
		collection = new HashSet<>();
		
		for (final var jclass : Reflection.collectClassesAnnotatedWith(GWXResource.class)) {
			final GWXResource resource;
			
			resource = jclass.getAnnotation(GWXResource.class);
			
			if (resource.owner() == serviceClass) {
				collection.add((Class<? extends GWXNode>)jclass);
			}
		}
		
		return collection;
	}
	
	public static <T extends GWXObject> T lookupRoot (final Class<T> jclass){
		final GWXResource resource;
		
		resource = jclass.getAnnotation(GWXResource.class);
		
		if (null == resource) {
			return null;
		}
		
		return (T) lookupRoot(resource.root());
	}
	
	public static GWXObject lookupRoot (final String name){
		return globals.lookupRoot(name);
	}
	
	
	
	private final Set<Class<? extends GWXObject>> resourceClassList;
	private final Class<? extends GWXSession>     serviceClass;
	private final GWXServiceProvider              serviceProvider;
	
	
	
	GWXResourceManager (final GWXServiceProvider serviceProvider, final Class<? extends GWXSession> serviceClass){
		super();
		
		this.serviceProvider = serviceProvider;
		this.serviceClass = serviceClass;
		this.resourceClassList = collectResourcesClassesFor(serviceClass);
		
		System.out.println("Building Global Root");
		
		this.buildRoot(globals, (resource) -> !resource.local());
		
		System.out.println("Finished building the Global Root");
	}
	
	
	
	GWXContext buildContext (final GWXSession session, final GWXPath.Pattern pattern, final GWXPath path){
		return GWXContext.buildContext(this, session, pattern, path, null);
	}
	
	GWXContext buildContext (final GWXSession session, final GWXPath.Pattern pattern, final GWXPath path, final GWXPermission mode){
		return GWXContext.buildContext(this, session, pattern, path, mode);
	}
	
	private void buildRoot (final GWXNodeTree tree, final Filter<GWXResource> filter){
		final List<Class<? extends GWXObject>> classList;
		
		classList = new ArrayList<>(this.resourceClassList.size());
		classList.addAll(this.resourceClassList);
		
		while (! classList.isEmpty()) {
			final Iterator<Class<? extends GWXObject>> it;
			
			it = classList.iterator();
			
			while (it.hasNext()) {
				final GWXResource                     resource;
				final Class<? extends GWXObject>      resourceClass;
				final String                          resourceName;
				final String                          resourcePath;
				final Reflection<? extends GWXObject> resourceReflection;
				final String                          resourceRoot;
				final int                             resourceDelimiter;
				GWXObject                             resourceParent;
				final GWXObject                       resourceSingleton;
				
				resourceParent = tree.rootNode();
				
				resourceClass = it.next();
				
				resource = resourceClass.getAnnotation(GWXResource.class);
				
				resourcePath = resource.root();
				
				if (resourcePath.isBlank()) {
					it.remove();
					
					continue;
				}
				
				if (! filter.accept(resource)) {
					it.remove();
					
					continue;
				}

				resourceDelimiter = resourcePath.lastIndexOf('/');
				
				if (resourceDelimiter == -1) {
					resourceName = resourcePath;
				} else {
					resourceRoot = resourcePath.substring(0, resourceDelimiter);
					
					resourceName = resourcePath.substring(resourceDelimiter + 1);
					
					resourceParent = tree.lookup(resourceRoot);
					
					if ((tree != globals) && (null == resourceParent)) {
						resourceParent = globals.lookup(resourceRoot);
					}
					
					if (null == resourceParent) {
						continue;
					}
				}
				
				resourceReflection = Reflection.of(resourceClass);
				
				resourceSingleton = resourceReflection.newInstanceOrDefault();
				
				resourceSingleton.name(resourceName);
				
				resourceSingleton.link(resourceParent);
				
				tree.root().put(resourcePath, resourceSingleton);
				
				System.out.println("Registered '%s' = [%s]".formatted(resourceName, resourceSingleton.path()));
				
				it.remove();
			}
		}
	}
	
	void injectLocals (final GWXNodeTree locals){
		this.buildRoot(locals, (resource) -> resource.local());
	}
	
	public void emit (final GWXEvent eventObject){
		this.execute(() -> eventObject.target.fireEvent(eventObject));
	}
	
	public void execute (final Runnable routine){
		this.serviceProvider.getExecutor().execute(routine);
	}
	
	GWXEventSubscription listen (final GWXPath target, final GWXEventAdapter handler){
		return GWXEventSubscription.createSubscription(this, target, handler);
	}
	
	public GWXObject lookup (final GWXSession session, final GWXPath path){
		GWXObject result = null;
		
		if (null != session) {
			result = session.root.lookup(path);
		}
		
		if (null == result) {
			result = globals.lookup(path);
		}
		
		return result;
	}
	
	public GWXObject lookup (final GWXSession session, final String path){
		return this.lookup(session, GWXPath.create(path));
	}
	
	public GWXObject[] resolve (final GWXSession session, final GWXPath path, final int resolveCount, final OrderedConsumer<GWXObject> consumer){
		final AtomicBoolean callStatus;
		GWXObject[]         resolvePath;
		
		callStatus = new AtomicBoolean(true);
		
		if (null == session) {
			resolvePath = new GWXObject[0];
		} else {
			resolvePath = session.root.resolve(path, resolveCount, (object) -> {
				final boolean userStatus;
				
				userStatus = consumer.accept(object);
				
				callStatus.set(userStatus);
				
				return userStatus;
			}, (resolveItemPath, resolveItemName) -> {
				return this.lookup(session, resolveItemPath);
			});
		}
		
		if ((callStatus.get() == true) && (resolvePath.length == 0)) {
			resolvePath = globals.resolve(path, resolveCount, consumer, (resolveItemPath, resolveItemName) -> {
				return this.lookup(session, resolveItemPath);
			});
		}
		
		return resolvePath;
	}
	
	public void select (final GWXSession session, final GWXPath path, final GWXSelector consumer){
		this.select(session, path.withoutExtension(), consumer);
	}
	
	public void select (final GWXSession session, final String path, final GWXSelector consumer){
		final AtomicBoolean callStatus;
		
		callStatus = new AtomicBoolean(true);
		
		if (null != session) {
			session.root.select(path, (objectPath, object) -> {
				final boolean userStatus;
				
				userStatus = consumer.call(objectPath, object);
				
				callStatus.set(userStatus);
				
				return userStatus;
			});
		}
		
		if (callStatus.get() == true) {
			globals.select(path, consumer);
		}
	}
}