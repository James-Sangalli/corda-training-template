package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.training.contract.IOUContract
import java.security.PublicKey
import java.util.*

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 * Remove the "val data: String = "data" property before starting the [IOUState] tasks.
 */
data class IOUState(val amount : Amount<Currency>,
                    val lender : Party,
                    val borrower : Party,
                    val paid : Amount<Currency> = Amount(0, amount.token),
                    override val linearId: UniqueIdentifier = UniqueIdentifier()): ContractState, LinearState {

    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean {
        return false
    }

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    /**
     * A Contract code reference to the IOUContract. Make sure this is not part of the [IOUState] constructor.
     * **Don't change this definition!**
     */
    override val contract get() = IOUContract()
}