# AuraOnDevice-AI 🤖🎮

### 📥 Download the latest APK
[**Download AuraOnDeviceAI-v1.0.apk**](https://github.com/rajureddi/AuraOnDevice-AI_HUB/releases/download/V1.0.0/auraondeivceai.apk) 


[![Android Studio](https://img.shields.io/badge/Android%20Studio-4285F4?style=flat&logo=android%20studio&logoColor=white)](https://developer.android.com/studio)
[![Inference: MNN Engine](https://img.shields.io/badge/Inference-MNN%20Engine-orange)](https://github.com/alibaba/MNN)
[![MediaPipe Tasks](https://img.shields.io/badge/MediaPipe-Tasks-blue)](https://developers.google.com/mediapipe/solutions/genai/llm_inference/android)

AuraOnDevice-AI is a high-performance, features-packed Android application designed for exploring on-device AI capabilities. It integrates top-tier inference engines (MNN, MediaPipe, LiteRT-LM) to deliver a seamless and privacy-first user experience. 

### 🔥 New: Gemma 4 Support
We are excited to bring support for the latest **Gemma 4** models to your mobile device! These cutting-edge models are optimized for high-performance on-device inference, enabling more complex reasoning and creative tasks entirely offline.


---

## 🏗️ Core Components

AuraOnDevice-AI is built around four major pillars:
1. **MNN-LLM Engine**: Alibaba's ultra-fast inference engine for Large Language Models.
2. **MediaPipe LLM Inference**: Google's standardized API for running LLMs like **Gemma 3** on-device.
3. **LiteRT-LM (Modern)**: The latest inference stack for **Gemma 4**, **FastVLM**, and **SmolLM2** models.
4. **AI Studio**: A modular toolbox for specialized AI tasks (Vision, Audio, Text).


---

## 🎨 AI Studio: The Ultimate AI Toolbox

The **AI Studio** fragment provides a simplified interface for downloading and launching various specialized models.

| Vision Tasks | Audio Tasks | Text Tasks |
| :--- | :--- | :--- |
| **Face Landmarker**: 468-point mesh | **Audio Classifier**: Sound identification | **Language Detector**: Input ID |
| **Hand Landmarker**: 21-point tracking | **YamNet**: Environmental sound detection | **Text Classifier**: Topic & Sentiment |
| **Pose Landmarker**: Heavy/Full/Lite | | **BERT Embedder**: Contextual analysis |
| **Object Detector**: Real-time detection | | |
| **Interactive Segmenter**: Image clipping | | |

### 🔍 AI Studio Visuals

| Pose Landmarker (Full) | Face Landmarker | LLM Interaction (Gemma/Qwen) |

---

## 💬 LLM Chat Capabilities

Built on top of `MnnLlmChat`, this app expands features for modern mobile hardware.

- **Streaming Inference**: Real-time response generation.
- **Multi-turn conversations**: Context-aware chat sessions.
- **Model Sources**:
  - **Hugging Face**: Integrated model browser for seamless downloading.
  - **Local Storage**: Sideload and use your custom models easily.
- **Multimodal Support**: Future-ready integration for vision-language tasks (LLaVA-MNN).
- **Internet RAG (Web Search)**: Integrated real-time web search via DuckDuckGo.
  - **Dynamic Citations**: Provides "Proof" links at the end of responses.
  - **Wikipedia Priority**: Automatically prioritizes high-quality information sources.
  - **Optional Toggle**: Enable/Disable internet access per message for privacy and data saving.
  - **Advanced Engine Integration (LiteRT-LM)**:
  - **Gemma 4 Support**: Seamlessly run the latest Gemma 4 family including experimental variants.
  - **FastVLM Support**: High-speed Vision-Language model inference.
  - **SmolLM2 Support**: Highly efficient, small-parameter models for any mobile hardware.
- **Search Memory (Local Cache)**: Saved search results are stored locally to save power and data on repeat questions.


---

## 🛠️ Technical Details

### Engines Integration
- **MNN**: Leveraging NPU and GPU acceleration for low-latency inference on varying Android hardware.
- **MediaPipe**: Using `LlmInferenceSession` for high-level LLM task management, ensuring stability and performance for Google-optimized models.

### Assets Management
- Built-in downloader for MediaPipe LiteRT (.task) and TFLite models.
- Automatic splitting and management of large model files.
- Download mnn and mediapipe models from any source
---

## 🚀 Setup & Build

### Prerequisites
- Android Studio r.
- Android NDK (Version 25.1.8937393 recommended).
- A modern Android device (Snapdragon or mediatek with above 8gb ram for best LLM performance).

### Build steps
1. Clone the repository.
2. Open the project in Android Studio.
3. Run the Gradle sync.
4. Build and install on your device.

---



---

## 📄 License & Credits
AuraOnDevice-AI is based on the **MnnLlmChat** project by Alibaba and incorporates **MediaPipe** by Google.

**Contributors:**
- [Raju Reddi](https://github.com/rajureddi) (Main Developer)

---
*Elevate your mobile experience with local intelligence.*
