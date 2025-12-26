package com.idcard.ocr.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API service for ID Card OCR backend
 */
interface ApiService {

    /**
     * Process ID card images and extract data
     *
     * @param request OCR request with front and back images (base64 encoded)
     * @return OCR response with extracted data
     */
    @POST("ocr/idcard")
    suspend fun processIdCard(@Body request: OCRRequest): Response<OCRResponse>

    /**
     * Process only front of ID card
     *
     * @param frontImage Base64 encoded front image
     * @return Extracted front fields
     */
    @POST("ocr/front")
    suspend fun processFront(@Body frontImage: Map<String, String>): Response<FrontResponse>

    /**
     * Process only back of ID card
     *
     * @param backImage Base64 encoded back image
     * @return Extracted back fields
     */
    @POST("ocr/back")
    suspend fun processBack(@Body backImage: Map<String, String>): Response<BackResponse>
}
