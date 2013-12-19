package raft.postvayler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO implement me in compiler.
 *  
 * Includes a class and its package to be scanned. Can be in any @Persistent class. 
 * 
 * @author  r a f t
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Include {
	
	//TODO is there a way to say this?
	//Class<? extends Persistent>[] value();
	
	Class<?>[] value();
}
