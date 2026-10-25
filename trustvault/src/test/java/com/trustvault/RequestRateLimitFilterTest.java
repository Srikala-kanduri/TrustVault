package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestRateLimitFilterTest {

    @Test
    void limitsLoginRequestsPerForwardedClientAddress() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter(5, 2);

        assertEquals(200, send(filter, "/login", "203.0.113.4"));
        assertEquals(200, send(filter, "/login", "203.0.113.4"));

        MockHttpServletRequest blockedRequest = request(
                "/login", "203.0.113.4");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(
                blockedRequest,
                blockedResponse,
                new MockFilterChain());

        assertEquals(429, blockedResponse.getStatus());
        assertEquals("30", blockedResponse.getHeader("Retry-After"));
        assertEquals(
                "application/json;charset=UTF-8",
                blockedResponse.getContentType());
    }

    @Test
    void keepsSeparateClientsAndApiBucketsIndependent() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter(2, 1);

        assertEquals(200, send(filter, "/login", "203.0.113.5"));
        assertEquals(200, send(filter, "/login", "203.0.113.6"));
        assertEquals(200, send(filter, "/api/documents", "203.0.113.5"));
        assertEquals(200, send(filter, "/api/documents", "203.0.113.5"));
    }

    private int send(
            RequestRateLimitFilter filter,
            String path,
            String clientAddress) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(
                request(path, clientAddress),
                response,
                new MockFilterChain());
        return response.getStatus();
    }

    private MockHttpServletRequest request(String path, String clientAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        request.addHeader("X-Forwarded-For", clientAddress);
        return request;
    }
}
