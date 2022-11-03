package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.domain.TransferState;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TransferRepositoryInMemory implements TransferRepository {

    private final Map<String, Transfer> transfers = new ConcurrentHashMap<>();

    @Override
    public void createTransfer(Transfer transfer) {
        Date date = new Date();
        transfer.setId(String.valueOf(date.getTime()));
        transfer.setDate(date);
        save(transfer);
    }

    @Override
    public List<Transfer> findByState(TransferState state) {
        return transfers.values().stream().filter(transfer -> transfer.getState() == state).collect(Collectors.toList());
    }

    @Override
    @Valid
    public void executeTransfer(Transfer transfer) throws SQLIntegrityConstraintViolationException {
        if (transfer.getAccountFrom().getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new SQLIntegrityConstraintViolationException();
        }
        transfer.getAccountFrom().setBalance(transfer.getAccountFrom().getBalance().subtract(transfer.getAmount()));
        transfer.getAccountTo().setBalance(transfer.getAccountTo().getBalance().add(transfer.getAmount()));
        transfer.setState(TransferState.COMPLETED);
    }

    @Override
    public void save(Transfer transfer) {
        transfers.put(transfer.getId(), transfer);
    }

    @Override
    public void clearTransfers() {
        transfers.clear();
    }

}
