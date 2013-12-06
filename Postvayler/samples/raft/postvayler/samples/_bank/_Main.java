package raft.postvayler.samples._bank;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import raft.postvayler.Postvayler;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.GCPreventingPrevayler;
import raft.postvayler.impl.IsRoot;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class _Main {

	public static void main(String[] args) throws Exception {
//		doStressTest();
		runEqualityTest();
	}
	
	/** IMPORTANT: this test must be run with persist directory empty: persist/raft.postvayler.samples._bank._Bank/ */
	public static void runEqualityTest() throws Exception {
		_Bank pojoBank = new _Bank();
		populateBank(pojoBank, new Random(42));
		System.out.println("populated pojo bank");
		
		_Bank bank = createPersistentBank();
		populateBank(bank, new Random(42));
		System.out.println("populated persisted bank");

		System.out.println("checking for equality");
		checkEqual(bank, pojoBank);
		System.out.println("-- initial populate test completed --");
		
		for (int i = 0; i < 10; i++) {
		
			long seed = new Random().nextLong();
			
			resetPrevayler();
	
			populateBank(pojoBank, new Random(seed));
			System.out.println("populated pojo bank");
			
			bank = createPersistentBank();
			populateBank(bank, new Random(seed));
			System.out.println("populated persisted bank");
	
			System.out.println("checking for equality");
			checkEqual(bank, pojoBank);
			System.out.println("-- load/continue test " + (i+1) + " completed --");
		}
		
		System.out.println("final customers: " + bank.getCustomers().size());
		
//		System.out.println("--");
//		for (_Customer cust : pojoBank.getCustomers()) {
//			System.out.println(cust.__postvayler_getId() + ":" + cust + ", phone: " + cust.getPhone());
//		}
	}
	
	public static void doStressTest() throws Exception {
		final _Bank bank = createPersistentBank();
		
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
		new Thread() {
			public void run() {
				while (true) {
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
		System.out.println("all threads completed");
		
	}
	
	private static void checkEqual(_Bank bank, _Bank pojoBank) throws Exception {
		List<_Customer> customers = bank.getCustomers();
		List<_Customer> pojoCustomers = pojoBank.getCustomers();
		
		if (customers.size() != pojoCustomers.size())
			throw new Exception("customer sizes differ " + customers.size() + ", " + pojoCustomers.size());
		
		for (int i = 0; i < customers.size(); i++) {
			_Customer customer = customers.get(i);
			_Customer pojoCustomer = pojoCustomers.get(i);
			
			checkEqual(customer, pojoCustomer);
			
			for (_Account account : customer.getAccounts()) {
				// check object identity
				if (bank.getAccount(account.getId()) != account)
					throw new Exception("test failed");
			}
		}

		List<_Account> accounts = bank.getAccounts();
		List<_Account> pojoAccounts = pojoBank.getAccounts();
		
		if (accounts.size() != pojoAccounts.size())
			throw new Exception("account sizes differ " + accounts.size() + ", " + pojoAccounts.size());
		
		for (int i = 0; i < accounts.size(); i++) {
			_Account account = accounts.get(i);
			_Account pojoAccount = pojoAccounts.get(i);
			
			checkEqual(account, pojoAccount);
		}
	}

	private static void checkEqual(_Customer customer, _Customer pojoCustomer) throws Exception {
		if (customer.getId() != pojoCustomer.getId())
			throw new Exception("test failed");
		
		if (!equals(customer.getName(), pojoCustomer.getName()))
			throw new Exception("test failed");

		if (!equals(customer.getPhone(), pojoCustomer.getPhone()))
			throw new Exception("test failed");
		
		if (customer.getAccounts().size() != pojoCustomer.getAccounts().size())
			throw new Exception("test failed");
	}
	
	private static void checkEqual(_Account account, _Account pojoAccount) throws Exception {
		if (account.getId() != pojoAccount.getId())
			throw new Exception("test failed");
		
		if (account.getBalance() != pojoAccount.getBalance())
			throw new Exception("test failed");
		
		if (!equals(account.getName(), pojoAccount.getName()))
			throw new Exception("test failed");

		_Customer owner = account.getOwner();
		_Customer pojoOwner = pojoAccount.getOwner();
		
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

	/** emulates what {@link Postvayler#create()} does*/
	private static _Bank createPersistentBank() throws Exception {
		PrevaylerFactory<IsRoot> factory = new PrevaylerFactory<IsRoot>();
		factory.configurePrevalenceDirectory("persist/raft.postvayler.samples._bank._Bank");
		factory.configurePrevalentSystem(new _Bank());
	
		Prevayler<IsRoot> prevayler = new GCPreventingPrevayler(factory.create());
		IsRoot root = prevayler.prevalentSystem();
		root.__postvayler_onRecoveryCompleted();
		
		new __Postvayler(prevayler, (_Bank)root);
		return (_Bank) root;
	}
	
	private static void resetPrevayler() throws Exception {
		__Postvayler.getInstance().prevayler.close();
		
		Field field = Context.class.getDeclaredField("instance");
		field.setAccessible(true);
		field.set(null, null);
	}

	
	private static void populateBank(_Bank bank, Random random) throws Exception {
		
		// add some initial customers and accounts
		for (int i = 0; i < 50 + random.nextInt(50); i++) {
			_Customer customer = bank.createCustomer("initial:" + random.nextInt());
			customer.addAccount(bank.createAccount());
		}
		
		int count = 1000 + random.nextInt(1000);
		
		for (int action = 0; action < count; action++) {
			
			int next = random.nextInt(12);
			
			switch (next) {
			 case 0: {
				 // create a customer via bank
				 _Customer customer = bank.createCustomer("create:" + random.nextInt());
				 break;
			 }
			 case 1: {
				 // create customer
				 _Customer customer = new _Customer("add:" + random.nextInt());
				 bank.addCustomer(customer);
				 break;
			 	}
			 case 2: {
				 // create customer via bank and set phone
				 _Customer customer = bank.createCustomer("create:" + random.nextInt());
				 customer.setPhone("phone:" +  + random.nextInt());
				 break;
			 	}
			 // set phones of some customers
			 case 3: {
				 List<_Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int ci = 0; ci < random.nextInt(Math.max(1, customers.size()/5)); ci++) {
					 _Customer customer = customers.get(random.nextInt(customers.size()));
					 customer.setPhone("phone:ran" + random.nextInt());
				 }
				 break;
			 	}
			 // remove some customers
			 case 4: {
				 List<_Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int i = 0; i < random.nextInt(Math.max(1, customers.size()/10)); i++) {
					 _Customer customer = customers.get(random.nextInt(customers.size()));
					 bank.removeCustomer(customer);
				 }
				 break;
			 	}
			 // create some accounts
			 case 5: {
				 bank.createAccount();
				 break;
			 }
			 // create some accounts and deposit money
			 case 6: {
				 bank.createAccount().deposit(10 + random.nextInt(50));
				 break;
			 }
			 // create some accounts and add to customers
			 case 7: {
				 List<_Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int ci = 0; ci < random.nextInt(Math.max(1, customers.size()/10)); ci++) {
					 _Customer customer = customers.get(random.nextInt(customers.size()));
					 _Account account = bank.createAccount();
					 account.setName("account:" + random.nextInt());
					 account.deposit(100 + random.nextInt(100));
					 customer.addAccount(account);
				 }
				 break;
			 	}
			 // transfer some money
			 case 8: 
			 case 9: 
			 case 10: 
			 case 11: {
				 List<_Account> accounts = bank.getAccounts();
				 if (accounts.size() < 2)
					 break;
				 
				 for (int i = 0; i < random.nextInt(10); i++) {
					 _Account from, to;
					 while ( (from = accounts.get(random.nextInt(accounts.size()))) == 
							 (to = accounts.get(random.nextInt(accounts.size())))) {}
					 
					 int amount = Math.min(from.getBalance(), to.getBalance()) / 3;
					 if (amount == 0)
						 continue;
					 
					 bank.transferAmount(from, to, amount);
				 }
				 
				 break;
			 }
			}
		}
	}
	
	
}
