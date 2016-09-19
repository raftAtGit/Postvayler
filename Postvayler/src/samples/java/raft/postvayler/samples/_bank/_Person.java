package raft.postvayler.samples._bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;
import raft.postvayler.impl.Utils;

/**
 * A person.
 * 
 * @author r a f t
 */
@Persistent
public class _Person implements Serializable, IsPersistent {
	private static final long serialVersionUID = 1L;

	@_Injected private final Long __postvayler_Id;
	
	private String name;
	private String phone;

	public _Person() throws Exception {
		// @_Injected
		try {
			if (__Postvayler.isBound()) { 
				Context context = __Postvayler.getInstance();
				
				if (context.isInTransaction()) {
					this.__postvayler_Id = context.root.putObject(this);
				} else {
					//System.out.println("starting constructor transaction @" + _Person.class + " for " + Utils.identityCode(this));
					context.setInTransaction(true);
					context.setConstructorTransactionInitiater(this);
					
					try {
						ConstructorCall<? extends IsPersistent> constructorCall = context.getConstructorCall(); 
						if (constructorCall == null) {
							if (getClass() != _Person.class)
								throw new Error("subclass constructor " + getClass().getName() + " is running but there is no stored constructorCall");
							
							constructorCall = new ConstructorCall<_Person>(
									_Person.class, new Class[]{}, new Object[]{});
						}
						this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(this, constructorCall));
					} finally {
						//context.setInTransaction(false);
						context.setConstructorCall(null);
					}
				}
			} else if (Context.isInRecovery()) {
				this.__postvayler_Id = Context.getRecoveryRoot().putObject(this);
			} else {
				// no Postvayler, object will not have an id
				this.__postvayler_Id = null;
			}
		} catch (Exception e) {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this);
			}
			throw e;
		} finally {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this, _Person.class);
			}
		}
	}
	
	public _Person(String name) throws Exception {
		this();
		// @_Injected
		try {
			
			this.name = name;
			
			if ("HellBoy".equals(name))
				throw new IllegalArgumentException(name);
			
			
		} catch (Exception e) {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this);
			}
			throw e;
		} finally {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this, _Person.class);
			}
		}
	}

	public String getPhone() {
		return phone;
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
					this, new MethodCall("__postvayler__setName", _Person.class, new Class[] {String.class}), new Object[] { name } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@Persist
	public void setPhone(String phone) {
		if (!__Postvayler.isBound()) { 
			__postvayler__setPhone(phone);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__setPhone(phone);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__setPhone", _Person.class, new Class[] {String.class}), new Object[] { phone } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@_Injected
	private void __postvayler__setPhone(String phone) {
		this.phone = phone;
	}

	@_Injected
	private void __postvayler__setName(String name) {
		this.name = name;
	}
	
	@_Injected 
	public final Long __postvayler_getId() {
		return __postvayler_Id;
	}
	
	
	
}
