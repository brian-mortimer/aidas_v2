# AI Driver Assistance System using Computer Vision

Android application for my final year project, Driver Assistance system using Computer Vision. This repo contains the Android studio project with two object detection models. 

- Traffic Sign Detection Model:
    - MediaPipe Framework for training.
    - Custom dataset ~5000 images
    - mAP 0.794
    - Base Model MobileNetV2 I320
    
- Pedestrian Detection Model:
    - MobileNet V1 Model.
    - Filter just person classes.
    - [link](https://huggingface.co/docs/transformers/en/model_doc/mobilenet_v1)

## Project Overview

![](images/project%20overview.png)

## Class Structure

![](images/App%20structure.png)


## Demo

![](images/Gallery%20Mode%201.jpg)
![](images/Gallery%20Mode%202.jpg)
![](images/Live%20Mode%201.png)
![](images/Live%20Mode%202.png)
![](images/Live%20Mode%203.png)

## Links
- [Roboflow dataset](https://universe.roboflow.com/brian-mortimer-mk3tc/road-traffic-sign-dataset)

- [MediaPipe Framework](https://developers.google.com/mediapipe/framework)

- [Model Training/Tools Repo](https://github.com/brian-mortimer/ai_driver_assistant_cv)
