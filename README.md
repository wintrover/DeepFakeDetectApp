Blue Check - Deepfake Detection Android Application
<img src="app/src/main/res/drawable/logo2.png" alt="Project Logo" style="width:200px; height:auto;"/>
📖 Overview
Blue Check is an Android application that detects deepfakes in images using YOLO object detection model and ONNX runtime-based deep learning model. Features modern UI implemented with Jetpack Compose and real-time analysis capabilities.

✨ Key Features
Deepfake Detection:

Detects faces in uploaded images and analyzes for deepfake characteristics

Authentication Mark:

Adds verification watermark to real photos and saves to gallery

Image Upload Support:

Select from device gallery or input via URL

Intuitive UI:

User-friendly interface built with Jetpack Compose

🛠 Tech Stack
Category	Technologies
Language	Kotlin 1.9+
UI Framework	Jetpack Compose Material3
Machine Learning	ONNX Runtime 1.16+/YOLOv11
Asynchronous	Kotlin Coroutine
Image Loading	Coil 2.5+
Animation	Lottie 6.1+
🚀 Getting Started
Prerequisites
Android Studio Giraffe or newer

Android SDK 34 (API 34)

Installation
bash
git clone https://github.com/wintrover/DeepFakeDetectApp.git
Running the App
Open MainActivity.kt in Android Studio

Run on physical device or emulator

🖥 Usage
Image Upload

[Select Image] button: Access local gallery

[Input Image URL] button: Enter web image URL

Add Verification Mark

For "Real" results: Activate [Auth Mark] button → Save to gallery

📁 Project Structure

```bash
.
├── app
│   ├── src/main
│   │   ├── java/com/garam/cvproject
│   │   │   ├── DeepfakeDetector.kt  # AI model handler
│   │   │   └── MainActivity.kt      # Compose UI main
│   │   ├── res
│   │   │   ├── drawable           # Vector assets
│   │   │   └── mipmap             # Launcher icons
│   │   └── assets                 # ONNX model files
└── build.gradle                   # Dependency management
```

📄 License
Apache License 2.0
Model files and training data may have separate licensing.

Additional Notes:

XML: snake_case naming

Compose: Material3 design guidelines

Kotlin: Android Kotlin Style Guide compliance

🤝 Team
Team Name: CloseAI

Role	Member
Project Lead	Seungheon Lee
AI Model Dev	Suhyeok Yun
App Development	Garam Kim
UI/UX Design	Soyeon Kim
📬 Contact
wintrover@gmail.com
