package raft.postvayler.samples._bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;

/**
 * An account in a bank
 * 
 * @author  r a f t
 */
public class _Account implements Serializable, IsPersistent {

	private static final long serialVersionUID = 1L;

	private final int id;
	private String name;
	private _Customer owner;
	private int balance = 0;
	
	@_Injected private Long __postvayler_Id;
	
	_Account(int id) throws Exception {
		// @_Injected
		try {
			if (__Postvayler.isBound()) { 
				Context context = __Postvayler.getInstance();
				
				if (context.isInTransaction()) {
					this.__postvayler_Id = context.root.putObject(this);
				} else {
					//System.out.println("starting constructor transaction @" + _Account.class + " for " + Utils.identityCode(this));
					context.setInTransaction(true);
					try {
						ConstructorCall<? extends IsPersistent> constructorCall = context.getConstructorCall(); 
						if (constructorCall == null) {
							if (getClass() != _Account.class)
								throw new Error("subclass constructor " + getClass().getName() + " is running but there is no stored constructorCall");
							
							constructorCall = new ConstructorCall<_Account>(
									_Account.class, new Class[]{int.class}, new Object[]{id});
						}
						this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(this, constructorCall));
					} finally {
						context.setInTransaction(false);
						context.setConstructorCall(null);
						System.out.println("ending transaction: " + this);
					}
				}
			} else if (Context.isInRecovery()) {
				this.__postvayler_Id = Context.getRecoveryRoot().putObject(this);
			} else {
				// no Postvayler, object will not have an id
			}
			
			this.id = id;
		} catch (Exception e) {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this);
			}
			throw e;
		} finally {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this, _Account.class);
			}
		}
			
	}
	
	public int getBalance() {
		return balance;
	}

	public int getId() {
		return id;
	}

	public _Customer getOwner() {
		return owner;
	}

	void setOwner(_Customer owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	@Persist
	public void setName(String name) {
		if (!__Postvayler.isBound()) { 
			__postvayler__setName(name);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__setName(name);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__setName", _Account.class, new Class[] {String.class}), new Object[] { name } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected
	private void __postvayler__setName(String name) {
		this.name = name;
	}
	
	@Persist
	public void deposit(int amount) {
		if (!__Postvayler.isBound()) { 
			__postvayler__deposit(amount);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__deposit(amount);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__deposit", _Account.class, new Class[] {Integer.TYPE}), new Object[] { amount } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected
	private void __postvayler__deposit(int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("amount: " + amount);
		this.balance += amount;
	}
	
	@Persist
	public void withdraw(int amount) {
		if (!__Postvayler.isBound()) { 
			__postvayler__withdraw(amount);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__withdraw(amount);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__withdraw", _Account.class, new Class[] {Integer.TYPE}), new Object[] { amount } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected
	private void __postvayler__withdraw(int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("amount: " + amount);
		if (balance < amount)
			throw new IllegalArgumentException("balance < amount");
		
		this.balance -= amount;
	}
	
	@_Injected 
	public final Long __postvayler_getId() {
		return __postvayler_Id;
	}
}
