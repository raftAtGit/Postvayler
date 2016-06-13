package raft.postvayler.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PostvaylerConfiguration.class)
public @interface EnablePostvayler {
	//Class<?> value();
	
	/** Name of root class in persistent object graph. */
	String rootClass();
	
	/** Directory to store persist files, defaults to empty string which translated into 'persist/<rootClassName>' in current directory. */
	String persistDir() default "";
}
