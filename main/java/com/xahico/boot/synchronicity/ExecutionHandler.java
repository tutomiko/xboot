/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.synchronicity;

import java.util.concurrent.ExecutorService;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public interface ExecutionHandler {
	public void call (final Runnable routine);
	
	default void callops (final Runnable routine){
		if (this.isCallingThread()) {
			routine.run();
		} else {
			this.call(routine);
		}
	}
	
	default boolean isCallingThread (){
		return this.isOwnerOfThread(Thread.currentThread());
	}
	
	public boolean isOwnerOfThread (final long threadId);
	
	default boolean isOwnerOfThread (final Thread thread){
		return this.isOwnerOfThread(thread.getId());
	}
}