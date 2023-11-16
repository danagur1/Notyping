import hashlib
import math
import os
import pickle
import re
import socket
import json
import string

import db
import encryption
import notes
import a
from SchoolProject import ocr


import mailtrap as mt
from email.mime.multipart import MIMEMultipart
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

HOST = '0.0.0.0'
PORT = 1024
MESSAGE_SIZE = 255  # bytes
MESSAGE_SIZE_LENGTH = 3
even_num = 0
rsa = None
last_connected = "2"


# ----------------------------------------OVERALL COMMUNICATION---------------------------------------------------------


def client_comm():
    """Does all the communication with the client, receives requests and sends an appropriate response
    """
    global rsa

    rsa = encryption.RSA()

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_socket.bind((HOST, PORT))
        server_socket.listen()
        print(f'Server listening on port {PORT}')
        listen(server_socket)


def handle_client(server_socket, response_needed):
    """ Do all the communication with the client: receives message, sends response and closes socket
    :param response_needed: True iff needs to return response to the following accepted message
    :param server_socket:  the opened socket that can be used for opening sockets for communication with client
    :return the message that received
    """
    msg, client_socket = connect_to_client_no_json(server_socket)
    if response_needed:
        send_response(client_socket, msg)
    close_socket(client_socket)
    return msg


def close_socket(in_socket):
    """uses shutdown for sending package with FIN flag before closing socket, to inform the client about closing and
    preventing connection reset error
    code source: https://stackoverflow.com/questions/44551677/python-sockets-sending-unexpected-rst-packets
    :param in_socket: the socket that needs to be closed
    """
    in_socket.shutdown(socket.SHUT_WR)
    in_socket.close()


# ----------------------------------------RECEIVE-----------------------------------------------------------------------


def listen(server_socket):
    """listening to client on an infinite loop, connecting to client and handling it
    :param server_socket: the socket to listen on
    """
    while True:
        handle_request_by_len(server_socket)


def connect_to_client_no_json(server_socket):
    """Later added function. help the connect_to_client function with everything but the json conversion

    :param server_socket: th socket for the connect_to_client function
    :return: the client socket and the data (as there was on connect_to_client)
    """
    client_socket, addr = server_socket.accept()
    # first receive length
    data_len = improved_receive(client_socket, MESSAGE_SIZE_LENGTH)
    # then receive actual message
    return rsa.decrypt(improved_receive(client_socket, int(data_len.decode('latin-1')))).decode('latin-1'), \
           client_socket


def connect_to_client(server_socket):
    """ creates a new socket for communication with client, receives message from client and decodes it (using the
    length in the message start)
    :param server_socket: the opened socket that can be used for opening sockets for communication with client
    :return: 1. the opened socket for further communication with client 2. the client's request
    """
    global rsa

    data_encrypted, client_socket = connect_to_client_no_json(server_socket)
    request = json.loads(data_encrypted)
    return client_socket, request


def handle_request_by_len(server_socket):
    """ receive the expected amount of messages from the client. Then receive MESSAGE_SIZE sized messages, in a loop
    running the amount of messages times. Return response only after the last message sent
    :param server_socket: the opened socket that can be used for opening sockets for communication with client
    :return the message that received
    """
    msg_messages_amount, client_socket = connect_to_client_no_json(server_socket)
    close_socket(client_socket)
    num_messages_amount = int(msg_messages_amount)
    # receive every MESSAGE_SIZE message separately:
    client_whole_message = ""
    if num_messages_amount > 1:
        for request_idx in range(num_messages_amount - 1):
            client_whole_message += handle_client(server_socket, False)
    last_msg, client_socket = connect_to_client_no_json(server_socket)
    request = json.loads(client_whole_message + last_msg)
    send_response(client_socket, request)
    close_socket(client_socket)


