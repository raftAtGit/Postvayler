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
import raft.postvayler.impl.ConstructorCall;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;
import raft.postvayler.impl.MethodTransactionWithQuery;

/**
 * A bank.
 * 
 * @author r a f t
 */
@Persistent 
public class _Bank extends _Company implements Serializable, Storage {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, _Customer> customers = new TreeMap<Integer, _Customer>();
	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	private int lastCustomerId = 1;
	private int lastAccountId = 1;
	
	private _Bank sister;
	
	@_Injected("this method is not actually injected but contents is injected before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	private static Void __postvayler_maybeInitConstructorTransaction() { 
		if (__Postvayler.isBound()) { 
			Context context = __Postvayler.getInstance();
			
			if (!context.isInTransaction() && (context.getConstructorCall() == null)) {
				context.setConstructorCall(new ConstructorCall<_Bank>(
						_Bank.class, new Class[]{}, new Object[]{}));
			}
		}
		
		return null; 
	}
	
	public _Bank() throws Exception {
		// @Injected
		super(__postvayler_maybeInitConstructorTransaction());
		
		if (__Postvayler.isBound()) {
			__Postvayler.getInstance().maybeEndTransaction(this, _Bank.class);
		}
	}

	@_Injected("this constructor is not actually injected. we inject some code in sub classes before invkoing super type's constructor."
			+ "as there is no way to emulate this behaviour in Java code, we use this workaround")
	protected _Bank(Void v) throws Exception {
		this();
	}

	@Persist
	public _Customer createCustomer(String name) throws Exception {
		if (!__Postvayler.isBound()) 
			return __postvayler__createCustomer(name);
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) 
			return __postvayler__createCustomer(name);
		
