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
import raft.postvayler.impl.InitRootTransaction;
import raft.postvayler.impl.RootHolder;

/**
 * Entry point of sample.
 * 
 * @author r a f t
 */
public class _Main {

	public static void main(String[] args) throws Exception {
//		runStressTest();
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
	}
	
	public static void runStressTest() throws Exception {
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
	private static void checkEqual(_Bank bank, _Bank pojoBank) throws Exception {
		_RichPerson owner = bank.getOwner();
		_RichPerson pojoOwner = pojoBank.getOwner();
		
		if ((owner == null) ^ (pojoOwner == null))
			throw new Exception("owners differ");
		
		checkEqual(bank.getSister().getOwner(), pojoBank.getSister().getOwner());
		
		if (owner != null) {
			checkEqual(owner, pojoOwner);
			
			List<_Bank> banks = owner.getBanks();
			List<_Bank> pojoBanks = pojoOwner.getBanks();
			
			if (banks.size() != pojoBanks.size())
				throw new Exception("banks sizes differ");
			
			for (int i = 0; i < banks.size(); i++) {
				checkEqualWithoutOwner(banks.get(i), pojoBanks.get(i));
			}
		} else {
			checkEqualWithoutOwner(bank, pojoBank);
		}
			
	}	
	private static void checkEqualWithoutOwner(_Bank bank, _Bank pojoBank) throws Exception {
		if (bank.getClass() != pojoBank.getClass())
			throw new Exception("bank classes differ: " + bank.getClass() + ", " + pojoBank.getClass());
		
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
	private static void checkEqual(_RichPerson richPerson, _RichPerson pojoRichPerson) throws Exception {
		checkEqual((_Person)richPerson, (_Person)pojoRichPerson);
		
		checkEqual(richPerson.getSister(), pojoRichPerson.getSister());
		checkEqual(richPerson.getBrother(), pojoRichPerson.getBrother());
	}
	
	private static void checkEqual(_Customer customer, _Customer pojoCustomer) throws Exception {
		checkEqual((_Person)customer, (_Person)pojoCustomer);
		
		if (customer.getId() != pojoCustomer.getId())
			throw new Exception("test failed");
		
		if (customer.getAccounts().size() != pojoCustomer.getAccounts().size())
			throw new Exception("test failed");
	}
	
	private static void checkEqual(_Person person, _Person pojoPerson) throws Exception {
		if (!equals(person.getName(), pojoPerson.getName()))
			throw new Exception("test failed " + person.getName() + " != " + pojoPerson.getName());

		if (!equals(person.getPhone(), pojoPerson.getPhone()))
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
		PrevaylerFactory<RootHolder> factory = new PrevaylerFactory<RootHolder>();
		factory.configurePrevalenceDirectory("persist/raft.postvayler.samples._bank._Bank");
		
		//RootHolder root = new RootHolder();
		factory.configurePrevalentSystem(new RootHolder());

//		_Bank empty = new _Bank();
//		root.__postvayler_put(empty);
		
		Prevayler<RootHolder> prevayler = new GCPreventingPrevayler(factory.create());
		RootHolder root = prevayler.prevalentSystem();
		root.onRecoveryCompleted();
		
		new __Postvayler(prevayler, root);
		
		if (!root.isInitialized()) {
			prevayler.execute(new InitRootTransaction(_Bank.class));
		}
		
		return (_Bank) root.getRoot();
	}
	
	private static void resetPrevayler() throws Exception {
		__Postvayler.getInstance().prevayler.close();
		
		Field field = Context.class.getDeclaredField("instance");
		field.setAccessible(true);
		field.set(null, null);
	}

	
	private static void populateBank(_Bank bank, Random random) throws Exception {
		
		// add some initial customers and accounts and also owner
		bank.getOwner().setName("richie rich");
		
		if (bank.getSister() == null) {
			bank.setSister(new _Bank());
			bank.getSister().getOwner().setName("even more rich");
		}
		
		for (int i = 0; i < 50 + random.nextInt(50); i++) {
			_Customer customer = bank.createCustomer("initial:" + random.nextInt());
			customer.addAccount(bank.createAccount());
			System.out.print('.');
		}
		
		
		int count = 100 + random.nextInt(100);
		
		for (int action = 0; action < count; action++) {
			
			int next = random.nextInt(BANK_ACTIONS);
			doSomethingRandomWithBank(bank, next, random);
		}
		System.out.println();
	}
	
	static final int BANK_ACTIONS = 19;

	private static void doSomethingRandomWithBank(_Bank bank, int action, Random random) throws Exception {
		System.out.print('.');
		
		switch (action) {
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
				 // create a detached customer
				 _Customer customer = new _Customer("add:" + random.nextInt());
				 customer.setPhone("phone" + random.nextInt());
				 break;
			 	}
			 case 3: {
				 // create customer via bank and set phone
				 _Customer customer = bank.createCustomer("create:" + random.nextInt());
				 customer.setPhone("phone:" +  + random.nextInt());
				 break;
			 	}
			 // set phones of some customers
			 case 4: {
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
			 case 5: {
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
			 // create a RichPerson which will throw exception at Person constructor
			 case 9: {
				 try {
					 new _RichPerson("HellBoy");
				 } catch (IllegalArgumentException e) {}
				 break;
			 }
			 // create a RichPerson which will throw exception at RichPerson constructor
			 case 10: {
				 try {
					 new _RichPerson("Dracula");
				 } catch (IllegalArgumentException e) {}
				 break;
			 }
			 case 11: {
				 _RichPerson rich = bank.getOwner(); 
				 if (rich != null) {
					 rich.getSister().setName("sister:" + random.nextInt());
					 rich.getBrother().setName("brother:" + random.nextInt());
				 }
				 break;
			 }
			 // transfer some money
			 case 12: {
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
			 // assign bank an owner 
			 case 13: {
				 _RichPerson richPerson = new _RichPerson("richie rich:" + random.nextInt());
				 bank.setOwner(richPerson);
				 break;
			 }
			 // create another bank   
			 case 14: {
				 _Bank other = new _Bank();
				 _RichPerson rich = bank.getOwner(); 
				 if (rich != null) {
					 other.setOwner(rich);
				 }
				 break;
			 }
			 // create another bank and create an owner if there is none   
			 case 15: {
				 _Bank other = new _Bank();
				 _RichPerson rich = bank.getOwner(); 
				 if (rich == null) {
					 rich = new _RichPerson("richie rich:" + random.nextInt());
					 bank.setOwner(rich);
				 }
				 other.setOwner(rich);
				 break;
			 }
			 // create a central bank and create an owner if there is none   
			 case 16: {
				 _CentralBank central = new _CentralBank();
				 _RichPerson rich = bank.getOwner(); 
				 if (rich == null) {
					 rich = new _RichPerson("richie rich:" + random.nextInt());
					 bank.setOwner(rich);
				 }
				 central.setOwner(rich);
				 break;
			 }
			 
			 
			 // create a detached bank and do something random on it   
			 case 17: {
				 _Bank other = new _Bank();
				 for (int i = 0; i < 50 + random.nextInt(100); i++) {
					 // -2 to avoid stack overflow
					 doSomethingRandomWithBank(other, random.nextInt(BANK_ACTIONS-2), random);
				 }
				 break;
			 }
			 // do something random with other banks
			 case 18: {
				 _RichPerson owner = bank.getOwner();
				 if (owner != null) {
					 List<_Bank> banks = owner.getBanks();
					 if (banks.size() > 1) {
						 _Bank otherBank = banks.get(random.nextInt(banks.size()-1)+1);
						 // -2 to avoid stack overflow
						 doSomethingRandomWithBank(otherBank, random.nextInt(BANK_ACTIONS-2), random);
					 }
				 }
				 break;
			 }
		}	
	}
	
	
}
