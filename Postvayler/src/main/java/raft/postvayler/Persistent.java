package raft.postvayler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as Persistent. Persistence is inherited so subclasses of a persistent class 
 * is also persistent. But it's a good practice to annotate them too. 
 * 
 * @see Persist
 * 
 * @author r a f t
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Persistent {

}
