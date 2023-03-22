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

package org.springframework.web.cors.reactive;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.options;

/**
 * Test case for reactive {@link CorsUtils}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUtilsTests {

	/**
	 * 解释：检查给定的HTTP请求是否为跨域请求
	 * 它创建一个ServerHttpRequest对象，该对象表示一个请求，其中包含一个名为“ORIGIN”的HTTP头，该头指示请求的来源。
	 * 然后，它使用该请求调用CorsUtils.isCorsRequest()方法，该方法检查是否存在名为“ORIGIN”的HTTP头，如果存在则认为是跨域请求。
	 * 最后，它使用assertThat()方法和isTrue()断言来检查isCorsRequest()方法的返回值是否为true。如果是true，则表示该请求为跨域请求。
	 */
	@Test
	public void isCorsRequest() {
		ServerHttpRequest request = get("http://domain.example/").header(HttpHeaders.ORIGIN, "https://domain.com").build();
		assertThat(CorsUtils.isCorsRequest(request)).isTrue();
	}

	@Test
	public void isNotCorsRequest() {
		ServerHttpRequest request = get("/").build();
		assertThat(CorsUtils.isCorsRequest(request)).isFalse();
	}

	/**
	 * 解释：测试CorsUtils类中的isPreFlightRequest方法。
	 * 首先创建一个ServerHttpRequest对象，该对象模拟了一个跨域请求的情况，并且设置了请求头中的Origin和Access-Control-Request-Method字段。
	 * 然后，该方法调用了CorsUtils类中的isPreFlightRequest方法，并使用assertThat断言该方法返回值为true。
	 *
	 * 总体来说，该测试方法的目的是测试CorsUtils类中的isPreFlightRequest方法是否能够正确地判断是否为预检请求。
	 */
	@Test
	public void isPreFlightRequest() {
		ServerHttpRequest request = options("/")
				.header(HttpHeaders.ORIGIN, "https://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isTrue();
	}

	@Test
	public void isNotPreFlightRequest() {
		ServerHttpRequest request = get("/").build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();

		request = options("/").header(HttpHeaders.ORIGIN, "https://domain.com").build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();
	}

	@Test  // SPR-16262
	public void isSameOriginWithXForwardedHeaders() {
		String server = "mydomain1.example";
		testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
	}

	@Test  // SPR-16262
	public void isSameOriginWithForwardedHeader() {
		String server = "mydomain1.example";
		testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
	}

	/**
	 * 解释：用于测试CorsUtils类中的isSameOrigin()方法的行为
	 * 在这个测试中，使用MockServerHttpRequest构建一个GET请求，请求的URL是"http://mydomain1.example"，同时设置请求头中的"Origin"为"https://mydomain1.example"。然后调用CorsUtils.isSameOrigin(request)方法来判断请求是否来自相同的源。
	 * 由于请求的URL和请求头中的"Origin"使用了不同的协议（http和https），因此isSameOrigin()方法应该返回false。最后使用assertThat()方法来验证结果是否如预期。
	 */
	@Test  // SPR-16362
	@SuppressWarnings("deprecation")
	public void isSameOriginWithDifferentSchemes() {
		MockServerHttpRequest request = MockServerHttpRequest
				.get("http://mydomain1.example")
				.header(HttpHeaders.ORIGIN, "https://mydomain1.example")
				.build();
		assertThat(CorsUtils.isSameOrigin(request)).isFalse();
	}

	@SuppressWarnings("deprecation")
	private void testWithXForwardedHeaders(String serverName, int port,
			String forwardedProto, String forwardedHost, int forwardedPort, String originHeader) {

		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}

		MockServerHttpRequest.BaseBuilder<?> builder = get(url).header(HttpHeaders.ORIGIN, originHeader);
		if (forwardedProto != null) {
			builder.header("X-Forwarded-Proto", forwardedProto);
		}
		if (forwardedHost != null) {
			builder.header("X-Forwarded-Host", forwardedHost);
		}
		if (forwardedPort != -1) {
			builder.header("X-Forwarded-Port", String.valueOf(forwardedPort));
		}

		ServerHttpRequest request = adaptFromForwardedHeaders(builder);
		assertThat(CorsUtils.isSameOrigin(request)).isTrue();
	}

	@SuppressWarnings("deprecation")
	private void testWithForwardedHeader(String serverName, int port,
			String forwardedHeader, String originHeader) {

		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}

		MockServerHttpRequest.BaseBuilder<?> builder = get(url)
				.header("Forwarded", forwardedHeader)
				.header(HttpHeaders.ORIGIN, originHeader);

		ServerHttpRequest request = adaptFromForwardedHeaders(builder);
		assertThat(CorsUtils.isSameOrigin(request)).isTrue();
	}

	/**
	 * 解释：用于从转发的请求头中适配出一个 `ServerHttpRequest` 对象。
	 * @param builder
	 * @return
	 */
	// SPR-16668
	@SuppressWarnings("deprecation")
	private ServerHttpRequest adaptFromForwardedHeaders(MockServerHttpRequest.BaseBuilder<?> builder) {
		//1. 创建一个 `AtomicReference<ServerHttpRequest>` 对象，用于存放最终适配出来的 `ServerHttpRequest` 对象。
		AtomicReference<ServerHttpRequest> requestRef = new AtomicReference<>();

		//2. 将传入的 `MockServerHttpRequest.BaseBuilder<?>` 对象转换为 `MockServerWebExchange` 对象。

		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		//3. 创建一个 `org.springframework.web.filter.reactive.ForwardedHeaderFilter` 过滤器，并使用它对 `exchange` 进行过滤。
		//4. 在过滤器中，将 `exchange2` 中的 `ServerHttpRequest` 对象设置到之前创建的 `AtomicReference` 对象中。
		new org.springframework.web.filter.reactive.ForwardedHeaderFilter().filter(exchange, exchange2 -> {
			requestRef.set(exchange2.getRequest());
			return Mono.empty();
		}).block();//5. 最后通过 `block()` 方法阻塞当前线程，等待过滤器过滤完成，然后返回 `AtomicReference` 中存放的 `ServerHttpRequest` 对象。
		return requestRef.get();
	}

}
