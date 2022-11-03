package com.db.awmd.challenge.config;

import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.TransferService;
import jdk.nashorn.internal.objects.annotations.Getter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ProcessPendingTransactions implements ApplicationRunner {

    private TransferService transferService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Transfer> pendingTransfers = transferService.findPendingTransfers();
        log.info("Found {} pending transfers waiting to be processed.", pendingTransfers.size());

        for(Transfer transfer : pendingTransfers) {
            log.info("Processing transfer with ID {}", transfer.getId());
            transferService.makeTransfer(transfer);
        }
    }
}
