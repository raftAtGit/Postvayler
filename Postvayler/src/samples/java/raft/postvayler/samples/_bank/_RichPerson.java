package raft.postvayler.samples._bank;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import raft.postvayler.Persistent;
import raft.postvayler.Synch;
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.Context;

/**
 * A rich person who owns companies and banks.
 * 
 * @author r a f t
 */
@Persistent
public class _RichPerson extends _Person {

	private static final long serialVersionUID = 1L;
	
	/** we cannot use a regular HashSet since the iteration order is not deterministic */
	private final Set<_Company> companies = new LinkedHashSet<_Company>();
	
	/** we cannot use a regular HashSet since the iteration order is not deterministic */
	private final Set<_Bank> banks = new LinkedHashSet<_Bank>();
	
	// in non-emulated real Postvayler, @Persistent fields can be created where they are defined.
	// in emulated code, we cannot invoke our code before these fields are initialized, so we moved them to constructor
	private _Person sister;
	private _Person brother;
	
	@_Injected("this method is not actually injected but contents is injected before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	private static String __postvayler_maybeInitConstructorTransaction(String name) { 
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (!context.isInTransaction() && (context.getConstructorCall() == null)) {
				context.setConstructorCall(new ConstructorCall<_RichPerson>(
						_RichPerson.class, new Class[]{ String.class }, new Object[] {name}));
			}
		}
		
		return name; 
	}
	
	public _RichPerson(String name) throws Exception {
		// @_Injected
		super(__postvayler_maybeInitConstructorTransaction(name));
		
		try {
			sister = new _Person("cat girl");
			brother = new _Person("octopus");
		
			if ("Dracula".equals(name))
				throw new IllegalArgumentException(name);	
			
			// @_Injected
		} finally {
			if (__Postvayler.isBound()) {
				__Postvayler.getInstance().maybeEndTransaction(this, _RichPerson.class);
			}
		}
	}

	@Synch
	public List<_Bank> getBanks() {
		if (!__Postvayler.isBound()) 
			return __postvayler__getBanks();
		
		Context context = __Postvayler.getInstance();
		
		if (context.isInQuery() || context.isInTransaction()) {
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
	
	public _Person getSister() {
		return sister;
	}

	public _Person getBrother() {
		return brother;
	}

	boolean addCompany(_Company company) {
		boolean result = companies.add(company);
		if (company instanceof _Bank) 
			banks.add((_Bank)company);
		return result;
	}

	boolean removeCompany(_Company company) {
		banks.remove(company);
		return companies.remove(company);
	}
}
