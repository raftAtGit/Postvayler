package raft.postvayler.compiler;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.SignatureAttribute;

import org.prevayler.Prevayler;

import raft.postvayler.IsLocator;
import raft.postvayler.Key;
import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.IsRoot;

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
//	private final Map<String, String> locatorMethods = new TreeMap<String, String>();
	
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
		
		contextClass.addField(setModifiers(new CtField(contextClass, "instance", contextClass), 
				Modifier.PRIVATE | Modifier.STATIC));
		
		contextClass.addField(setModifiers(new CtField(pool.get(Prevayler.class.getName()), "prevayler", contextClass), 
				Modifier.PUBLIC | Modifier.FINAL));
		
		contextClass.addField(setModifiers(new CtField(rootClazz, "root", contextClass), 
				Modifier.PUBLIC | Modifier.FINAL));
		
		contextClass.addField(setModifiers(new CtField(CtClass.booleanType, "inTransaction", contextClass), 
				Modifier.PUBLIC));
		
		contextClass.addConstructor(setModifiers(CtNewConstructor.make("__Postvayler(org.prevayler.Prevayler prevayler, " + rootClass + " root) {" +
				"synchronized (" + contextClass.getName() + ".class) {" +
				"  if (instance != null) throw new IllegalStateException(\"an instance already created\");" +
				"  this.prevayler = prevayler;" + 
				"  this.root = root;" +
				"  instance = this;" + 
				"}" + 
				"}", contextClass), Modifier.PUBLIC));
		
		contextClass.addMethod(CtNewMethod.make(Modifier.PUBLIC | Modifier.STATIC, 
				contextClass, "getInstance", null, null, "return instance;", contextClass));
		
		contextClass.addMethod(CtNewMethod.make(Modifier.PUBLIC | Modifier.STATIC, 
				CtClass.booleanType, "isBound", null, null, "return (instance != null);", contextClass));
		
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
		
//		// loop once to check what class requires 
//		boolean needLocator = false;
//		for (CtMethod method : clazz.getDeclaredMethods()) {
//			if (method.getAnnotation(Persist.class) != null)
//				needLocator = true;
//		}		
//		if (needLocator) {
//			findOrCreateLocator(clazz);
//		}
		
		
		for (CtMethod method : clazz.getDeclaredMethods()) {
			System.out.println("method: " + method);

			Persist t = (Persist) method.getAnnotation(Persist.class);
			if (t != null) {
				createTransaction(method);
			}
		}
		
		// add a static final field for Root class to mark this class as enhanced
		clazz.addField(CtField.make("public static final String __postvayler_root = \"" + rootClass + "\";", clazz));
		
		
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
		
		List<CtClass> interfaces = new ArrayList<CtClass>(Arrays.asList(clazz.getInterfaces()));
		for (Iterator<CtClass> i = interfaces.iterator(); i.hasNext();) {
			CtClass inttf = i.next();
			if (inttf.getName().equals(IsPersistent.class.getName()) || 
					inttf.getName().equals(IsRoot.class.getName())) {
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
			clazz.addField(CtField.make("private final Pool __postvayler_pool = new Pool();", clazz));
			System.out.println("added Pool __postvayler_pool field to " + clazz.getName());

			// implement the IsRoot interface
			clazz.addMethod(CtNewMethod.make("public IsPersistent __postvayler_get(Long id) { return __postvayler_pool.get(id);}", clazz));
			System.out.println("added IsPersistent __postvayler_get(Long id) method to " + clazz.getName());
			
			clazz.addMethod(CtNewMethod.make("public void __postvayler_put(IsPersistent persistent) { __postvayler_pool.put(persistent);}", clazz));
			System.out.println("added void __postvayler_put(IsPersistent persistent) method to " + clazz.getName());
			
			clazz.addMethod(CtNewMethod.make("public Long __postvayler_getNextId() { return __postvayler_pool.getNextId();}", clazz));
			System.out.println("added Long __postvayler_getNextId() method to " + clazz.getName());
			
			// add code to add this object to pool
			for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
				constructor.insertAfter("__postvayler_put(this);");
			}
			System.out.println("added __postvayler_put(this) call to " + clazz.getDeclaredConstructors().length + " constructor(s) for class " + clazz.getName());
			
		}

		if (!implementedInterface(clazz, IsPersistent.class)) {
			
			if (rootClass.equals(clazz.getName())) {
				clazz.addField(CtField.make("private final Long __postvayler_Id = __postvayler_getNextId();", clazz));
				System.out.println("added Long __postvaylerId field to " + clazz.getName());
				
			} else {
				clazz.addInterface(pool.get(IsPersistent.class.getName()));
				System.out.println("added IsPersistent interface to " + clazz.getName());
				
				String source = 
						"private static final Long __postvayler_createId() { \n" +
							"if (!" + contextClass.getName() + ".isBound()) { \n" +
							"  System.out.println(\"postvayler context not bound, no id will be given to object\"); \n" + // TODO log
							"  return null; \n" + 
							"} \n" +
							// __postvayler_getNextId should also should be wrapped in a transaction
							"Long id = (Long) " + contextClass.getName() + ".getInstance().prevayler.execute(new GetNextIdTransaction()); \n" +
							"System.out.println(\"created id \" + id + \" for object " + clazz.getName() + "\"); \n" +
							"return id; \n" + 
						"}";
				System.out.println(source);
				clazz.addMethod(CtNewMethod.make(source, clazz));
				System.out.println("added Long __postvayler_createId method to " + clazz.getName());
				
				clazz.addField(CtField.make("private final Long __postvayler_Id = __postvayler_createId();", clazz));
				System.out.println("added Long __postvaylerId field to " + clazz.getName());
				
				source = 
						"private void __postvayler_maybeAddToPool() { \n" +
							"if (__postvayler_Id != null) { \n" +
							// __postvayler_put should also should be wrapped in a transaction
							"  " + contextClass.getName() + ".getInstance().prevayler.execute(new PutObjectTransaction(this)); \n" +
							"} \n" +
						"}";
				System.out.println(source);
				clazz.addMethod(CtNewMethod.make(source, clazz));
				System.out.println("added void __postvayler_maybeAddToPool() method to " + clazz.getName());
				
				// add code to add this object to pool
				for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
					constructor.insertAfter("__postvayler_maybeAddToPool();");
				}
				System.out.println("added __postvayler_maybeAddToPool() call to " + clazz.getDeclaredConstructors().length + " constructor(s) for class " + clazz.getName());
			}
			
			// implement the IsPersistent interface
			clazz.addMethod(CtNewMethod.make("public Long __postvayler_getId() { return __postvayler_Id;}", clazz));
			System.out.println("added Long __postvayler_getId() method to " + clazz.getName());

		}
	}

	
