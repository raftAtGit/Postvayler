package raft.postvayler.impl;

import java.lang.reflect.Method;
import java.util.Date;

import org.prevayler.TransactionWithQuery;

import raft.postvayler.NotPersistentException;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class MethodTransactionWithQuery<R> implements TransactionWithQuery<IsRoot, R> {

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

	public MethodTransactionWithQuery(IsPersistent target, MethodCall method, Object[] arguments) {
		this.transientTarget = target;
		this.transientArguments = arguments;
		
		this.targetId = target.__postvayler_getId();
		if (targetId == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + Utils.identityCode(target));
		this.method = method;
		this.arguments = Utils.referenceArguments(arguments);
	}

	@SuppressWarnings("unchecked")
	@Override
	public R executeAndQuery(IsRoot root, Date date) throws Exception {
		if (!Context.isBound()) Context.recoveryRoot = root;
		ClockBase.setDate(date);
		
		try {
			IsPersistent target = root.__postvayler_get(targetId);
			
			if (target == null) {
				throw new Error("couldnt get object from the pool, id: " + targetId); // we throw error to halt Prevayler
	//			// target is garbage collected
	//			System.out.println("couldnt find target object with id " + targetId + ", possibly it's garbage collected, ignoring transaction");
	//			return null;
			}
			Method m = method.getJavaMethod();
			m.setAccessible(true);
			return (R) m.invoke(target, Utils.dereferenceArguments((IsRoot)root, arguments));
			
		} finally {
			Context.recoveryRoot = null;
			ClockBase.setDate(null);
		}
	}


}
