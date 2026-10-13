package com.example.pokedex.web.error;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.FailingController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.FailingController.class})
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unexpectedErrorReturnsJsonBodyWithStatusMessageAndTimestamp() throws Exception {
		mockMvc.perform(get("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Unexpected error"))
				.andExpect(jsonPath("$.fieldErrors").value(emptyIterable()))
				.andExpect(jsonPath("$.timestamp").value(not(blankOrNullString())));
	}

	@Test
	void unexpectedErrorDoesNotLeakInternalDetails() throws Exception {
		mockMvc.perform(get("/test/boom"))
				.andExpect(jsonPath("$.message").value(not("secret internal detail")));
	}

	@Test
	void unknownRouteKeepsItsStatusAndUsesTheCommonFormat() throws Exception {
		mockMvc.perform(get("/test/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Not Found"))
				.andExpect(jsonPath("$.fieldErrors").value(emptyIterable()))
				.andExpect(jsonPath("$.timestamp").value(not(blankOrNullString())));
	}

	@Test
	void unsupportedMethodKeepsItsStatusAndUsesTheCommonFormat() throws Exception {
		mockMvc.perform(delete("/test/boom"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Method Not Allowed"));
	}

	@Test
	void aBrokenBusinessRuleReturns400WithTheField() throws Exception {
		mockMvc.perform(get("/test/invalid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Invalid request"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must be between 1 and 50"));
	}

	@RestController
	static class FailingController {

		@GetMapping("/test/boom")
		String boom() {
			throw new IllegalStateException("secret internal detail");
		}

		@GetMapping("/test/invalid")
		String invalid() {
			throw new InvalidRequestException("size", "must be between 1 and 50");
		}
	}
}
