package com.template.states;

import com.template.contracts.AverageStateContract;
import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CordaSerializable
@BelongsToContract(AverageStateContract.class)
public class AverageState implements ContractState {

    //private variables
    private double avg;
    private Party recipient;
    private Party secondRecipient;
    private Party sender;

    private List<AbstractParty> participants;


    @ConstructorForDeserialization
    public AverageState(double avg, Party sender, Party recipient, Party secondRecipient) {
        this.avg = avg;
        this.sender = sender;

        participants = new ArrayList<AbstractParty>();
        participants.add(sender);
        participants.add(recipient);
        participants.add(secondRecipient);
    }

    //getters
    public double getAvg() { return avg; }
    public Party getSender() { return sender; }
    public Party getRecipient() { return recipient; }
    public Party getSecondRecipient() { return secondRecipient; }


    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    //@NotNull
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}
