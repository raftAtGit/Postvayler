package raft.postvayler.samples._bank;

import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persistent;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.Utils;

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
		
		//@_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Customer.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall(_Customer.class, new Class[] {String.class}), new Object[] { name } ));
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
