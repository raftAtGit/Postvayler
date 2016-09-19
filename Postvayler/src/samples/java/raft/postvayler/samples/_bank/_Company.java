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
 * A company.
 * 
 * @author r a f t
 */
@Persistent 
public class _Company implements Serializable, IsPersistent {

	private static final long serialVersionUID = 1L;

	@_Injected private Long __postvayler_Id;

	private _RichPerson owner; // = new _RichPerson("<no name>");
	
	public _Company() throws Exception {
		// @_Injected
		try {
			if (__Postvayler.isBound()) {
				Context context = __Postvayler.getInstance();
				
				if (context.isInTransaction()) {
					this.__postvayler_Id = context.root.putObject(this);
				} else {
					//System.out.println("starting constructor transaction @" + _Company.class + " for " + Utils.identityCode(this));
					context.setInTransaction(true);
					context.setConstructorTransactionInitiater(this);
					
					try {
						ConstructorCall<? extends IsPersistent> constructorCall = context.getConstructorCall(); 
						if (constructorCall == null) {
							if (getClass() != _Company.class)
								throw new Error("subclass constructor " + getClass().getName() + " is running but there is no stored constructorCall");
							
							constructorCall = new ConstructorCall<_Company>(
									_Company.class, new Class[]{}, new Object[]{});
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
			}
			
			owner = new _RichPerson("<no name>");
			
		} catch (Exception e) {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this);
			}
			throw e;
		} finally {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this, _Company.class);
			}
		}
	}

	@_Injected("this constructor is not actually injected. we inject some code in sub classes before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	protected _Company(Void v)throws Exception {
		this();
	}

	@_Injected
	public final Long __postvayler_getId() {
		return __postvayler_Id;
	}

	public _RichPerson getOwner() {
		return owner;
	}

	@Persist
	public void setOwner(_RichPerson newOwner) {
		if (!__Postvayler.isBound()) { 
			__postvayler__setOwner(newOwner);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__setOwner(newOwner);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__setOwner", _Company.class, new Class[] {_RichPerson.class}), new Object[] { newOwner } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@_Injected("renamed from setOwner and made private")
	private void __postvayler__setOwner(_RichPerson newOwner) {
		if (this.owner != null) {
			this.owner.removeCompany(this);
		}
		this.owner = newOwner;
		
		if (newOwner != null) {
			newOwner.addCompany(this);
		}
	}
	

}
