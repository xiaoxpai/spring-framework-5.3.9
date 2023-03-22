/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import javax.servlet.ServletContextEvent;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.web.context.ContextCleanupListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class WebApplicationContextScopeTests {

	private static final String NAME = "scoped";


	private WebApplicationContext initApplicationContext(String scope) {
		//创建一个MockServletContext对象，这个对象是一个模拟的Servlet上下文，用于模拟Servlet容器的功能。
		MockServletContext sc = new MockServletContext();
		//创建一个GenericWebApplicationContext对象，并将MockServletContext对象作为构造函数的参数，这个对象是一个通用的Web应用程序上下文，用于管理Bean的生命周期。
		GenericWebApplicationContext ac = new GenericWebApplicationContext(sc);
		//创建一个GenericBeanDefinition对象，这个对象是一个通用的Bean定义对象，用于描述Bean的属性和行为。
		GenericBeanDefinition bd = new GenericBeanDefinition();
		//创建一个GenericBeanDefinition对象，并设置其Bean类为DerivedTestBean，这个对象是一个通用的Bean定义对象，用于描述Bean的属性和行为。
		bd.setBeanClass(DerivedTestBean.class);
		bd.setScope(scope);
		//将Bean定义对象注册到Web应用程序上下文中，使用NAME作为Bean的名称
		ac.registerBeanDefinition(NAME, bd);
		//调用Web应用程序上下文的refresh()方法，以便启动应用程序上下文并完成Bean的初始化。
		ac.refresh();
		return ac;
	}

	@Test
	public void testRequestScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_REQUEST);
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertThat(request.getAttribute(NAME)).isNull();
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertThat(request.getAttribute(NAME)).isSameAs(bean);
			assertThat(ac.getBean(NAME)).isSameAs(bean);
			requestAttributes.requestCompleted();
			assertThat(bean.wasDestroyed()).isTrue();
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testSessionScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_SESSION);
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertThat(request.getSession().getAttribute(NAME)).isNull();
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertThat(request.getSession().getAttribute(NAME)).isSameAs(bean);
			assertThat(ac.getBean(NAME)).isSameAs(bean);
			request.getSession().invalidate();
			assertThat(bean.wasDestroyed()).isTrue();
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testApplicationScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_APPLICATION);
		assertThat(ac.getServletContext().getAttribute(NAME)).isNull();
		DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
		assertThat(ac.getServletContext().getAttribute(NAME)).isSameAs(bean);
		assertThat(ac.getBean(NAME)).isSameAs(bean);
		new ContextCleanupListener().contextDestroyed(new ServletContextEvent(ac.getServletContext()));
		assertThat(bean.wasDestroyed()).isTrue();
	}

}
