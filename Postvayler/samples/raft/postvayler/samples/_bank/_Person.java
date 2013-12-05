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
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class _Person implements Serializable, IsPersistent {
	private static final long serialVersionUID = 1L;

	private String name;
	private String phone;

	@_Injected protected Long __postvayler_Id;
	
	public _Person() throws Exception {
		//@_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Person.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall(_Person.class, new Class[] {String.class}), new Object[] { name } ));
				} finally {
					context.setInTransaction(false);
				}
			}
		} else if (Context.isInRecovery()) {
			__postvayler_Id = Context.getRecoveryRoot().__postvayler_put(this);
		} else {
			// no Postvayler, object will not have an id
		}
	}
	
	public _Person(String name) throws Exception {
		this(); // since there is a call to this(..) constructor, we omit bytecode injection
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public String getName() {
		return name;
	}
	
	@Persist
	public void setPhone(String phone) {
		if (!__Postvayler.isBound()) { 
			__postvayler__setPhone(phone);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) { 
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
	public Long __postvayler_getId() {
		return __postvayler_Id;
	}
	
	
	
}
