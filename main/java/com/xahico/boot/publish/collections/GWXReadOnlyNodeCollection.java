/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish.collections;

import com.xahico.boot.publish.GWXNode;
import com.xahico.boot.publish.GWXNodeCollection;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public abstract class GWXReadOnlyNodeCollection <T extends GWXNode> extends GWXNodeCollection<T> {
	protected GWXReadOnlyNodeCollection (){
		super();
	}
	
	protected GWXReadOnlyNodeCollection (final GWXNode owner){
		super(owner);
	}
	
	
	
	@Override
	public final void add (final T node){
		throw new UnsupportedOperationException("Not supported.");
	}
	
	@Override
	public final void remove (final T node){
		throw new UnsupportedOperationException("Not supported.");
	}
}