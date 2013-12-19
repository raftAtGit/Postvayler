package raft.postvayler.samples._bank;

import java.io.Serializable;

import raft.postvayler.Persistent;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;

/**
 * 
 * @author r a f t
 */
@Persistent 
public class _Organization implements Serializable, IsPersistent {

	private static final long serialVersionUID = 1L;

	@_Injected protected Long __postvayler_Id;

	public _Organization() throws Exception {
		//@_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Organization.class)
			return;
		
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				this.__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall<IsPersistent>(_Organization.class, new Class[0]), new Object[0]));
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


}
