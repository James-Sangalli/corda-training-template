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
        //check if either lender or borrower is in the set of keys, if so return true and vice versa
        return ourKeys
                .filterIndexed { index, key -> ourKeys.contains(participants.elementAt(index).owningKey) }
                .any()
    }

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    /**
     * A Contract code reference to the IOUContract. Make sure this is not part of the [IOUState] constructor.
     * **Don't change this definition!**
     */
    override val contract get() = IOUContract()

    //must recreate the state with paid amended as states are immutable
    //Kotlin is very vague on this... haha
    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid.plus(amountToPay))

    /**
     * A toString() helper method for displaying IOUs in the console.
     */
    override fun toString() = "IOU($linearId): ${borrower.name} owes ${lender.name} $amount and has paid $paid so far."

}