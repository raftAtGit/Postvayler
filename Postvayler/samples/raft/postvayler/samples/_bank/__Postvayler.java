package raft.postvayler.samples._bank;

import org.prevayler.Prevayler;

import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsRoot;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
@_Injected
public class __Postvayler extends Context {

	public __Postvayler(Prevayler<IsRoot> prevayler, _Bank root) {
		super(prevayler, root);
	}
}
