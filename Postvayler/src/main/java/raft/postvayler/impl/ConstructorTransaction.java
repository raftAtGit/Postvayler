package raft.postvayler.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.prevayler.TransactionWithQuery;

/**
 * A special Transaction which behaves different at <i>recovery</i> and <i>regular</i> run. 
 * This transaction stores given target object in a temporary memory location and returns it as it's during regular run. 
 * At recovery, an object is created with given constructor. This mechanism escapes target object from 
 * Prevayler's transaction serialization. Coordinated with injected code to object's constructor, 
 * at either recovery or regular run, after this transaction is executed the object gets a unique id. Since Prevayler
 * guarantees transaction ordering, it's guaranteed that the target object will get the same id.     
 * 
 * @author r a f t
 */
public class ConstructorTransaction implements TransactionWithQuery<RootHolder, Long> {

	private static final long serialVersionUID = 1L;

	private static final Map<Long, IsPersistent> tempTargets = new HashMap<Long, IsPersistent>();
	private static long lastTempId;
	
	private static final Long putTemp(IsPersistent target) {
		synchronized (tempTargets) {
			Long id = lastTempId++;
			tempTargets.put(id, target);
			return id;
		}
	} 
	
	private final Long tempTargetId;
	private final ConstructorCall<? extends IsPersistent> constructor;

	public ConstructorTransaction(IsPersistent target, ConstructorCall<? extends IsPersistent> constructor) {
		this.tempTargetId = putTemp(target);
		this.constructor = constructor;
	}

	@Override
	public Long executeAndQuery(RootHolder root, Date date) throws Exception {
		ClockBase.setDate(date);
		try {
			if (Context.isBound()) {
				// regular run, just retrieve object from temp storage and put into pool
				IsPersistent target = tempTargets.remove(tempTargetId);
				if (target == null)
					throw new Error("couldnt get object from temp pool, id: " + tempTargetId); // we throw error to halt Prevayler

				return root.putObject(target);
				
			} else {
				// recovery
				Context.recoveryRoot = root;
				try {
					IsPersistent target = constructor.newInstance(root);
					assert (target.__postvayler_getId() != null);
					return null; // return type is not actually used in this case
				} finally {
					Context.recoveryRoot = null;
				}
			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw e;
		} finally {
			ClockBase.setDate(null);
		}
	}

}
