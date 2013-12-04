package raft.postvayler.samples._bank;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

import raft.postvayler.Postvayler;
import raft.postvayler.impl.IsRoot;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class _Main {

	public static void main(String[] args) throws Exception {
		_Bank bank = createBank();
		
		_Customer customer = new _Customer("cust1");
		bank.addCustomer(customer);

//		bank.createCustomer("hey");
		
		System.out.println("--");
		for (_Customer cust : bank.getCustomers()) {
			System.out.println(cust.__postvayler_getId() + ":" + cust);
		}
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
}