//	private void findOrCreateLocator(CtClass clazz) throws Exception {
//		List<CtMethod> foundMethods = new LinkedList<CtMethod>();
//		
//		for (CtMethod method : clazz.getDeclaredMethods()) {
//			if (method.getAnnotation(IsLocator.class) != null) {
//				if (method.getReturnType() != pool.get(Locator.class.getName()))
//					throw new CompileException("return type of @IsLocator method should be " + Locator.class.getName() + " in " + method.getLongName());
//				// TODO check generic types of return type Locator
//				if (method.getParameterTypes().length > 0)
//					throw new CompileException("@IsLocator method cannot have arguments " + method.getLongName());
//				
//				foundMethods.add(method);
//			}
//		}
//		if (foundMethods.size() > 1)
//			throw new CompileException("multiple @IsLocator methods in class " + clazz.getName());
//		
//		if (clazz.getName().equals(rootClass)) {
//			if (!foundMethods.isEmpty()) {
//				// TODO log
//				System.err.println("warning: root class " + rootClass + " has a @IsLocator method, this is not necessary and it wont be used.");
//			} 
//			String source = 
//					"private static final Locator __postvayler_locator() {" +
//					"  return new Locators$Identity();" +
//					"}"; 
//			System.out.println(source);
//			clazz.addMethod(CtNewMethod.make(source, clazz));
//			locatorMethods.put(clazz.getName(), "__postvayler_locator");
//			
//		} else {
//		
//			if (foundMethods.isEmpty()) {
//				// TODO auto locator
//				throw new CompileException("no @IsLocator method in class " + clazz.getName());
//			}
//			locatorMethods.put(clazz.getName(), foundMethods.get(0).getName());
//			
//		}
//		
//		// create validate locator method
//		String source = 
//				"private final void __postvayler_validate_locator(Locator locator) { \n" +
//				"   Locator copy = (Locator) org.prevayler.foundation.DeepCopier#deepCopy(locator); \n" +
//				"   " + contextClass.getName() + " context = " + contextClass.getName() + ".getInstance(); \n" +
//				"   Object located = copy.findObject(context.root); \n" +
//				"   if (located == null) \n" +
//				"       throw new IllegalStateException(\"copy of locator returned a null object, are you suffering from Baptism problem?\"); \n" +
//				"   if (located != this) \n" +
//				"       throw new IllegalStateException(\"copy of locator returned a different instance, are you suffering from Baptism problem?\"); \n" +
//				"}";
//		System.out.println(source);
//		clazz.addMethod(CtMethod.make(source, clazz));
//	}

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
		if (Modifier.isAbstract(method.getModifiers())) 
			throw new CompileException("abstract method cannot be @Transaction " + method.getLongName());
		if (method.getAnnotation(IsLocator.class) != null)
			throw new CompileException("@Transaction and @IsLocator cannot be used for same method " + method.getLongName());

		CtMethod copy = CtNewMethod.copy(method, method.getDeclaringClass(), null);
		String newName = "__postvayler__" + method.getName();
		System.out.println("renaming Transaction method to: " + newName);
		copy.setName(newName);
		makePrivate(copy);
		method.getDeclaringClass().addMethod(copy);
		
		boolean hasReturnType = (CtClass.voidType != method.getReturnType());
		String origCallStatement = hasReturnType 
				? "return " + newName + "($$); \n" 
				: newName + "($$); return; \n";   
		
		// TODO Unboxing for primitive return types
		
		String txCallStatement = hasReturnType 
				? "return (" + method.getReturnType().getName() + ") context.prevayler.execute(new MethodTransactionWithQuery((IsPersistent)this, method, $args)); \n" 
				: "context.prevayler.execute(new MethodTransaction((IsPersistent)this, method, $args)); \n";
		
