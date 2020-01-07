package ElectionsRemoteInterfaceRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ElectionsCommitteeInstructionRemote extends Remote {
    <T> T executeTask(ElectionsCommitteeTask<T> task) throws RemoteException;
}

