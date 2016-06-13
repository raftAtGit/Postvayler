package raft.postvayler.impl;

import org.prevayler.Prevayler;

/**
 * 
 * @author r a f t
 */
// TODO maybe made methods of this class protected and override in __Postvayler? 
public abstract class Context {

	private static Context instance;
	
	public final Prevayler<IsRoot> prevayler;
	public final IsRoot root;
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
	
	static IsRoot recoveryRoot;

	protected Context(Prevayler<IsRoot> prevayler, IsRoot root) {
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
	
	public static final IsRoot getRecoveryRoot() {
		return recoveryRoot;
	}
	
	public boolean inTransaction() {
		return transactionStatus.get();
	}
	
	public void setInTransaction(boolean bool) {
		transactionStatus.set(bool);
	}
	
	public boolean inQuery() {
		return queryStatus.get();
	}
	
	public void setInQuery(boolean bool) {
		queryStatus.set(bool);
	}
	
	public static final Long put(IsPersistent persistent) {
		if (instance != null) {
			if (instance.inTransaction()) {
				return instance.root.__postvayler_put(persistent);
			} else {
				instance.setInTransaction(true);
				try {
					return instance.prevayler.execute(new PutObjectTransaction(persistent));
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					instance.setInTransaction(false);
				}
			}
		}
		if (recoveryRoot != null)
			return recoveryRoot.__postvayler_put(persistent);
		
		System.out.println("Postvayler context not bound, object will not be stored: " + Utils.identityCode(persistent));
		return null;
	}
	
}
