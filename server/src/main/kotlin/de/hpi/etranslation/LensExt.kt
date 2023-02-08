package de.hpi.etranslation

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensInjector

fun <IN, OUT> LensExtractor<IN, OUT>.extractCatching(target: IN): Result<OUT, LensFailure> = try {
    Ok(this@extractCatching.extract(target))
} catch (e: LensFailure) {
    Err(e)
}

fun <IN, OUT> LensInjector<IN, OUT>.injectCatching(value: IN, target: OUT): Result<OUT, LensFailure> = try {
    Ok(this@injectCatching.inject(value, target))
} catch (e: LensFailure) {
    Err(e)
}
