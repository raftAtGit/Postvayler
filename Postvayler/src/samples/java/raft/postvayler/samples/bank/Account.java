package raft.postvayler.samples.bank;

import java.io.Serializable;

import raft.postvayler.Persist;
import raft.postvayler.Persistent;

/**
 * An account in a bank
 * 
 * @author  r a f t
 */
@Persistent
public class Account implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int id;
	private String name;
	private Customer owner;
	private int balance = 0;

	Account(int id) throws Exception {
		this.id = id;
	}	
	
	public int getBalance() {
		return balance;
	}

	public int getId() {
		return id;
	}

	public Customer getOwner() {
		return owner;
	}

	void setOwner(Customer owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	@Persist
	public void setName(String name) {
		this.name = name;
	}
	
	@Persist
	public void deposit(int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("amount: " + amount);
		this.balance += amount;
	}
	
	@Persist
	public void withdraw(int amount) {
		if (amount <= 0)
			throw new IllegalArgumentException("amount: " + amount);
		if (balance < amount)
			throw new IllegalArgumentException("balance < amount");
		
		this.balance -= amount;
	}
	
	
}
