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

@Service
@AllArgsConstructor
@Slf4j
public class TransferService {

    @Getter
    private final TransferRepository transferRepository;

    @Getter
    private final AccountsService accountsService;

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
        performTransfer(transfer);
    }

    public void performTransfer(Transfer transfer) {
        synchronized (transfer.getAccountFrom()) {
            synchronized (transfer.getAccountTo()) {
                try {
                    transferRepository.performTransfer(transfer);
                    log.info("Transfer with ID {} successful", transfer.getId());
                } catch (Exception e) {
                    transfer.setState(TransferState.FAILED);
                    transferRepository.save(transfer);
                    log.info("Transfer with ID {} NOT successful", transfer.getId());
                    throw new TransferRejectedException("Transfer cannot be processed.");
                }
            }
        }
    }

    private Account getAccount(String accountId){
        Account account = accountsService.getAccount(accountId);
        if(account == null) {
            throw new AccountNotFoundException(StringFormatter.format("Account %s doesn't exist.", accountId));
        }
        return account;
    }
}
