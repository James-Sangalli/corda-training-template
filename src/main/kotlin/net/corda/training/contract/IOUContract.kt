package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.training.state.IOUState

/**
 * This is where you'll add the contract code which defines how the [IOUState] behaves. Looks at the unit tests in
 * [IOUContractTests] for instructions on how to complete the [IOUContract] class.
 */
class IOUContract : Contract {
    /**
     * Legal prose reference. This is just a dummy string for the time being.
     */
    override val legalContractReference: SecureHash = SecureHash.sha256("Prose contract.")

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a range of commands which implement this interface.
     */
    interface Commands : CommandData
    {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: TransactionForContract) {

        val commands = tx.commands.requireSingleCommand<IOUContract.Commands>()

        when(commands.value)
        {
            is Commands.Issue -> requireThat{
                val output = tx.outputs.first() as IOUState
                "A newly issued IOU must have a positive amount." using (output.amount > Amount(0, output.amount.token))
                tx.commands.requireSingleCommand<Commands.Issue>()
                "Only one output state should be created when issuing an IOU." using (tx.outputs.size == 1)
                //"Legal contract should be attached" using tx.attachments.isNotEmpty()
                "No inputs should be consumed when issuing an IOU." using tx.inputs.isEmpty()
                "The lender and borrower cannot be the same identity." using (output.participants.get(0) != output.participants.get(1))
                "Both lender and borrower together only may sign IOU issue transaction." using
                        (commands.signers.toSet() == output.participants.map { it.owningKey }.toSet() )
            }

            is Commands.Transfer -> requireThat {

                "An IOU transfer transaction should only consume one input state." using (tx.inputs.size == 1)
                "An IOU transfer transaction should only create one output state." using (tx.outputs.size == 1)

                val lender = tx.inputs.first().participants.get(0)
                val newLender = tx.outputs.first().participants.get(0)
                val input = tx.inputs.first() as IOUState
                val output = tx.outputs.first() as IOUState
                val keysThatSigned = commands.signers.toSet()
                //must add old lender as they are not included in the new output
                val keysThatShouldSign = tx.outputs.first().participants + lender
                "Only the lender property may change." using (
                       input.copy(lender = tx.inputs.first().participants.get(0) as Party) ==
                               output.copy(lender = tx.inputs.first().participants.get(0) as Party)
                )
                "The lender property must change in a transfer." using (lender != newLender)
                "The borrower, old lender and new lender only must sign an IOU transfer transaction." using(
                        keysThatSigned == keysThatShouldSign.map { it.owningKey }.toSet()
                )
            }

            is Commands.Settle -> requireThat {
               "List has more than one element." using ( tx.groupStates<IOUState, Any> { it.linearId }.size == 1 )
                "There must be one input IOU." using (tx.inputs.size >= 1)
            }
        }

    }

}
