package raft.postvayler.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SyntheticAttribute;
import raft.postvayler.Include;
import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Storage;
import raft.postvayler.Synch;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.IsRoot;
import raft.postvayler.inject.Key;

/**
 * Postvayler compiler. Works on <strong>javac</strong> compiled bytecode.
 * 
 * @author r a f t
 */
public class Compiler {

	// TODO somehow must check if a class is already enhanced
	
	private static final boolean DEBUG = false;
	
	public static void main(String[] args) throws Exception {
		String rootClass = args[0];
		
		Compiler compiler = new Compiler(rootClass);
		compiler.run();
	}
	
	private final String rootClassName;
	private boolean scanSubPackages = false; 
	
	
//	private final String outputFolder;
	private CtClass contextClass;

	
	private final ClassPool pool;
	
	private final Set<String> scannedClasses = new HashSet<String>();
	private final Set<String> scannedPackages = new HashSet<String>();
	private final Map<String, CtClass> packageQueue = new LinkedHashMap<String, CtClass>();
	
	public Compiler(String rootClass) {
		this.rootClassName = rootClass; 
		
		this.pool = ClassPool.getDefault();
		
		pool.importPackage("raft.postvayler");
		pool.importPackage("raft.postvayler.impl");
	}
	
	public synchronized void run() throws Exception {
		CtClass rootClass = pool.get(rootClassName);
		if (rootClass.getAnnotation(Persistent.class) == null)
			throw new CompileException("root class " + rootClassName + " is not annotated with @Persistent");
		
		if (rootClass.isInterface())
			throw new CompileException("root class " + rootClassName + " is an Interface: rootClass");
		if (Modifier.isAbstract(rootClass.getModifiers())) 
			throw new CompileException("root class is Abstract: " + rootClassName);
	
		if (rootClass.getPackageName() == null)
			throw new CompileException("@Persistent classes in default package is not supported " + rootClass.getName());
		
		Tree tree = createTree(rootClass);
		
		System.out.println("-- scanned classes --");
		for (String scanned : scannedClasses)
			System.out.println(scanned);

		System.out.println("-- tree --");
		tree.print(System.out);
		System.out.println("----");
		
		createContextClass();
		
		instrumentTree(tree);
//		instrumentClass(rootClass);
	}

	private void createContextClass() throws Exception {
		// TODO we need better encapsulation for pravayler and inTransaction fields
		
		CtClass rootClazz = pool.get(rootClassName);
		contextClass = pool.makeClass(rootClazz.getPackageName() + ".__Postvayler");
		contextClass.setSuperclass(pool.get(Context.class.getName()));
		
		contextClass.addField(CtField.make("public static final Class rootClass = " + rootClassName + ".class;", contextClass));
		contextClass.addConstructor(CtNewConstructor.make(createSource("Context.init.java.txt", rootClassName), contextClass));
		
		contextClass.writeFile(getClassWriteDir(rootClazz));
	}

	private Tree createTree(CtClass rootClass) throws Exception {
		Tree tree = new Tree();
		
		scanPackage(tree, rootClass);
		tree.findNode(rootClass).isRoot = true;
		
		while (!packageQueue.isEmpty()) {
			String nextPackage = packageQueue.keySet().iterator().next(); 
			scanPackage(tree, packageQueue.remove(nextPackage));
		}
		
		return tree;
	}
	/** @param clazz a class in package, required to locate package location */
	private void scanPackage(Tree tree, CtClass clazz) throws Exception {
		System.out.println("-scanning package " + clazz.getPackageName());
		scannedPackages.add(clazz.getPackageName());
		
		List<CtClass> packageClasses = getPackageClasses(clazz);
		
		for (CtClass cls : packageClasses) {
			if (cls.isInterface())
				continue;
			if (!isPersistent(cls)) {
				warnIfHasUnusedAnnotations(cls);
				continue;
			}

			List<CtClass> hierarchy = tree.add(cls);
			
			for (CtClass hClass : hierarchy) {
				// TODO check class is not a inner class. inner classes cannot be created via reflection (sure?)
				scanClass(hClass);
			}
		}
	}	
	
