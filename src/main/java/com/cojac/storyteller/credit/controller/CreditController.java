package com.cojac.storyteller.credit.controller;

import com.cojac.storyteller.common.swagger.CreditControllerDocs;
import com.cojac.storyteller.credit.dto.ConfirmPaymentRequest;
import com.cojac.storyteller.credit.dto.CreateOrderRequest;
import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.dto.CreditOrderDTO;
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

    @PostMapping("/orders")
    public ResponseEntity<ResponseDTO<CreditOrderDTO>> createOrder(@PathVariable Integer profileId, @RequestBody CreateOrderRequest request) {
        CreditOrderDTO response = creditService.createOrder(profileId, request.getCreditAmount());
        return ResponseEntity
                .status(ResponseCode.SUCCESS_CREATE_ORDER.getStatus().value())
                .body(new ResponseDTO<>(ResponseCode.SUCCESS_CREATE_ORDER, response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ResponseDTO<CreditDTO>> confirmPayment(@PathVariable Integer profileId, @RequestBody ConfirmPaymentRequest request) {
        CreditDTO response = creditService.confirmCharge(request);
        return ResponseEntity
                .status(ResponseCode.SUCCESS_CONFIRM_PAYMENT.getStatus().value())
                .body(new ResponseDTO<>(ResponseCode.SUCCESS_CONFIRM_PAYMENT, response));
    }
}
