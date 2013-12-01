package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.TransactionWithQuery;

public class GetNextIdTransaction implements TransactionWithQuery<IsRoot, Long> {

	private static final long serialVersionUID = 1L;

	public Long executeAndQuery(IsRoot root, Date date) throws Exception {
		Long nextId = root.__postvayler_getNextId(); 
		System.out.println("got nextId in transaction " + nextId);
		return nextId;
	}

}
