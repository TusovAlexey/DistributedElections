package ElectionsCommittee;

import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;

import java.rmi.RemoteException;

import static ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.START_ELECTIONS;

public class ElectionsCommitteeClientMain {
    public static void main(String args[]){


        String serverName = "serverCA0";
        ElectionsCommitteeClient client = new ElectionsCommitteeClient(serverName);
        ElectionsCommitteeInstruction instruction = new ElectionsCommitteeInstruction(START_ELECTIONS);
        try {
            ElectionsCommitteeInstruction response = client.remoteExecute(instruction);
        }catch (RemoteException e){
            e.printStackTrace();
        }

    }

}
