package com.idcard.ocr.network

import com.google.gson.annotations.SerializedName

/**
 * Request model for OCR processing
 */
data class OCRRequest(
    @SerializedName("front_image")
    val frontImage: String,

    @SerializedName("back_image")
    val backImage: String
)

/**
 * Response model from OCR API
 */
data class OCRResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: OCRData,

    @SerializedName("message")
    val message: String? = null
)

/**
 * OCR data containing extracted fields
 */
data class OCRData(
    // Front side fields
    @SerializedName("f_nazwisko")
    val fNazwisko: String = "",

    @SerializedName("f_imiona")
    val fImiona: String = "",

    @SerializedName("f_obywatelstwo")
    val fObywatelstwo: String = "",

    @SerializedName("f_data_urodzenia")
    val fDataUrodzenia: String = "",

    @SerializedName("f_plec")
    val fPlec: String = "",

    @SerializedName("f_numer_ID")
    val fNumerId: String = "",

    @SerializedName("f_data_waznosci")
    val fDataWaznosci: String = "",

    @SerializedName("f_numer_kodu")
    val fNumerKodu: String = "",

    // Back side fields
    @SerializedName("b_seria_id")
    val bSeriaId: String = "",

    @SerializedName("b_numer_id")
    val bNumerId: String = "",

    @SerializedName("b_numer_ident")
    val bNumerIdent: String = "",

    @SerializedName("b_data_wydania")
    val bDataWydania: String = "",

    @SerializedName("b_kto_wydal")
    val bKtoWydal: String = "",

    @SerializedName("b_imiona_rodzicow")
    val bImionaRodzicow: String = "",

    @SerializedName("b_nazwisko_rodowe")
    val bNazwiskoRodowe: String = "",

    @SerializedName("b_miejsce_urodzenia")
    val bMiejsceUrodzenia: String = "",

    @SerializedName("MRZ")
    val mrz: String = ""
)

/**
 * Response for front-only processing
 */
data class FrontResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: Map<String, String>
)

/**
 * Response for back-only processing
 */
data class BackResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: Map<String, String>
)

/**
 * Error response model
 */
data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)
