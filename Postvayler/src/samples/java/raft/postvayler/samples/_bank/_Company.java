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

/**
 * A company.
 * 
 * @author r a f t
 */
@Persistent 
public class _Company implements Serializable, IsPersistent {

	private static final long serialVersionUID = 1L;

	@_Injected protected Long __postvayler_Id;

	private _RichPerson owner;
	
	public _Company() throws Exception {
		//@_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Company.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				this.__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall<IsPersistent>(_Company.class, new Class[0]), new Object[0]));
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
		if (context.inTransaction()) { 
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
