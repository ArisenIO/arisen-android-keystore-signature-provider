package one.block.arisenjavaandroidkeystoresignatureprovider

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.runner.AndroidJUnit4
import one.block.arisenjava.error.signatureProvider.SignTransactionError
import one.block.arisenjava.models.signatureProvider.ArisenTransactionSignatureRequest
import one.block.arisenjava.models.signatureProvider.ArisenTransactionSignatureResponse
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.GENERATE_KEY_ECGEN_MUST_USE_SECP256R1
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.GENERATE_KEY_KEYGENSPEC_MUST_USE_EC
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.GENERATE_KEY_MUST_HAS_PURPOSE_SIGN
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.QUERY_ANDROID_KEYSTORE_GENERIC_ERROR
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.ErrorString.Companion.SIGN_TRANSACTION_PREPARE_FOR_SIGNING_GENERIC_ERROR
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.InvalidKeyGenParameter
import one.block.arisenjavaandroidkeystoresignatureprovider.errors.QueryAndroidKeyStoreError
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.math.BigInteger
import java.security.NoSuchAlgorithmException
import java.security.spec.ECGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Test class for [ArisenAndroidKeyStoreSignatureProvider]
 */
@RunWith(AndroidJUnit4::class)
class ArisenAndroidKeyStoreSignatureProviderInstrumentedTest {

    companion object {
        const val TEST_CONST_TEST_KEY_NAME = "test_key"
        const val TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT: Int = 5
        const val TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT_MAX_TO_STRESS: Int = 1000
        const val TEST_CONST_SECP256R1_RIX_PREFIX = "PUB_R1_"
        const val TEST_CONST_SERIALIZED_TRANSACTION: String =
            "8BC2A35CF56E6CC25F7F000000000100A6823403EA3055000000572D3CCDCD01000000000000C03400000000A8ED32322A000000000000C034000000000000A682A08601000000000004454F530000000009536F6D657468696E6700"
        const val TEST_CONST_CHAIN_ID: String = "687fa513e18843ad3e820744f4ffcf93b1354036d80737db8dc444fe4b15ad17"
    }

