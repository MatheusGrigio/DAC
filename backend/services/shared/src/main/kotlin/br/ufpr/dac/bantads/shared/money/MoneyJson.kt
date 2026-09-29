package br.ufpr.dac.bantads.shared.money

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JacksonAnnotationsInside
@JsonSerialize(using = MoneySerializer::class)
@JsonDeserialize(using = MoneyDeserializer::class)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
annotation class MoneyJson