		context.setInTransaction(true);
		try {
			return context.prevayler.execute(new MethodTransactionWithQuery<_Customer>(
					this, new MethodCall("__postvayler__createCustomer", _Bank.class, new Class[] {String.class}), new Object[] { name } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected("renamed from createCustomer and made private")
	private _Customer __postvayler__createCustomer(String name) throws Exception {
		_Customer customer = new _Customer(name);
		addCustomer(customer);
		_Account account = createAccount();
		account.setName("Default");
		customer.addAccount(account);
		return customer;
	}
	
	@Persist
	public int addCustomer(_Customer customer) throws Exception {
		if (!__Postvayler.isBound()) 
			return __postvayler__addCustomer(customer);
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) 
			return __postvayler__addCustomer(customer);
		
		context.setInTransaction(true);
		try {
			return context.prevayler.execute(new MethodTransactionWithQuery<Integer>(
					this, new MethodCall("__postvayler__addCustomer", _Bank.class, new Class[] {_Customer.class}), new Object[] { customer } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected("renamed from addCustomer and made private")
	private Integer __postvayler__addCustomer(_Customer customer) {
		customer.setId(lastCustomerId++);
		customers.put(customer.getId(), customer);
		return customer.getId();
	}
	
	@Persist
	public void addCustomers(_Customer... customers) throws Exception {
		if (!__Postvayler.isBound()) { 
			__postvayler__addCustomers(customers);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__addCustomers(customers);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__addCustomers", _Bank.class, new Class[] {_Customer[].class}), new Object[] { customers } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected("renamed from addCustomers and made private")
	private void __postvayler__addCustomers(_Customer... customers) throws Exception {
		for (_Customer customer : customers) {
			addCustomer(customer);
		}
	}
	
	@Persist
	public void removeCustomer(_Customer customer) {
		if (!__Postvayler.isBound()) { 
			__postvayler__removeCustomer(customer);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__removeCustomer(customer);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__removeCustomer", _Bank.class, new Class[] {_Customer.class}), new Object[] { customer } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@_Injected("renamed from removeCustomer and made private")
	private void __postvayler__removeCustomer(_Customer customer) {
		customers.remove(customer.getId());
	}
	
	@Persist
	public _Account createAccount() throws Exception {
		if (!__Postvayler.isBound()) { 
			return __postvayler__createAccount();
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			return __postvayler__createAccount();
		}
		
		context.setInTransaction(true);
		try {
			return context.prevayler.execute(new MethodTransactionWithQuery<_Account>(
					this, new MethodCall("__postvayler__createAccount", _Bank.class, new Class[0]), new Object[0]));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected("renamed from createAccountFor and made private")
	private _Account __postvayler__createAccount() throws Exception {
		_Account account = new _Account(lastAccountId++);
		accounts.put(account.getId(), account);
		return account;
	}
	
	@Persist
	public void transferAmount(_Account from, _Account to, int amount) throws Exception {
		if (!__Postvayler.isBound()) { 
			__postvayler__transferAmount(from, to, amount);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__transferAmount(from, to, amount);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__transferAmount", _Bank.class, new Class[] {_Account.class, _Account.class, Integer.TYPE}), new Object[] { from, to, amount } ));
		} finally {
			context.setInTransaction(false);
		}
	}
	
	@_Injected("renamed from transferAmount and made private")
	private void __postvayler__transferAmount(_Account from, _Account to, int amount) throws Exception {
		if (from.getId() == to.getId()) {
			assert (from == to);
			throw new IllegalArgumentException("from and to are same accounts");
		}
		if (amount <= 0)
			throw new IllegalArgumentException("amount: " + amount);
		if (from.getBalance() < amount)
			throw new IllegalArgumentException("balance < amount");
		
		from.withdraw(amount);
		to.deposit(amount);
	}
	
	public _Customer getCustomer(int customerId) {
		return customers.get(customerId);
	}
	
	@Synch
	public List<_Customer> getCustomers() {
		if (!__Postvayler.isBound()) 
			return __postvayler__getCustomers();
		
		Context context = __Postvayler.getInstance();
		
		if (context.isInQuery() || context.isInTransaction()) {
			return __postvayler__getCustomers();
		}
		
		synchronized (context.root) {
			context.setInQuery(true);
		    try {
				return __postvayler__getCustomers();
			} finally {
				context.setInQuery(false);
			}
		}
	}

	@_Injected("renamed from getCustomers and made private")
	private List<_Customer> __postvayler__getCustomers() {
		return new ArrayList<_Customer>(customers.values());
	}

	@Synch
	public List<_Account> getAccounts() {
		if (!__Postvayler.isBound()) 
			return __postvayler__getAccounts();
		
		Context context = __Postvayler.getInstance();
		
		if (context.isInQuery() || context.isInTransaction()) {
			return __postvayler__getAccounts();
		}
		
		synchronized (context.root) {
			context.setInQuery(true);
		    try {
				return __postvayler__getAccounts();
			} finally {
				context.setInQuery(false);
			}
		}
	}

	@_Injected("renamed from getAccounts and made private")
	private List<_Account> __postvayler__getAccounts() {
		return new ArrayList<_Account>(accounts.values());
	}
	
	public _Account getAccount(int id) {
		return accounts.get(id);
	}

//	@_Injected
//	public final IsPersistent __postvayler_get(Long id) {
//		return __postvayler_pool.get(id);
//	}
//
//	@_Injected
//	public final Long __postvayler_put(IsPersistent persistent) {
//		if (persistent.__postvayler_getId() != null) {
//			// we throw error to halt Prevayler
//			throw new Error("object already has an id\n" + Utils.identityCode(persistent));
//		}
//		
//		Long id = __postvayler_pool.put(persistent); 
//		if (persistent == this) {
//			 this.__postvayler_Id = id;
//		}
//		return id; 
//	}
//
//	@_Injected
//	public final void __postvayler_onRecoveryCompleted() {
//		__postvayler_pool.switchToWeakValues();
//	}
	
	@_Injected
	public final File takeSnapshot() throws Exception {
		if (! __Postvayler.isBound())
		   throw new NotPersistentException("no postvayler context found");
		
		return __Postvayler.getInstance().prevayler.takeSnapshot();
	}

	public _Bank getSister() {
		return sister;
	}

	public void setSister(_Bank sister) {
		if (!__Postvayler.isBound()) { 
			__postvayler__setSister(sister);
			return;
		}
		
		Context context = __Postvayler.getInstance();
		if (context.isInTransaction()) { 
			__postvayler__setSister(sister);
			return;
		}
		
		context.setInTransaction(true);
		try {
			context.prevayler.execute(new MethodTransaction(
					this, new MethodCall("__postvayler__setSister", _Bank.class, new Class[] {_Bank.class}), new Object[] { sister } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@Persist
	private void __postvayler__setSister(_Bank sister) {
		this.sister = sister;
	}
	
}
