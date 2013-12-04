package raft.postvayler.samples._bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.MethodTransaction;
import raft.postvayler.impl.MethodWrapper;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class _Person implements Serializable, IsPersistent {
	private static final long serialVersionUID = 1L;

	private String name;
	private String phone;

	// TODO id will be created here but object will be put in every constructor
	@_Injected private final Long __postvayler_Id = __Postvayler.put(this);
	
	public _Person() {
	}	
	public _Person(String name) {
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
					this, new MethodWrapper("__postvayler__setPhone", getClass(), new Class[] {String.class}), new Object[] { phone } ));
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