	private void scanClass(CtClass clazz) throws Exception {
		scannedClasses.add(clazz.getName());

		String packageName = clazz.getPackageName();
		if (!scannedPackages.contains(packageName))
			packageQueue.put(packageName, clazz);
		
		for (Object ref : clazz.getRefClasses()) {
			CtClass refClass = pool.get((String)ref);
			String refPackage = refClass.getPackageName();
			
			if (!scannedPackages.contains(refPackage))
				packageQueue.put(refPackage, refClass);
		}
		
		if (clazz.hasAnnotation(Include.class)) {
			Include include = (Include) clazz.getAnnotation(Include.class);
			// TODO will this work with javaaagent? since class itself is already loaded in this scenario 
			for (Class<?> cls : include.value()) {
				String includedPackage = cls.getPackage().getName();
				if (!scannedPackages.contains(includedPackage))
					packageQueue.put(includedPackage, pool.get(cls.getName()));
			}
		}
		
		
		scanFields(clazz);
		
		String genericSignature = clazz.getGenericSignature();
		if (genericSignature != null) {
			SignatureAttribute.ClassSignature classSignature = SignatureAttribute.toClassSignature(genericSignature);
			for (SignatureAttribute.TypeParameter parameter : classSignature.getParameters()) {
				// TODO what?
				scanObjectType(parameter.getClassBound());
			}
		}
	}
	
	/** scans class' fields recursively */
	private void scanFields(CtClass clazz) throws Exception {
		
		for (CtField field : clazz.getDeclaredFields()) {

			if (Modifier.isTransient(field.getModifiers())) {
				checkTransientFieldAnnotations(field);
				continue;
			}
			if (Modifier.isStatic(field.getModifiers())) {
//				checkStaticFieldAnnotations(field);
				continue;
			}

			CtClass fieldClass = field.getType();
			scanField(clazz, fieldClass);
			
			String genericSignature = field.getGenericSignature();
			if (genericSignature != null) {
				scanObjectType(SignatureAttribute.toFieldSignature(genericSignature));
			}
		}
	}
	
		
	private void scanObjectType(SignatureAttribute.ObjectType objectType) throws Exception {
		if (objectType instanceof SignatureAttribute.ClassType) {
			SignatureAttribute.ClassType classType = (SignatureAttribute.ClassType) objectType;
			if (!scannedClasses.contains(classType.getName())) {
				scanClass(pool.get(classType.getName()));
			}  
			SignatureAttribute.TypeArgument[] typeArguments = classType.getTypeArguments();
			if (typeArguments== null)
				return;
			
			for (SignatureAttribute.TypeArgument typeArgument : typeArguments) {
				SignatureAttribute.ObjectType type = typeArgument.getType();
				if (type == null) {
					System.out.println("warning, couldnt determine type arguments of field " + objectType);
					continue;
				}
				scanObjectType(type);
			}			
		} else if (objectType instanceof SignatureAttribute.ArrayType) {
			SignatureAttribute.ArrayType arrayType = (SignatureAttribute.ArrayType) objectType;
			SignatureAttribute.Type compType = arrayType.getComponentType();
			if (compType instanceof SignatureAttribute.BaseType) {
				// primitive
				return;
			}
			if (compType instanceof SignatureAttribute.ObjectType) {
				scanObjectType((SignatureAttribute.ObjectType) compType);
			}
		} else if (objectType instanceof SignatureAttribute.TypeVariable) {
			// none of out business
		} else {
			assert false : objectType;
		}
	}

	private void scanField(CtClass declaringClass, CtClass fieldClass) throws Exception {
		if (fieldClass.isPrimitive() || scannedClasses.contains(fieldClass.getName())) {
			return;
		}

		scannedClasses.add(fieldClass.getName());
				
		String fieldPackage = fieldClass.getPackageName(); 
		if (fieldPackage == null) {
			if (isPersistent(fieldClass))
				throw new CompileException("@Persistent classes in default package is not supported, " + fieldClass.getName() 
						+ " @ " + declaringClass.getName() + "." + declaringClass.getName());
		} else {
			if (!scannedPackages.contains(fieldPackage))
				packageQueue.put(fieldPackage, fieldClass);
		} 
		
		if (fieldClass.isArray()) {
			CtClass arrayClass = fieldClass;
			while (arrayClass.isArray()) {
				arrayClass = arrayClass.getComponentType();
			}
			scanField(declaringClass, arrayClass);
		}
	}	

	private void instrumentTree(Tree tree) throws Exception {
		// cleans previously injected code
//		for (Node rootNode : tree.roots.values()) {
//			clean(rootNode);
//		}
		for (Node rootNode : tree.roots.values()) {
			checkCorrectlyInstrumented(rootNode);
		}
		
		for (Node rootNode : tree.roots.values()) {
			
			// inject interfaces to top level classes
			injectIsPersistent(rootNode);
			instrumentNode(rootNode);
		}
	}
	
