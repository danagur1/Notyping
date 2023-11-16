import db
import json_server

notes = None


def notes_response(client_socket, params):
    """ The notes request require multiple responses. One for each relevant note. This function loop over all the
    relevant notes
    :param client_socket: the socket to sent the responses through
    :param params: the type param that defines what notes are relevant
    """
    global notes
    notes_col = db.connection_to_db("notes")
    criteria = {}
    notes = iter(notes_col.find(criteria))


def next_note(client_socket):
    if notes.hasNext():
        note = notes.next()
        note.pop("_id")
        response = {'return_code': note}
        json_server.send_dict(response, client_socket)
    else:
        response = {'return_code': "END"}
        json_server.send_dict(response, client_socket)
