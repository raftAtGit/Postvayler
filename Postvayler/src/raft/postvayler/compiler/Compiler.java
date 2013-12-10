package raft.postvayler.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SignatureAttribute.ObjectType;
import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Storage;
import raft.postvayler.Synch;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.IsRoot;
import raft.postvayler.inject.Key;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class Compiler {

	private static final CtClass CLASS_COLLECTION, CLASS_MAP; 
	
	private static final List<CtClass> KNOWN_CONTAINER_TYPES;
	
	static {
		ClassPool pool = ClassPool.getDefault();
		List<CtClass> list = new ArrayList<CtClass>();
		try {
			CLASS_COLLECTION = pool.get(Collection.class.getName()); 
			CLASS_MAP = pool.get(Map.class.getName());
			
			list.add(CLASS_COLLECTION);
			list.add(CLASS_MAP);
			
		} catch (NotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
		KNOWN_CONTAINER_TYPES = Collections.unmodifiableList(list);
	}
	
	// TODO somehow must check if a class is already enhanced
	
	public static void main(String[] args) throws Exception {
		String rootClass = args[0];
		
		Compiler compiler = new Compiler(rootClass);
		compiler.run();
	}
	
	private final String rootClass;
//	private final String outputFolder;
	private CtClass contextClass;

	
	private final ClassPool pool;
	
	private final Set<String> processedClasses = new HashSet<String>();
	
	private final Set<String> processedPackages = new HashSet<String>();
	private final Set<String> packageQueue = new LinkedHashSet<String>();

	private final Set<String> scannedClasses = new HashSet<String>();
	
	public Compiler(String rootClass) {
		this.rootClass = rootClass; 
//		this.outputFolder = outputFolder;
		
		this.pool = ClassPool.getDefault();
		
		pool.importPackage("raft.postvayler");
		pool.importPackage("raft.postvayler.impl");
	}
	
	public void run() throws Exception {
		CtClass clazz = pool.get(rootClass);
		if (clazz.getAnnotation(Persistent.class) == null)
			throw new CompileException("root class " + rootClass + " is not annotated with @Persistent");
		
		if (clazz.isInterface())
			throw new CompileException("root class " + rootClass + " is an Interface: rootClass");
		if (Modifier.isAbstract(clazz.getModifiers())) 
			throw new CompileException("root class is Abstract: " + rootClass);
	
		if (clazz.getPackageName() == null)
			throw new CompileException("@Persistent classes in default package is not supported " + clazz.getName());
		
		Tree tree = new Tree();
		createTree(tree, clazz);
		tree.print(System.out);
		
		System.out.println("-- scanned --");
		for (String scanned : scannedClasses)
			System.out.println(scanned);
		
		System.out.println("-- 	queued --"); 
		for (String queued : packageQueue)
			System.out.println(queued);
		
		createContextClass();
		
		instrumentClass(clazz);
	}

	private void createContextClass() throws Exception {
		// TODO we need better encapsulation for pravayler and inTransaction fields
		
		CtClass rootClazz = pool.get(rootClass);
		contextClass = pool.makeClass(rootClazz.getPackageName() + ".__Postvayler");
		contextClass.setSuperclass(pool.get(Context.class.getName()));
		
		contextClass.addField(CtField.make("public static final Class rootClass = " + rootClass + ".class;", contextClass));
		contextClass.addConstructor(CtNewConstructor.make(createSource("Context.init.java.txt", rootClass), contextClass));
		
		contextClass.writeFile(getClassWriteDir(rootClazz));
	}

	private void createTree(Tree tree, CtClass clazz) throws Exception {
		String rootDir = getClassWriteDir(clazz);
		String packageName = clazz.getPackageName();
		File packageDir = new File(rootDir, packageName.replace('.', '/'));
		
		processedPackages.add(packageName);
		
		for (String file : packageDir.list()) {
			if (!file.endsWith(".class"))
				continue;
			
			CtClass cls = pool.get(packageName + "." + file.substring(0, file.length() - 6)); // omit the .class part
//			processedClasses.add(cls.getName());
			
			if (cls.isInterface() || !isPersistent(cls))
				continue;

			List<CtClass> hierarchy = tree.add(cls);
			
			for (CtClass hClass : hierarchy) {
				// TODO check class is not a inner class. inner classes cannot be created via reflection (sure?)
				String hPackage = hClass.getPackageName();
				if (!processedPackages.contains(hPackage))
					packageQueue.add(hPackage);
				
				scanClass(hClass);
			}
		}
	}
	
	private void scanClass(CtClass clazz) throws Exception {
		scannedClasses.add(clazz.getName());
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
			if (!processedPackages.contains(fieldPackage))
				packageQueue.add(fieldPackage);
		} 
		
		if (fieldClass.isArray()) {
			CtClass arrayClass = fieldClass;
			while (arrayClass.isArray()) {
				arrayClass = arrayClass.getComponentType();
			}
			scanField(declaringClass, arrayClass);
		}
	}	

	private void instrumentClass(CtClass clazz) throws Exception {
		if (processedClasses.contains(clazz.getName()))
			return;
		
		processedClasses.add(clazz.getName());
		
		if (clazz.getAnnotation(Persistent.class) == null) {
			System.out.println("skipping none @Persistent class " + clazz.getName());
			return;
		}
		
		if (clazz.getPackageName() == null)
			throw new CompileException("@Persistent classes in default package is not supported " + clazz.getName());
		packageQueue.add(clazz.getPackageName());
		
		// start with superclass, so if super class is persistent, IsPersistent and id will be injected into that
		CtClass superClass = clazz.getSuperclass();
		while (superClass != null) {
			if (!processedClasses.contains(superClass.getName())) {
				instrumentClass(superClass);
			}
			superClass = superClass.getSuperclass();
		}
		
		clean(clazz);
		
		injectInterfaces(clazz);
		
		addToPool(clazz);
		

		
		for (CtField field : clazz.getDeclaredFields()) {
			System.out.println("field: " + field);

			if (Modifier.isTransient(field.getModifiers())) {
				checkTransientFieldAnnotations(field);
				continue;
			}
			
			Key key = (Key) field.getAnnotation(Key.class);
			if (key != null) {
				Class<?>[] parents = key.value();
				System.out.println("key field: " + field.getName() + ", parents: " + Arrays.toString(parents));
			}
			
			CtClass fieldClass = field.getType();
			if (isContainer(fieldClass)) {
				System.out.println("container field " + field);
				CtClass childClass = determineChildType(field);
			} else {
				instrumentClass(fieldClass);
			}
		}
		
		for (CtMethod method : clazz.getDeclaredMethods()) {
			System.out.println("method: " + method);

			Persist persist = (Persist) method.getAnnotation(Persist.class);
			if (persist != null) {
				createTransaction(method);
			}

			Synch synch = (Synch) method.getAnnotation(Synch.class);
			if (synch != null) {
				createSynch(method);
			}
		}
		
		// add a static final field for Root class to mark this class as enhanced
		String classSuffix = getClassNameForJavaIdentifier(clazz.getName());
		clazz.addField(CtField.make("public static final String __postvayler_root_" + classSuffix + " = \"" + rootClass + "\";", clazz));
		
		
		String dir = getClassWriteDir(clazz);
		System.out.println("writing class " + clazz.getName() + " to " + dir);
		clazz.writeFile(dir);
		processedClasses.add(clazz.getName());

	}

	/** removes all existing instrumentation by Postvayler. this is necessary to allow running compiler on same classes again */
	private void clean(CtClass clazz) throws Exception {
		for (CtField field : clazz.getDeclaredFields()) {
			if (field.getName().startsWith("__postvayler_")) {
				clazz.removeField(field);
				System.out.println("removed old field " + clazz.getName() + "." + field.getName());
			}
		}
		for (CtMethod method : clazz.getDeclaredMethods()) {
			if (method.getName().startsWith("__postvayler_")) { 
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

	private void injectInterfaces(CtClass clazz) throws Exception {
		if (rootClass.equals(clazz.getName())) {
			if (!implementedInterface(clazz, IsRoot.class)) {
				clazz.addInterface(pool.get(IsRoot.class.getName()));
				System.out.println("added IsRoot interface to " + clazz.getName());
			}
			if (!implementedInterface(clazz, Storage.class)) {
				clazz.addInterface(pool.get(Storage.class.getName()));
				System.out.println("added Storage interface to " + clazz.getName());
			}
			
			clazz.addField(CtField.make("private final Pool __postvayler_pool = new Pool();", clazz));
			System.out.println("added Pool __postvayler_pool field to " + clazz.getName());

			// implement the IsRoot interface
			clazz.addMethod(CtNewMethod.make("public final IsPersistent __postvayler_get(Long id) { return __postvayler_pool.get(id);}", clazz));
			System.out.println("added IsPersistent __postvayler_get(Long id) method to " + clazz.getName());
			
			clazz.addMethod(CtNewMethod.make("public final Long __postvayler_put(IsPersistent persistent) { return __postvayler_pool.put(persistent);}", clazz));
			System.out.println("added void __postvayler_put(IsPersistent persistent) method to " + clazz.getName());
			
			clazz.addMethod(CtNewMethod.make("public final void __postvayler_onRecoveryCompleted() { __postvayler_pool.switchToWeakValues();}", clazz));
			System.out.println("added void __postvayler_onRecoveryCompleted() method to " + clazz.getName());
			
			String source = createSource("IsRoot.takeSnapshot.java.txt", contextClass.getName());
			System.out.println(source);
			clazz.addMethod(CtNewMethod.make(source, clazz));
			System.out.println("added public File takeSnapshot() method to " + clazz.getName());
			
		} // if root class

		if (!implementedInterface(clazz, IsPersistent.class)) {
			
			if (rootClass.equals(clazz.getName())) {
				clazz.addField(CtField.make("private final Long __postvayler_Id = __postvayler_pool.put(this);", clazz));
				System.out.println("added Long __postvaylerId field to " + clazz.getName());
				
			} else {
				clazz.addInterface(pool.get(IsPersistent.class.getName()));
				System.out.println("added IsPersistent interface to " + clazz.getName());
				
				clazz.addField(CtField.make("protected Long __postvayler_Id;", clazz));
				System.out.println("added Long __postvaylerId field to " + clazz.getName());
				
				String validateSource = createSource("IsPersistent.init.validateClass.java.txt", contextClass.getName());
						
				// add code to validate runtime type
				for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
					// TODO optimization: omit validate call if there is a call to this(constructor) 
					constructor.insertAfter(validateSource);
					System.out.println("added validateClass call to " + constructor.getLongName());
				}
			}
			
			// implement the IsPersistent interface
			clazz.addMethod(CtNewMethod.make("public final Long __postvayler_getId() { return __postvayler_Id;}", clazz));
			System.out.println("added Long __postvayler_getId() method to " + clazz.getName());
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

	/** injects code to constructors to put this object into pool */
	private void addToPool(CtClass clazz) throws Exception {
		if (clazz.getName().endsWith(rootClass))
			return;
		
		if (Modifier.isAbstract(clazz.getModifiers()))
			return;
		
		String source = createSource("IsPersistent.init.putToPool.java.txt", contextClass.getName(), clazz.getName());
		
		for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
			// TODO optimization: omit validate call if there is a call to this(constructor) 
			constructor.insertAfter(source);
			System.out.println("added add to pool call to " + constructor.getLongName());
		}
	}
	
	private void checkTransientFieldAnnotations(CtField field) throws Exception{
		if (field.getAnnotation(Key.class) != null) 
			throw new CompileException("transient field canoot be @Key field: " + field);
		// TODO others
	}

	private boolean isContainer(CtClass clazz) throws Exception {
		if (clazz.isArray()) 
			return true;
		
		for (CtClass containerClass : KNOWN_CONTAINER_TYPES) {
			if (clazz.subtypeOf(containerClass))
				return true;
			
		}
		return false;
	}

	private CtClass determineChildType(CtField field) throws Exception {
		CtClass fieldClass = field.getType();
		
		if (fieldClass.isArray()) 
			return fieldClass.getComponentType();
		
		System.out.println("--Generic: " + field.getGenericSignature());
		SignatureAttribute.ClassType genericType = (SignatureAttribute.ClassType) 
				SignatureAttribute.toFieldSignature(field.getGenericSignature()); 
		System.out.println("--" + Arrays.toString(genericType.getTypeArguments()));
		
		for (SignatureAttribute.TypeArgument type : genericType.getTypeArguments()) {
			System.out.println("---" + type.getType() + ": " + type.getType().getClass());
		}
		
		
		return null;
	}

	private void createTransaction(CtMethod method) throws Exception {
		if (method.getAnnotation(Synch.class) != null)
			throw new CompileException("@Synch and @Persist cannot be on same method" + method.getLongName());
		if (Modifier.isAbstract(method.getModifiers())) 
			throw new CompileException("abstract method cannot be @Persist " + method.getLongName());

		CtMethod copy = CtNewMethod.copy(method, method.getDeclaringClass(), null);
		String newName = "__postvayler__" + method.getName();
		copy.setName(newName);
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
				
		System.out.println(source);
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
		
		// TODO Unboxing for primitive return types

		String source = hasReturnType 
				? createSource("IsPersistent.synch.java.txt", contextClass.getName(), method.getName()) 
				: createSource("IsPersistent.synchVoid.java.txt", contextClass.getName(), method.getName());
		
		System.out.println(source);
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
	
	private static <T extends CtBehavior> T makePrivate(T behavior) throws Exception {
		behavior.setModifiers(Modifier.setPrivate(behavior.getModifiers()));
		return behavior;
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
		
		void print(PrintStream out, int indentation) {
			out.println(indent(indentation) + clazz.getName());
			
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
