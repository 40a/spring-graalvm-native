/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework;

import java.util.ArrayList;
import java.util.List;

import org.springframework.graalvm.extension.ComponentProcessor;
import org.springframework.graalvm.extension.NativeImageContext;
import org.springframework.graalvm.type.Type;

/**
 * Recognize spring.components that need transactional proxies and register them.
 *
 * @author Andy Clement
 */
public class TransactionalComponentProcessor implements ComponentProcessor {

	@Override
	public boolean handle(NativeImageContext imageContext, String componentType, List<String> classifiers) {
		Type type = imageContext.getTypeSystem().resolveName(componentType);
		boolean isInteresting =  (type != null && (type.isTransactional() || type.hasTransactionalMethods())); 
		return isInteresting;
	}

	@Override
	public void process(NativeImageContext imageContext, String componentType, List<String> classifiers) {
		Type type = imageContext.getTypeSystem().resolveName(componentType);
		List<String> transactionalInterfaces = new ArrayList<>();
		if (transactionalInterfaces.size()==0) {
			imageContext.log("TransactionalComponentProcessor: unable to find interfaces to proxy on "+componentType);
			return;
		}
		for (Type intface: type.getInterfaces()) {
			transactionalInterfaces.add(intface.getDottedName());
		}
		transactionalInterfaces.add("org.springframework.aop.SpringProxy");
		transactionalInterfaces.add("org.springframework.aop.framework.Advised");
		transactionalInterfaces.add("org.springframework.core.DecoratingProxy");
		imageContext.addProxy(transactionalInterfaces);
		imageContext.log("TransactionalComponentProcessor: creating proxy for these interfaces: "+transactionalInterfaces);
	}

}
