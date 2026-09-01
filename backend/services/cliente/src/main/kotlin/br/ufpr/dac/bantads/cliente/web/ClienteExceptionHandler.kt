package br.ufpr.dac.bantads.cliente.web

import br.ufpr.dac.bantads.shared.error.ErroBody
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ClienteExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun api(ex: ApiException): ResponseEntity<ErroBody> = ResponseEntity.status(ex.body.status).body(ex.body)

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun malformado(): ResponseEntity<ErroBody> = ResponseEntity.badRequest().body(ErroBody.badRequest("Requisição malformada"))

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun semIdentidade(): ResponseEntity<ErroBody> = ResponseEntity.status(403).body(ErroBody.forbidden("Acesso negado"))
}
