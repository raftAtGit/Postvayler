package raft.postvayler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as persistent. {@link Postvayler} records calls to such methods with parameters 
 * and executes them again in the same order when system is restarted. The calls are  
 * synchronized on persistence root. 
 * 
 * @see Persistent
 * @see Synch
 * 
 * @author  r a f t
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Persist {

}
