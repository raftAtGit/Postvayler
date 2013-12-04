package raft.postvayler.samples._bank;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.NotPersistentException;
import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Storage;
import raft.postvayler.Synch;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.IsRoot;
import raft.postvayler.impl.MethodTransaction;
import raft.postvayler.impl.MethodTransactionWithQuery;
import raft.postvayler.impl.MethodWrapper;
import raft.postvayler.impl.Pool;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent 
public class _Bank implements Serializable, IsRoot, IsPersistent, Storage {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, _Customer> customers = new TreeMap<Integer, _Customer>();
	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	// TODO tmp
	private _Customer aCustomer;

	@_Injected private final Pool __postvayler_pool = new Pool();
	@_Injected private final Long __postvayler_Id = __postvayler_pool.put(this);
	
	private int lastCustomerId = 1;
	
	public _Bank() {}

	@Persist
	_Customer createCustomer(String name) throws Exception {
		if (!__Postvayler.isBound()) 
			return __postvayler__createCustomer(name);
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) 
			return __postvayler__createCustomer(name);
		
		context.setInTransaction(true);
		try {
			return context.prevayler.execute(new MethodTransactionWithQuery<_Customer>(
					this, new MethodWrapper("__postvayler__createCustomer", getClass(), new Class[] {String.class}), new Object[] { name } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@Persist
	Integer addCustomer(_Customer customer) throws Exception {
		if (!__Postvayler.isBound()) 
			return __postvayler__addCustomer(customer);
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) 
			return __postvayler__addCustomer(customer);
		
		context.setInTransaction(true);
		try {
			return context.prevayler.execute(new MethodTransactionWithQuery<Integer>(
					this, new MethodWrapper("__postvayler__addCustomer", getClass(), new Class[] {_Customer.class}), new Object[] { customer } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@Persist
	void addCustomers(_Customer... customers) throws Exception {
		if (!__Postvayler.isBound()) { 
			__postvayler__addCustomers(customers);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) { 
			__postvayler__addCustomers(customers);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodWrapper("__postvayler__addCustomers", getClass(), new Class[] {_Customer[].class}), new Object[] { customers } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@Persist
	public void removeCustomer(_Customer customer) {
		if (!__Postvayler.isBound()) { 
			__postvayler__removeCustomer(customer);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) { 
			__postvayler__removeCustomer(customer);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodWrapper("__postvayler__removeCustomer", getClass(), new Class[] {_Customer.class}), new Object[] { customer } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	
	@_Injected
	private _Customer __postvayler__createCustomer(String name) throws Exception {
		_Customer customer = new _Customer(name);
		addCustomer(customer);
		return customer;
	}
	
	@_Injected
	private Integer __postvayler__addCustomer(_Customer customer) {
		customer.setId(lastCustomerId++);
		customers.put(customer.getId(), customer);
		System.out.println("added customer, new size: " + customers.size());
		return customer.getId();
	}

	@_Injected
	private void __postvayler__addCustomers(_Customer... customers) throws Exception {
		System.out.println("add customers");
		for (_Customer customer : customers) {
			addCustomer(customer);
		}
		System.out.println("added " + customers.length + ", customers new size: " + this.customers.size());
	}
	
	@_Injected
	private void __postvayler__removeCustomer(_Customer customer) {
		customers.remove(customer.getId());
	}

	public _Customer getCustomer(int customerId) {
		return customers.get(customerId);
	}
	
	@Synch
	public List<_Customer> getCustomers() {
		return new ArrayList<_Customer>(customers.values());
	}

	
	@_Injected
	public Long __postvayler_getId() {
		return __postvayler_Id;
	}

	@_Injected
	public IsPersistent __postvayler_get(Long id) {
		return __postvayler_pool.get(id);
	}

	@_Injected
	public Long __postvayler_put(IsPersistent persistent) {
		return __postvayler_pool.put(persistent);
	}

	@_Injected
	public File takeSnapshot() throws Exception {
		if (! __Postvayler.isBound())
		   throw new NotPersistentException("no postvayler context found");
		
		return __Postvayler.getInstance().prevayler.takeSnapshot();
	}

	
	 
}
