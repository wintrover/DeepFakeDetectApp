# BLUE CHECK - 딥페이크 탐지 안드로이드 애플리케이션

![로고](./res/mipmap-anydpi-v26/logo2.xml)

## 📖 개요
YOLO 객체 감지 모델과 ONNX 런타임 기반의 딥러닝 모델을 활용해 이미지 내 딥페이크를 탐지하는 안드로이드 앱입니다.
Jetpack Compose로 구현된 현대적 UI와 실시간 분석 기능을 제공합니다.

## ✨ 주요 기능
- **멀티 소스 이미지 업로드**
  - 갤러리 선택 / URL 입력 지원
- **AI 기반 분석**
  - YOLO v11n-face 모델: 얼굴 영역 감지
  - Custom CNN 모델: 딥페이크 이진 분류 (128x128 입력)
- **시각화 시스템**
  - Lottie 애니메이션 인터랙션
  - 결과에 따른 색상 강조 (Real: 파랑, Fake: 빨강)
- **인증 시스템**
  - Real 판정 시 투명도 50% 인증마크 자동 추가
  - 갤러리 저장 기능 지원

## 🛠 기술 스택
| 분류 | 기술 |
|------|------|
| 언어 | Kotlin 1.9+ |
| UI | Jetpack Compose Material3 |
| ML | ONNX Runtime 1.16+ |
| 비동기 | Kotlin Coroutine |
| 이미지 | Coil 2.5+ |
| 애니메이션 | Lottie 6.1+ |

## 🚀 시작하기
### 전제 조건
- Android Studio Giraffe 이상
- Android SDK 34 (API 34)
- Gradle 8.0+

### 설치 방법
```bash
git clone https://github.com/your-repo/blue-check.git
cd blue-check
./gradlew assembleDebug
