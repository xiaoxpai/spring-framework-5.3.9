package org.xiaolh.ux.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserService {
	public void getUserInfo(){
		List<String>  list = new ArrayList<>();
		list.add("Olive");
		list.add("Lucy");
		list.add("Lily");
		list.add("Bob");
		list.add("Jack");
		list.add("Tom");
		list.add("Jerry");

		new ArrayList<>(list).forEach(System.out::println);
	}
}
