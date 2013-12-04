package raft.postvayler.impl;

public interface IsRoot extends IsPersistent {
	
//	Long __postvayler_getNextId();
	
	IsPersistent __postvayler_get(Long id);
	
	Long __postvayler_put(IsPersistent persistent);
	
}