def improved_receive(client_socket, bytes_amount):
    """ Does the receive function of socket without stopping on special characters like \n, \r

    :param client_socket: the socket to do recv from
    :param bytes_amount: the amount of bytes to read
    :return: the bytes_amount received bytes
    """
    data = b''  # Initialize an empty byte string to store the received data
    total_received = 0  # Track the total number of bytes received

    while total_received < bytes_amount:
        chunk = client_socket.recv(1)
        if not chunk:
            break  # Connection closed
        data += chunk
        total_received += len(chunk)
    return data


# ----------------------------------------HANDLE REQUESTS---------------------------------------------------------------


def register(params):
    """ Check the register fields and return message for the user

    :param params: the parameters passed from the client, representing the register fields
    :return: message response for the user according to the data
    """
    users_col = db.connection_to_db("users")
    if len(list(users_col.find({"username": params["username"]}))) > 1:
        return "user already exist"
    if any(c in params["username"] for c in list(string.punctuation)):
        return "special characters can't be in username"
    if len(params["password"]) < 8:
        return "password length must be al least 8"
    if len(params["password"]) > 20:
        return "password length must be at most 20"
    params["phone"] = re.sub("[^0-9]", "", params["phone"])
    mail_pattern = re.compile(r"""^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$""")
    if not mail_pattern.match(params["mail"]):
        return "not a valid mail address"
    params["password"] = hash_password(params["password"])
    db.connection_to_db("users").insert_one(params)
    return "Registered Successfully"


def login_result(search_criteria):
    """returns the result code for login OK or not by the username and password

    :param search_criteria: the username and password dict
    :return: the relevant return code
    """
    users_col = db.connection_to_db("users")
    if len(search_criteria["password"]) > 20:
        return 0
    users_list = list(users_col.find({"username": search_criteria["username"]}))
    if len(users_list) > 0:
        if verify_password(search_criteria["password"], users_list[0]["password"]):
            return 1
        else:
            return 0
    else:
        return 0


def hash_password(password):
    """Returns the hashed password, using SHA256, 100000 iterations and randomly generated salt

    :param password: the password to hash
    :return: the hashed password
    """
    # Use a predetermined salt value
    salt = b'5fc7a9f893bc532aabb880c928257d7bc3a6ac89'
    # Combine salt and password and hash using SHA-256 algorithm
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000).hex()
    return hashed_password


def verify_password(password, hashed_password):
    """ Verify the hashed password to the password hash, by using the save hash function, salt and number of iterations

    :param password: the password to hash and compare
    :param hashed_password: the hashed password for comparing
    :return: True iff hash(password) == hashed_password
    """
    # Use the same predetermined salt value for verification
    salt = b'5fc7a9f893bc532aabb880c928257d7bc3a6ac89'
    # Hash the input password with the same salt and compare it with the stored hash
    return hashed_password == hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000).hex()


def next_id():
    """Generate new id number that hadn't been used yet

    :return: the id number
    """
    if not os.path.exists("last_num.pickle"):
        last_num = 0
    else:
        with open("last_num.pickle", "rb") as f:
            last_num = pickle.load(f)
    new_num = last_num + 1
    with open("last_num.pickle", "wb") as f:
        pickle.dump(new_num, f)
    return new_num


def new_note(note_params):
    """ Inserts the new note to the data base and return a the code 1 (representing no error)

    :param note_params: the parameters for the note that needs to be saved
    :return: the json object with the return code
    """
    return_id = False
    existing_notes = db.connection_to_db("notes")
    if "id" in note_params:
        filter_id = {"id": note_params["id"]}
        if "star" in note_params:
            curr_note = existing_notes.find_one(filter_id)
            existing_notes.delete_one(filter_id)
            curr_note["star"] = not curr_note["star"]
            existing_notes.insert_one(curr_note)
            return {"return code": curr_note["star"]}
        print("found! "+str(filter_id))
        existing_notes.delete_one(filter_id)
        note_params["star"] = False
        existing_notes.insert_one(note_params)
    elif note_params["note_type"] == "SCAN":
        image_str = note_params["image"]
        note_params = {"title": "", "text": ocr.file_to_text(image_str), "paintFlags": False, "textSize": 20,
                       "star": False, "owner": last_connected, "id": next_id()}
        existing_notes.insert_one(note_params)
        return_id = True
    else:
        note_params["id"] = next_id()
        note_params["star"] = False
        existing_notes.insert_one(note_params)
    if return_id:
        return {'return_code': note_params["id"]}
    return {'return_code': 1}


