package raft.postvayler.samples._bank;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import raft.postvayler.Persistent;
import raft.postvayler.Synch;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;

@Persistent
public class _RichPerson extends _Person {

	private static final long serialVersionUID = 1L;
	
	/** we cannot use a regular HashSet since the iteration order is not deterministic */
	private final Set<_Bank> banks = new LinkedHashSet<_Bank>();
	
	public _RichPerson(String name) throws Exception {
		super(name);
		
		
		// @_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _RichPerson.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				this.__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall<IsPersistent>(_RichPerson.class, new Class[] {String.class}), new Object[] { name } ));
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

	@Synch
	public List<_Bank> getBanks() {
		if (!__Postvayler.isBound()) 
			return __postvayler__getBanks();
		
		Context context = __Postvayler.getInstance();
		
		if (context.inQuery() || context.inTransaction()) {
			return __postvayler__getBanks();
		}
		
		synchronized (context.root) {
			context.setInQuery(true);
		    try {
				return __postvayler__getBanks();
			} finally {
				context.setInQuery(false);
			}
		}
	}

	@_Injected("renamed from getBanks and made private")
	public List<_Bank> __postvayler__getBanks() {
		return new ArrayList<_Bank>(banks);
	}



	boolean addBank(_Bank bank) {
		return banks.add(bank);
	}

	boolean removeBank(_Bank bank) {
		return banks.remove(bank);
	}
}
