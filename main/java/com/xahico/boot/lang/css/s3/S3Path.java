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
public final class S3Path {
	private final boolean interconnect;
	private final boolean nestable;
	private final String  path;
	
	
	
	S3Path (final String path, final boolean nestable, final boolean interconnect){
		super();
		
		this.path = path;
		this.nestable = nestable;
		this.interconnect = interconnect;
	}
	
	
	
	public boolean nestable (){
		return this.nestable;
	}
	
	public String path (){
		return this.path;
	}
	
	public boolean interconnect (){
		return this.interconnect;
	}
}