def forgot_password(params):
    """Sends a user a mail for restoring his password

    :param params: the parameters from the client, including the user's email address
    :return:
    """
    sender = "<notyping@gmail.com>"
    receiver = "<danagur2005@gmail.com>"
    find_user = db.connection_to_db("users").find_one({"mail": params["mail"]})
    if find_user is None:
        return
    username = str(find_user["username"])

    message = f"""\
    Subject: Your Username for Notyping
    To: {receiver}
    From: {sender}

    This is a your username: {username}"""

    with smtplib.SMTP("sandbox.smtp.mailtrap.io", 2525) as server:
        server.login("70bbcd4ae72db3", "2cc5ea35d8f9dd")
        server.sendmail(sender, receiver, message)


def send_response(client_socket, msg):
    """ creates and sends the response for the client's request

    :param client_socket: the socket to send the response on
    :param msg: the input message i.e the request
    """
    # for tests:
    print("msg is: " + str(msg))
    request = msg["request"]
    params = msg["params"]
    if request == "length":
        response = {'return_code': 1}
    elif request == "login":
        response = {'return_code': login_result(params)}
    elif request == "register":
        response = {'return_code': register(params)}
    elif request == "notes":
        response = notes.notes_response(client_socket, params)
    elif request == "new_note":
        response = new_note(params)
    elif request == "next_note":
        response = notes.next_note(client_socket)
    elif request == "forgot_pass":
        response = {'return_code': forgot_password(params)}
    else:
        response = {'return_code': 0}
    send_dict(response, client_socket)


# ----------------------------------------SEND--------------------------------------------------------------------------


def find_messages_amount(client_socket, response_length):
    """Find the amount of messages expected to be sent.
      Send message to client to notify it and return it.

    :param client_socket: the socket to send the notification message to client through
    :param response_length: the original response string to be sent to client
    :return: the amount of messages expected to be sent
    """
    messages_amount = math.ceil(response_length / MESSAGE_SIZE)
    messages_amount_str = str(messages_amount)
    complete_messages_amount = "0" * (3 - len(messages_amount_str)) + messages_amount_str
    client_socket.sendall(complete_messages_amount.encode('latin-1'))
    return messages_amount


def send_message_with_length(client_socket, partial_message):
    """Send part of the original message response to the client with its length at first (completed to 3 bytes)

    :param client_socket: the socket to send the message through
    :param partial_message: the part of the response message to be sent to client
    :return:
    """
    encrypted_response = rsa.encrypt(bytes(partial_message, 'latin-1'))
    msg_length = str(len(encrypted_response))
    completed_length = ("0" * (3 - len(msg_length)) + msg_length).encode("latin-1")
    client_socket.sendall(completed_length + encrypted_response)


def send_dict(dictionary, client_socket):
    """Turns the dictionary into a json object and sends it by utf-8 decoding

    :param dictionary: the dictionary representing the message to be sent
    :param client_socket: the client to sent the message on
    :return:
    """
    global rsa
    print("The dictionary to be sent is: " + str(dictionary))
    response_str = json.dumps(dictionary)
    response_len = len(response_str)
    print("and its length is " + str(len(str(dictionary))))
    messages_amount = find_messages_amount(client_socket, response_len)
    print("and messages amount is " + str(messages_amount))
    for message_idx in range(messages_amount - 1):
        # send slices of the original response message separately
        send_message_with_length(client_socket, response_str[message_idx *
                                                             MESSAGE_SIZE:(message_idx + 1) * MESSAGE_SIZE])
    send_message_with_length(client_socket, response_str[MESSAGE_SIZE * (messages_amount - 1): response_len])


if __name__ == '__main__':
    client_comm()
