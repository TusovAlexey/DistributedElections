package ElectionsCommittee;

import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ElectionsCommitteeClient {
    private ElectionsCommitteeInstructionRemote channel;

    public ElectionsCommitteeClient(String server){
        try {
            Registry registry = LocateRegistry.getRegistry(server);
            this.channel = (ElectionsCommitteeInstructionRemote)registry
                    .lookup("ElectionsCommitteeInstructionRemote");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public ElectionsCommitteeInstruction remoteExecute(ElectionsCommitteeInstruction instruction) throws RemoteException {
        return channel.executeTask(instruction);
    }


}
