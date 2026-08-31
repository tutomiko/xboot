package com.xahico.boot.util;

import com.xahico.boot.pilot.Time;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * Factory for CUIDS (= Chronologically Unique Identifiers.)
 * 
 * @author Tuomas Kontiainen
**/
public final class CUIDFactory {
	private static final long ONE_MILLION_NANOSECONDS = 1_000_000L;
	
	private static final ThreadLocal<CUIDFactory> INSTANCE = ThreadLocal.withInitial(() -> CUIDFactory.getSecureFactory());
	
	
	
	public static TimeSource createGlobalTimeSource (){
		return () -> Time.hostTimeMillisNow();
	}
	
	public static TimeSource createLocalTimeSource (){
		return () -> System.currentTimeMillis();
	}
	
	public static RandomSource createPseudoRandomSource (){
		return new RandomSource() {
			Random random = new Random();
			
			@Override
			public long get (){
				return random.nextLong();
			}
		};
	}
	
	public static RandomSource createSecureRandomSource (){
		return new RandomSource() {
			SecureRandom random = new SecureRandom();
			
			@Override
			public long get (){
				return random.nextLong();
			}
		};
	}
	
	public static CUIDFactory getSecureFactory (){
		final CUIDFactory factory;
		
		factory = new CUIDFactory();
		factory.setRandomSource(CUIDFactory.createSecureRandomSource());
		factory.setTimeSource(CUIDFactory.createGlobalTimeSource());
		
		return factory;
	}
	
	public static CUIDFactory getTestFactory (){
		final CUIDFactory factory;
		
		factory = new CUIDFactory();
		factory.setRandomSource(CUIDFactory.createPseudoRandomSource());
		factory.setTimeSource(CUIDFactory.createLocalTimeSource());
		
		return factory;
	}
	
	public static UUID random (){
		synchronized (INSTANCE) {
			return (INSTANCE).get().generate();
		}
	}
	
	
	
	private long         prevMillis = System.currentTimeMillis();
	private long         prevNanos = System.nanoTime();
	private RandomSource randomSource = CUIDFactory.createSecureRandomSource();
	private TimeSource   timeSource = CUIDFactory.createGlobalTimeSource();
	
	
	
	public CUIDFactory (){
		super();
	}
	
	
	
	public synchronized UUID generate (){
		return new UUID(this.getTimeComponent(), this.getRandomComponent());
	}
	
	private long getRandomComponent (){
		return this.randomSource.get();
	}
	
	private long getTimeComponent (){
		long elapsed = 0;
		long timeNanos = System.nanoTime();
		long timeMillis = this.timeSource.get();
		
		if (timeMillis == prevMillis) {
			elapsed = (timeNanos - prevNanos);
		} else {
			prevNanos = timeNanos;
		}
		
		prevMillis = timeMillis;
		
		return ((timeMillis * ONE_MILLION_NANOSECONDS) + elapsed);
	}
	
	public void setRandomSource (final RandomSource source){
		this.randomSource = source;
	}
	
	public void setTimeSource (final TimeSource source){
		this.timeSource = source;
	}
	
	
	
	@FunctionalInterface
	public static interface RandomSource {
		long get ();
	}
	
	@FunctionalInterface
	public static interface TimeSource {
		long get ();
	}
}