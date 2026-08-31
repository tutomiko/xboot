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
public final class S3Utilities {
	public static S3Selector parseSelectors (final String string){
		final S3SelectorParser parser;
		
		parser = new S3SelectorParser();
		
		return parser.parseString(string);
	}
	
	
	
	private S3Utilities (){
		throw new UnsupportedOperationException("Not supported.");
	}
}