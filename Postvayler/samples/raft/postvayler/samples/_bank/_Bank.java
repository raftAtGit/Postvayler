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
import raft.postvayler.impl.ConstructorTransaction;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsPersistent;
import raft.postvayler.impl.IsRoot;
import raft.postvayler.impl.MethodCall;
import raft.postvayler.impl.MethodTransaction;
import raft.postvayler.impl.MethodTransactionWithQuery;
import raft.postvayler.impl.Pool;
import raft.postvayler.impl.Utils;

/**
 * 
 * @author  r a f t
 */
@Persistent 
public class _Bank extends _Organization implements Serializable, IsRoot, Storage {

	private static final long serialVersionUID = 1L;

	@_Injected private final Pool __postvayler_pool = new Pool();
	
	private final Map<Integer, _Customer> customers = new TreeMap<Integer, _Customer>();
	private final Map<Integer, _Account> accounts = new TreeMap<Integer, _Account>();

	private int lastCustomerId = 1;
	private int lastAccountId = 1;
	
	private _RichPerson owner;
	
	public _Bank() throws Exception {
		//@_Injected
		// a subclass constructor is running, let him do the job
		if (getClass() != _Bank.class)
			return;
		
		if (__Postvayler.isBound()) { 
			// there is already a persisted _Bank instance as root.
			// this is just an ordinary _Bank instance, so put this as other ordinary IsPersistent objects
			Context context = __Postvayler.getInstance();
			
			if (context.inTransaction()) {
				this.__postvayler_Id = context.root.__postvayler_put(this);
			} else {
			
				context.setInTransaction(true);
				try {
					this.__postvayler_Id = context.prevayler.execute(new ConstructorTransaction(
							this, new ConstructorCall<IsPersistent>(_Bank.class, new Class[0]), new Object[0]));
				} finally {
					context.setInTransaction(false);
				}
			}
		} else if (Context.isInRecovery()) {
			// we are in recovery, there is already a persisted _Bank instance as root. 
			// this is just an ordinary _Bank instance, so put this as other ordinary IsPersistent objects
			assert (Context.getRecoveryRoot() != this);
			this.__postvayler_Id = Context.getRecoveryRoot().__postvayler_put(this);
		} else {
			// no Postvayler, object will not have an id
		}
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
					this, new MethodCall("__postvayler__setOwner", _Bank.class, new Class[] {_RichPerson.class}), new Object[] { newOwner } ));
		} finally {
			context.setInTransaction(false);
		}
	}

	@_Injected("renamed from setOwner and made private")
	private void __postvayler__setOwner(_RichPerson newOwner) {
		if (this.owner != null) {
			this.owner.removeBank(this);
		}
		this.owner = newOwner;
		
		if (newOwner != null) {
			newOwner.addBank(this);
		}
	}
	
	@Persist
	public _Customer createCustomer(String name) throws Exception {
		if (!__Postvayler.isBound()) 
			return __postvayler__createCustomer(name);
		
		Context context = __Postvayler.getInstance();
		if (context.inTransaction()) 
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
		if (context.inTransaction()) 
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
		if (context.inTransaction()) { 
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
		if (context.inTransaction()) { 
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
		if (context.inTransaction()) { 
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
		if (context.inTransaction()) { 
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
		
		if (context.inQuery() || context.inTransaction()) {
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
		
		if (context.inQuery() || context.inTransaction()) {
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

	@_Injected
	public final IsPersistent __postvayler_get(Long id) {
		return __postvayler_pool.get(id);
	}

	@_Injected
	public final Long __postvayler_put(IsPersistent persistent) {
		if (persistent.__postvayler_getId() != null) {
			// we throw error to halt Prevayler
			throw new Error("object already has an id\n" + Utils.identityCode(persistent));
		}
		
		Long id = __postvayler_pool.put(persistent); 
		if (persistent == this) {
			 this.__postvayler_Id = id;
		}
		return id; 
	}

	@_Injected
	public final void __postvayler_onRecoveryCompleted() {
		__postvayler_pool.switchToWeakValues();
	}
	
	@_Injected
	public final File takeSnapshot() throws Exception {
		if (! __Postvayler.isBound())
		   throw new NotPersistentException("no postvayler context found");
		
		return __Postvayler.getInstance().prevayler.takeSnapshot();
	}

	 
}
