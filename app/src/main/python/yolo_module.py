
from ultralytics import YOLO
import cv2
import base64
import json
from os.path import dirname, join
import numpy as np

class YOLO_object:
    def __init__(self, path):
        self.path = path
        self.model = YOLO(join(dirname(__file__), path), task='detect')

    def main(self, image): # can be bitmap or base64str
        if isinstance(image, str):
            image = base64.b64decode(image)
            image = np.frombuffer(image, np.uint8)
            image = cv2.imdecode(image, cv2.IMREAD_UNCHANGED)

        results = self.model.track(image, persist=True, device="cpu", \
                                   tracker="bytetrack.yaml", classes=[0,1])[0]

        list_person = results.boxes.data.cpu().numpy().tolist()
#         annotated_frame = results.plot()

#         _, annotated_frame = cv2.imencode('.jpg', annotated_frame)
#         annotated_frame = base64.b64encode(annotated_frame).decode('utf-8')

        dict_result = {
#             "annotated_frame": annotated_frame,
            "list_person": list_person
        }

        return json.dumps(dict_result)