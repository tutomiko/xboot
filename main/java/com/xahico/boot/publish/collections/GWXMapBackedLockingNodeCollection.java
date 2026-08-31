/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish.collections;

import com.xahico.boot.publish.GWXNode;
import com.xahico.boot.util.OrderedConsumer;
import java.util.Collections;
import java.util.Map;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public class GWXMapBackedLockingNodeCollection <T extends GWXNode> extends GWXMapBackedNodeCollection<T> {
	public GWXMapBackedLockingNodeCollection (final Map<Object, T> backing){
		super(Collections.synchronizedMap(backing));
	}
	
	public GWXMapBackedLockingNodeCollection (final GWXNode owner, final Map<Object, T> backing){
		super(owner, Collections.synchronizedMap(backing));
	}
	
	
	
	@Override
	public boolean contains (final T node){
		synchronized (nodes) {
			return super.contains(node);
		}
	}
	
	@Override
	public T lookup (final Object key){
		synchronized (nodes) {
			return super.lookup(key);
		}
	}
	
	@Override
	public int size (){
		synchronized (nodes) {
			return super.size();
		}
	}
	
	@Override
	public void walk (final OrderedConsumer<T> consumer) {
		synchronized (nodes) {
			super.walk(consumer);
		}
	}
}