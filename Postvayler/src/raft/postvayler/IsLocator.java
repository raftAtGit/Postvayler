package raft.postvayler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * marks methods to find child objects 
 *  
 * @author hakan eryargi (r a f t)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IsLocator {

	// TODO later maybe add Root object's class as argument 
//	Class<?> value() default void.class;
	
}
