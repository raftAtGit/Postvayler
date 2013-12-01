package raft.postvayler.samples.bank;

import raft.postvayler.Postvayler;

public class Main {
	
	public static void main(String[] args) throws Exception {
		Bank bank = Postvayler.create(Bank.class);
		//Bank bank = new Bank();
		
		int customerCount = bank.getCustomers().size();
		System.out.println("got bank, customers: " + customerCount);
		
		Customer customer = new Customer("name_" + customerCount);
		bank.addCustomer(customer);
//		bank.addCustomers(new Customer("name_" + customerCount++), 
//				new Customer("name_" + customerCount++), 
//				new Customer("name_" + customerCount++));
		
//		for (Customer customer : bank.getCustomers()) {
//			System.out.println(customer);
//		}
		
//		HeadQuarters head = new HeadQuarters();
//		head.setCity("istanbul");
//		head.setAddress("istiklal caddesi, beyoglu");
//		
//		bank.setHeadQuarters(head);
		
//		bank.getHeadQuarters().setCity("old istanbul");
//		System.out.println(bank.getHeadQuarters().getCity());
//		System.out.println(bank.getHeadQuarters().getAddress());
		
//		IsRoot root = (IsRoot) bank;
//		root.__postvayler_get(3L);
		
		//((IsRoot) bank).__postvayler_getNextId();
	}
}
