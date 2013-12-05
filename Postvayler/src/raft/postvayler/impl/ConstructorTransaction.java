package raft.postvayler.impl;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.prevayler.TransactionWithQuery;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
public class ConstructorTransaction implements TransactionWithQuery<IsRoot, Long> {

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
	private final ConstructorCall<IsPersistent> constructor;
	private final Object[] arguments;

	public ConstructorTransaction(IsPersistent target, ConstructorCall<IsPersistent> constructor, Object[] arguments) {
		this.tempTargetId = putTemp(target);
		this.constructor = constructor;
		this.arguments = Utils.referenceArguments(arguments);
	}

	public Long executeAndQuery(IsRoot root, Date date) throws Exception {
		if (Context.isBound()) {
			// regular run, just retrieve object from temp storage and put into pool
			IsPersistent target = tempTargets.remove(tempTargetId);
			if (target == null)
				throw new Error("couldnt get object from temp pool, id: " + tempTargetId); // we throw error to halt Prevayler
			return root.__postvayler_put(target);
			
		} else {
			// recovery
			Context.recoveryRoot = root;
			try {
				Constructor<IsPersistent> cons = constructor.getJavaConstructor();
				cons.setAccessible(true);
				IsPersistent target = cons.newInstance(arguments);
				assert (target.__postvayler_getId() != null);
				return target.__postvayler_getId(); // return type is not actually used in this case
			} finally {
				Context.recoveryRoot = null;
			}
		}
	}

}
