package raft.postvayler.samples._bank;

import raft.postvayler.Persistent;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.Context;

/**
 * Central bank.
 * 
 * @author r a f t
 */
@Persistent
public class _CentralBank extends _Bank {

	private static final long serialVersionUID = 1L;

	@_Injected("this method is not actually injected but contents is injected before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	private static Void __postvayler_maybeInitConstructorTransaction() { 
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (!context.isInTransaction() && (context.getConstructorCall() == null)) {
				context.setConstructorCall(new ConstructorCall<_CentralBank>(
						_CentralBank.class, new Class[]{}, new Object[]{}));
			}
		}
		
		return null; 
	}
	
	public _CentralBank() throws Exception {
		// @Injected
		super(__postvayler_maybeInitConstructorTransaction());
		
		if (__Postvayler.isBound()) {
			__Postvayler.getInstance().maybeEndTransaction(this, _CentralBank.class);
		}
	}

}
