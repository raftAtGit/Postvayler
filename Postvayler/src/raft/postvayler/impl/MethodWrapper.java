package raft.postvayler.impl;

import java.io.Serializable;


/**
 * A <code>Serializable</code> representation of a
 * <code>Method</code>.
 *
 * @since 0_1
 * @author Jay Sachs [jay@contravariant.org]
 * @author raft
 */
public class MethodWrapper implements Serializable {
    private static final long serialVersionUID = 1;

    private final String name;
    private final String className;
    private final String[] argTypes;
    
    public MethodWrapper(java.lang.reflect.Method method) {
    	this(method.getName(), method.getDeclaringClass().getName(), getTypeNames(method.getParameterTypes()));
    }

    public MethodWrapper(String name, Class<?> clazz, Class<?>[] argTypes) {
    	this(name, clazz.getName(), getTypeNames(argTypes));
    }
    
    public MethodWrapper(String name, String className, String[] argTypes) {
		this.name = name;
		this.className = className;
        this.argTypes = argTypes;
	}

    /** reconstructs the wrapped java method */
    public java.lang.reflect.Method getJavaMethod() throws Exception {
    	
        Class<?> [] args = new Class[argTypes.length];
        for (int i = 0; i < args.length; ++i) {
            args[i] = Class.forName(argTypes[i]);
        }
        return Class.forName(className).getDeclaredMethod(name, args);
    }

    private static String[] getTypeNames(Class<?>[] types) {
    	String[] names = new String[types.length];
    	
        for (int i = 0; i < types.length; ++i) {
        	names[i] = types[i].getName();
        }
        return names;
    }
    

}