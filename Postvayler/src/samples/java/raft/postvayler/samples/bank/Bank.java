package raft.postvayler.samples.bank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Include;
import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Synch;
import raft.postvayler.samples.bank.secret.SecretCustomer;

/**
 * A bank.
 * 
 * @author r a f t
 */
@Include(SecretCustomer.class)
@Persistent 
public class Bank extends Company {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, Customer> customers = new TreeMap<Integer, Customer>();
	private final Map<Integer, Account> accounts = new TreeMap<Integer, Account>();

	private int lastCustomerId = 1;
	private int lastAccountId = 1;
	
	public Bank() {}

	@Persist
	public Customer createCustomer(String name) {
		Customer customer = new Customer(name);
		addCustomer(customer);
		return customer;
	}
	
	@Persist
	public int addCustomer(Customer customer) {
		customer.setId(lastCustomerId++);
		customers.put(customer.getId(), customer);
		return customer.getId();
	}

	@Persist
	void addCustomers(Customer... customers) {
		for (Customer customer : customers) {
			addCustomer(customer);
		}
	}
	
	@Persist
	public Account createAccount() throws Exception {
		Account account = new Account(lastAccountId++);
		accounts.put(account.getId(), account);
		return account;
	}
	
	@Persist
	public void transferAmount(Account from, Account to, int amount) throws Exception {
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
	
	public Customer getCustomer(int customerId) {
		return customers.get(customerId);
	}
	
	@Synch
	public List<Customer> getCustomers() {
		return new ArrayList<Customer>(customers.values());
	}

	@Persist
	public void removeCustomer(Customer customer) {
		customers.remove(customer.getId());
	}

	@Synch
	public List<Account> getAccounts() {
		return new ArrayList<Account>(accounts.values());
	}
	
	public Account getAccount(int id) {
		return accounts.get(id);
	}
}
