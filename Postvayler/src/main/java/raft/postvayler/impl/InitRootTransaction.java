package raft.postvayler.impl;

import java.util.Date;

import org.prevayler.TransactionWithQuery;

// TODO remove
public class InitRootTransaction implements TransactionWithQuery<RootHolder, Void> {

	private static final long serialVersionUID = 1L;

	private final Class<? extends IsPersistent> persistentRootClass;
	
	public InitRootTransaction(Class<? extends IsPersistent> persistentRootClass) {
		this.persistentRootClass = persistentRootClass;
	}

	@Override
	public Void executeAndQuery(RootHolder root, Date date) throws Exception {
		if (!Context.isBound()) 
			Context.recoveryRoot = root;
		else Context.getInstance().setInTransaction(true);
		
		ClockBase.setDate(date);
		try {
			IsPersistent persistentRoot = persistentRootClass.newInstance();
			root.setRoot(persistentRoot);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (Context.isBound()) 
				Context.getInstance().setInTransaction(false);
			ClockBase.setDate(null);
		}
	}

}
