/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish.collections;

import com.xahico.boot.publish.GWXNode;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public class GWXMapBackedConcurrentNodeCollection <T extends GWXNode> extends GWXMapBackedNodeCollection<T> {
	public GWXMapBackedConcurrentNodeCollection (){
		super(new ConcurrentHashMap<>());
	}
	
	public GWXMapBackedConcurrentNodeCollection (final GWXNode owner){
		super(owner, new ConcurrentHashMap<>());
	}
}