package one.block.arisenjavaandroidkeystoresignatureprovider

import one.block.arisenjava.error.signatureProvider.SignTransactionError
import one.block.arisenjava.error.utilities.RIXFormatterError
import one.block.arisenjava.interfaces.ISignatureProvider
import one.block.arisenjava.models.signatureProvider.ArisenTransactionSignatureRequest
import one.block.arisenjava.models.signatureProvider.ArisenTransactionSignatureResponse
import one.block.arisenjava.utilities.RIXFormatter
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.SIGN_TRANSACTION_PREPARE_FOR_SIGNING_GENERIC_ERROR
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.SIGN_TRANSACTION_RAW_SIGNATURE_IS_NULL
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.SIGN_TRANSACTION_UNABLE_TO_FIND_KEY_TO_SIGN
import org.bouncycastle.util.encoders.Hex
import java.security.KeyStore

/**
 * ARISEN signature provider for Android KeyStore
 * 
 * This provider only works with the SECP256R1 curve.
 * 
 * When a key gets generated in the Android KeyStore, or imported into it, the key is protected and cannot be read.
 *
 * @property password ProtectionParameter? - the password protection entity for adding, using and removing keys. Its default value is NULL. It is a private field and can only be set by calling [ArisenAndroidKeyStoreSignatureProvider.Builder.setPassword]
 * @property loadStoreParameter LoadStoreParameter? - the load KeyStore Parameter to load the KeyStore instance. Its default value is NULL. It is a private field and can only be set by calling [ArisenAndroidKeyStoreSignatureProvider.Builder.setLoadStoreParameter]
 */
class ArisenAndroidKeyStoreSignatureProvider private constructor() : ISignatureProvider {
    private var password: KeyStore.ProtectionParameter? = null
    private var loadStoreParameter: KeyStore.LoadStoreParameter? = null

    override fun signTransaction(arisenTransactionSignatureRequest: ArisenTransactionSignatureRequest): ArisenTransactionSignatureResponse {
        if (arisenTransactionSignatureRequest.chainId.isNullOrEmpty()) {
            throw SignTransactionError(ErrorString.SIGN_TRANS_EMPTY_CHAIN_ID)
        }

        // Prepare message to be signed.
        // Getting serializedTransaction and preparing signable transaction
        val serializedTransaction: String = arisenTransactionSignatureRequest.serializedTransaction

        // This is the un-hashed message which is used to recover public key
        val message: ByteArray

        try {
            message = Hex.decode(
                RIXFormatter.prepareSerializedTransactionForSigning(
                    serializedTransaction,
                    arisenTransactionSignatureRequest.chainId
                ).toUpperCase()
            )
        } catch (rixFormatterError: RIXFormatterError) {
            throw SignTransactionError(
                String.format(
                    SIGN_TRANSACTION_PREPARE_FOR_SIGNING_GENERIC_ERROR,
                    serializedTransaction
                ), rixFormatterError
            )
        }

        val aliasKeyPairs: List<Pair<String, String>> =
            ArisenAndroidKeyStoreUtility.getAllAndroidKeyStoreKeysInRIXFormat(
                password = this.password,
                loadStoreParameter = this.loadStoreParameter
            )
        val signingPublicKeys: List<String> = arisenTransactionSignatureRequest.signingPublicKeys
        val signatures: MutableList<String> = emptyList<String>().toMutableList()

        for (signingPublicKey in signingPublicKeys) {
            var keyAlias: String = ""

            for (aliasKeyPair in aliasKeyPairs) {
                if (signingPublicKey == aliasKeyPair.second) {
                    keyAlias = aliasKeyPair.first
                    break
                }
            }

            if (keyAlias.isEmpty()) {
                throw SignTransactionError(SIGN_TRANSACTION_UNABLE_TO_FIND_KEY_TO_SIGN)
            }

            val rawSignature =
                ArisenAndroidKeyStoreUtility.sign(
                    data = message,
                    alias = keyAlias,
                    password = this.password,
                    loadStoreParameter = this.loadStoreParameter
                )
                    ?: throw SignTransactionError(SIGN_TRANSACTION_RAW_SIGNATURE_IS_NULL)
            signatures.add(
                RIXFormatter.convertDERSignatureToRIXFormat(
                    rawSignature,
                    message,
                    RIXFormatter.convertRIXPublicKeyToPEMFormat(signingPublicKey)
                )
            )
        }

        return ArisenTransactionSignatureResponse(serializedTransaction, signatures, null)
    }

    override fun getAvailableKeys(): MutableList<String> {
        return ArisenAndroidKeyStoreUtility.getAllAndroidKeyStoreKeysInRIXFormat(
            password = this.password,
            loadStoreParameter = this.loadStoreParameter
        )
            .map { it.second }.toMutableList()
    }

    /**
     * Builder class for Android KeyStore Signature Provider
     *
     * @property androidKeyStoreSignatureProvider AndroidKeyStoreSignatureProvider
     */
    class Builder {

        private val androidKeyStoreSignatureProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider()

        /**
         * Set password protection for adding, using and removing key
         *
         * @param password KeyStore.ProtectionParameter
         * @return Builder
         */
        fun setPassword(password: KeyStore.ProtectionParameter): Builder {
            this.androidKeyStoreSignatureProvider.password = password
            return this
        }

        /**
         * Set Load KeyStore Parameter to load the KeyStore instance
         *
         * @param loadStoreParameter KeyStore.LoadStoreParameter
         * @return Builder
         */
        fun setLoadStoreParameter(loadStoreParameter: KeyStore.LoadStoreParameter): Builder {
            this.androidKeyStoreSignatureProvider.loadStoreParameter = loadStoreParameter
            return this
        }

        /**
         * Build and return the Android KeyStore Signature Provider instance
         *
         * @return AndroidKeyStoreSignatureProvider
         */
        fun build(): ArisenAndroidKeyStoreSignatureProvider {
            return this.androidKeyStoreSignatureProvider
        }
    }
}