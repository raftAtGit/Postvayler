package raft.postvayler.samples._bank;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 
 * dummy annotation to mark injected methods 
 * 
 * @author r a f t
 */
@Target({ ElementType.FIELD, ElementType.METHOD } )
public @interface _Injected {

	String value() default "";
}
