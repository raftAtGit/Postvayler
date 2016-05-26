package raft.postvayler.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import raft.postvayler.compiler.Transformer;

@Configuration
//@EnableLoadTimeWeaving
class PostvaylerConfiguration implements ImportAware, LoadTimeWeaverAware { 

	private AnnotationAttributes annotationAttributes;
	private LoadTimeWeaver loadTimeWeaver;
	
	@Bean
	public static BeanProcessor postProcessor() {
		return new BeanProcessor();
	}
	
	// LoadTimeWeaverAware
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		System.out.println("setLoadTimeWeaver");
		this.loadTimeWeaver = loadTimeWeaver;
	}
	
	// ImportAware
	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		System.out.println("setImportMetadata");
		
		this.annotationAttributes = attributesFor(importMetadata, EnablePostvayler.class);
		if (this.annotationAttributes == null) {
			throw new IllegalArgumentException(
					"@EnableLoadTimeWeaving is not present on importing class " + importMetadata.getClassName());
		}
		
		if (loadTimeWeaver == null)
			throw new IllegalStateException("No LoadTimeWeaver! is LoadTimeWeaving enabled in this context?");
		
		String rootClass = annotationAttributes.getString("rootClass");
		
		try {
			loadTimeWeaver.addTransformer(new Transformer(rootClass));
		} catch (Exception e) {
			throw new IllegalStateException("Couldn't create Postvayler ClassFileTransformer", e);
		}
	}

	private static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, Class<?> annotationClass) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotationClass.getName(), true));
	}
	
	
}
