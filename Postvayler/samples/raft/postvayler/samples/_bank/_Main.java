package raft.postvayler.samples._bank;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import raft.postvayler.Postvayler;
import raft.postvayler.impl.Context;
import raft.postvayler.impl.IsRoot;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class _Main {

	public static void main(String[] args) throws Exception {
		doStressTest();
		runEqualityTest();
	}
	
	public static void doStressTest() throws Exception {
		final _Bank bank = createBank();
		
		for (int i = 0; i < 10; i++) {
			new Thread() {
				public void run() {
					try {
						stress(bank);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				};
			}.start();
		}
		
		stress(bank);
	}
	
	/** IMPORTANT: this test must be run with persist directory empty: persist/raft.postvayler.samples._bank._Bank/ */
	public static void runEqualityTest() throws Exception {
		_Bank pojoBank = new _Bank();
		populate(pojoBank, new Random(42));
		System.out.println("populated pojo bank");
		
		_Bank bank = createBank();
		populate(bank, new Random(42));
		System.out.println("populated persisted bank");

		System.out.println("checking for equality");
		checkEqual(bank, pojoBank);
		System.out.println("-- initial populate test completed --");
		
		for (int i = 0; i < 10; i++) {
		
			long seed = new Random().nextLong();
			
			resetPrevayler();
	
			populate(pojoBank, new Random(seed));
			System.out.println("populated pojo bank");
			
			bank = createBank();
			populate(bank, new Random(seed));
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
	
	private static void checkEqual(_Bank bank, _Bank pojoBank) throws Exception {
		List<_Customer> customers = bank.getCustomers();
		List<_Customer> pojoCustomers = pojoBank.getCustomers();
		
		if (customers.size() != pojoCustomers.size())
			throw new Exception("customer sizes differ " + customers.size() + ", " + pojoCustomers.size());
		
		for (int i = 0; i < customers.size(); i++) {
			_Customer customer = customers.get(i);
			_Customer pojoCustomer = pojoCustomers.get(i);
			
			checkEqual(customer, pojoCustomer);
		}
	}

	private static void checkEqual(_Customer customer, _Customer pojoCustomer) throws Exception {
		if (customer.getId() != pojoCustomer.getId())
			throw new Exception("test failed");
		
		if (!equals(customer.getName(), pojoCustomer.getName()))
			throw new Exception("test failed");

		if (!equals(customer.getPhone(), pojoCustomer.getPhone()))
			throw new Exception("test failed");
	}
	
	private static boolean equals(Object o1, Object o2) {
		if (o1 == null) return (o2 == null);
		if (o2 == null) return false;
		return o1.equals(o2);
	}

	/** emulates what {@link Postvayler#create()} does*/
	private static _Bank createBank() throws Exception {
		PrevaylerFactory<IsRoot> factory = new PrevaylerFactory<IsRoot>();
		factory.configurePrevalenceDirectory("persist/raft.postvayler.samples._bank._Bank");
		factory.configurePrevalentSystem(new _Bank());
	
		Prevayler<IsRoot> prevayler = factory.create();
		IsRoot root = prevayler.prevalentSystem();
		
		new __Postvayler(prevayler, root);
		return (_Bank) root;
	}
	
	private static void resetPrevayler() throws Exception {
		__Postvayler.getInstance().prevayler.close();
		
		Field field = Context.class.getDeclaredField("instance");
		field.setAccessible(true);
		field.set(null, null);
	}

	
	private static void populate(_Bank bank, Random random) throws Exception {
		int count = 100 + random.nextInt(100);
		
		for (int i = 0; i < count; i++) {
			int next = random.nextInt(5);
			switch (next) {
			 case 0: {
				 _Customer customer = new _Customer("add:" + random.nextInt());
				 bank.addCustomer(customer);
				 break;
			 }
			 case 1: {
				 _Customer customer = bank.createCustomer("create:" + random.nextInt());
				 break;
			 	}
			 case 2: {
				 _Customer customer = bank.createCustomer("create:" + random.nextInt());
				 customer.setPhone("phone:" +  + random.nextInt());
				 break;
			 	}
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
			 case 4: {
				 List<_Customer> customers = bank.getCustomers();
				 if (customers.isEmpty())
					 break;
				 for (int ci = 0; ci < random.nextInt(Math.max(1, customers.size()/10)); ci++) {
					 _Customer customer = customers.get(random.nextInt(customers.size()));
					 bank.removeCustomer(customer);
				 }
				 break;
			 	}
			}
		}
	}
	
	private static void stress(_Bank bank) throws Exception {
		for (int i = 0; i < 10000; i++) {

			if (Math.random() < 0.5) {
				bank.createCustomer("bla bla");
			}
			
			_Customer customer = new _Customer("name");
			if (Math.random() < 0.8) {
				customer.setPhone("bla bla");
			}
			if (Math.random() < 0.5) {
				bank.addCustomer(customer);
			}
			if (Math.random() < 0.1) {
				bank.removeCustomer(customer);
			}
			
//			if (Math.random() < 0.001) {
//				System.gc();
//			}
			System.out.println(Thread.currentThread());
		}
	}
	
}
