/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish.collections;

import com.xahico.boot.publish.GWXNode;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public class GWXListBackedConcurrentNodeCollection <T extends GWXNode> extends GWXListBackedNodeCollection<T> {
	public GWXListBackedConcurrentNodeCollection (){
		super(new CopyOnWriteArrayList<>());
	}
	
	public GWXListBackedConcurrentNodeCollection (final GWXNode owner){
		super(owner, new CopyOnWriteArrayList<>());
	}
}