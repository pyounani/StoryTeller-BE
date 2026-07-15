package com.cojac.storyteller.common.swagger;

import com.cojac.storyteller.credit.dto.ConfirmPaymentRequest;
import com.cojac.storyteller.credit.dto.CreateOrderRequest;
import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.dto.CreditOrderDTO;
import com.cojac.storyteller.response.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Credit Controller", description = "동화책 생성 크레딧 관련 API")
public interface CreditControllerDocs {

    /**
     * 크레딧 잔액 조회
     */
    @Operation(
            summary = "크레딧 잔액 조회",
            description = "프로필의 현재 크레딧 잔액 조회 API",
            parameters = {
                    @Parameter(name = "profileId", in = ParameterIn.PATH, description = "프로필 ID", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "크레딧 잔액을 성공적으로 조회했습니다", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없습니다.", content = @Content(mediaType = "application/json")),
            }
    )
    ResponseEntity<ResponseDTO<CreditDTO>> getBalance(@PathVariable Integer profileId);

    /**
     * 결제 주문 생성
     */
    @Operation(
            summary = "결제 주문 생성",
            description = "토스페이먼츠 결제위젯 호출 전, 서버가 주문 ID와 결제 금액을 먼저 기록하는 API. " +
                    "이후 승인(confirm) 시 클라이언트가 보낸 금액과 대조해 위변조를 막는 데 사용된다.",
            parameters = {
                    @Parameter(name = "profileId", in = ParameterIn.PATH, description = "프로필 ID", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "충전할 크레딧 수량",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateOrderRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "결제 주문이 성공적으로 생성되었습니다", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없습니다.", content = @Content(mediaType = "application/json")),
            }
    )
    ResponseEntity<ResponseDTO<CreditOrderDTO>> createOrder(@PathVariable Integer profileId, @RequestBody CreateOrderRequest request);

    /**
     * 결제 승인 확정
     */
    @Operation(
            summary = "결제 승인 확정",
            description = "토스페이먼츠 결제위젯 완료 후 전달받은 paymentKey/orderId/amount로 토스 승인(confirm) API를 호출해 " +
                    "결제를 최종 확정하고 크레딧을 지급하는 API",
            parameters = {
                    @Parameter(name = "profileId", in = ParameterIn.PATH, description = "프로필 ID", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "토스 결제위젯 완료 후 전달받은 결제 정보",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfirmPaymentRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "크레딧을 성공적으로 충전했습니다", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "결제 금액이 일치하지 않습니다.", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "결제 주문을 찾을 수 없습니다.", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "502", description = "결제 승인에 실패했습니다.", content = @Content(mediaType = "application/json")),
            }
    )
    ResponseEntity<ResponseDTO<CreditDTO>> confirmPayment(@PathVariable Integer profileId, @RequestBody ConfirmPaymentRequest request);
}
