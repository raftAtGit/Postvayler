package raft.postvayler.samples.bank;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import raft.postvayler.Postvayler;
import raft.postvayler.impl.Context;

/**
 * Entry point of sample.
 * 
 * @author r a f t
 */
public class Main {

	public static void main(String[] args) throws Exception {
//		runStressTest();
		runEqualityTest();
	}
	
	/** IMPORTANT: this test must be run with persist directory empty: persist/raft.postvayler.samples.bank.Bank/ */
	public static void runEqualityTest() throws Exception {
		Bank pojoBank = new Bank();
		populateBank(pojoBank, new Random(42));
		System.out.println("populated pojo bank");
		
		Bank bank = Postvayler.create(Bank.class);
		populateBank(bank, new Random(42));
		System.out.println("populated persisted bank");

		System.out.println("checking for equality");
		checkEqual(bank, pojoBank);
		System.out.println("-- initial populate test completed --");
		
		for (int i = 0; i < 10; i++) {
		
			long seed = new Random().nextLong();
			
			resetPostvayler();
	
			populateBank(pojoBank, new Random(seed));
			System.out.println("populated pojo bank");
			
			bank = Postvayler.create(Bank.class);
			populateBank(bank, new Random(seed));
			System.out.println("populated persisted bank");
	
			System.out.println("checking for equality");
			checkEqual(bank, pojoBank);
			System.out.println("-- load/continue test " + (i+1) + " completed --");
		}
		
		System.out.println("final customers: " + bank.getCustomers().size());
	}
	
