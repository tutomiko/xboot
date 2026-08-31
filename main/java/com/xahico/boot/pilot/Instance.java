/*
 * Written by Tuomas Kontiainen <https://www.github.com/tutomiko>
 * 
 * Copyright (c) 2023, Tuomas Kontiainen
 */
package com.xahico.boot.pilot;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public final class Instance extends Launchable {
	private final Map<ExportType, MethodHandle> exports;
	private boolean                             started = false;
	
	
	
	Instance (final Class<?> jclass, final Map<ExportType, MethodHandle> exports){
		super(jclass);
		
		this.exports = exports;
	}
	
	
	
	public String getAlias (){
		return this.getInstanceClass().getSimpleName();
	}
	
	@Override
	public Class<?>[] getDependencies (){
		final Dependencies annotation;
		
		if (! this.getInstanceClass().isAnnotationPresent(Dependencies.class)) {
			return new Class<?>[0];
		}
		
		annotation = this.getInstanceClass().getAnnotation(Dependencies.class);
		
		return annotation.value();
	}
	
	public Class<?> getReflectedClass (){
		return this.getInstanceClass();
	}
	
	private void invoke (final ExportType xtp) throws ExecutionException {
		final MethodHandle methodHandle;
		
		methodHandle = this.exports.get(xtp);
		
		if (null != methodHandle) {
			try {
				methodHandle.invoke();
			} catch (final Throwable ex) {
				throw new ExecutionException(ex);
			}
		}
	}
	
	public boolean isStarted (){
		return this.started;
	}
	
	@Override
	public void start (){
		try {
			this.invoke(ExportType.START);
			
			this.started = true;
		} catch (final ExecutionException ex) {
			throw new Error(ex);
		}
	}
	
	@Override
	public void stop (){
		try {
			this.invoke(ExportType.STOP);
		} catch (final ExecutionException ex) {
			throw new Error(ex);
		}
	}
}