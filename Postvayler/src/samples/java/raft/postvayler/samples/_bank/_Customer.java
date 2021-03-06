package raft.postvayler.samples._bank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Synch;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;

/**
 * A customer.
 * 
 * @author r a f t
 */
@Persistent
public class _Customer extends _Person {
	private static final long serialVersionUID = 1L;

	private int id;

	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	@_Injected("this method is not actually injected but contents is injected before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	private static String __postvayler_maybeInitConstructorTransaction(String name) { 
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (!context.isInTransaction() && (context.getConstructorCall() == null)) {
				context.setConstructorCall(new ConstructorCall<_Customer>(
						_Customer.class, new Class[]{ String.class }, new Object[] {name}));
			}
		}
		
		return name; 
	}
	
	public _Customer(String name) throws Exception {
		// @_Injected
		super(__postvayler_maybeInitConstructorTransaction(name));
		
		if (__Postvayler.isBound()) {
			__Postvayler.getInstance().maybeEndTransaction(this, _Customer.class);
		}
		
	}
	
	@Persist
	public void addAccount(_Account account) {
		if (!__Postvayler.isBound()) { 
			__postvayler__addAccount(account);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
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
		
		Context context = __Postvayler.getInstance();
		
		if (context.isInQuery() || context.isInTransaction()) {
			return __postvayler__getAccounts();
		}
		
		synchronized (context.root) {
			context.setInQuery(true);
		    try {
				return __postvayler__getAccounts();
			} finally {
				context.setInQuery(false);
			}
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
