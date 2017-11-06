package io.mockk

import io.mockk.impl.MockKGatewayImpl
import kotlin.reflect.KClass

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val mockFactory: MockFactory
    val stubber: Stubber
    val verifier: Verifier
    val callRecorder: CallRecorder
    val instantiator: Instantiator

    fun verifier(ordering: Ordering): CallVerifier

    companion object {
        internal val defaultImpl: MockKGateway = MockKGatewayImpl()
        var LOCATOR: () -> MockKGateway = { defaultImpl }

        fun registerInstanceFactory(factory: InstanceFactory): AutoCloseable {
            LOCATOR().instantiator.registerFactory(factory)
            return AutoCloseable {
                LOCATOR().instantiator.unregisterFactory(factory)
            }
        }

        fun registerInstanceFactory(filterClass: KClass<*>,
                                    factory: () -> Any) : AutoCloseable {
            return registerInstanceFactory(object : InstanceFactory {
                override fun instantiate(cls: Class<*>): Any? {
                    if (filterClass.java == cls) {
                        return factory()
                    }
                    return null
                }
            })
        }
    }
}

/**
 * Create new mocks or spies
 */
interface MockFactory {
    fun <T> mockk(cls: Class<T>,
                  name: String?,
                  moreInterfaces: Array<out KClass<*>>): T

    fun <T> spyk(cls: Class<T>,
                 objToCopy: T?,
                 name: String?,
                 moreInterfaces: Array<out KClass<*>>): T

}

/**
 * Stub calls
 */
interface Stubber {
    fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                  coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T>
}

/**
 * Verify calls
 */
interface Verifier {
    fun <T> verify(ordering: Ordering,
                   inverse: Boolean,
                   atLeast: Int,
                   atMost: Int,
                   exactly: Int,
                   mockBlock: (MockKVerificationScope.() -> T)?,
                   coMockBlock: (suspend MockKVerificationScope.() -> T)?)
}

/**
 * Builds a list of calls
 */
interface CallRecorder {
    val calls: List<Call>

    fun startStubbing()

    fun startVerification()

    fun catchArgs(round: Int, n: Int)

    fun <T> matcher(matcher: Matcher<*>, cls: Class<T>): T

    fun call(invocation: Invocation): Any?

    fun answer(answer: Answer<*>)

    fun doneVerification()

    fun hintNextReturnType(cls: Class<*>, n: Int)

    fun cancel()
}

/**
 * Verifier takes the list of calls and checks what invocations happened to the mocks
 */
interface CallVerifier {
    fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult
}

/**
 * Result of verification
 */
data class VerificationResult(val matches: Boolean, val message: String? = null)

/**
 * Instantiates empty object for provided class
 */
interface Instantiator {
    fun <T> instantiate(cls: Class<T>): T

    fun anyValue(cls: Class<*>, orInstantiateVia: () -> Any? = { instantiate(cls) }): Any?

    fun <T> proxy(cls: Class<T>, useDefaultConstructor: Boolean, moreInterfaces: Array<out KClass<*>>): Any

    fun <T> signatureValue(cls: Class<T>): T

    fun isPassedByValue(cls: Class<*>): Boolean

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean

    fun registerFactory(factory: InstanceFactory)

    fun unregisterFactory(factory: InstanceFactory)
}

/**
 * Factory of dummy objects
 */
interface InstanceFactory {
    fun instantiate(cls: Class<*>): Any?
}