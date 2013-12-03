package raft.postvayler.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import raft.postvayler.NotPersistentException;

public class Pool implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// TODO later replace with a WeakValueMap
//	private Map<Long, IsPersistent> objects = new WeakValueHashMap<Long, IsPersistent>();
	private Map<Long, IsPersistent> objects = new HashMap<Long, IsPersistent>();
	
	private long lastId = 1;
	
	public final synchronized void put(IsPersistent persistent) {
		Long id = persistent.__postvayler_getId();
		if (id == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + persistent);
		
		IsPersistent old = objects.put(id, persistent);
		if ((old != null) && (old != persistent))
			throw new Error("duplicate objects with same id: " + id + "\n" + persistent + "\n" + old); // we throw error to halt Prevayler
	}
	
	public final synchronized IsPersistent get(Long id) {
		return objects.get(id);
	} 
	
	public final synchronized Long getNextId() {
		return lastId++;
	}
}
