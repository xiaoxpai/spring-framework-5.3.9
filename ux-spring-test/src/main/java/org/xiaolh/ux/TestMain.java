package org.xiaolh.ux;

import org.xiaolh.ux.service.MyService;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.xiaolh.ux.service.UserService;


public class TestMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(UserService.class);
		//MyService service = (MyService) context.getBean("myService");
		UserService service = (UserService) context.getBean("userService");
		service.getUserInfo();
	}
}
