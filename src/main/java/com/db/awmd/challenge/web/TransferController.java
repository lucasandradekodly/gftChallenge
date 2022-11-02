package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.dto.TransferDto;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.TransferRejectedException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/transfer")
@Slf4j
@AllArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transfer(@RequestBody @Valid TransferDto transfer) {
    log.info("Creating transfer from {} to {} in the amount of {}", transfer.getAccountFrom(), transfer.getAccountTo(), transfer.getAmount() );
    try {
      transferService.createTransfer(transfer);
      return new ResponseEntity<>(null, HttpStatus.OK);
    }
    catch (AccountNotFoundException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }
    catch (DuplicateAccountIdException | TransferRejectedException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

}
