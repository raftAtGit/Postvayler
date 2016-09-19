package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.Transaction;

import raft.postvayler.NotPersistentException;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class MethodTransaction implements Transaction<RootHolder> {

	private static final long serialVersionUID = 1L;

	private final Long targetId;
	private final MethodCall method;
	private final Object[] arguments;
	
	/** 
	 * we keep a transient reference to our target. GCPreventingPrevayler keeps a reference to this Transaction. these two references 
	 * safely prevents garbage collector cleaning our target before we are done
	 * 
	 * @see GCPreventingPrevayler
	 */  
	@SuppressWarnings("unused")
	private transient IsPersistent transientTarget;
	@SuppressWarnings("unused")
	private transient Object[] transientArguments;

	// TODO make this similar to ConstrutorTransaction
	public MethodTransaction(IsPersistent target, MethodCall method, Object[] arguments) {
		this.transientTarget = target;
		this.transientArguments = arguments;
		
		this.targetId = target.__postvayler_getId();
		if (targetId == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + Utils.identityCode(target));
		this.method = method;
		this.arguments = Utils.referenceArguments(arguments);
	}

	@Override
	public void executeOn(RootHolder root, Date date) {
		if (!Context.isBound()) Context.recoveryRoot = root;
		ClockBase.setDate(date);
		
		try {
			IsPersistent target = root.getObject(targetId);
			if (target == null) 
				throw new Error("couldnt get object from the pool, id: " + targetId); // we throw error to halt Prevayler
			
			java.lang.reflect.Method m = method.getJavaMethod();
			m.setAccessible(true);
			m.invoke(target, Utils.dereferenceArguments(root, arguments));
		} catch (RuntimeException e) {
//			e.printStackTrace();
			throw e;
		} catch (Exception e) {
//			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			Context.recoveryRoot = null;
			ClockBase.setDate(null);
		}
	}
	
//	private void printDebug(Root root) throws Exception {
//		System.out.println(method.getJavaMethod());
//		System.out.println("targetId: " + targetId);
//		System.out.println("target: " + Utils.identityCode(root.__postvayler_get(targetId)));
//		System.out.println("args: " + Arrays.toString(arguments));
//		System.out.println("def args: " + Arrays.toString(Utils.dereferenceArguments(root, arguments)));
//		
//		IsPersistent target = root.__postvayler_get(targetId);
//		
//		java.lang.reflect.Method m = method.getJavaMethod();
//		m.setAccessible(true);
//		m.invoke(target, Utils.dereferenceArguments(root, arguments));
//	}

}
