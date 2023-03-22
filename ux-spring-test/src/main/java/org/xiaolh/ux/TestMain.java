package org.xiaolh.ux;

import org.xiaolh.ux.service.MyService;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class TestMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyService.class);
		MyService service = (MyService) context.getBean("myService");
		service.hello();
	}
}
