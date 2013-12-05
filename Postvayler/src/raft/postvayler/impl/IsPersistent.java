package raft.postvayler.impl;

/** 
 * Injected into persistent classes 
 * 
 * @author r a f t
 */
public interface IsPersistent {
	Long __postvayler_getId();
	
	// add getRoot?
	//IsRoot __postvayler_getRoot();
}
