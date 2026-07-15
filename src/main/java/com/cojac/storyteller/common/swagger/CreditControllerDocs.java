package com.cojac.storyteller.common.swagger;

import com.cojac.storyteller.credit.dto.ChargeCreditRequest;
import com.cojac.storyteller.credit.dto.CreditDTO;
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
     * 크레딧 충전
     */
    @Operation(
            summary = "크레딧 충전",
            description = "크레딧 충전 API (mock 승인 - 항상 성공 처리)",
            parameters = {
                    @Parameter(name = "profileId", in = ParameterIn.PATH, description = "프로필 ID", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "충전할 크레딧 수량",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChargeCreditRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "크레딧을 성공적으로 충전했습니다", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없습니다.", content = @Content(mediaType = "application/json")),
            }
    )
    ResponseEntity<ResponseDTO<CreditDTO>> chargeCredit(@PathVariable Integer profileId, @RequestBody ChargeCreditRequest request);
}
