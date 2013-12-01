package raft.postvayler.impl;


/**
 * 
 * @author  hakan eryargi (r a f t)
 */
class Utils {

	static boolean doParametersMatch(Class<?>[] paramTypes, Object[] values) {
		if (paramTypes.length != values.length)
			return false;
		
		for (int i = 0; i < paramTypes.length; i++) {
			if (values[i] == null)
				continue;
			if (!paramTypes[i].isAssignableFrom(values[i].getClass()))
				return false;
		}
		return true;
	}
	
	/** replaces {@link IsPersistent} arguments with {@link Reference} */
	static Object[] referenceArguments(Object[] arguments) {
		for (int i = 0; i < arguments.length; i++) {
			Object arg = arguments[i];
			if (arg instanceof IsPersistent) {
				IsPersistent persistent = (IsPersistent) arg;
//				IsPersistent stored = root.__postvayler_get(persistent.__postvayler_getId());
//				if (stored != persistent)
//					throw new Error("internal error"); // we throw error to halt Prevayler
				arguments[i] = new Reference(persistent);
			}
		}
		return arguments;
	}
	
	/** replaces {@link Reference} arguments with {@link IsPersistent} */
	static Object[] dereferenceArguments(IsRoot root, Object[] arguments) {
		for (int i = 0; i < arguments.length; i++) {
			Object arg = arguments[i];
			if (arg instanceof Reference) {
				IsPersistent persistent = root.__postvayler_get(((Reference)arg).id);
				// TODO can persistent be null? ie: garbage collected?
				arguments[i] = persistent;
			}
		}
		return arguments;
	}
}
