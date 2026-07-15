package com.cojac.storyteller.credit.controller;

import com.cojac.storyteller.common.swagger.CreditControllerDocs;
import com.cojac.storyteller.credit.dto.ChargeCreditRequest;
import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.service.CreditService;
import com.cojac.storyteller.response.code.ResponseCode;
import com.cojac.storyteller.response.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles/{profileId}/credits")
@RequiredArgsConstructor
public class CreditController implements CreditControllerDocs {

    private final CreditService creditService;

    @GetMapping
    public ResponseEntity<ResponseDTO<CreditDTO>> getBalance(@PathVariable Integer profileId) {
        CreditDTO response = creditService.getBalance(profileId);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_GET_CREDIT_BALANCE.getStatus().value())
                .body(new ResponseDTO<>(ResponseCode.SUCCESS_GET_CREDIT_BALANCE, response));
    }

    @PostMapping("/charge")
    public ResponseEntity<ResponseDTO<CreditDTO>> chargeCredit(@PathVariable Integer profileId, @RequestBody ChargeCreditRequest request) {
        CreditDTO response = creditService.chargeCredit(profileId, request.getAmount());
        return ResponseEntity
                .status(ResponseCode.SUCCESS_CHARGE_CREDIT.getStatus().value())
                .body(new ResponseDTO<>(ResponseCode.SUCCESS_CHARGE_CREDIT, response));
    }
}
