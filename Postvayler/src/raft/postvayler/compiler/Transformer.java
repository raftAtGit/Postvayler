package raft.postvayler.compiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import javassist.CtClass;

/** {@link ClassFileTransformer} for load time weaving. This is a proof of concept implementation, works as it is,
 * but cannot cooperate with other agents at the moment*/
class Transformer implements ClassFileTransformer {
	 
	private final String rootClassName;
	private final Compiler compiler;
	private final Tree tree;

	private CtClass contextClass;
	private Map<String, CtClass> cachedClasses = new HashMap<String, CtClass>();
	private Instrumentation instrumentation; 
	
	Transformer(String rootClassName, Instrumentation inst) throws Exception {
		this.rootClassName = rootClassName;
		this.instrumentation = inst;
		
		this.compiler = new Compiler(rootClassName); 
		
		this.tree = compiler.getTree();
	}

	//@Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                  ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

    	try {
	    	if (this.contextClass == null) {
	    		this.contextClass = compiler.createContextClass();
	    		// TODO is this enough for web apps?
	    		contextClass.toClass(loader, protectionDomain);
	    	}
	    	// TODO load class via ByteArrayClassPath into javaassit and make modifications
	
	    	System.out.println("transform: " + className);

	    	// class name in another format here (like java/util/List), transform it
	    	className = className.replace('/', '.');
	    	
	    	Node node = tree.findNode(className);
	    	if (node == null) {
	    		// none of our business, class is not in persistent class hierarchy, just omit it
	    		return null;
	    	}
//	    	if (true)
//	    		return null;
	    	
	    	System.out.println("--instrumenting " + className);
	    	
	    	
	    	CtClass cached = cachedClasses.get(className);
	    	if (cached != null) {
	    		System.out.println("class is already instrumented and cached, returning it: " + className);
	    		return cached.toBytecode();
	    	}
	    	
    		// TODO replace CtClass of node here, with given classfileBuffer.
    		// o/w we will override other transformer's modifications
	    	
	    	boolean isTopLevelClass = tree.roots.containsKey(className);
	    	if (isTopLevelClass) {
	    		System.out.println("class " + className + " is top level, injecting IsPersistent");
	    		node = tree.roots.get(className);
	    		compiler.injectIsPersistent(node);
	    	} else {
	    		Node topLevelNode = node.getTopLevelNode();
	    		if (cachedClasses.containsKey(topLevelNode.clazz.getName())) {
		    		System.out.println("class " + className + " is NOT top level, and its top level parent " + topLevelNode.clazz.getName() + " is already instrumented, skipping it");
	    		} else {
		    		System.out.println("class " + className + " is NOT top level, injecting IsPersistent to its top level parent " + topLevelNode.clazz.getName());
		    		compiler.injectIsPersistent(topLevelNode);
		    		compiler.instrumentNode(topLevelNode);
		    		cachedClasses.put(topLevelNode.clazz.getName(), topLevelNode.clazz);
	    		}
	    	}
    		CtClass clazz = compiler.instrumentNode(node);
    		//clazz.toClass(loader, protectionDomain);
	    	return clazz.toBytecode();
    	
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new RuntimeException(e);
    	}
    }
    
//    private void printLoadedSampleClasses() {
//    	System.out.println("-- loaded classes");
//    	for (Class c : inst.getAllLoadedClasses()) {
//    		if (c.getName().startsWith("raft.postvayler.samples.bank"))
//    			System.out.println(c.getName());
//    	}
//    	System.out.println("--");
//    } 

}