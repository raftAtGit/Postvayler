package raft.postvayler.impl;

import java.io.Serializable;

import raft.postvayler.NotPersistentException;

/**
 * Reference to an IsPersistent object 
 * 
 * @author  hakan eryargi (r a f t)
 */
class Reference implements Serializable {
	
	private static final long serialVersionUID = 1L;

	final Long id;

	Reference(IsPersistent persistent) {
		this.id = persistent.__postvayler_getId();
		if (id == null)
			throw new NotPersistentException("object has no id, did you create this object before Postvayler is created?\n" + Utils.identityCode(persistent));
	}
	
	
}
