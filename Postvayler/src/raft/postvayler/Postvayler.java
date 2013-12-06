package raft.postvayler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import raft.postvayler.impl.GCPreventingPrevayler;
import raft.postvayler.impl.IsRoot;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class Postvayler<T> {

	private static Postvayler<?> instance;
	private T root;
	private T empty;
	
	private PrevaylerFactory<T> factory;
	
	public Postvayler(Class<T> clazz) throws Exception {
		this(clazz.newInstance());
	}
	
	public Postvayler(T t) throws Exception {
		this.empty = t;
	}
	
	@SuppressWarnings("unchecked")
	public T create() throws Exception {
		synchronized (Postvayler.class) {
			if (instance != null) {
				if (instance.root.getClass().equals(empty.getClass()))
					return (T) instance.root;
				throw new IllegalStateException("a persisted object already created for a different class: " + instance.root.getClass());
			}

			// TODO check root class is actually instrumented
			if (!(empty instanceof IsRoot))
				throw new NotCompiledException(empty.getClass().getName());
			
			String contextClassName = empty.getClass().getPackage().getName() + ".__Postvayler";
			Class<?> contextClass;
			Field contextRootField;
			Constructor<?> contextRootConstructor;
			
			try {
				contextClass = Class.forName(contextClassName);
				contextRootField = contextClass.getField("rootClass");
				contextRootConstructor = contextClass.getConstructor(Prevayler.class, empty.getClass());
				
				if (!contextRootField.get(null).equals(empty.getClass())) 
					throw new NotCompiledException("cannot create Postvayler for " + empty.getClass().getName() 
							+ ", root class is " + contextRootField.getType().getName());
				
				String classSuffix = getClassNameForJavaIdentifier(empty.getClass());
				String instrumentationRoot = (String) empty.getClass().getField("__postvayler_root_" + classSuffix).get(null);
				if (!empty.getClass().getName().equals(instrumentationRoot))
					throw new NotCompiledException("given root class " + empty.getClass().getName() + " is instrumented for root class " + instrumentationRoot); 
				
			} catch (ClassNotFoundException e) {
				throw new NotCompiledException(empty.getClass().getName(), e);
			} catch (NoSuchMethodException e) {
				throw new NotCompiledException(empty.getClass().getName(), e);
			} catch (NoSuchFieldException e) {
				throw new NotCompiledException(empty.getClass().getName(), e);
			}
			
			
			if (factory == null) {
				factory = new PrevaylerFactory<T>();
				factory.configurePrevalenceDirectory("persist/" + empty.getClass().getName());
				factory.configurePrevalentSystem(empty);
			}
			Prevayler<T> prevayler = factory.create();
			root = prevayler.prevalentSystem();
			((IsRoot) root).__postvayler_onRecoveryCompleted();
			
			contextRootConstructor.newInstance(new GCPreventingPrevayler((Prevayler<IsRoot>)prevayler), root);
			
			instance = this;
			
			return root;
		}
	}
	
	
	public Postvayler<T> setPrevaylerFactory(PrevaylerFactory<T> factory) {
		this.factory = factory;
		return this;
	}

	public static <T> T create(Class<T> clazz) throws Exception {
		return new Postvayler<T>(clazz).create();
	}
	
	public static <T> T create(T t) throws Exception {
		return new Postvayler<T>(t).create();
	}
	
	private static String getClassNameForJavaIdentifier(Class<?> clazz) {
		return clazz.getName().replace('.', '_').replace('$', '_');
	}

}
