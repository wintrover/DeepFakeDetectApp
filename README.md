# BLUE CHECK - 딥페이크 탐지 안드로이드 애플리케이션

![로고](./res/mipmap-anydpi-v26/logo2.xml)

## 📖 개요
YOLO 객체 감지 모델과 ONNX 런타임 기반의 딥러닝 모델을 활용해 이미지 내 딥페이크를 탐지하는 안드로이드 앱입니다. </br>
Jetpack Compose로 구현된 현대적 UI와 실시간 분석 기능을 제공합니다.

## ✨ 주요 기능
- **Deepfake 탐지**:
  - 업로드된 이미지에서 얼굴을 검출하고 Deepfake 여부 분석
- **인증마크 부여**:
  - 실제 사진(Real) 판정 시 인증 마크 추가 및 갤러리 저장
- **이미지 업로드 지원**:
  - 기기 갤러리에서 사진 선택 또는 URL을 통한 이미지 입력
- **직관적인 UI**:
  - Jetpack Compose 기반의 사용자 친화적인 인터페이스

## 🛠 기술 스택
| 분류 | 기술 |
|------|------|
| 언어 | Kotlin 1.9+ |
| UI | Jetpack Compose Material3 |
| ML | ONNX Runtime 1.16+ / YOLOv11 |
| 비동기 | Kotlin Coroutine |
| 이미지 | Coil 2.5+ |
| 애니메이션 | Lottie 6.1+ |

## 🚀 시작하기
### 전제 조건
- Android Studio Giraffe 이상
- Android SDK 34 (API 34)

### 설치 방법
```bash
git clone https://github.com/wintrover/DeepFakeDetectApp.git
```

## 앱 실행
- Android Studio에서 MainActivity.kt를 실행
- 물리 기기 또는 에뮬레이터에서 실행 가능

## 🖥 사용 방법
1. 이미지 업로드
- [이미지 선택] 버튼: 로컬 갤러리 접근
- [이미지 주소 입력] 버튼: 웹 이미지 URL 입력
2. 인증 마크 추가
- Real 판정 시 [인증마크] 버튼 활성화 → 갤러리에 저장

📁 프로젝트 구조
```bash
.
├── app
│   ├── src/main
│   │   ├── java/com/garam/cvproject
│   │   │   ├── DeepfakeDetector.kt  # AI 모델 핸들러
│   │   │   └── MainActivity.kt      # 컴포즈 UI 메인
│   │   ├── res
│   │   │   ├── drawable           # 벡터 애셋
│   │   │   └── mipmap             # 런처 아이콘
│   │   └── assets                 # ONNX 모델 파일
└── build.gradle                   # 종속성 관리
```

## 📄 라이선스
Apache License 2.0 </br>
모델 파일 및 학습 데이터는 별도 라이선스가 적용될 수 있습니다.
- 기타사항
  - XML: snake_case 네이밍
  - 컴포즈: Material3 디자인 가이드라인 적용
  - Kotlin: Android Kotlin Style Guide 준수
    
## 🤝 기여
팀 이름 : CloseAI

| 역할 | 이름 |
|------|------|
| 기획 및 총괄 | 이승헌 |
| AI모델 개발 | 윤수혁 |
| 앱 제작 | 김가람 |
| UI/UX 디자인 | 김소연 |

## 📬 문의
wintrover@gmail.com </br>
