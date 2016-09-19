package raft.postvayler.impl;

import org.prevayler.Prevayler;

/**
 * 
 * @author r a f t
 */
// TODO maybe made methods of this class protected and override in __Postvayler? 
public abstract class Context {

	private static Context instance;
	
	public final Prevayler<RootHolder> prevayler;
	public final RootHolder root;
	public final ClassCache classCache;
	
	private final ThreadLocal<Boolean> transactionStatus = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		};
	};
	
	private final ThreadLocal<Boolean> queryStatus = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		};
	};
	
	private final ThreadLocal<ConstructorCall<? extends IsPersistent>> constructorCall = new ThreadLocal<ConstructorCall<? extends IsPersistent>>() {
		@Override
		protected ConstructorCall<? extends IsPersistent> initialValue() {
			return null;
		};
	};
	
	private final ThreadLocal<IsPersistent> constructorTransactionInitiater = new ThreadLocal<IsPersistent>() {
		@Override
		protected IsPersistent initialValue() {
			return null;
		};
	};
	
	static RootHolder recoveryRoot;

	protected Context(Prevayler<RootHolder> prevayler, RootHolder root) {
		synchronized (Context.class) {
			if (instance != null)
				throw new IllegalStateException("an instance already created");
			
			this.prevayler = prevayler;
			this.root = root;
			this.classCache = new ClassCache(root.getClass().getName());
			
			instance = this;
		}
	}
	
	public static final boolean isBound() {
		return (instance != null);
	}
	
	public static final Context getInstance() {
		return instance;
	}
	
	public static final boolean isInRecovery() {
		return (recoveryRoot != null);
	}
	
	public static final RootHolder getRecoveryRoot() {
		return recoveryRoot;
	}
	
	public boolean isInTransaction() {
		return transactionStatus.get();
	}
	
	public void setInTransaction(boolean bool) {
		transactionStatus.set(bool);
	}
	
	public final boolean isInQuery() {
		return queryStatus.get();
	}
	
	public final void setInQuery(boolean bool) {
		queryStatus.set(bool);
	}

	public final ConstructorCall<? extends IsPersistent> getConstructorCall() {
		return constructorCall.get();
	}

	public final void setConstructorCall(ConstructorCall<? extends IsPersistent> call) {
		constructorCall.set(call);
	}

	public final IsPersistent getConstructorTransactionInitiater() {
		return constructorTransactionInitiater.get();
	}

	public final void setConstructorTransactionInitiater(IsPersistent initiater) {
		constructorTransactionInitiater.set(initiater);
	}
	
	public final void maybeEndTransaction(IsPersistent caller) {
		if (isInTransaction() && (getConstructorTransactionInitiater() == caller)) {

			setInTransaction(false);
			//System.out.println("ending transaction by: " + Utils.identityCode(caller));
		}
	}
	
	public final void maybeEndTransaction(IsPersistent caller, Class<? extends IsPersistent> clazz) {
		if (isInTransaction() && (getConstructorTransactionInitiater() == caller)
				&& (getConstructorTransactionInitiater().getClass() == clazz)) {

			setInTransaction(false);
			//System.out.println("ending transaction by: " + Utils.identityCode(caller));
		}
	}
}
