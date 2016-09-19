package raft.postvayler.spring;

import java.util.LinkedHashSet;
import java.util.Set;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;

import org.prevayler.PrevaylerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import raft.postvayler.Postvayler;
import raft.postvayler.impl.RootHolder;

/** Registers Postvayler root bean into Spring's {@link BeanDefinitionRegistry} so it can be {@link Autowired}. */
class BeanProcessor implements BeanDefinitionRegistryPostProcessor, BeanPostProcessor {

	private static final String ROOT_BEAN_NAME = "postvayler.root";
	
	private EnablePostvayler enablePostvayler;

	/** Does nothing. */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// we actually only add a new bean definition, but do not post process the existing definitions
		System.out.println("postProcessBeanFactory");
	}

	/** Registers Postvayler root bean into given {@link BeanDefinitionRegistry}. 
	 * This is required for autowiring root bean to other beans. */
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		
		System.out.println("postProcessBeanDefinitionRegistry");
		
		try {
			this.enablePostvayler = getConfigAnnotation(registry);
			
			BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(enablePostvayler.rootClass());
			registry.registerBeanDefinition(ROOT_BEAN_NAME, definitionBuilder.getBeanDefinition());
			
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ApplicationContextException("Couldn't register Postvayler root bean", e);
		}
	}
	
	/** Replaces Spring created root bean with Postvayler created one. */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!ROOT_BEAN_NAME.equals(beanName))
			return bean;
		
		Assert.isTrue(bean.getClass().getName().equals(enablePostvayler.rootClass()));
		
		try {
			// TODO configure Postvayler here. rollback support (food-tester) etc.
			PrevaylerFactory<RootHolder> factory = new PrevaylerFactory<RootHolder>();

			//factory.configurePrevalentSystem(new RootHolder());
			factory.configurePrevalenceDirectory("".equals(enablePostvayler.persistDir()) 
					? "persist/" + bean.getClass().getName() : enablePostvayler.persistDir());
			
			return new Postvayler<Object>((Class)bean.getClass())
					.setPrevaylerFactory(factory)
					.create();
			
		} catch (Exception e) {
			throw new BeanCreationException("Couldnt create '[" + ROOT_BEAN_NAME + "'] via Postvayler", e);
		}
		
	}

	/** Does nothing. */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/** Iterates over beans in registry and looks for the one with {@link EnablePostvayler} annotation. */
	private EnablePostvayler getConfigAnnotation(BeanDefinitionRegistry registry) throws Exception {
		Set<CtClass> foundClasses = new LinkedHashSet<CtClass>();
			
		ClassPool classPool = new ClassPool(true);
		classPool.insertClassPath(new ClassClassPath(getClass()));
		
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName); 
			System.out.println("\t" + beanName + " -> " + beanDefinition);
			
			if (beanDefinition.getFactoryMethodName() != null) {
				System.out.println("skipping bean definitition with factory method");
				continue;
			}
			
			String className = beanDefinition.getBeanClassName();
			if (!StringUtils.hasLength(className)) {
				System.out.println("warn, skipping null class for bean " + beanName);
				continue;
			}
			
			CtClass clazz = classPool.get(className);
			EnablePostvayler enablePostvayler = (EnablePostvayler) clazz.getAnnotation(EnablePostvayler.class);
			if (enablePostvayler == null)
				continue;
			System.out.println("found --" + clazz.getName());
			foundClasses.add(clazz);
		}
		
		if (foundClasses.isEmpty())
			throw new NoSuchBeanDefinitionException(EnablePostvayler.class, "No bean found with annotation @EnablePostvayler");
		if (foundClasses.size() > 1)
			throw new NoUniqueBeanDefinitionException(EnablePostvayler.class, foundClasses.size(), "Exactly one bean required with annotation @EnablePostvayler, found " + foundClasses.size());

		return (EnablePostvayler) foundClasses.iterator().next().getAnnotation(EnablePostvayler.class);
	}
	
}