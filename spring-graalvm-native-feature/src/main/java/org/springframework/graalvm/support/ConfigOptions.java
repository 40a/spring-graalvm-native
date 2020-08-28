/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.graalvm.support;

import java.util.Map;

import org.graalvm.nativeimage.hosted.Feature.DuringSetupAccess;
import org.springframework.graalvm.domain.reflect.ReflectionDescriptor;
import org.springframework.graalvm.type.TypeSystem;

import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;

/**
 * Encapsulate configurable feature behaviour.
 * 
 * @author Andy Clement
 * @author Sebastien Deleuze
 */
public abstract class ConfigOptions {
	
	private final static boolean IGNORE_HINTS_ON_EXCLUDED_CONFIG;

	private final static boolean REMOVE_UNUSED_AUTOCONFIG;

	private final static boolean REMOVE_YAML_SUPPORT;

	private final static boolean REMOVE_XML_SUPPORT;

	private final static boolean REMOVE_SPEL_SUPPORT;

	private final static boolean REMOVE_JMX_SUPPORT;
	
	private final static String DUMP_CONFIG;

	private final static boolean VERBOSE;

	private final static boolean FAIL_ON_VERSION_CHECK;
	
	private final static String MISSING_SELECTOR_HINTS;
	
	private final static boolean VERIFIER_ON;
	
	private static Mode MODE; // Default is 'reflection'
	
	// Temporary, for exploration
	private final static boolean SKIP_AT_BEAN_HINT_PROCESSING;
	private final static boolean SKIP_AT_BEAN_SIGNATURE_TYPES;

	static {
		IGNORE_HINTS_ON_EXCLUDED_CONFIG = Boolean.valueOf(System.getProperty("spring.native.ignore-hints-on-excluded-config","true"));
		if (!IGNORE_HINTS_ON_EXCLUDED_CONFIG) {
			System.out.println("Currently not processing any spring.autoconfigure.exclude property in application.properties)");
		}
		SKIP_AT_BEAN_HINT_PROCESSING = Boolean.valueOf(System.getProperty("spring.native.skip-at-bean-hint-processing", "false"));
		if (SKIP_AT_BEAN_HINT_PROCESSING) {
			System.out.println("Skipping @Bean hint processing");
		}
		SKIP_AT_BEAN_SIGNATURE_TYPES = Boolean.valueOf(System.getProperty("spring.native.skip-at-bean-signature-types-processing", "false"));
		if (SKIP_AT_BEAN_SIGNATURE_TYPES) {
			System.out.println("Skipping @Bean signature type processing");
		}
		String modeValue = System.getProperty("spring.native.mode");
		if (modeValue != null) {
			MODE = Mode.valueOf(modeValue.toUpperCase());
			if (MODE == null) {
				// Default
				MODE = Mode.REFLECTION;
			}
			System.out.println("Feature operating in "+MODE+" mode");
		}
		REMOVE_UNUSED_AUTOCONFIG = Boolean.valueOf(System.getProperty("spring.native.remove-unused-autoconfig", "true"));
		if(REMOVE_UNUSED_AUTOCONFIG) {
			System.out.println("Removing unused configurations");
		}
		VERIFIER_ON = Boolean.valueOf(System.getProperty("spring.native.verify","false"));
		if(VERIFIER_ON) {
			System.out.println("Verification turned on");
		}
		VERBOSE = Boolean.valueOf(System.getProperty("spring.native.verbose","false"));
		if (VERBOSE) {
			System.out.println("Turning on verbose mode for the feature");
		}
		FAIL_ON_VERSION_CHECK = Boolean.valueOf(System.getProperty("spring.native.fail-on-version-check","true"));
		if (!FAIL_ON_VERSION_CHECK) {
			System.out.println("Turning off Spring Boot version check");
		}
		MISSING_SELECTOR_HINTS = System.getProperty("spring.native.missing-selector-hints","error");
		if (MISSING_SELECTOR_HINTS.equals("warning")) {
			System.out.println("Selectors missing hints will be reported as a warning, not an error");
		} else if (!MISSING_SELECTOR_HINTS.equals("error")) {
			throw new IllegalStateException("Supported values for 'spring.native.missing-selector-hints' are 'error' (default) or 'warning'");
		}
		REMOVE_YAML_SUPPORT = Boolean.valueOf(System.getProperty("spring.native.remove-yaml-support", "false"));
		if (REMOVE_YAML_SUPPORT) {
			System.out.println("Removing Yaml support");
		}
		REMOVE_XML_SUPPORT = Boolean.valueOf(System.getProperty("spring.native.remove-xml-support", "false"));
		if (REMOVE_XML_SUPPORT) {
			System.out.println("Removing XML support");
		}
		REMOVE_SPEL_SUPPORT = Boolean.valueOf(System.getProperty("spring.native.remove-spel-support", "false"));
		if (REMOVE_SPEL_SUPPORT) {
			System.out.println("Removing SpEL support");
		}
		REMOVE_JMX_SUPPORT = Boolean.valueOf(System.getProperty("spring.native.remove-jmx-support", "false"));
		if (REMOVE_JMX_SUPPORT) {
			System.out.println("Removing JMX support");
		}
		DUMP_CONFIG = System.getProperty("spring.native.dump-config");
		if (DUMP_CONFIG!=null) {
			System.out.println("Dumping computed config to "+DUMP_CONFIG);
		}
	}

