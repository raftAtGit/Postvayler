package raft.postvayler.impl;

import java.io.Serializable;
import java.lang.reflect.Constructor;


/**
 * @author raft
 */
public class ConstructorCall<T> implements Serializable {
    private static final long serialVersionUID = 1;

    private final String className;
    private final String[] argTypes;
    
    public ConstructorCall(java.lang.reflect.Constructor<T> constructor) {
    	this(constructor.getDeclaringClass().getName(), Utils.getTypeNames(constructor.getParameterTypes()));
    }

    public ConstructorCall(Class<? extends IsPersistent> clazz, Class<?>[] argTypes) {
    	this(clazz.getName(), Utils.getTypeNames(argTypes));
    }
    
    public ConstructorCall(String className, String[] argTypes) {
		this.className = className;
        this.argTypes = argTypes;
	}

    /** reconstructs the wrapped java Constructor */
    @SuppressWarnings("unchecked")
	public java.lang.reflect.Constructor<IsPersistent> getJavaConstructor() throws Exception {
    	
        Class<?> [] args = new Class[argTypes.length];
        for (int i = 0; i < args.length; ++i) {
            args[i] = Class.forName(argTypes[i]);
        }
        return (Constructor<IsPersistent>) Class.forName(className).getDeclaredConstructor(args);
    }

}