	public static void runStressTest() throws Exception {
		final Bank bank = Postvayler.create(Bank.class);
//		((Storage) bank).takeSnapshot();
		
		List<Thread> threads = new ArrayList<Thread>();
		
		for (int i = 0; i < 10; i++) {
			threads.add(new Thread() {
				public void run() {
					try {
						populateBank(bank, new Random());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				};
			});
		}
		final boolean[] doGC = new boolean[] {true};
		new Thread() {
			public void run() {
				while (doGC[0]) {
					System.gc();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
			};
		}.start();
		
		for (Thread t : threads) {
			t.start();
		}
		for (Thread t : threads) {
			t.join();
		}
		doGC[0] = false;
		System.out.println("all threads completed");
		
	}
	
	private static void checkEqual(Bank bank, Bank pojoBank) throws Exception {
		List<Customer> customers = bank.getCustomers();
		List<Customer> pojoCustomers = pojoBank.getCustomers();
		
		if (customers.size() != pojoCustomers.size())
			throw new Exception("customer sizes differ " + customers.size() + ", " + pojoCustomers.size());
		
		for (int i = 0; i < customers.size(); i++) {
			Customer customer = customers.get(i);
			Customer pojoCustomer = pojoCustomers.get(i);
			
			checkEqual(customer, pojoCustomer);
			
			for (Account account : customer.getAccounts()) {
				// check object identity
				if (bank.getAccount(account.getId()) != account)
					throw new Exception("test failed");
			}
		}

		List<Account> accounts = bank.getAccounts();
		List<Account> pojoAccounts = pojoBank.getAccounts();
		
		if (accounts.size() != pojoAccounts.size())
			throw new Exception("account sizes differ " + accounts.size() + ", " + pojoAccounts.size());
		
		for (int i = 0; i < accounts.size(); i++) {
			Account account = accounts.get(i);
			Account pojoAccount = pojoAccounts.get(i);
			
			checkEqual(account, pojoAccount);
		}
	}

	private static void checkEqual(Customer customer, Customer pojoCustomer) throws Exception {
		if (customer.getId() != pojoCustomer.getId())
			throw new Exception("test failed");
		
		try {
			if (!equals(customer.getName(), pojoCustomer.getName()))
				throw new Exception("test failed");
		} catch (UnsupportedOperationException e) {
			assert (customer.getClass().getName().equals("raft.postvayler.samples.bank.secret.SecretCustomer") 
					&& (pojoCustomer.getClass().getName().equals("raft.postvayler.samples.bank.secret.SecretCustomer"))); 
		}

		if (!equals(customer.getPhone(), pojoCustomer.getPhone()))
			throw new Exception("test failed");
		
		if (customer.getAccounts().size() != pojoCustomer.getAccounts().size())
			throw new Exception("test failed");
	}
	
	private static void checkEqual(Account account, Account pojoAccount) throws Exception {
		if (account.getId() != pojoAccount.getId())
			throw new Exception("test failed");
		
		if (account.getBalance() != pojoAccount.getBalance())
			throw new Exception("test failed");
		
		if (!equals(account.getName(), pojoAccount.getName()))
			throw new Exception("test failed");

		Customer owner = account.getOwner();
		Customer pojoOwner = pojoAccount.getOwner();
		
		if ((owner == null) ^ (pojoOwner == null))
			throw new Exception("test failed");
		
		if (owner != null)
			checkEqual(owner, pojoOwner);
	}
	
	private static boolean equals(Object o1, Object o2) {
		if (o1 == null) return (o2 == null);
		if (o2 == null) return false;
		return o1.equals(o2);
	}
	
	private static void resetPostvayler() throws Exception {
		Context.getInstance().prevayler.close();
		
		Field field = Context.class.getDeclaredField("instance");
		field.setAccessible(true);
		field.set(null, null);
		
		field = Postvayler.class.getDeclaredField("instance");
		field.setAccessible(true);
		field.set(null, null);
	}
	
	private static void populateBank(Bank bank, Random random) throws Exception {
		
		// this call demonstrates @Include is implemented at compiler 
		bank.addCustomer((Customer) Class.forName("raft.postvayler.samples.bank.secret.SecretCustomer").newInstance());
		
		// add some initial customers and accounts and also owner
		bank.setOwner(new RichPerson("kingpin"));
		
		for (int i = 0; i < 50 + random.nextInt(50); i++) {
			Customer customer = bank.createCustomer("initial:" + random.nextInt());
			customer.addAccount(bank.createAccount());
		}
		
		
		int count = 1000 + random.nextInt(1000);
		
		for (int action = 0; action < count; action++) {
			
			int next = random.nextInt(BANK_ACTIONS);
			doSomethingRandomWithBank(bank, next, random);
		}
	}
	
	static final int BANK_ACTIONS = 19;

	private static void doSomethingRandomWithBank(Bank bank, int action, Random random) throws Exception {
		switch (action) {
			 case 0: {
				 // create a customer via bank
				 Customer customer = bank.createCustomer("create:" + random.nextInt());
				 break;
			 }
			 case 1: {
				 // create customer
				 Customer customer = new Customer("add:" + random.nextInt());
				 bank.addCustomer(customer);
				 break;
			 	}
			 case 2: {
				 // create a detached customer
				 Customer customer = new Customer("add:" + random.nextInt());
				 customer.setPhone("phone" + random.nextInt());
				 break;
			 	}
			 case 3: {
				 // create customer via bank and set phone
				 Customer customer = bank.createCustomer("create:" + random.nextInt());
				 customer.setPhone("phone:" +  + random.nextInt());
				 break;
			 	}
			 // set phones of some customers
			 case 4: {
				 List<Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int ci = 0; ci < random.nextInt(Math.max(1, customers.size()/5)); ci++) {
					 Customer customer = customers.get(random.nextInt(customers.size()));
					 customer.setPhone("phone:ran" + random.nextInt());
				 }
				 break;
			 	}
			 // remove some customers
			 case 5: {
				 List<Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int i = 0; i < random.nextInt(Math.max(1, customers.size()/10)); i++) {
					 Customer customer = customers.get(random.nextInt(customers.size()));
					 bank.removeCustomer(customer);
				 }
				 break;
			 	}
			 // create some accounts
			 case 6: {
				 bank.createAccount();
				 break;
			 }
			 // create some accounts and deposit money
			 case 7: {
				 bank.createAccount().deposit(10 + random.nextInt(50));
				 break;
			 }
			 // create some accounts and add to customers
			 case 8: {
				 List<Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int ci = 0; ci < random.nextInt(Math.max(1, customers.size()/10)); ci++) {
					 Customer customer = customers.get(random.nextInt(customers.size()));
					 Account account = bank.createAccount();
					 account.setName("account:" + random.nextInt());
					 account.deposit(100 + random.nextInt(100));
					 customer.addAccount(account);
				 }
				 break;
			 	}
			 // transfer some money
			 case 9: 
			 case 10: 
			 case 11: 
			 case 12: {
				 List<Account> accounts = bank.getAccounts();
				 if (accounts.size() < 2)
					 break;
				 
				 for (int i = 0; i < random.nextInt(10); i++) {
					 Account from, to;
					 while ( (from = accounts.get(random.nextInt(accounts.size()))) == 
							 (to = accounts.get(random.nextInt(accounts.size())))) {}
					 
					 int amount = Math.min(from.getBalance(), to.getBalance()) / 3;
					 if (amount == 0)
						 continue;
					 
					 bank.transferAmount(from, to, amount);
				 }
				 
				 break;
			 }
			 // assign bank an owner 
			 case 13: {
				 RichPerson richPerson = new RichPerson("richie rich:" + random.nextInt());
				 bank.setOwner(richPerson);
				 break;
			 }
			 // create another bank   
			 case 14: {
				 Bank other = new Bank();
				 RichPerson rich = bank.getOwner(); 
				 if (rich != null) {
					 other.setOwner(rich);
				 }
				 break;
			 }
			 // create another bank and create an owner if there is none   
			 case 15: {
				 Bank other = new Bank();
				 RichPerson rich = bank.getOwner(); 
				 if (rich == null) {
					 rich = new RichPerson("richie rich:" + random.nextInt());
					 bank.setOwner(rich);
				 }
				 other.setOwner(rich);
				 break;
			 }
			 // create a central bank and create an owner if there is none   
			 case 16: {
				 CentralBank central = new CentralBank();
				 RichPerson rich = bank.getOwner(); 
				 if (rich == null) {
					 rich = new RichPerson("richie rich:" + random.nextInt());
					 bank.setOwner(rich);
				 }
				 central.setOwner(rich);
				 break;
			 }
			 
			 
			 // create a detached bank and do something random on it   
			 case 17: {
				 Bank other = new Bank();
				 for (int i = 0; i < 50 + random.nextInt(100); i++) {
					 // -2 to avoid stack overflow
					 doSomethingRandomWithBank(other, random.nextInt(BANK_ACTIONS-2), random);
				 }
				 break;
			 }
			 // do something random with other banks
			 case 18: {
				 RichPerson owner = bank.getOwner();
				 if (owner != null) {
					 List<Bank> banks = owner.getBanks();
					 if (banks.size() > 1) {
						 Bank otherBank = banks.get(random.nextInt(banks.size()-1)+1);
						 // -2 to avoid stack overflow
						 doSomethingRandomWithBank(otherBank, random.nextInt(BANK_ACTIONS-2), random);
					 }
				 }
				 break;
			 }
		}	
	}
	
	
}
