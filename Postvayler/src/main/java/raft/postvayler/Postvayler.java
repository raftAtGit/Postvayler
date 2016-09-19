package raft.postvayler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import raft.postvayler.impl.GCPreventingPrevayler;
import raft.postvayler.impl.InitRootTransaction;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.RootHolder;

/**
 * <p>Entry point to create a @Persistent object. After @Persistent classes is instrumented 
 * (i.e. they are compiled with Postvayler compiler), it's enough to call 
 * <code>Postvayler.create(MyRoot.class)</code> to get persisted instance of <code>MyRoot</code> class.</p>
 * 
 * <p>Postvayler persists an <a href="http://en.wikipedia.org/wiki/Object_graph">object graph</a> 
 * via <a href="http://prevayler.org/">prevalence</a>. MyRoot here denotes the entry point of 
 * the object graph. It may be the root node of a tree, a container class around other data structures
 * or something else.</p>
 * 
 * <p>Postvayler compiler instruments classes in regard to root class. In runtime, 
 * {@link Postvayler#create(Class)} should be called with the same root class or with an instance of it.  
 *  This suggests there can be only one root instance per JVM (indeed per {@link ClassLoader}).</p>
 *  
 *  <p>Postvayler creates {@link Prevayler} with default settings and 
 *  <i>./persist/root.class.qualified.Name</i> directory as prevalance directory. These can be 
 *  configured by passing a {@link PrevaylerFactory} instance to Postvayler before calling {@link #create()}.</p>
 * 
 * @see Persistent
 * @see Persist
 * @see PrevaylerFactory
 * 
 * @author r a f t
 */
public class Postvayler<T> {

	private static Postvayler<?> instance;
	
	private final Class<T> rootClass;
	
	private PrevaylerFactory<RootHolder> factory;
	
	public Postvayler(Class<T> rootClass) throws Exception {
		this.rootClass = rootClass;
	}
	
	@SuppressWarnings("unchecked")
	public T create() throws Exception {
		synchronized (Postvayler.class) {
			if (instance != null) {
				// TODO checking class is not enough after allowing multiple instances of root (siblings). 
				// any flexibility here?
//				if (instance.root.getClass().equals(empty.getClass()))
//					return (T) instance.root;
//				throw new IllegalStateException("a persisted object already created for a different class: " + instance.root.getClass());
				throw new IllegalStateException("an instance is already created");
			}

			// TODO check root class is actually instrumented
			
			if (!IsPersistent.class.isAssignableFrom(rootClass))
				throw new NotCompiledException(rootClass.getName());
			
			String contextClassName = rootClass.getPackage().getName() + ".__Postvayler";
			Class<?> contextClass;
			Field contextRootField;
			Constructor<?> contextRootConstructor;
			
			try {
				contextClass = Class.forName(contextClassName);
				contextRootField = contextClass.getField("rootClass");
				contextRootConstructor = contextClass.getConstructor(Prevayler.class, RootHolder.class);
				
				if (!contextRootField.get(null).equals(rootClass)) 
					throw new NotCompiledException("cannot create Postvayler for " + rootClass.getName() 
							+ ", root class is " + contextRootField.getType().getName());
				
				String classSuffix = getClassNameForJavaIdentifier(rootClass);
				String instrumentationRoot = (String) rootClass.getField("__postvayler_root_" + classSuffix).get(null);
				if (!rootClass.getName().equals(instrumentationRoot))
					throw new NotCompiledException("given root class " + rootClass.getName() + " is instrumented for root class " + instrumentationRoot); 
				
			} catch (ClassNotFoundException e) {
				throw new NotCompiledException(rootClass.getName(), e);
			} catch (NoSuchMethodException e) {
				throw new NotCompiledException(rootClass.getName(), e);
			} catch (NoSuchFieldException e) {
				throw new NotCompiledException(rootClass.getName(), e);
			}
			
			if (factory == null) {
				factory = new PrevaylerFactory<RootHolder>();
				factory.configurePrevalenceDirectory("persist/" + rootClass.getName());
				//factory.configurePrevalentSystem(new RootHolder());
			}
			
			factory.configurePrevalentSystem(new RootHolder());
			
			Prevayler<RootHolder> prevayler = factory.create();
			RootHolder rootHolder = prevayler.prevalentSystem();
			rootHolder.onRecoveryCompleted();
			
			contextRootConstructor.newInstance(new GCPreventingPrevayler(prevayler), rootHolder);
			
			if (!rootHolder.isInitialized()) {
				prevayler.execute(new InitRootTransaction((Class)rootClass));
			}
			
			instance = this;
			
			return (T) rootHolder.getRoot();
		}
	}
	
	
	public Postvayler<T> setPrevaylerFactory(PrevaylerFactory<RootHolder> factory) {
		this.factory = factory;
		return this;
	}

	public static <T> T create(Class<T> clazz) throws Exception {
		return new Postvayler<T>(clazz).create();
	}
	
	private static String getClassNameForJavaIdentifier(Class<?> clazz) {
		return clazz.getName().replace('.', '_').replace('$', '_');
	}

}
