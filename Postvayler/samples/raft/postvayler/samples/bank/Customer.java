package raft.postvayler.samples.bank;

import java.util.Map;
import java.util.TreeMap;

import raft.postvayler.Key;
import raft.postvayler.Persistent;

/**
 * 
 * @author  hakan eryargi (r a f t)
 */
@Persistent
public class Customer extends Person {
	private static final long serialVersionUID = 1L;

	@Key(Bank.class)
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

	@Override
	public String toString() {
		return "Customer:" + id + ":" + getName();
	}
	
}
