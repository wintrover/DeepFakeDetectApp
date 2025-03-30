# Blue Check - Deepfake Detection Android Application

<div align="center">
  <img src="app/src/main/res/drawable/logo2.png" alt="Project Logo" style="width:200px; height:auto;"/>
  <br>
  
  [![English](https://img.shields.io/badge/language-English-blue.svg)](README.md) [![한국어](https://img.shields.io/badge/language-한국어-red.svg)](README.kr.md)
</div>

## 📖 Overview
Blue Check is an Android application that detects deepfakes in images using YOLO object detection model and ONNX runtime-based deep learning model. Features modern UI implemented with Jetpack Compose and real-time analysis capabilities.

## 🎬 Demo
<div align="center">
  <a href="https://youtu.be/O3X-rWDxpi8">
    <img src="https://img.youtube.com/vi/O3X-rWDxpi8/0.jpg" alt="Blue Check App Demo" style="width:300px; height:auto;"/>
  </a>
</div>

## ✨ Key Features
- **Deepfake Detection**:
  - Detects faces in uploaded images and analyzes for deepfake characteristics
- **Authentication Mark**:
  - Adds verification watermark to real photos and saves to gallery
- **Image Upload Support**:
  - Select from device gallery or input via URL
- **Intuitive UI**:
  - User-friendly interface built with Jetpack Compose

## 🛠 Tech Stack
| Category | Technologies |
|----------|--------------|
| Language | Kotlin 1.9+ |
| UI Framework | Jetpack Compose Material3 |
| Machine Learning | ONNX Runtime 1.16+/YOLOv11 |
| Asynchronous | Kotlin Coroutine |
| Image Loading | Coil 2.5+ |
| Animation | Lottie 6.1+ |

## 🧠 AI Model Architecture
| Component | Description |
|-----------|-------------|
| Face Detection | YOLOv11n optimized for mobile face detection |
| Deepfake Classifier | Binary classification CNN with early exit (97.5% accuracy) |
| Model Format | ONNX for cross-platform compatibility and hardware acceleration |
| Input Size | 128x128px for classification, 640x640px for face detection |
| Optimization | Int8 quantization, memory optimization, and inference caching |

## 📊 Performance Optimizations
- **Model Quantization**: Int8 quantization reduces model size by ~70% with minimal accuracy loss
- **Inference Optimization**: 
  - Memory buffer reuse and custom preprocessing pipeline
  - Asynchronous processing with Kotlin Coroutines
  - GPU delegation where available
- **Runtime Caching**: Model and inference results caching reduces repeated computations
- **Performance Monitoring**: Real-time memory and CPU usage tracking with automatic optimization

## 🚀 Getting Started
### Prerequisites
- Android Studio Giraffe or newer
- Android SDK 34 (API 34)

### Installation
Clone the repository:
```bash
git clone https://github.com/wintrover/DeepFakeDetectApp.git
```

### Running the App
- Open project in Android Studio
- Build and run on a physical device or emulator

## 🖥 Usage
1. **Image Upload**
   - [Select Image] button: Access local gallery
   - [Input Image URL] button: Enter web image URL
2. **Detection Process**
   - The app automatically analyzes the image for deepfakes
   - Results are displayed with confidence score
3. **Add Verification Mark**
   - For "Real" results: Activate [Auth Mark] button → Save to gallery

## 📁 Project Structure
```
.
├── app
│ ├── src/main
│ │ ├── java/com/garam/cvproject
│ │ │ ├── DeepfakeDetector.kt # AI model handler
│ │ │ ├── ModelOptimizer.kt   # Inference optimization
│ │ │ ├── ModelQuantizer.kt   # Model compression
│ │ │ ├── PerformanceMonitor.kt # Runtime metrics
│ │ │ └── MainActivity.kt # Compose UI main
│ │ ├── res
│ │ │ ├── drawable # Vector assets
│ │ │ └── mipmap # Launcher icons
│ │ └── assets # ONNX model files
│ │    ├── yolov11n-face.onnx # Face detection
│ │    └── deepfake_binary_s128_e5_early.onnx # Classification
└── build.gradle # Dependency management
```

## 📄 License
MIT License  
Model files and training data may have separate licensing.
- Additional Notes:
  - XML: snake_case naming
  - Compose: Material3 design guidelines
  - Kotlin: Android Kotlin Style Guide compliance

## 🤝 Team
Team Name: CloseAI

| Role | Member |
|------|--------|
| Project Lead | Seungheon Lee |
| AI Model Dev | Suhyok Yun |
| App Development | Garam Kim |
| UI/UX Design | Soyeon Kim |

## 📬 Contact
For inquiries, please contact:  
wintrover@gmail.com  