package raft.postvayler.impl;

import java.util.HashSet;
import java.util.Set;

import raft.postvayler.NotCompiledException;

/**
 * 
 * @author r a f t
 */
public class ClassCache {

	
	private final String rootClassName;
	private final Set<Class<?>> knownInstrumentedClasses = new HashSet<Class<?>>();
	
	public ClassCache(String rootClassName) {
		this.rootClassName = rootClassName;
	}

	// no need synchronize this, at worst case same class is validate more than once
	public void validateClass(Class<?> clazz) throws Exception {
		if (knownInstrumentedClasses.contains(clazz))
			return;
		
		try {
			String classSuffix = getClassNameForJavaIdentifier(clazz);
			String instrumentationRoot = (String) clazz.getField("__postvayler_root_" + classSuffix).get(null);
			if (!rootClassName.equals(instrumentationRoot))
				throw new NotCompiledException("class " + clazz.getName() + " is not instrumented for root class " + 
						rootClassName + " but for class " + instrumentationRoot); 

			knownInstrumentedClasses.add(clazz);
			
		} catch (NoSuchFieldException e) {
			throw new NotCompiledException(clazz.getName(), e);
		}
	}
	
	private static String getClassNameForJavaIdentifier(Class<?> clazz) {
		return clazz.getName().replace('.', '_').replace('$', '_');
	}
	
}
