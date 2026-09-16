package br.ufpr.dac.bantads.auth.user

import br.ufpr.dac.bantads.auth.dto.VerificarRequest
import br.ufpr.dac.bantads.auth.dto.VerificarResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/verificar")
    fun verificar(
        @Valid @RequestBody body: VerificarRequest,
    ): ResponseEntity<VerificarResponse> {
        val resultado =
            authService.verificar(body.email, body.senha)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(resultado)
    }
}
