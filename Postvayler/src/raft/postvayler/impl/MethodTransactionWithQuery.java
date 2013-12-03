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
	private final MethodWrapper method;
	private final Object[] arguments;

	public MethodTransactionWithQuery(IsPersistent target, MethodWrapper method, Object[] arguments) {
		this.targetId = target.__postvayler_getId();
		if (targetId == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + target);
		this.method = method;
		this.arguments = Utils.referenceArguments(arguments);
	}

	@SuppressWarnings("unchecked")
	public R executeAndQuery(IsRoot root, Date date) throws Exception {
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
	}


}
