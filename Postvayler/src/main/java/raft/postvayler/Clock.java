package raft.postvayler;

import java.util.Date;

import raft.postvayler.impl.ClockBase;

// TODO how to test?
/** 
 * Clock facility. A persistent system should not use {@link System#currentTimeMillis()} or new {@link #Date()} 
 * as these methods are not deterministic, or better to say produce same value at each call. Instead the static methods 
 * of this class should be used.  
 * */
public class Clock extends ClockBase {

	private Clock() {
		// should not be instantiated
	}
	
	/** Returns the current time in milliseconds. */
	public static long now() {
		Date date = getDate();
		return (date == null) ? System.currentTimeMillis() : date.getTime();
	}
	
	/** Returns the current time as {@link Date}. */
	public static Date today() {
		Date date = getDate();
		return (date == null) ? new Date() : date;
	}
}
