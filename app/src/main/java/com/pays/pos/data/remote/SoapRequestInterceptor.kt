package com.pays.pos.data.remote

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class SoapRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Build the SOAP envelope XML structure
        val soapEnvelope = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://webservice.example.com/">
                <soapenv:Header/>
                <soapenv:Body>
                    <web:YourMethod>
                        <!-- Add your method parameters here -->
                    </web:YourMethod>
                </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()

        // Set the body as XML
        val mediaType = "text/xml; charset=utf-8".toMediaTypeOrNull()
        val body: RequestBody = soapEnvelope.toRequestBody(mediaType)

        // Modify the request with the SOAP envelope
        val newRequest = originalRequest.newBuilder()
            .post(body)
            .addHeader("Content-Type", "text/xml")
//            .addHeader("SOAPAction", "SOAPAction")
            .build()

        return chain.proceed(newRequest)
    }
}
