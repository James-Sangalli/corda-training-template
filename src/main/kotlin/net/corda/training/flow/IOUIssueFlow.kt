package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.identity.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.flows.CollectSignaturesFlow
import net.corda.flows.FinalityFlow
import net.corda.flows.SignTransactionFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(val state: IOUState, val otherParty: Party): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction
    {
        //Had to look at solutions for this one, was lost and doc was vague
        //get the notary
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        //invoke the issue command
        val issue = Command(IOUContract.Commands.Issue(), state.participants.map { it.owningKey })
        //build the tx with the issue command (so it knows it is an issue tx)
        val txBuilder = TransactionType.General.Builder(notary)
        txBuilder.withItems(state, issue)
        txBuilder.toWireTransaction().toLedgerTransaction(serviceHub).verify()
        //sign it
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val stx = subFlow(CollectSignaturesFlow(ptx))

        return subFlow(FinalityFlow(stx, setOf(serviceHub.myInfo.legalIdentity, otherParty))).single()
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [CollectSignaturesFlow].
 */
@InitiatedBy(IOUIssueFlow::class)
class IOUIssueFlowResponder(val otherParty: Party): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherParty) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Define checking logic.
            }
        }

        subFlow(signTransactionFlow)
    }
}