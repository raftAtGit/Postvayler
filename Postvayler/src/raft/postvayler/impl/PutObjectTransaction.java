package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.TransactionWithQuery;

public class PutObjectTransaction implements TransactionWithQuery<IsRoot, Long> {

	private static final long serialVersionUID = 1L;

	private final IsPersistent persistent;
	
	public PutObjectTransaction(IsPersistent persistent) {
		this.persistent = persistent;
	}

//	public void executeOn(IsRoot root, Date date) {
//		root.__postvayler_put(persistent);
//		System.out.println("put object in transaction " + persistent.__postvayler_getId() + ":" + Utils.identityCode(persistent));
//	}

	public Long executeAndQuery(IsRoot root, Date date) throws Exception {
//		Long nextId = root.__postvayler_getNextId(); 
		Long id = root.__postvayler_put(persistent);
		System.out.println("put object in transaction " + id + ":" + persistent.__postvayler_getId() + ":" + Utils.identityCode(persistent));
		return id;
	}

}
