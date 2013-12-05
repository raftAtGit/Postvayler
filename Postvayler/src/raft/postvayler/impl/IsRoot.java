package raft.postvayler.impl;

/** 
 * Injected into root class 
 * 
 * @author r a f t
 */
public interface IsRoot extends IsPersistent {
	
	public IsPersistent __postvayler_get(Long id);
	
	public Long __postvayler_put(IsPersistent persistent);

	public void __postvayler_onRecoveryCompleted();
}
