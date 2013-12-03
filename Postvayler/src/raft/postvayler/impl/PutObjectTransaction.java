package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.Transaction;

public class PutObjectTransaction implements Transaction<IsRoot> {

	private static final long serialVersionUID = 1L;

	private final IsPersistent persistent;
	
	public PutObjectTransaction(IsPersistent persistent) {
		this.persistent = persistent;
	}

	public void executeOn(IsRoot root, Date date) {
		root.__postvayler_put(persistent);
		System.out.println("put object in transaction " + persistent.__postvayler_getId() + ":" + Utils.identityCode(persistent));
	}

}
