package com.example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.coyote.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;


import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ListAssert.*;
//import static org.springframework.test.annotation.DirtiesContext.*;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	public void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	public void shouldNotReturnCashCardWithAnUnknownID() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	public void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00, null);
		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.postForEntity("/cashcards", newCashCard, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity(locationOfNewCashCard, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void shouldReturnAllCashCardsWhenListRequested() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		net.minidev.json.JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsOnly(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00);
	}

	@Test
	public void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	public void shouldReturnASortedPateOfCashCards() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	public void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00,123.45, 150.00);
	}

	@Test
	public void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("hank-owns-no-cards", "qrs456")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/102", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	public void shouldUpdateAnExistingCashCard() {
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	public void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99999",  HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard kumarsCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(kumarsCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/102",  HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	public void shouldDeleteAnExistingCard() {
		ResponseEntity<Void> response =restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void shouldNotDeleteACashCardThatDoesNotExist() {
		ResponseEntity<Void> response =restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		ResponseEntity<Void> response =restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("kumar2", "xyz789")
				.getForEntity("/cashcards/102", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
