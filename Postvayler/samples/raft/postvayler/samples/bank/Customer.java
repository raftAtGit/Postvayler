package raft.postvayler.samples.bank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;
import raft.postvayler.Synch;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class Customer extends Person {
	private static final long serialVersionUID = 1L;

	private int id;

	private final Map<Integer, Account> accounts = new TreeMap<Integer, Account>();
	
	public Customer(String name) {
		super(name);
	}
	
	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}
	
	@Persist
	public void addAccount(Account account) {
		if (account.getOwner() != null)
			throw new IllegalStateException("Account already has an owner");
		
		accounts.put(account.getId(), account);
	}
	
	@Synch
	public List<Account> getAccounts() {
		return new ArrayList<Account>(accounts.values());
	}

	@Override
	public String toString() {
		return "Customer:" + id + ":" + getName();
	}
	
}
