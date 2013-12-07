package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.Transaction;

import raft.postvayler.NotPersistentException;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class MethodTransaction implements Transaction<IsRoot> {

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

	public MethodTransaction(IsPersistent target, MethodCall method, Object[] arguments) {
		this.transientTarget = target;
		this.transientArguments = arguments;
		
		this.targetId = target.__postvayler_getId();
		if (targetId == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + Utils.identityCode(target));
		this.method = method;
		this.arguments = Utils.referenceArguments(arguments);
	}

	public void executeOn(IsRoot root, Date date) {
		if (!Context.isBound()) Context.recoveryRoot = root;
		
		try {
			IsPersistent target = root.__postvayler_get(targetId);
			if (target == null) {
				throw new Error("couldnt get object from the pool, id: " + targetId); // we throw error to halt Prevayler
				// target is garbage collected
//				System.out.println("couldnt find target object with id " + targetId + ", possibly it's garbage collected, ignoring transaction");
//				return;
			}
			java.lang.reflect.Method m = method.getJavaMethod();
			m.setAccessible(true);
			m.invoke(target, Utils.dereferenceArguments(root, arguments));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Context.recoveryRoot = null;
		}
	}

}
