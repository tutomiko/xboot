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
public final class S3Document extends S3Node {
	S3Document (){
		super(null);
	}
	
	
	
	@Override
	public String toStyleString (){
		return this.toStyleString(-1, true);
	}
}