    @Rule
    @JvmField
    val exceptionRule: ExpectedException = ExpectedException.none()

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.getAvailableKeys] method
     *
     * Add a test key
     *
     * Expect to get 1 available key in the AndroidKeyStore with RIX format
     *
     * Remove the test key
     */
    @Test
    fun getAvailableKeyTest() {
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)
        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(alias = TEST_CONST_TEST_KEY_NAME)

        val keyStoreProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()

        val allKeyInKeyStore: List<String> = keyStoreProvider.availableKeys

        Assert.assertEquals(1, allKeyInKeyStore.size)
        Assert.assertNotNull(allKeyInKeyStore[0])
        Assert.assertNotEquals("", allKeyInKeyStore[0])
        Assert.assertTrue(allKeyInKeyStore[0].contains(other = TEST_CONST_SECP256R1_RIX_PREFIX, ignoreCase = true))

        this.deleteKeyInAndroidKeyStore(alias = TEST_CONST_TEST_KEY_NAME)
    }

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.getAvailableKeys] method
     *
     * Clear all the keys before calling the method
     *
     * Expect to get an empty list from Android KeyStore
     */
    @Test
    fun getAvailableKeyWithNoKey_expectEmpty() {
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)

        val keyStoreProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()

        val allKeyInKeyStore: List<String> = keyStoreProvider.availableKeys
        Assert.assertEquals(0, allKeyInKeyStore.size)
    }

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.getAvailableKeys] method
     *
     * Clear all keys
     *
     * Generate [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT] keys
     *
     * Expect to get [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT] keys with RIX format
     *
     * Clear all keys
     */
    @Test
    fun getAvailableKeyWithMultipleKeyAdded_expectMultipleKey() {
        // Clear all keys to make sure we get the exact amount
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)

        // Generate keys
        for (i in 0 until TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT) {
            ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(alias = "${TEST_CONST_TEST_KEY_NAME}_$i")
        }

        // Test
        val keyStoreProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()

        val allKeyInKeyStore: List<String> = keyStoreProvider.availableKeys

        Assert.assertEquals(TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT, allKeyInKeyStore.size)

        allKeyInKeyStore.forEach {
            Assert.assertTrue(it.contains(other = TEST_CONST_SECP256R1_RIX_PREFIX, ignoreCase = true))
        }

        // Clear keys
        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)
    }

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.getAvailableKeys] method
     *
     * Clear all keys
     *
     * Generate [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT_MAX_TO_STRESS] keys
     *
     * Expect to get [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT_MAX_TO_STRESS] keys with RIX format
     *
     * Clear all keys
     */
    @Test
    fun getAvailableKeyWithStressOutMaxMultipleKeyAdded_expectMultipleKeyStressOutMax() {
        // Clear all keys to make sure we get the exact amount
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)

        // Generate keys
        for (i in 0 until TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT_MAX_TO_STRESS) {
            ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(alias = "${TEST_CONST_TEST_KEY_NAME}_$i")
        }

        // Test
        val keyStoreProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()

        val allKeyInKeyStore: List<String> = keyStoreProvider.availableKeys

        Assert.assertEquals(TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT_MAX_TO_STRESS, allKeyInKeyStore.size)

        allKeyInKeyStore.forEach {
            Assert.assertTrue(it.contains(other = TEST_CONST_SECP256R1_RIX_PREFIX, ignoreCase = true))
        }

        // Clear keys
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)
    }

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.signTransaction] method
     *
     * Generate new key
     *
     * Making a mocked transaction request
     *
     * Sign transaction
     *
     * Verify transaction with public key
     *
     * Clear key
     */
    @Test
    fun signTransaction() {
        val signingPublicKeys: MutableList<String> = ArrayList()

        // Use the key that was just added to the keystore to sign a transaction.
        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(TEST_CONST_TEST_KEY_NAME)
        signingPublicKeys.add(
            ArisenAndroidKeyStoreUtility.getAndroidKeyStoreKeyInRIXFormat(
                alias = TEST_CONST_TEST_KEY_NAME,
                password = null,
                loadStoreParameter = null
            )
        )

        val transactionSignatureRequest: ArisenTransactionSignatureRequest =
            ArisenTransactionSignatureRequest(
                TEST_CONST_SERIALIZED_TRANSACTION,
                signingPublicKeys,
                TEST_CONST_CHAIN_ID,
                ArrayList(),
                false
            )

        val arisenAndroidKeyStoreSignatureProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()
        val transactionSignatureResponse: ArisenTransactionSignatureResponse =
            arisenAndroidKeyStoreSignatureProvider.signTransaction(transactionSignatureRequest)

        Assert.assertNull(transactionSignatureResponse.error)
        Assert.assertEquals(TEST_CONST_SERIALIZED_TRANSACTION, transactionSignatureResponse.serializeTransaction)
        Assert.assertEquals(1, transactionSignatureResponse.signatures.size)
        Assert.assertNotEquals("", transactionSignatureResponse.signatures[0])
        Assert.assertTrue(transactionSignatureResponse.signatures[0].contains("SIG_R1_", true))

        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)

    }

    /**
     * Test [ArisenAndroidKeyStoreSignatureProvider.signTransaction] method to sign with [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT] keys
     *
     * Generate new [TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT] keys
     *
     * Making a mocked transaction request
     *
     * Sign transaction
     *
     * Verify transaction with public keys
     *
     * Clear keys
     */
    @Test
    fun signTransactionWithMultipleKey_expectMultipleSignature() {
        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)

        val signingPublicKeys: MutableList<String> = ArrayList()

        // Use keys generated in KeyStore to sign a transaction.
        // Generate keys
        for (i in 0 until TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT) {
            ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(alias = "${TEST_CONST_TEST_KEY_NAME}_$i")
            signingPublicKeys.add(
                ArisenAndroidKeyStoreUtility.getAndroidKeyStoreKeyInRIXFormat(
                    alias = "${TEST_CONST_TEST_KEY_NAME}_$i",
                    password = null,
                    loadStoreParameter = null
                )
            )
        }

        val transactionSignatureRequest: ArisenTransactionSignatureRequest =
            ArisenTransactionSignatureRequest(
                TEST_CONST_SERIALIZED_TRANSACTION,
                signingPublicKeys,
                TEST_CONST_CHAIN_ID,
                ArrayList(),
                false
            )

        val arisenAndroidKeyStoreSignatureProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()
        val transactionSignatureResponse: ArisenTransactionSignatureResponse =
            arisenAndroidKeyStoreSignatureProvider.signTransaction(transactionSignatureRequest)

        Assert.assertNull(transactionSignatureResponse.error)
        Assert.assertEquals(TEST_CONST_SERIALIZED_TRANSACTION, transactionSignatureResponse.serializeTransaction)
        Assert.assertEquals(
            TEST_CONST_GET_AVAILABLE_KEY_MULTIPLE_KEY_AMOUNT,
            transactionSignatureResponse.signatures.size
        )

        transactionSignatureResponse.signatures.forEach {
            Assert.assertTrue(it.contains("SIG_R1_", true))
        }


        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)
    }

    /**
     * Signing with empty serialized transaction
     *
     * Expect to fail and throw SignTransactionError
     *
     * @throws SignTransactionError
     */
    @Throws(SignTransactionError::class)
    @Test
    fun signTransactionWithEmptySerializedTransaction_expectedSignTransactionError() {
        exceptionRule.expect(SignTransactionError::class.java)
        exceptionRule.expectMessage(String.format(SIGN_TRANSACTION_PREPARE_FOR_SIGNING_GENERIC_ERROR, ""))

        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)
        val signingPublicKeys: MutableList<String> = ArrayList()

        //Use keys generated in KeyStore to sign a transaction.
        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(TEST_CONST_TEST_KEY_NAME)
        signingPublicKeys.add(
            ArisenAndroidKeyStoreUtility.getAndroidKeyStoreKeyInRIXFormat(
                alias = TEST_CONST_TEST_KEY_NAME,
                password = null,
                loadStoreParameter = null
            )
        )

        val transactionSignatureRequest: ArisenTransactionSignatureRequest =
            ArisenTransactionSignatureRequest(
                "",
                signingPublicKeys,
                TEST_CONST_CHAIN_ID,
                ArrayList(),
                false
            )

        val arisenAndroidKeyStoreSignatureProvider: ArisenAndroidKeyStoreSignatureProvider =
            ArisenAndroidKeyStoreSignatureProvider.Builder().build()

        arisenAndroidKeyStoreSignatureProvider.signTransaction(transactionSignatureRequest)
    }

    /**
     * Unhappy path test - sign transaction without a key
     *
     * Expect to throw [QueryAndroidKeyStoreError] with message [QUERY_ANDROID_KEYSTORE_GENERIC_ERROR]
     *
     * Steps:
     *
     * Delete all keys
     *
     * Call [ArisenAndroidKeyStoreUtility.getAndroidKeyStoreKeyInRIXFormat] to query an RIX public key from Android KeyStore without adding it
     *
     * [QueryAndroidKeyStoreError] is expected to be thrown
     */
    @Test
    fun signTransactionWithNoKey_expectFail() {
        exceptionRule.expect(QueryAndroidKeyStoreError::class.java)
        exceptionRule.expectMessage(QUERY_ANDROID_KEYSTORE_GENERIC_ERROR)

        ArisenAndroidKeyStoreUtility.deleteAllKeys(loadStoreParameter = null)
        val signingPublicKeys: MutableList<String> = ArrayList()

        signingPublicKeys.add(
            ArisenAndroidKeyStoreUtility.getAndroidKeyStoreKeyInRIXFormat(
                alias = TEST_CONST_TEST_KEY_NAME,
                password = null,
                loadStoreParameter = null
            )
        )
    }

    /**
     * Unhappy path test - [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with an [KeyGenParameterSpec] which has invalid algorithm
     *
     * Expect to throw [InvalidKeyGenParameter] with message [GENERATE_KEY_KEYGENSPEC_MUST_USE_EC]
     *
     * Steps:
     *
     * Create an [KeyGenParameterSpec] with [RSAKeyGenParameterSpec] as its Algorithm Parameter Spec
     *
     * Call [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with the new [KeyGenParameterSpec]
     *
     * [InvalidKeyGenParameter] is expected to be thrown
     */
    @Test
    fun testGenerateAndroidKeyStoreKey_expectFailWithInvalidAlgorithm() {
        exceptionRule.expect(InvalidKeyGenParameter::class.java)
        exceptionRule.expectMessage(GENERATE_KEY_KEYGENSPEC_MUST_USE_EC)

        val invalidAlgorithmKeyGenParameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            "sample alias",
            KeyProperties.PURPOSE_SIGN
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(1, BigInteger.ONE)).build()

        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(invalidAlgorithmKeyGenParameterSpec)
    }

    /**
     * Unhappy test [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with an [KeyGenParameterSpec] which does not include [KeyProperties.PURPOSE_SIGN]
     *
     * Expect to throw [InvalidKeyGenParameter] with message [GENERATE_KEY_MUST_HAS_PURPOSE_SIGN]
     *
     * Steps:
     *
     * Create an [KeyGenParameterSpec] and include [KeyProperties.PURPOSE_ENCRYPT] or [KeyProperties.PURPOSE_DECRYPT] as its purposes
     *
     * Call [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with the new [KeyGenParameterSpec]
     *
     * [InvalidKeyGenParameter] is expected to be thrown
     */
    @Test
    fun testGenerateAndroidKeyStoreKey_expectFailWithoutPurposeSign() {
        exceptionRule.expect(InvalidKeyGenParameter::class.java)
        exceptionRule.expectMessage(GENERATE_KEY_MUST_HAS_PURPOSE_SIGN)

        val keyGenParameterSpecWithoutPurposeSign: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            "sample alias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).build()

        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(keyGenParameterSpecWithoutPurposeSign)
    }

    /**
     * Unhappy test [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with an [KeyGenParameterSpec] which has a wrong curve
     *
     * Expect to throw [InvalidKeyGenParameter] with message [GENERATE_KEY_ECGEN_MUST_USE_SECP256R1]
     *
     * Steps:
     *
     * Create an [KeyGenParameterSpec] with secp256k1 as its curve name of ECGenParameterSpec
     *
     * Call [ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey] with the new [KeyGenParameterSpec]
     *
     * [InvalidKeyGenParameter] is expected to be thrown
     */
    @Test
    fun testGenerateAndroidKeyStoreKey_expectFailWithInvalidCurve() {
        exceptionRule.expect(InvalidKeyGenParameter::class.java)
        exceptionRule.expectMessage(GENERATE_KEY_ECGEN_MUST_USE_SECP256R1)

        val invalidCurveKeyGenParameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            "sample alias",
            KeyProperties.PURPOSE_SIGN
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256k1")).build()

        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(invalidCurveKeyGenParameterSpec)
    }

    @Test
    fun testGetAvailableKeysWithPrivateKeyEntryOnly() {
        val differentKeyName: String = "different_key_type_alias"
        // Create different kinds of secured key in AndroidKeyStore
        val keyGenerator: KeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            differentKeyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()

        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_AES, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_AES}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_HMAC_SHA1, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_HMAC_SHA1}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_HMAC_SHA224, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_HMAC_SHA224}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_HMAC_SHA256}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_HMAC_SHA384, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_HMAC_SHA384}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_HMAC_SHA512, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_HMAC_SHA512}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_RSA, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_RSA}")
        this.generateKeyByAlgorithmName(KeyProperties.KEY_ALGORITHM_3DES, "${differentKeyName}_${KeyProperties.KEY_ALGORITHM_3DES}")

        ArisenAndroidKeyStoreUtility.generateAndroidKeyStoreKey(alias = TEST_CONST_TEST_KEY_NAME)

        val allKeyInKeyStore: List<Pair<String, String>> =
            ArisenAndroidKeyStoreUtility.getAllAndroidKeyStoreKeysInRIXFormat(null, null)

        Assert.assertEquals(1, allKeyInKeyStore.size)
        Assert.assertEquals(TEST_CONST_TEST_KEY_NAME, allKeyInKeyStore[0].first)

        // Clear keys
        ArisenAndroidKeyStoreUtility.deleteAllKeys(null)
    }

    private fun generateKeyByAlgorithmName(algorithmName : String, keyName : String) {
        try {
            val keyGenerator: KeyGenerator = KeyGenerator.getInstance(algorithmName, "AndroidKeyStore")

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (ex : NoSuchAlgorithmException) {
            ex.printStackTrace()
        }
    }

    /**
     * Delete a key in Android KeyStore for testing
     */
    private fun deleteKeyInAndroidKeyStore(alias: String) {
        Assert.assertTrue(
            ArisenAndroidKeyStoreUtility.deleteKeyByAlias(
                keyAliasToDelete = alias,
                loadStoreParameter = null
            )
        )
    }
}