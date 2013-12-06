package raft.postvayler.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Pool implements Serializable {

	private static final long serialVersionUID = 1L;

	/** the objects. we start with a regular {@link Map} and after recovery is completed switch to a 
	 * {@link WeakValueHashMap}. This is necessary because during recovery there are no external references
	 * to created objects and nothing prevents them from garbage collected from a WeakMap.
	 * 
	 *  @see IsRoot#__postvayler_onRecoveryCompleted()
	 *  @see #switchToWeakValues() */
	private Map<Long, IsPersistent> objects = new HashMap<Long, IsPersistent>();

	private long lastId = 1;
	
	public final synchronized Long put(IsPersistent persistent) {
		Long id = lastId++;
		objects.put(id, persistent);
//		System.out.println("put " + id + ":" + Utils.identityCode(persistent) + ", size: " + objects.size());
		return id;
	}
	
	public final synchronized IsPersistent get(Long id) {
		return objects.get(id);
	} 

	public final synchronized void switchToWeakValues() {
		System.out.println("--switching to weak values");
		this.objects = new WeakValueHashMap<Long, IsPersistent>(objects); 
		System.out.println("--done");
	}
	
	/** replaces the WeakValueMap with a regular HashMap so after a snapshot read we always start with a HashMap */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		Map<Long, IsPersistent> nonWeakObjects = new HashMap<Long, IsPersistent>(objects);
		this.objects = nonWeakObjects;
		out.defaultWriteObject();
	}

	
	
	
}
