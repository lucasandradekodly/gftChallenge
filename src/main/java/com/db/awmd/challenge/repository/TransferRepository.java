package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferState;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public interface TransferRepository {

    void createTransfer(Transfer transfer);

    List<Transfer> findByState(TransferState state);

    /**
     * This should be a query that updates the balances of the accounts and the transfer status in the same transaction
     */
    void executeTransfer(Transfer transfer) throws SQLIntegrityConstraintViolationException;

    void save(Transfer transfer);

    void clearTransfers();
}
