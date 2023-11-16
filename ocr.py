import pytesseract
from PIL import Image
import base64
import io

pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'


def decode_bitmap(encoded_string):
    """ saves encoded_string representing a bitmap as PIL.Image object

    :param encoded_string: the string got from the client representing an image
    :return: the PIL.Image object for the bitmap
    """
    imageBytes = base64.b64decode(encoded_string)
    image = Image.open(io.BytesIO(imageBytes))
    image.save("test_image.jpg")
    return image


def file_to_text(image_str):
    """Returns text extracted from the image coded in image_str. Decodes it, convert colors and uses pytesseract engine.

    :param image_str: encoded_string: the string got from the client representing an image
    :return: the text extracted from the image
    """
    image = decode_bitmap(image_str)
    text = pytesseract.image_to_string(image.convert('L'))
    return text