	public static boolean shouldRemoveUnusedAutoconfig() {
		return REMOVE_UNUSED_AUTOCONFIG;
	}
	
	public static boolean areMissingSelectorHintsAnError() {
		return MISSING_SELECTOR_HINTS.equals("error");
	}

	public static boolean isVerbose() {
		return VERBOSE;
	}

	public static boolean shouldFailOnVersionCheck() {
		return FAIL_ON_VERSION_CHECK;
	}

	public static boolean isVerifierOn() {
		return VERIFIER_ON;
	}

	public static boolean shouldDumpConfig() {
		return DUMP_CONFIG != null;
	}

	public static boolean shouldRemoveYamlSupport() {
		return REMOVE_YAML_SUPPORT;
	}

	public static boolean shouldRemoveXmlSupport() {
		return REMOVE_XML_SUPPORT;
	}

	public static boolean shouldRemoveSpelSupport() {
		return REMOVE_SPEL_SUPPORT;
	}

	public static boolean shouldRemoveJmxSupport() {
		return REMOVE_JMX_SUPPORT;
	}

	public static boolean isInitMode() {
		return getMode()==Mode.INIT;
	}
	
	public static boolean isAgentMode() {
		return getMode()==Mode.AGENT;
	}

	public static boolean isAnnotationMode() {
		return getMode()==Mode.REFLECTION;
	}

	public static boolean isFunctionalMode() {
		return getMode()==Mode.FUNCTIONAL;
	}

	public static boolean isIgnoreHintsOnExcludedConfig() {
		return IGNORE_HINTS_ON_EXCLUDED_CONFIG;
	}

	public static String getDumpConfigLocation() {
		return DUMP_CONFIG;
	}

	public static boolean isSkipAtBeanHintProcessing() {
		return SKIP_AT_BEAN_HINT_PROCESSING;
	}

	public static boolean isSkipAtBeanSignatureTypes() {
		return SKIP_AT_BEAN_SIGNATURE_TYPES;
	}

	public static Mode getMode() {
		return MODE;
	}
	
	/*
	 * Note - some similar inferencing for the substitutions is in FunctionalMode class.
	 */
	public static void ensureModeInitialized(DuringSetupAccess access) {
		if (MODE == null) {
			DuringSetupAccessImpl dsai = (DuringSetupAccessImpl) access;
			ImageClassLoader icl = dsai.getImageClassLoader();
			TypeSystem ts = TypeSystem.get(icl.getClasspath());
			if (ts.resolveDotted("org.springframework.init.func.InfrastructureInitializer", true) != null
					|| ts.resolveDotted("org.springframework.fu.kofu.KofuApplication", true) != null
					|| ts.resolveDotted("org.springframework.fu.jafu.JafuApplication", true) != null) {
				MODE = Mode.FUNCTIONAL;
			} else {
				Map<String, ReflectionDescriptor> reflectionConfigurationsOnClasspath = ts
						.getReflectionConfigurationsOnClasspath();
				for (ReflectionDescriptor reflectionDescriptor : reflectionConfigurationsOnClasspath.values()) {
					if (reflectionDescriptor
							.hasClassDescriptor("org.springframework.boot.autoconfigure.SpringBootApplication")) {
						MODE = Mode.AGENT;
						break;
					}
				}
			}
			if (MODE == null) {
				MODE = Mode.REFLECTION;
			}
			System.out.println("Inferred feature operating mode: " + MODE.name().toLowerCase());
		}
	}

}