package one.block.arisenjavaandroidkeystoresignatureprovider.errors

import one.block.arisenjava.error.signatureProvider.GetAvailableKeysError
import org.jetbrains.annotations.NotNull
import java.lang.Exception

/**
 * Error class that will be thrown from [one.block.arisenjavaandroidkeystoresignatureprovider.ArisenAndroidKeyStoreUtility.getAllAndroidKeyStoreKeysInRIXFormat]
 */
class QueryAndroidKeyStoreError : GetAvailableKeysError {
    constructor() : super()
    constructor(message: @NotNull String) : super(message)
    constructor(message: @NotNull String, exception: @NotNull Exception) : super(message, exception)
    constructor(exception: @NotNull Exception) : super(exception)
}