	private void injectIsRoot(Node node) throws Exception {
		CtClass clazz = node.clazz;
		
		clazz.addInterface(pool.get(IsRoot.class.getName()));
		System.out.println("added IsRoot interface to " + clazz.getName());
			
		clazz.addInterface(pool.get(Storage.class.getName()));
		System.out.println("added Storage interface to " + clazz.getName());
		
		clazz.addField(CtField.make("private final Pool __postvayler_pool = new Pool();", clazz));
		System.out.println("added Pool __postvayler_pool field to " + clazz.getName());

		// implement the IsRoot interface
		clazz.addMethod(CtNewMethod.make("public final IsPersistent __postvayler_get(Long id) { return __postvayler_pool.get(id);}", clazz));
		System.out.println("added IsPersistent __postvayler_get(Long id) method to " + clazz.getName());
		
		String source = createSource("IsRoot.put.java.txt");
		if (DEBUG) System.out.println(source);
		clazz.addMethod(CtNewMethod.make(source, clazz));
		System.out.println("added void __postvayler_put(IsPersistent persistent) method to " + clazz.getName());
		
		clazz.addMethod(CtNewMethod.make("public final void __postvayler_onRecoveryCompleted() { __postvayler_pool.switchToWeakValues();}", clazz));
		System.out.println("added void __postvayler_onRecoveryCompleted() method to " + clazz.getName());
		
		source = createSource("IsRoot.takeSnapshot.java.txt", contextClass.getName());
		if (DEBUG) System.out.println(source);
		clazz.addMethod(CtNewMethod.make(source, clazz));
		System.out.println("added public File takeSnapshot() method to " + clazz.getName());
	}

