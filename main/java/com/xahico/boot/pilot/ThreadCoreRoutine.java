/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.xahico.boot.pilot;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public interface ThreadCoreRoutine extends Runnable {
	boolean canRun ();
	
	default Intensity intensity (){
		return Intensity.NORMAL;
	}
	
	default Priority priority (){
		return Priority.NORMAL;
	}
	
	
	
	public static enum Intensity {
		HIGH(4),
		LOW(2),
		NORMAL(3),
		VERY_HIGH(5),
		VERY_LOW(1);
		
		
		
		public static Intensity closest (final double score){
			final int scoreRounded;
			
			scoreRounded = (int) Math.round(score);
			
			if (scoreRounded >= VERY_HIGH.score) 
				return VERY_HIGH;
			
			if (scoreRounded <= VERY_LOW.score) 
				return VERY_LOW;
			
			for (final var intensity : Intensity.values()) {
				if (intensity.score == score) {
					return intensity;
				}
			}
			
			return NORMAL;
		}
		
		
		
		private final int score;
		
		
		
		Intensity (final int score){
			this.score = score;
		}
		
		
		
		public int score (){
			return this.score;
		}
	}
	
	public static enum Priority {
		CRITICAL(0),
		HIGH(4),
		LOW(2),
		NORMAL(3),
		VERY_HIGH(5),
		VERY_LOW(1);
		
		
		
		private final int value;
		
		
		
		Priority (final int value){
			this.value = value;
		}
		
		
		
		public int value (){
			return this.value;
		}
	}
}