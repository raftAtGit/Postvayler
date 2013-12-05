package raft.postvayler.samples._bank;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class Test {

	static class Super {
		void doSth() {
			System.out.println("im parent");
		}
	}
	
	static class Sub extends Super {
		void doSth() {
			System.out.println("im Child");
		}
		
		void doAnother() {
			super.doSth();
		}
	}
	
	public static void main(String[] args) {
		Super  sup = new Sub();
		sup.doSth();
		Sub sub = new Sub();
		
		//sub.doAnother();
		
		Super s2 = ((Super)sub);
		s2.doSth();
		
	}
}
