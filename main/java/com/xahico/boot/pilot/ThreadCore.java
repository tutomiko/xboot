/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.pilot;

import com.xahico.boot.pilot.ThreadCoreRoutine.Intensity;
import com.xahico.boot.util.TimeUtilities;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public abstract class ThreadCore {
	private static int                           activeRoutines = 0;
	private static double                        burden = 0.0;
	private static ThreadCoreRoutine.Intensity   burdenIntensity = ThreadCoreRoutine.Intensity.VERY_LOW;
	private static final Executor                executor = Executors.newSingleThreadExecutor(routine -> ThreadCore.executorThread = new Thread(routine));
	private static Thread                        executorThread = null;
	private static double                        minimumImportance = 0.0;
	private static final List<ThreadCoreRoutine> routines = new LinkedList<>();
	private static boolean                       shutdown = false;
	
	
	
	public static void call (final Runnable procedure){
		ThreadCore.getExecutor().execute(procedure);
	}
	
	/**
	 * Executes {@code procedure} immediately if within the 
	 * {@link #getExecutorThread() executor thread}, 
	 * otherwise 
	 * {@link #call(java.lang.Runnable) queues} 
	 * it for synchronized execution by the executor thread in the future.
	 * <br>
	 * The given procedure is guaranteed to be executed in synchronicity 
	 * with the executor thread.
	 * 
	 * @param procedure 
	 * Procedure to be executed.
	**/
	public static void callops (final Runnable procedure){
		if (! ThreadCore.isExecutorThread()) 
			ThreadCore.call(procedure);
		else {
			procedure.run();
		}
	}
	
	public static long clock (){
		return System.nanoTime();
	}
	
	public static void createRoutine (final ThreadCoreRoutine routine){
		ThreadCore.call(() -> routines.add(routine));
	}
	
	private static double evaluateImportance (final ThreadCoreRoutine routine){
		return (((double)routine.priority().value()) / ((double)routine.intensity().score()));
	}
	
	public static synchronized Executor getExecutor (){
		return ThreadCore.executor;
	}
	
	public static synchronized Thread getExecutorThread (){
		return ThreadCore.executorThread;
	}
	
	public static boolean isExecutorThread (){
		return (Thread.currentThread() == ThreadCore.executorThread);
	}
	
	private static boolean isIncludeExecution (final ThreadCoreRoutine routine){
		if (routine.priority() == ThreadCoreRoutine.Priority.CRITICAL) 
			return true;
		else {
			return (evaluateImportance(routine) >= minimumImportance);
		}
	}
	
	private static boolean isOverloaded (){
		return ((ThreadCore.burdenIntensity == Intensity.VERY_HIGH) && (ThreadCore.activeRoutines >= Math.ceil(ThreadCore.routines.size())));
	}
	
	public static void removeRoutine (final ThreadCoreRoutine routine){
		ThreadCore.call(() -> routines.remove(routine));
	}
	
	@Export(ExportType.STOP)
	private static void shutdown (){
		ThreadCore.call(() -> shutdown = true);
	}
	
	@Export(ExportType.START)
	private static void start (){
		ThreadCore.call(ThreadCore::sweep);
	}
	
	private static void sweep (){
		final Iterator<ThreadCoreRoutine> it;
		
		activeRoutines = 0;
		
		for (final var routine : routines) {
			if (! routine.canRun()) 
				continue;
			
			minimumImportance += evaluateImportance(routine);
			minimumImportance /= 2.0;
			
			activeRoutines++;
			
			burden += routine.intensity().score();
			burden /= 2.0;
		}
		
		burdenIntensity = ThreadCoreRoutine.Intensity.closest(burden);
		
		it = routines.iterator();
		
		while (it.hasNext()) {
			final ThreadCoreRoutine routine;
			
			routine = it.next();
			
			if (routine.canRun()) try {
				if (ThreadCore.isOverloaded() && !ThreadCore.isIncludeExecution(routine)) {
					continue;
				}
				
				routine.run();
			} catch (final Throwable t) {
				t.printStackTrace();
				it.remove();
			}
		}
		
		if (! shutdown) {
			ThreadCore.call(ThreadCore::sweep);
		}
	}
	
	
	
	protected ThreadCore (){
		throw new UnsupportedOperationException("Not supported.");
	}
}