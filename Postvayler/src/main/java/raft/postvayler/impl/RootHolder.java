package raft.postvayler.impl;

import java.io.Serializable;

/** 
 * Container class that hold persistence root. 
 * 
 * @author r a f t
 */
public class RootHolder implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final Pool pool = new Pool();
	private IsPersistent root;
	
	public final IsPersistent getObject(Long id) {
		return pool.get(id);
	}

	public final Long putObject(IsPersistent persistent) {
		if (persistent.__postvayler_getId() != null) {
			// we throw error to halt Prevayler
			throw new Error("object already has an id\n" + Utils.identityCode(persistent));
		}
		
		return pool.put(persistent); 
	}

	public final void onRecoveryCompleted() {
		pool.switchToWeakValues();
	}

	public final boolean isInitialized() {
		return (root != null);
	}
	
	public final IsPersistent getRoot() {
		return root;
	}

	final void setRoot(IsPersistent root) {
		assert (this.root == null);
		this.root = root;
	}
	
	
}
