package raft.postvayler;

import java.util.Date;

// TODO implement me
public class Clock {

	private Clock() {
		// should not be instantiated
	}
	
	public static long now() {
		// TODO implement me
		return System.currentTimeMillis();
	}
	
	public static Date today() {
		return new Date(now());
	}
}
