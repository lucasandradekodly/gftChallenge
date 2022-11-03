package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferState;
import com.db.awmd.challenge.dto.TransferDto;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.TransferRejectedException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.repository.TransferRepository;
import com.sun.javafx.binding.StringFormatter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@AllArgsConstructor
@Slf4j
public class TransferService {

    @Getter
    private final TransferRepository transferRepository;

    @Getter
    private final AccountsService accountsService;

    @Getter
    private final NotificationService notificationService;

    private static final ReentrantLock ARBITRATOR = new ReentrantLock();

    public void createTransfer(TransferDto transferDto) {

        if (transferDto.getAccountTo().equals(transferDto.getAccountFrom())) {
            throw new DuplicateAccountIdException("Cannot transfer to the same account");
        }

        Account accountFrom = getAccount(transferDto.getAccountFrom());
        Account accountTo = getAccount(transferDto.getAccountTo());

        Transfer transfer = Transfer.builder()
                .accountFrom(accountFrom)
                .accountTo(accountTo)
                .amount(transferDto.getAmount())
                .state(TransferState.PENDING)
                .build();

        transferRepository.createTransfer(transfer);
        log.info("Registered new transfer from {} to {} in the amount of {}, with the ID {}",
                transfer.getAccountFrom(), transfer.getAccountTo(), transfer.getAmount(), transfer.getId() );

        makeTransfer(transfer);
    }

    public void makeTransfer(Transfer transfer) {
        TransferService.ARBITRATOR.lock();
        synchronized (transfer.getAccountFrom()) {
            synchronized (transfer.getAccountTo()) {
                TransferService.ARBITRATOR.unlock();
                try {
                    transferRepository.executeTransfer(transfer);
                    log.info("Transfer with ID {} successful", transfer.getId());
                } catch (Exception e) {
                    transfer.setState(TransferState.FAILED);
                    transferRepository.save(transfer);
                    log.info("Transfer with ID {} NOT successful", transfer.getId());
                    throw new TransferRejectedException("Transfer cannot be processed.");
                }
            }
        }

        notificationService.notifyAboutTransfer(transfer.getAccountFrom(),
                StringFormatter.format("Sent transfer to %s in the amount of %s", transfer.getAccountTo().getAccountId(), transfer.getAmount()).getValue());
        notificationService.notifyAboutTransfer(transfer.getAccountTo(),
                StringFormatter.format("Received transfer from %s in the amount of %s", transfer.getAccountFrom().getAccountId(), transfer.getAmount()).getValue());

    }

    public List<Transfer> findPendingTransfers() {
        return getTransferRepository().findByState(TransferState.PENDING);
    }

    private Account getAccount(String accountId){
        Account account = accountsService.getAccount(accountId);
        if(account == null) {
            throw new AccountNotFoundException(StringFormatter.format("Account %s doesn't exist.", accountId));
        }
        return account;
    }
}
