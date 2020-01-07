package ElectionsRemoteInterfaceRMI;

import ElectionsServer.models.Candidate;

import java.util.HashMap;

public interface ElectionsCommitteeTask<T> {
    T execute();
    T responseStartElections();
    T responseStopElections();
    T responseGetResults(HashMap<Integer, Candidate> results);
    T responseSystemUp();
    T responseSystemDown();

}
