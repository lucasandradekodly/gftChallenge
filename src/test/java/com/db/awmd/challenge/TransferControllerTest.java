package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransferControllerTest {

  private static final String ACCOUNT_ID1 = "acc-001";
  private static final String ACCOUNT_ID2 = "acc-002";
  private static final String URI = "/v1/transfer";

  private MockMvc mockMvc;

  @Autowired
  private TransferService transferService;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
    transferService.getTransferRepository().clearTransfers();
    accountsService.getAccountsRepository().clearAccounts();

    accountsService.createAccount(Account.builder()
                    .accountId(ACCOUNT_ID1)
                    .balance(BigDecimal.valueOf(100))
                    .build());
    accountsService.createAccount(Account.builder()
                    .accountId(ACCOUNT_ID2)
                    .balance(BigDecimal.valueOf(100))
                    .build());
  }

  @Test
  public void whenCreatingTransfer_theAccountsShouldHaveCorrectBalances() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
      .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID2, 50))).andExpect(status().isCreated());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(50.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(150.0);
  }

  @Test
  public void whenCreatingTransfer_theAccountsFromBallanceShouldBeAllowedToBeZero() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID2, 100))).andExpect(status().isCreated());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(0.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(200.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferMoreThanTheBalanceContains() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID2, 101))).andExpect(status().isBadRequest());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferZero() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID2, 0))).andExpect(status().isBadRequest());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferANegativeValue() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID2, -10))).andExpect(status().isBadRequest());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferToAnUnexistentAccount() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, "12345", 10))).andExpect(status().isNotFound());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferFromAnUnexistentAccount() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson("12345", ACCOUNT_ID2, 10))).andExpect(status().isNotFound());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  @Test
  public void whenCreatingTransfer_shouldNotAllowToTransferToTheSameAccount() throws Exception {
    this.mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
            .content(transferJson(ACCOUNT_ID1, ACCOUNT_ID1, 10))).andExpect(status().isBadRequest());

    Account account1 = accountsService.getAccount(ACCOUNT_ID1);
    Account account2 = accountsService.getAccount(ACCOUNT_ID2);
    assertThat(account1.getBalance().doubleValue()).isEqualTo(100.0);
    assertThat(account2.getBalance().doubleValue()).isEqualTo(100.0);
  }

  private String transferJson(String from, String to, double amount) {
    return "{\"accountFrom\": \"" + from + "\",\n" +
            "\"accountTo\": \"" + to + "\",\n" +
            "\"amount\": \"" + amount + "\"}";
  }

}
