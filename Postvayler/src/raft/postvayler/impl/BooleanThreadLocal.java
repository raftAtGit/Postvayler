package raft.postvayler.impl;

/**
 * 
 * @author hakan eryargi (r a f t)
 */
public class BooleanThreadLocal extends ThreadLocal<Boolean> {

	private boolean initialValue;

	public BooleanThreadLocal() {
		this(false);
	}
	
	public BooleanThreadLocal(boolean initialValue) {
		this.initialValue = initialValue;
	}
	
	@Override
	protected Boolean initialValue() {
		return initialValue;
	}
	
	public boolean isSet() {
		return get().booleanValue();
	}
	
	public void set(boolean value) {
		super.set(Boolean.valueOf(value));
	}
}
