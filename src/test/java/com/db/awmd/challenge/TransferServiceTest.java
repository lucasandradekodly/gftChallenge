package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferState;
import com.db.awmd.challenge.dto.TransferDto;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.TransferRejectedException;
import com.db.awmd.challenge.repository.TransferRepository;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.TransferService;
import com.sun.javafx.binding.StringFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransferServiceTest {

    private static final String ACCOUNT_ID1 = "acc-001";
    private static final String ACCOUNT_ID2 = "acc-002";

    @Mock
    private TransferRepository transferRepositoryMock;
    @Mock
    private AccountsService accountsServiceMock;
    @Mock
    private NotificationService notificationServiceMock;

    @InjectMocks
    private TransferService testObj;

    @Before
    public void setup() {
        testObj = new TransferService(transferRepositoryMock, accountsServiceMock, notificationServiceMock);
    }

    @Test(expected = DuplicateAccountIdException.class)
    public void testingCreatingATransfer_whenToAndFromAccountsAreTheSame_shouldThrowException() {
        TransferDto transfer = TransferDto.builder()
                .accountFrom(ACCOUNT_ID1)
                .accountTo(ACCOUNT_ID1)
                .amount(BigDecimal.valueOf(50.0))
                .build();

        testObj.createTransfer(transfer);

        verify(notificationServiceMock, times(0)).notifyAboutTransfer(any(), any());
    }

    @Test(expected = AccountNotFoundException.class)
    public void testingCreatingATransfer_whenTheAccountFromDoesntExist_shouldThrowException() {
        TransferDto transfer = TransferDto.builder()
                .accountFrom(ACCOUNT_ID1)
                .accountTo(ACCOUNT_ID2)
                .amount(BigDecimal.valueOf(50.0))
                .build();

        when(accountsServiceMock.getAccount(ACCOUNT_ID1)).thenReturn(null);

        testObj.createTransfer(transfer);

        verify(notificationServiceMock, times(0)).notifyAboutTransfer(any(), any());
    }

    @Test(expected = AccountNotFoundException.class)
    public void testingCreatingATransfer_whenTheAccountToDoesntExist_shouldThrowException() {
        TransferDto transfer = TransferDto.builder()
                .accountFrom(ACCOUNT_ID1)
                .accountTo(ACCOUNT_ID2)
                .amount(BigDecimal.valueOf(50.0))
                .build();

        Account account1 = Account.builder()
                .balance(BigDecimal.valueOf(100))
                .accountId(ACCOUNT_ID1)
                .build();

        when(accountsServiceMock.getAccount(ACCOUNT_ID1)).thenReturn(account1);
        when(accountsServiceMock.getAccount(ACCOUNT_ID2)).thenReturn(null);

        //act
        testObj.createTransfer(transfer);

        //assert
        verify(notificationServiceMock, times(0)).notifyAboutTransfer(any(), any());
    }

    @Test
    public void testingCreatingATransfer_whenBothAccountsExist_shouldExecuteTransfer() throws SQLIntegrityConstraintViolationException {
        TransferDto transfer = TransferDto.builder()
                .accountFrom(ACCOUNT_ID1)
                .accountTo(ACCOUNT_ID2)
                .amount(BigDecimal.valueOf(50.0))
                .build();

        Account account1 = Account.builder()
                .balance(BigDecimal.valueOf(100))
                .accountId(ACCOUNT_ID1)
                .build();

        Account account2 = Account.builder()
                .balance(BigDecimal.valueOf(100))
                .accountId(ACCOUNT_ID2)
                .build();

        when(accountsServiceMock.getAccount(ACCOUNT_ID1)).thenReturn(account1);
        when(accountsServiceMock.getAccount(ACCOUNT_ID2)).thenReturn(account2);

        //act
        testObj.createTransfer(transfer);

        //assert
        verify(transferRepositoryMock, times(1)).createTransfer(
                argThat(t -> {
                    assertEquals(t.getAccountFrom(), account1);
                    assertEquals(t.getAccountTo(), account2);
                    assertEquals(t.getAmount(), transfer.getAmount());
                    assertEquals(t.getState(), TransferState.PENDING);
                    return true;
                })
        );

        verify(transferRepositoryMock, times(1)).executeTransfer(
                argThat(t -> {
                    assertEquals(t.getAccountFrom(), account1);
                    assertEquals(t.getAccountTo(), account2);
                    assertEquals(t.getAmount(), transfer.getAmount());
                    assertEquals(t.getState(), TransferState.PENDING);
                    return true;
                })
        );

        verify(notificationServiceMock, times(1))
                .notifyAboutTransfer(account1, "Sent transfer to acc-002 in the amount of 50.0");
        verify(notificationServiceMock, times(1))
                .notifyAboutTransfer(account2, "Received transfer from acc-001 in the amount of 50.0");
    }

    @Test(expected = TransferRejectedException.class)
    public void testingCreatingATransfer_whenTheAccountDoesntHaveEnoughBalance_shouldThrowException() throws SQLIntegrityConstraintViolationException {
        TransferDto transfer = TransferDto.builder()
                .accountFrom(ACCOUNT_ID1)
                .accountTo(ACCOUNT_ID2)
                .amount(BigDecimal.valueOf(50.0))
                .build();

        Account account1 = Account.builder()
                .balance(BigDecimal.valueOf(100))
                .accountId(ACCOUNT_ID1)
                .build();

        Account account2 = Account.builder()
                .balance(BigDecimal.valueOf(100))
                .accountId(ACCOUNT_ID2)
                .build();

        when(accountsServiceMock.getAccount(ACCOUNT_ID1)).thenReturn(account1);
        when(accountsServiceMock.getAccount(ACCOUNT_ID2)).thenReturn(account2);

        doThrow(new SQLIntegrityConstraintViolationException()).when(transferRepositoryMock).executeTransfer(any());

        //act
        testObj.createTransfer(transfer);

        //assert
        verify(transferRepositoryMock, times(1)).createTransfer(
                argThat(t -> {
                    assertEquals(t.getAccountFrom(), account1);
                    assertEquals(t.getAccountTo(), account2);
                    assertEquals(t.getAmount(), transfer.getAmount());
                    assertEquals(t.getState(), TransferState.PENDING);
                    return true;
                })
        );

        verify(transferRepositoryMock, times(1)).save(
                argThat(t -> {
                    assertEquals(t.getState(), TransferState.FAILED);
                    return true;
                })
        );

        verify(notificationServiceMock, times(0)).notifyAboutTransfer(any(), any());
    }
}