	private void injectIsPersistent(Node node) throws Exception {
		if (node.compiled)
			return;
		
		CtClass clazz = node.clazz;
		
		clazz.addInterface(pool.get(IsPersistent.class.getName()));
		System.out.println("added IsPersistent interface to " + clazz.getName());
		
		// TODO optimization: we can make __postvayler_Id private final if there are no subclasses of it
		clazz.addField(makeSynthetic(CtField.make("protected Long __postvayler_Id;", clazz)));
		System.out.println("added Long __postvaylerId field to " + clazz.getName());
		
		// implement the IsPersistent interface
		clazz.addMethod(CtNewMethod.make("public final Long __postvayler_getId() { return __postvayler_Id;}", clazz));
		System.out.println("added Long __postvayler_getId() method to " + clazz.getName());
		
		String validateSource = createSource("IsPersistent.init.validateClass.java.txt", contextClass.getName());
		
		// add code to validate runtime type
		for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
			// optimization: there is a call to this(constructor), omit validate call  
			if (!constructor.callsSuper())
				continue;
			constructor.insertAfter(validateSource);
			System.out.println("added validateClass call to " + constructor.getLongName());
		}
	}

	private void instrumentNode(Node node) throws Exception {
		if (node.compiled)
			return;
		
		CtClass clazz = node.clazz;
		
		// add a static final field for Root class to mark this class as enhanced
		String classSuffix = getClassNameForJavaIdentifier(clazz.getName());
		clazz.addField(CtField.make("public static final String __postvayler_root_" + classSuffix + " = \"" + rootClassName + "\";", clazz));
		
		if (node.isRoot) {
			injectIsRoot(node);
		}
		
		if (!Modifier.isAbstract(clazz.getModifiers())) {
			String source = createSource("IsPersistent.init.putToPool.java.txt", contextClass.getName(), clazz.getName());
			if (DEBUG) System.out.println(source);
			
			for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
				// optimization: there is a call to this(constructor), omit put to pool code 
				if (!constructor.callsSuper())
					continue;
				constructor.insertAfter(source);
				System.out.println("added add to pool call to " + constructor.getLongName());
			}
		}
		
		for (CtMethod method : clazz.getDeclaredMethods()) {
			System.out.println("method: " + method);

			if (method.hasAnnotation(Persist.class)) {
				createTransaction(method);
			}

			if (method.hasAnnotation(Synch.class)) {
				createSynch(method);
			}
		}
		
		String dir = getClassWriteDir(clazz);
		System.out.println("writing class " + clazz.getName() + " to " + dir);
		clazz.writeFile(dir);
		
		for (Node subNode : node.subClasses.values()) {
			instrumentNode(subNode);
		}
	}

	/** cleans tree recursively */
	private void clean(Node node) throws Exception {
		cleanClass(node.clazz);
		
		for (Node sub : node.subClasses.values()) {
			clean(sub);
		}
	}
	
	/** recursively checks tree if it's already instrumented for same root 
	* TODO a more detailed check is necessary I suppose
	 * */
	private void checkCorrectlyInstrumented(Node node) throws Exception {
		String classSuffix = getClassNameForJavaIdentifier(node.clazz.getName());
		try {
			String fieldName = "__postvayler_root_" + classSuffix;
			CtField field = node.clazz.getField(fieldName);
			if (field.getType() != pool.get(String.class.getName())) 
				throw new CompileException("Unexpected type " + field.getType().getName() + " of field " + fieldName + " @ " + node.clazz.getName());
			String value = (String) field.getConstantValue();
			if (!rootClassName.equals(value)) 
				throw new CompileException("Class " + node.clazz.getName() + " is compiled for another root " + value);
			node.compiled = true;
		} catch (NotFoundException e) {
			// ok not instrumented
		}
		
		for (Node sub : node.subClasses.values()) {
			checkCorrectlyInstrumented(sub);
		}
	}
	
	
	/** removes all existing instrumentation by Postvayler. this is necessary to allow running compiler on same classes again */
	private void cleanClass(CtClass clazz) throws Exception {
		for (CtField field : clazz.getDeclaredFields()) {
			if (field.getName().startsWith("__postvayler_")) {
				clazz.removeField(field);
				System.out.println("removed old field " + clazz.getName() + "." + field.getName());
			}
		}
		// TODO: a serious flaw here: when compiler is re-run on same class and previosly added @Persist and @Synch methods are removed,
		// original methods are lost!!    
		for (CtMethod method : clazz.getDeclaredMethods()) {
			if (method.getName().startsWith("__postvayler_")) {
				System.out.println(method.getMethodInfo().getAttribute(SyntheticAttribute.tag));
				clazz.removeMethod(method);
				System.out.println("removed old method " + method.getLongName());
			}
		}
		try {
			CtMethod method = clazz.getMethod("takeSnapshot", "()Ljava/io/File;");
			clazz.removeMethod(method);
			System.out.println("removed old method " + method.getLongName());
		} catch (NotFoundException e) {}
		
		List<CtClass> interfaces = new ArrayList<CtClass>(Arrays.asList(clazz.getInterfaces()));
		for (Iterator<CtClass> i = interfaces.iterator(); i.hasNext();) {
			CtClass inttf = i.next();
			if (inttf.getName().equals(IsPersistent.class.getName()) 
					|| inttf.getName().equals(IsRoot.class.getName())
					|| inttf.getName().equals(Storage.class.getName())) {
				i.remove();
			}
		}
		clazz.setInterfaces(interfaces.toArray(new CtClass[0]));
		
		// TODO removing a field does not remove its Initializer, so if compiler is called many times, 
		// __postvayler_Id field may be initialized many times with different values,  but this is not a problem 
		// see https://issues.jboss.org/browse/JASSIST-140
	}

	private void warnIfHasUnusedAnnotations(CtClass clazz) throws Exception {
		assert (!isPersistent(clazz));
		
		if (clazz.hasAnnotation(Include.class)) {
			warning("class {0} has @Include annotation but not @Persistent itself. @Include will not be processed!", clazz.getName());
		}
		
		for (CtMethod method : clazz.getDeclaredMethods()) {
			if (method.hasAnnotation(Persist.class)) {
				warning("class {0} has @Persist method {1} but not @Persistent itself. It will not be instrumented!", clazz.getName(), method.getSignature());
			}

			if (method.hasAnnotation(Synch.class)) {
				warning("class {0} has @Synch method {1} but not @Persistent itself. It will not be instrumented!", clazz.getName(), method.getSignature());
			}
		}
	}

	
	/** checks if given class or its Persistent superclasses implements the given interface.
	 * that is, even if super class implements interface but not Persistent, this method will return false;  
	 * */
	private boolean implementedInterface(CtClass clazz, Class<?> interfaceClass) throws Exception {
		assert (clazz.getAnnotation(Persistent.class) != null);
		
		while (clazz != null) {
			if (clazz.getAnnotation(Persistent.class) == null)
				break;
			for (CtClass interfce : clazz.getInterfaces()) {
				if (interfce.getName().equals(interfaceClass.getName())) {
					return true;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private void checkTransientFieldAnnotations(CtField field) throws Exception{
		if (field.getAnnotation(Key.class) != null) 
			throw new CompileException("transient field canoot be @Key field: " + field);
		// TODO others
	}

	private void createTransaction(CtMethod method) throws Exception {
		if (method.getAnnotation(Synch.class) != null)
			throw new CompileException("@Synch and @Persist cannot be on same method" + method.getLongName());
		if (Modifier.isAbstract(method.getModifiers())) 
			throw new CompileException("abstract method cannot be @Persist " + method.getLongName());

		CtMethod copy = CtNewMethod.copy(method, method.getDeclaringClass(), null);
		String newName = "__postvayler__" + method.getName();
		copy.setName(newName);
		makeSynthetic(copy);
		makePrivate(copy);
		method.getDeclaringClass().addMethod(copy);
		System.out.println("renamed " + method.getLongName() + " to " + copy.getLongName());
		
		CtClass returnType = method.getReturnType();
				
		final String source; 
		if (returnType == CtClass.voidType) { // no return type
			source = createSource("IsPersistent.transaction.java.txt", contextClass.getName(), method.getName()); 
		} else {
			if (returnType.isPrimitive()) {
				source = createSource("IsPersistent.transactionWithQueryUnboxing.java.txt", contextClass.getName(), 
						method.getName(), getBoxingType(returnType), getUnboxingMethod(returnType)); 
			} else {
				source = createSource("IsPersistent.transactionWithQuery.java.txt", contextClass.getName(), 
						method.getName(), returnType.getName()); 
			}
		}
				
		if (DEBUG) System.out.println(source);
		method.setBody(source);
		method.getMethodInfo().rebuildStackMap(pool);
		
		// TODO remove @Persist? from both source and copy?
	}

	private String getBoxingType(CtClass primitive) {
		if (primitive == CtClass.booleanType)
			return Boolean.class.getName();
		if (primitive == CtClass.byteType)
			return Byte.class.getName();
		if (primitive == CtClass.charType)
			return Character.class.getName();
		if (primitive == CtClass.doubleType)
			return Double.class.getName();
		if (primitive == CtClass.floatType)
			return Float.class.getName();
		if (primitive == CtClass.intType)
			return Integer.class.getName();
		if (primitive == CtClass.longType)
			return Long.class.getName();
		if (primitive == CtClass.shortType)
			return Short.class.getName();
		
		throw new AssertionError("unknown primitive: " + primitive.getName());
	}

	
	private String getUnboxingMethod(CtClass primitive) {
		
		if (primitive == CtClass.booleanType)
			return "booleanValue()";
		if (primitive == CtClass.byteType)
			return "byteValue()";
		if (primitive == CtClass.charType)
			return "charValue()";
		if (primitive == CtClass.doubleType)
			return "doubleValue()";
		if (primitive == CtClass.floatType)
			return "floatValue()";
		if (primitive == CtClass.intType)
			return "intValue()";
		if (primitive == CtClass.longType)
			return "longValue()";
		if (primitive == CtClass.shortType)
			return "shortValue()";
		
		throw new AssertionError("unknown primitive: " + primitive.getName());
	}
	
	private void createSynch(CtMethod method) throws Exception {
		if (method.getAnnotation(Persist.class) != null)
			throw new CompileException("@Synch and @Persist cannot be on same method" + method.getLongName());
		if (Modifier.isAbstract(method.getModifiers())) 
			throw new CompileException("abstract method cannot be @Synch " + method.getLongName());

		CtMethod copy = CtNewMethod.copy(method, method.getDeclaringClass(), null);
		String newName = "__postvayler__" + method.getName();
		copy.setName(newName);
		makePrivate(copy);
		method.getDeclaringClass().addMethod(copy);
		System.out.println("renamed " + method.getLongName() + " to " + copy.getLongName());
		
		boolean hasReturnType = (CtClass.voidType != method.getReturnType());
		
		String source = hasReturnType 
				? createSource("IsPersistent.synch.java.txt", contextClass.getName(), method.getName()) 
				: createSource("IsPersistent.synchVoid.java.txt", contextClass.getName(), method.getName());
		
		if (DEBUG) System.out.println(source);
		method.setBody(source);
		method.getMethodInfo().rebuildStackMap(pool);
		
		// TODO remove @Persist? from both source and copy?
	}
	
	// TODO this does not correctly handle arrays
	private String getParams(CtMethod method) throws Exception {
		List<String> params = new ArrayList<String>();
		for (CtClass paramType : method.getParameterTypes()) {
			if (paramType.isArray()) {
				params.add("\"L" + paramType.getComponentType().getName() + ";\"");
			} else {
				params.add("\"" + paramType.getName() + "\"");
			}
			System.out.println("---" + paramType.getName() + "  comp: " + paramType.getComponentType());
		}
		String paramS = params.toString();
		System.out.println("sig: new String[] {" + paramS.substring(1, paramS.length()-1) +  "}");
		return "new String[] {" + paramS.substring(1, paramS.length()-1) +  "}";
	}
	
	private String getMethodWrapperSource(CtMethod method) throws Exception {
		String s = "new raft.postvayler.impl.MethodWrapper(";
		s += "\"" + method.getName() + "\", ";
		s += "\"" + method.getDeclaringClass().getName() + "\", ";
		
		List<String> params = new ArrayList<String>();
		for (CtClass paramType : method.getParameterTypes()) {
			params.add("\"" + paramType.getName() + "\"");
		}
		String paramS = params.toString();
		s += " new String[] {" + paramS.substring(1, paramS.length()-1) +  "})";
//		s += " null)";
		
		return s;
	}
	
	private String getClassWriteDir(CtClass clazz) throws Exception {
		String className = clazz.getName();
		String path = pool.find(className).toURI().toString();
		int index = path.indexOf(className.replace('.', '/') + ".class");
		if (index < 0)
			throw new IllegalStateException("couldnt find class dir in path " + path);
		
		if (!path.startsWith("file:/"))
			throw new IllegalStateException("class location is not a flat file: " + path);
		
		File dir = new File(new URI(path.substring(0, index)));
//		System.out.println(dir);
		return dir.getName();
	}
	
	private List<CtClass> getPackageClasses(CtClass clazz) throws Exception {
		String className = clazz.getName();
		String path = pool.find(className).toURI().toString();
		int index = path.indexOf(className.replace('.', '/') + ".class");
		if (index < 0)
			throw new IllegalStateException("couldnt find class dir in path " + path);
		
		List<CtClass> result = new LinkedList<CtClass>();
		String packageName = clazz.getPackageName();
		
		if (path.startsWith("file:/")) {
			File classFile = new File(new URI(path));
			File packageDir = classFile.getParentFile();
			
			for (String file : packageDir.list()) {
				if (!file.endsWith(".class"))
					continue;
				
				String clsName = packageName + "." + file.substring(0, file.length() - 6); // omit the .class part
				result.add(pool.get(clsName));
			}			
		} else if (path.startsWith("jar:file:/")) {
			String packagePath = packageName.replace('.', '/');
			
			String jarPath = URLDecoder.decode(path, "UTF-8");
			jarPath = jarPath.substring(9, jarPath.lastIndexOf('!'));
			
			JarFile jarFile = new JarFile(jarPath);
			try {
				Enumeration<JarEntry> e = jarFile.entries();
				while (e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					
					if (entry.isDirectory()) 
						continue;
					
					String name = entry.getName(); 
					if (!name.startsWith(packagePath) || !name.endsWith(".class"))
						continue;
					
					String subPart = name.substring(packagePath.length() + 1, name.length() - 6); // omit the .class part
					if (!scanSubPackages && (subPart.indexOf('/') >= 0))
						continue;
					
					String clsName = packageName + "." + subPart.replace('/', '.');
					result.add(pool.get(clsName));
				}
			} finally {
				jarFile.close();
			}
		} else { 
			throw new IllegalStateException("could not scan package: " + path);
		}
		
		return result;
	}

	
	private static <T extends CtBehavior> T makePrivate(T behavior) throws Exception {
		behavior.setModifiers(Modifier.setPrivate(behavior.getModifiers()));
		return behavior;
	}
	
	private static <T extends CtBehavior> T makeSynthetic(T behavior) throws Exception {
		MethodInfo info = behavior.getMethodInfo();
		info.setAccessFlags(info.getAccessFlags() | AccessFlag.SYNTHETIC);
		info.addAttribute(new SyntheticAttribute(info.getConstPool()));
		return behavior;
	}
	
	private static <T extends CtField> T makeSynthetic(T field) throws Exception {
		FieldInfo info = field.getFieldInfo();
		info.setAccessFlags(info.getAccessFlags() | AccessFlag.SYNTHETIC);
		info.addAttribute(new SyntheticAttribute(info.getConstPool()));
		return field;
	}
	
	private static String getClassNameForJavaIdentifier(String className) {
		return className.replace('.', '_').replace('$', '_');
	}

	private String createSource(String fileName, Object... arguments) throws Exception {
		return MessageFormat.format(readFile(fileName), arguments);
	}
	
	final Map<String, String> fileCache = new HashMap<String, String>();
	
	private String readFile(String fileName) throws Exception {
		String cached = fileCache.get(fileName);
		if (cached != null)
			return cached;
		
		InputStream in = getClass().getResourceAsStream(fileName);
		if (in == null)
			throw new FileNotFoundException(fileName);
		
		try {
			String content = new String();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = reader.readLine()) != null) {
				content += line + "\n";
			}
			fileCache.put(fileName, content);
			return content;
			
		} finally {
			in.close();
		}
	}

	private static void warning(String pattern, Object... arguments) {
		System.err.println("WARNING: " + MessageFormat.format(pattern, arguments));
	}

	
	/** returns true if this class or any of it's super classes has @Persistent annotation */
	private static boolean isPersistent(CtClass clazz) throws Exception {
		CtClass supr = clazz;
		while (supr != null) {
			if (supr.hasAnnotation(Persistent.class))
				return true;
			supr = supr.getSuperclass();
		}
		return false;
	}
	
	private static List<CtClass> getPersistentHierarchy(CtClass clazz) throws Exception {
		List<CtClass> list = new LinkedList<CtClass>();
		
		while (isPersistent(clazz)) {
			list.add(0, clazz);
			clazz = clazz.getSuperclass();
		}
		return list;
	}
	
	private static class Tree {
		
		final Map<String, Node> roots = new TreeMap<String, Node>();
		
		List<CtClass> add(CtClass clazz) throws Exception {
			List<CtClass> hierarchy = getPersistentHierarchy(clazz);
			
			CtClass mostSuper = hierarchy.get(0);
			
			Node rootNode = roots.get(mostSuper.getName());
			if (rootNode == null) {
				rootNode = new Node(mostSuper);
				roots.put(mostSuper.getName(), rootNode);
			}
			
			rootNode.add(hierarchy);
			return hierarchy;
		}

		Node findNode(CtClass clazz) {
			Node root = roots.get(clazz.getName());
			if (root != null)
				return root;
			
			for (Node node : roots.values()) {
				Node n = node.findNode(clazz);
				if (n != null)
					return n;
			}
			
			return null;
		}

		void print(PrintStream out) {
			for (Node node : roots.values()) {
				node.print(out, 0);
			}
		}

	}	
	
	/** a node in class tree */
	private static class Node {
		final CtClass clazz;
		final Map<String, Node> subClasses = new TreeMap<String, Node>();
		boolean compiled;
		boolean isRoot;

		private Node(CtClass clazz) {
			this.clazz = clazz;
		}

		void add(List<CtClass> hierarchy) {
			assert (hierarchy.get(0) == clazz);
			
			if (hierarchy.size() == 1)
				return;
			
			CtClass subClass = hierarchy.get(1);
			Node subNode = subClasses.get(subClass.getName());
			
			if (subNode == null) {
				subNode = new Node(subClass);
				subClasses.put(subClass.getName(), subNode);
			}
			
			subNode.add(hierarchy.subList(1, hierarchy.size()));
		}
		
		Node findNode(CtClass clz) {
			if (clazz == clz)
				return this;
			
			Node sub = subClasses.get(clz.getName());
			if (sub != null)
				return sub;
			
			for (Node node : subClasses.values()) {
				Node n = node.findNode(clz);
				if (n != null)
					return n;
			}
			
			return null;
		}
		
		void print(PrintStream out, int indentation) {
			out.println(indent(indentation) + clazz.getName() + (isRoot ? " (*)" : ""));
			
			for (Node subNode : subClasses.values()) {
				subNode.print(out, indentation + 4);
			}
		}


		private String indent(int count) {
			String s = "";
			for (int i = 0; i < count; i++) {
				s += " ";
			}
			return s;
		}
	}
}
