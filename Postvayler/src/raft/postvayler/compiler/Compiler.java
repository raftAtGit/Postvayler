package raft.postvayler.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		
		Compiler compiler = new Compiler(rootClass, "./bin");
		compiler.run();
	}
	
	private final String rootClass;
//	private final String outputFolder;
	private CtClass contextClass;

	
	private final ClassPool pool;
	
	private final Set<String> processedClasses = new TreeSet<String>();
	private final Set<String> processedPackages = new TreeSet<String>();
	
	private final Set<String> packageQueue = new LinkedHashSet<String>();
	
	public Compiler(String rootClass, String outputFolder) {
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

	private void instrumentClass(CtClass clazz) throws Exception {
		if (processedClasses.contains(clazz.getName()))
			return;
		
		processedClasses.add(clazz.getName());
		
		if (clazz.getAnnotation(Persistent.class) == null) {
			System.out.println("skipping none @Persistent class " + clazz.getName());
			return;
		}
		
		if (clazz.getPackageName() == null)
			throw new CompileException("@Persistent classes in default package is not supported");
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
}
