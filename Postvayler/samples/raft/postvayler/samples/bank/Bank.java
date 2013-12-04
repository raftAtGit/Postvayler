package raft.postvayler.samples.bank;

import java.io.Serializable;
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
public class Bank implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, Customer> customers = new TreeMap<Integer, Customer>();
	private final Map<Integer, Account> accounts = new TreeMap<Integer, Account>();

	// TODO tmp
	private Customer aCustomer;
	
	int id; 

	private int lastCustomerId = 1;
	
	public Bank() {}

	@Persist
	Customer createCustomer(String name) {
		Customer customer = new Customer(name);
		addCustomer(customer);
		return customer;
	}
	
	@Persist
	Integer addCustomer(Customer customer) {
		customer.setId(lastCustomerId++);
		customers.put(customer.getId(), customer);
		System.out.println("added customer, new size: " + customers.size());
		return customer.getId();
	}

	@Persist
	void addCustomers(Customer... customers) {
		System.out.println("add customers");
		for (Customer customer : customers) {
			addCustomer(customer);
		}
		System.out.println("added " + customers.length + ", customers new size: " + this.customers.size());
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

//	public HeadQuarters getHeadQuarters() {
//		return headQuarters;
//	}
//
//	@Persist
//	public void setHeadQuarters(HeadQuarters headQuarters) {
//		this.headQuarters = headQuarters;
//		headQuarters.bank = this;
//	}
	
	
	 
}
