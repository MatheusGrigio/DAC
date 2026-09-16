package br.ufpr.dac.bantads.auth.web

import br.ufpr.dac.bantads.shared.error.ErroBody
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun malformado(): ResponseEntity<ErroBody> = ResponseEntity.badRequest().body(ErroBody.badRequest("Requisição malformada"))
}
