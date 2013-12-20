package raft.postvayler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks a method to be synchronized with @{@link Persist} methods. Just like @Persist 
 * methods, @Synch methods are also synchronized on persistence root.</p>  
 * 
 * <p>It's not allowed to call directly or indirectly a @Persist method inside a @Synch method 
 * and will result in a {@link PersistInSynchException}. </p>
 * 
 * @see Persist
 * @see PersistInSynchException
 * 
 * @author  r a f t
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Synch {

}
