package raft.postvayler.samples._bank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Synch;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class _Customer extends _Person {
	private static final long serialVersionUID = 1L;

	private int id;

	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	public _Customer(String name) throws Exception {
		super(name);
		
		// @_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Customer.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				this.__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall<IsPersistent>(_Customer.class, new Class[] {String.class}), new Object[] { name } ));
				} finally {
					context.setInTransaction(false);
				}
			}
		} else if (Context.isInRecovery()) {
			this.__postvayler_Id = Context.getRecoveryRoot().__postvayler_put(this);
		} else {
			// no Postvayler, object will not have an id
		}
	}

	@Persist
	public void addAccount(_Account account) {
		if (!__Postvayler.isBound()) { 
			__postvayler__addAccount(account);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) { 
			__postvayler__addAccount(account);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__addAccount", _Customer.class, new Class[] {_Account.class}), new Object[] { account} ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected
	private void __postvayler__addAccount(_Account account) {
		if (account.getOwner() != null)
			throw new IllegalArgumentException("Account already has an owner");
		
		accounts.put(account.getId(), account);
	}
	
	@Synch
	public List<_Account> getAccounts() {
		if (!__Postvayler.isBound()) 
			return __postvayler__getAccounts();
		
		synchronized (__Postvayler.getInstance().root) {
			return __postvayler__getAccounts();
		}
	}
	
	@_Injected
	private List<_Account> __postvayler__getAccounts() {
		return new ArrayList<_Account>(accounts.values());
	}

	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Customer:" + id + ":" + getName();
	}

}
