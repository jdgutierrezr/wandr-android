package com.kotlin.wandr.core

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.error.ErrorMapper
import com.kotlin.wandr.core.error.NotAuthenticatedException
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.error.safeCall
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test
    fun `rpc exception message reaches the user as is`() {
        val rpcError = PostgrestRestException("This event is full", null, null, "P0001", mockk(relaxed = true))
        assertEquals(AppError.Server("This event is full"), ErrorMapper.map(rpcError))
    }

    @Test
    fun `io errors are network errors`() {
        assertEquals(AppError.Network, ErrorMapper.map(IOException("timeout")))
    }

    @Test
    fun `missing session is unauthorized`() {
        assertEquals(AppError.Unauthorized, ErrorMapper.map(NotAuthenticatedException()))
    }

    @Test
    fun `safeCall wraps failures in AppException`() = runTest {
        val result = safeCall { throw IOException("down") }
        assertTrue(result.exceptionOrNull() is AppException)
        assertEquals(AppError.Network, result.exceptionOrNull()!!.appError)
    }

    @Test(expected = CancellationException::class)
    fun `safeCall never swallows cancellation`() = runTest {
        safeCall { throw CancellationException("cancelled") }
    }
}
