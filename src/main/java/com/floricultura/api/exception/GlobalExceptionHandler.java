package com.floricultura.api.exception;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

// um lugar so pra traduzir as exceptions em respostas HTTP bonitinhas
@RestControllerAdvice
public class GlobalExceptionHandler {

    // recurso nao encontrado (GET/PUT/DELETE em id que nao existe) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // regra do projeto: erro de validacao de body a gente devolve 422 e NAO o 400 padrao,
    // porque o json chegou certinho (sintaxe ok), o que ta errado sao os valores dos campos.
    // alem disso montamos a lista de qual campo falhou e por que.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorItem> campos = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.add(new ApiError.FieldErrorItem(fe.getField(), fe.getDefaultMessage()));
        }
        ApiError body = baseError(HttpStatus.UNPROCESSABLE_ENTITY, "Falha na validacao dos campos");
        body.setFieldErrors(campos);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // validacao de params/path (ex: @RequestParam com @Positive) tambem cai como 422.
    // tambem usamos isso quando o /search vem sem filtro nenhum: ai nao tem lista de campos,
    // entao devolvemos so a mensagem explicando o que faltou.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParamValidation(ConstraintViolationException ex) {
        var violations = ex.getConstraintViolations();
        if (violations == null || violations.isEmpty()) {
            return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }
        List<ApiError.FieldErrorItem> campos = new ArrayList<>();
        for (ConstraintViolation<?> cv : violations) {
            campos.add(new ApiError.FieldErrorItem(cv.getPropertyPath().toString(), cv.getMessage()));
        }
        ApiError body = baseError(HttpStatus.UNPROCESSABLE_ENTITY, "Falha na validacao dos campos");
        body.setFieldErrors(campos);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // e-mail/senha errados ou sessao de login ausente/expirada -> 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // conflito de regra de negocio -> 409.
    // detalhe importante: num POST a gente nunca devolve 404 por causa de um id relacionado
    // que nao existe. em vez disso o service joga ConflictException e cai aqui como 409.
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // violacao de integridade do banco (ex: email/nome unico duplicado) -> 409 tambem
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Violacao de integridade dos dados");
    }

    // recurso estatico/rota inexistente -> 404 (sem isso o catch-all abaixo mascarava como 500)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso nao encontrado");
    }

    // sort invalido na paginacao (ex: ?sort=campoQueNaoExiste) -> 422 em vez de 500.
    // sem isso o campo de ordenacao errado caia no catch-all generico e virava 500 confuso.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleSortInvalido(PropertyReferenceException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY,
                "Campo de ordenacao invalido: '" + ex.getPropertyName()
                        + "'. Use um campo real da entidade no formato 'campo,direcao' (ex: nome,asc).");
    }

    // json mal formado ou com tipo errado (ex: "preco": "abc", status que nao existe no enum) -> 400.
    // aqui sim e 400: o problema e a sintaxe/formato do corpo, nao a regra de negocio.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido ou mal formado. Confira o JSON e os tipos dos campos.");
    }

    // parametro de url/query com tipo errado (ex: /produtos/abc ou ?status=QUALQUER) -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Valor '" + ex.getValue() + "' invalido para o parametro '" + ex.getName() + "'.");
    }

    // header ou parametro obrigatorio faltando -> 400
    @ExceptionHandler({ MissingRequestHeaderException.class, MissingServletRequestParameterException.class })
    public ResponseEntity<ApiError> handleFaltando(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // metodo HTTP que a rota nao aceita (ex: PATCH /produtos/1) -> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMetodo(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Metodo " + ex.getMethod() + " nao e suportado nesta rota.");
    }

    // content-type diferente de application/json -> 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo como application/json.");
    }

    // qualquer outra coisa que escapou -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
    }

    // helper pra montar o ApiError sem fieldErrors
    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(baseError(status, message));
    }

    private ApiError baseError(HttpStatus status, String message) {
        return new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message);
    }
}
