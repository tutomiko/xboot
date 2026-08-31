/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot;

import com.xahico.boot.pilot.Boot;

/**
 * The {@code Launcher} class provides an entry point to invoking an x-boot 
 * application without a Java-native main class and method.
 * 
 * The application may annotate a class with 
 * {@link com.xahico.boot.pilot.MainClass} and *one of its static methods 
 * (regardless of access modifiers) with 
 * {@link com.xahico.boot.pilot.MainEntryPoint}.
 * 
 * @author Tuomas Kontiainen
**/
public final class Launcher {
	public static void main (final String[] args){
		Boot.launch(args);
	}
	
	
	
	private Launcher (){
		throw new UnsupportedOperationException("Not supported.");
	}
}