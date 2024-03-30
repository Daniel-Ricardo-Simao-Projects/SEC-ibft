#!/usr/bin/env python

import os
import json
import sys
import signal

# Terminal Emulator used to spawn the processes
terminal = "kitty"

# Blockchain node configuration file name
client_configs = [
    "client_regular_config.json",
    "client_overspend.json",
    "client_overaccess.json",
    "client_double_spend.json",
]

server_configs = [
    "regular_config.json",
    "fake_leader.json",
    "message_delay.json",
    "leader_delay.json",
    "round_change_prev_prepare.json",
    "fake_signature_leader.json",
    "fake_balance_response.json",
]

choiceStr = input("Choose a Client configuration:\n"
                  "1 - Regular\n"
                  "2 - Over Spend\n"
                  "3 - Over Access\n"
                  "4 - Double Spend\n"
                  ">> ")

choice = int(choiceStr)
client_config = client_configs[choice-1]

choiceStr = input("Choose a Server configuration:\n"
                  "1 - Regular\n"
                  "2 - Fake Leader\n"
                  "3 - Message Delay\n"
                  "4 - Leader Delay\n"
                  "5 - Round Change With Previous Prepare\n"
                  "6 - Fake Signature Leader\n"
                  "7 - Fake Balance Response\n"
                  ">> ")

choice = int(choiceStr)

server_config = server_configs[choice-1]

def quit_handler(*args):
    os.system(f"pkill -i {terminal}")
    sys.exit()


# Compile classes
os.system("mvn clean install")

# Clean up previous Keys
os.system("rm -rf Utilities/keys/")
os.system("mkdir Utilities/keys/")

# Compile RSAKeyGenerator
os.system("javac -d Utilities/out Utilities/src/main/java/pt/ulisboa/tecnico/hdsledger/utilities/RSAKeyGenerator.java")

# Compile SymmetricKeyGenerator
os.system("javac -d Utilities/out Utilities/src/main/java/pt/ulisboa/tecnico/hdsledger/utilities/SymmetricKeyGenerator.java")
os.system("java -cp Utilities/out pt.ulisboa.tecnico.hdsledger.utilities.SymmetricKeyGenerator Utilities/keys/symmetric.key")

# Spawn blockchain nodes
pos = 0
with open(f"Service/src/main/resources/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            config_id = key['id']
            priv_key_path = f"Utilities/keys/{config_id}Priv.key"
            pub_key_path = f"Utilities/keys/{config_id}Pub.key"
            os.system(f"java -cp Utilities/out pt.ulisboa.tecnico.hdsledger.utilities.RSAKeyGenerator {priv_key_path} {pub_key_path}")
            if (terminal == "konsole"):
                os.system(
                    f"{terminal} > /dev/null 2>&1 --qwindowgeometry 300x1000+{str(pos*310)}+0 -e sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config} {client_config}' ; sleep 500\"")
            else:
                os.system(
                    f"{terminal} > /dev/null 2>&1 sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config} {client_config}' ; sleep 500\"")
            sys.exit()
        pos += 1
        

with open(f"Client/src/main/resources/{client_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data:
        pid = os.fork()
        if pid == 0:
            config_id = key['id']
            priv_key_path = f"Utilities/keys/{config_id}Priv.key"
            pub_key_path = f"Utilities/keys/{config_id}Pub.key"
            os.system(f"java -cp Utilities/out pt.ulisboa.tecnico.hdsledger.utilities.RSAKeyGenerator {priv_key_path} {pub_key_path}")

            if (terminal == "konsole"):
                os.system(
                    f"{terminal} > /dev/null 2>&1 --qwindowgeometry 300x1000+{str(pos*310)}+0 -e sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {client_config} {server_config}' ; sleep 1000\"")
            else:
                os.system(
                    f"{terminal} > /dev/null 2>&1 sh -c \"cd Client; mvn exec:java -Dexec.args='{key['id']} {client_config} {server_config}' ; sleep 1000\"")
            sys.exit()
        pos += 1

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit" or command.strip() == "q":
        quit_handler()