//		String locatorMethod = locatorMethods.get(method.getDeclaringClass().getName());
//		assert (locatorMethod != null);
		
		String source = "{ \n" +
				"System.out.println(\"" + method.getLongName() + "\"); \n\n" + // TODO log
				
				"if (!" + contextClass.getName() + ".isBound()) { \n" +
				"  System.out.println(\"postvayler context not bound, proceeding to original method " + copy.getLongName() + "\"); \n" + // TODO log
				"  " + origCallStatement + 
				"} \n" + 
				
				// get a reference to context
				contextClass.getName() + " context = " + contextClass.getName() + ".getInstance(); \n" +
				
				"synchronized (context.root) { \n" +
				"    if (context.inTransaction) { \n" +
				"        System.out.println(\"already in transaction, proceeding to original method " + copy.getLongName() + "\"); \n" + // TODO log
				"        " + origCallStatement + 
				"    } \n" +
					  
				"    MethodWrapper method = new MethodWrapper(\"" + newName + "\", $class, $sig" + "); \n" + // TODO do not use $sig, it requires javassist.runtime.Desc at runtime  
//				"    MethodWrapper method = new MethodWrapper(\"" + newName + "\", $class, " + getParams(method) + "); \n" +  
				
//				"    Locator locator = " + locatorMethod + "(); \n\n" +
//				"    if (locator == null) { \n" +
//				"        System.out.println(\"locator method " + locatorMethod + " returned null, proceeding to original method " + copy.getLongName() + "\"); \n" + // TODO log
//				"        " + origCallStatement + 
//				"    } \n" +
//				"    __postvayler_validate_locator(locator); \n" +
//				"    if (!__postvayler_validate_locator(locator)) { \n" +
//				"        System.out.println(\"locator returned null, proceeding to original method " + copy.getLongName() + "\"); \n" + // TODO log
//				"        " + origCallStatement + 
//				"    } \n" +  	 
				
				"    context.inTransaction = true; \n" +  
				"    try { \n" +
				"        System.out.println(\"running prevayler transaction for " + copy.getLongName() + "\"); \n" +
				"        " + txCallStatement + 
				"    } finally { \n" +
				"       context.inTransaction = false; \n" +  
				"    } \n" + 
				
				
				"} // synchronized \n" +
				

				"}";
		
		
		
		
		// TODO make new method source depending on returun type
		
		System.out.println(source);
		method.setBody(source);
		method.getMethodInfo().rebuildStackMap(pool);
		
		// TODO remove @Transaction? from both source and copy?
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
	
	private static <T extends CtMember> T setModifiers(T member, int modifiers) throws Exception {
		member.setModifiers(modifiers);
		return member;
	}
}
