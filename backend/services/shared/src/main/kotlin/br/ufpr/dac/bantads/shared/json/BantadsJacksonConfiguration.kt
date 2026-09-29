package br.ufpr.dac.bantads.shared.json

import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDateTime

@Configuration
class BantadsJacksonConfiguration {
    @Bean
    fun bantadsJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder.featuresToDisable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
            )
            builder.featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            builder.serializerByType(LocalDateTime::class.java, LocalDateTimeSerializer(DateTimes.FORMATTER))
            builder.deserializerByType(LocalDateTime::class.java, LocalDateTimeDeserializer(DateTimes.FORMATTER))
        }
}
