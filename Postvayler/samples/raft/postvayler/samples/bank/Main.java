package raft.postvayler.samples.bank;

import raft.postvayler.Postvayler;
import raft.postvayler.Storage;

public class Main {

	private static void stress(Bank bank) {
		for (int i = 0; i < 10000; i++) {
			Customer customer = new Customer("name");
			if (Math.random() < 0.8) {
				customer.setPhone("bla bla");
			}
			if (Math.random() < 0.5) {
				bank.addCustomer(customer);
			}
			if (Math.random() < 0.1) {
				bank.removeCustomer(customer);
			}

			if (Math.random() < 0.001) {
				System.gc();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		final Bank bank = Postvayler.create(Bank.class);
		//Bank bank = new Bank();
		
		int customerCount = bank.getCustomers().size();
		System.out.println("got bank, customers: " + customerCount + " " + bank);

//		for (Customer customer : bank.getCustomers()) {
//			//customer.setPhone("phone:" + customer.getId());
//			System.out.println(customer.getPhone());
//		}

		for (int i = 0; i < 10; i++) {
			new Thread() {
				public void run() {
					stress(bank);
				};
			}.start();
		}
		
		stress(bank);
		
		//Person person = new Person();
		//Customer customer = new Customer("monica");
		
//		Storage storage = (Storage) bank;
//		storage.takeSnapshot();
//		System.out.println("took snapshot");
		
//		Customer customer = new Customer("name_" + customerCount);
//		bank.addCustomer(customer);
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
