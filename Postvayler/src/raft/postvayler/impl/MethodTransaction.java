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
	private final MethodWrapper method;
	private final Object[] arguments;

	public MethodTransaction(IsPersistent target, MethodWrapper method, Object[] arguments) {
		this.targetId = target.__postvayler_getId();
		if (targetId == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + target);
		this.method = method;
		this.arguments = Utils.referenceArguments(arguments);
	}

	public void executeOn(IsRoot root, Date date) {
		try {
			IsPersistent target = root.__postvayler_get(targetId);
			if (target == null) {
				// target is garbage collected
				System.out.println("couldnt find target object with id " + targetId + ", possibly it's garbage collected, ignoring transaction");
				return;
			}
			java.lang.reflect.Method m = method.getJavaMethod();
			m.setAccessible(true);
			m.invoke(target, Utils.dereferenceArguments(root, arguments));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
