# HDSLedger

## Introduction

HDSLedger is a simplified permissioned (closed membership) blockchain system with high dependability
guarantees. It uses the Istanbul BFT consensus algorithm to ensure that all nodes run commands
in the same order, achieving State Machine Replication (SMR) and guarantees that all nodes
have the same state.

## Requirements

- [Java 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) - Programming language;

- [Maven 3.8](https://maven.apache.org/) - Build and dependency management tool;

- [Python 3](https://www.python.org/downloads/) - Programming language;

---

# Configuration Files

### Node configuration

Can be found inside the `resources/` folder of the `Service` module.

```json
{
    "id": <NODE_ID>,
    "isLeader": <IS_LEADER>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
    "clientPort": <CLIENT_PORT>,
    "bizantineType": <BIZANTINE_TYPE>,
    "messageDelay": <MESSAGE_DELAY>,
}
```

### Client Configuration

Can be found inside the `resources/` folder of the `Client` module.

```json
{
    "id": <CLIENT_ID>,
    "hostname": "localhost",
    "port": <NODE_PORT>,
}
```

The `id` field is the unique identifier of the node or client. It shouldn't be repeated!

## Dependencies

To install the necessary dependencies run the following command:

```bash
./install_deps.sh
```

This should install the following dependencies:

- [Google's Gson](https://github.com/google/gson) - A Java library that can be used to convert Java Objects into their JSON representation.

## Puppet Master

The puppet master is a python script `puppet-master.py` which is responsible for creating keys for all the processes 
(clients and nodes) and starting the nodes of the blockchain.
The script runs with `kitty` terminal emulator by default since it's installed on the RNL labs.

To run the script you need to have `python3` installed.
The script has arguments which can be modified:

- `terminal` - the terminal emulator used by the script
- `server_config` - a string from the array `server_configs` which contains the possible configurations for the blockchain nodes

Run the script with the following command:

```bash
python3 puppet-master.py
```
Note: You may need to install **kitty** in your computer

## Tests

After running the script 'puppet-master.py' you will be prompted to choose a test to run. The tests are:
1. **Regular** - A regular test with no faults
2. **Fake Leader** - A test where one of the nodes is a fake leader
3. **Message Delay** - A test where all the nodes have different message delays
4. **Leader Delay** - A test where the leader has a message delay, and as consequence, there is a round change
5. **Round Change with Previous Prepare** - A test to check if the system can handle a round change when there was already a prepare message from some nodes

## Maven

It's also possible to run the project manually by using Maven.

### Instalation

Compile and install all modules using:

```
mvn clean install
```

### Execution

Run without arguments

```
cd <module>/
mvn compile exec:java
```

Run with arguments

```
cd <module>/
mvn compile exec:java -Dexec.args="..."
```
---
This codebase was adapted from last year's project solution, which was kindly provided by the following group: [David Belchior](https://github.com/DavidAkaFunky), [Diogo Santos](https://github.com/DiogoSantoss), [Vasco Correia](https://github.com/Vaascoo). We thank all the group members for sharing their code.

