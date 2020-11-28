exception SystemException {
  1: optional string message
}

enum ConsistencyLevel {
	ONE;
	QUORUM;
}

struct ReadOrWriteRequest {
    1: required bool isCoordinator;
	2: optional string timestamp;
	3: required ConsistencyLevel consistencyLevel;
}

struct ReplicaID{
	1: string id;
	2: string ip;
	3: i32 port;
}
service ReplicatedKeyValueStore {

  
  string get(1: ReadOrWriteRequest request, 2: i32 key,  3: ReplicaID replicaID)
    throws (1: SystemException systemException),

  bool put(1: ReadOrWriteRequest request, 2: i32 key, 3: string  value, 4: ReplicaID replicaID, 5:bool flag, 6: bool isFromHandOff)
    throws (1: SystemException systemException),

}
