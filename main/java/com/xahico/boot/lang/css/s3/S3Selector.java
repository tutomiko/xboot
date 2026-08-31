/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.lang.css.s3;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public final class S3Selector {
	private String         name = "";
	private S3Selector     next = null;
	private S3SelectorType type = S3SelectorType.UNKNOWN;
	
	
	
	S3Selector (){
		super();
	}
	
	S3Selector (final S3SelectorType type, final String name){
		super();
		
		this.setType(type);
		this.setName(name);
	}
	
	
	
	public String getName (){
		return this.name;
	}
	
	public S3SelectorType getType (){
		return this.type;
	}
	
	public void join (final S3Selector selector){
		if (null == this.next) {
			this.next = selector;
		} else {
			this.next.join(selector);
		}
	}
	
	public S3Selector next (){
		return this.next;
	}
	
	public String path (){
		final StringBuilder sb;
		
		sb = new StringBuilder();
		
		if (null == this.next) {
			sb.append(this.name);
		} else {
			sb.append(this.name);
			sb.append(this.type.requireConnect() ? "" : " ");
			
			if (this.next.path().startsWith(".")) sb.append(" ");
			
			sb.append(this.next.path());
		}
		
		return sb.toString();
	}
	
	public void setName (final String name){
		this.name = name;
	}
	
	public void setType (final S3SelectorType type){
		this.type = type;
	}
	
	@Override
	public String toString (){
		final StringBuilder sb;
		
		sb = new StringBuilder();
		sb.append(this.getName().strip());
		
		if (null != this.next) {
			sb.append(this.next);
		}
		
		return sb.toString();
	}
}