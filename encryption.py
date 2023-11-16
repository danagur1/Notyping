import codecs
import json
import random
import socket
from time import sleep


from sympy import randprime


from json_server import close_socket


def set_listening_socket(server_socket):
    host = '0.0.0.0'
    port = 1024
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((host, port))
    server_socket.listen()


class RSA:

    def __init__(self):
        # Generate two large prime numbers p and q
        p = randprime(2**1023, 2**1024)
        q = randprime(2**1023, 2**1024)

        # Calculate n = p * q and phi = (p - 1) * (q - 1)
        self.n = p * q
        phi = (p-1)*(q-1)

        # Choose e such that 1 < e < phi and gcd(e, phi) = 1
        self.e = 65537

        # Calculate d such that d = e^-1 mod phi
        self.d = pow(self.e, -1, phi)

        # those will be received later from client
        self.e_client = 0
        self.n_client = 0

        self.setup()

    def encrypt(self, message: bytes) -> bytes:
        if self.e_client == 0:
            return message
        big_int = int.from_bytes(message, byteorder='big')  # bytes -> int
        encrypted_bigint = pow(big_int, self.e_client, self.n_client)
        encoded_bytes = encrypted_bigint.to_bytes((encrypted_bigint.bit_length() + 7) // 8, 'big')  # int -> bytes
        return encoded_bytes

    def decrypt(self, ciphertext: bytes) -> bytes:
        if self.e_client == 0:
            return ciphertext
        big_int = int.from_bytes(ciphertext, byteorder='big')  # bytes -> int
        decrypted_bigint = pow(big_int, self.d, self.n)
        decoded_bytes = decrypted_bigint.to_bytes((decrypted_bigint.bit_length() + 7) // 8, 'big')
        return decoded_bytes

    def get_public_key(self):
        return self.e

    def get_modulus(self):
        return self.n

    def receive_values(self, client_socket):
        """Receive n and e values from client and set them for the encryption algorithm use

        :param client_socket: the socket to get the data through
        :return:
        """
        data = bytearray()
        while True:
            chunk = client_socket.recv(4096)
            data += chunk
            if len(chunk)<4096:
                break
        msg = json.loads(data.decode('latin-1'))
        self.e_client = int(msg["params"]["e"])
        self.n_client = int(msg["params"]["n"])
        print(f'RSA values received: e={self.e_client}, n={self.n_client}')

    def setup(self):
        """Setup for encryption. Get n and e values from client and send n and e values to it

        :return:
        """
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            set_listening_socket(server_socket)
            print("created socket")
            client_socket, addr = server_socket.accept()
            print("found client")
            self.receive_values(client_socket)
            client_socket.sendall(bytes(str(json.dumps({"e": self.e, "n": str(self.n)}))+"\n", 'latin-1'))
            close_socket(client_socket)
