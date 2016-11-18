package net.corda.bank.api

import net.corda.core.crypto.Party
import net.corda.core.crypto.generateKeyPair
import net.corda.core.serialization.OpaqueBytes
import net.corda.testing.MEGA_CORP
import java.security.KeyPair
import java.security.PublicKey

val defaultRef = OpaqueBytes(ByteArray(1, { 1 }))

/*
 * Bank Of Corda (BOC_ISSUER_PARTY)
 */
val BOC_KEY: KeyPair by lazy { generateKeyPair() }
val BOC_PUBKEY: PublicKey get() = BOC_KEY.public
val BOC_ISSUER_PARTY: Party get() = Party("BankOfCorda", BOC_PUBKEY)
val BOC_ISSUER_PARTY_AND_REF = BOC_ISSUER_PARTY.ref(defaultRef)
val BOC_ISSUER_PARTY_REF = BOC_ISSUER_PARTY_AND_REF.reference

/*
 *  If you need to check if something is of generic type T you need to to have an instance of Class<T> to check against.
 *  This is a common technique in Java hwoever in Kotlin we can make use of an inlined factory method that gets us the class object.
 */
class Generic<T : Any>(val klass: Class<T>) {
    companion object {
        inline operator fun <reified T : Any>invoke() = Generic(T::class.java)
    }

    fun checkType(t: Any) {
        when {
            klass.isAssignableFrom(t.javaClass) -> println("Correct type")
            else -> println("Wrong type")
        }
    }
}
