/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Generated code
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

public class JavaClient {
  public static void main(String [] args) {

    try {
      TTransport transport=null;
      boolean flag=false;
      if (args.length ==3) {
 
        transport = new TSocket(args[1], Integer.valueOf(args[2]));
        transport.open();
      }
      else {
    	  System.out.println("please provide required argumnets : ip/ port");
    	  System.exit(0);
      }
 
      TProtocol protocol = new  TBinaryProtocol(transport);
      ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(protocol);

     //perform (client);

      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    } 
  }

  public static List<ReplicaID> getReplicasFromSystem(String fName) {
	  File file= new File(fName);
	  BufferedReader fileReader=null;
	  List<ReplicaID> rep = new ArrayList<ReplicaID>();
	  try {
	    fileReader= new BufferedReader(new FileReader(file) );
		String l=null;
		while( null != (l=fileReader.readLine())) {
			String[] replica = l.split(" ");
			rep.add(createReplica(replica[0],replica[1], replica[2]));
		}
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally{ 
		 // fileReader.close();
    }
	  return rep;
	  
  }

	public static ReplicaID createReplica(String id, String ip, String port) {
		ReplicaID replicaID = new ReplicaID();
		replicaID.id = id;
		replicaID.ip = ip;
		replicaID.port = Integer.parseInt(port);
		return replicaID;
	}
}
