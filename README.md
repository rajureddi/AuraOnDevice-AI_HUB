# AuraOnDevice AI Hub 🧠📱

[![Project Status: Active](https://img.shields.io/badge/Project%20Status-Active-brightgreen.svg)](https://github.com/rajureddi/AuraOnDevice-AI_HUB)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Engine: MNN](https://img.shields.io/badge/Engine-MNN-orange.svg)](https://github.com/alibaba/MNN)
[![Inference: MediaPipe](https://img.shields.io/badge/Inference-MediaPipe-blue.svg)](https://developers.google.com/mediapipe)

---
| HomePage | Model Market |  Ai Studio |
| :---: | :---: |:---: |
| ![HomePage](screenshots/photo_2026-03-28_16-0-53.jpg) | ![Model Market ](screenshots/photo_2026-03-28_16-50-53.jpg) | ![Ai Studio ](screenshots/photo_2026-03-28_16-51-05.jpg) |

**AuraOnDevice AI Hub** is a cutting-edge, privacy-first mobile application designed to bring the power of state-of-the-art Large Language Models (LLMs) and Multi-modal AI directly to your Android device. Entirely offline, no APIs, no data leaks.


---

## ✨ Key Features

### 💬 Advanced LLM Chat
- **State-of-the-art Models**: Support for **Gemma 4**, **Gemma 3**, **Qwen**, **DeepSeek**, and more.
- **Advanced Engines**: High-performance inference using **MNN**, **MediaPipe**, and the new **LiteRT-LM (Modern)** engine.
- **Multimodal capabilities**: Interact with text, images, and audio.
- **Fast Inference**: Optimized for mobile NPU/GPU with latest quantization techniques.


### 🎨 AI Studio
A comprehensive suite of on-device AI tools for developers and enthusiasts:
- **Vision**: Face Detection, Hand tracking, Pose estimation, Object detection, and Interactive segmentation.
- **Audio**: Sound classification using YamNet.
- **Text**: Contextual embeddings (BERT), Language detection, and Sentiment analysis.

### 🔒 Privacy First
- **100% Offline**: All processing happens on-device.
- **No Data Harvesting**: Your conversations and data never leave your phone.
- **No API Costs**: Zero reliance on cloud providers like OpenAI or Anthropic.

---

## 📸 Screenshots & Demos

| Gemma 3 (MediaPipe) | Qwen 2.5 (MNN Engine) | |
| :---: | :---: | :---: |
| ![Gemma 3](screenshots/video_2026-03-28_16-07-28.gif) | ![Qwen](apps/Android/AuraOnDeviceAi/assets/qwen_3.gif) |

### 🎨 AI Studio Showcase
| Vision Studio (Pose Detection) | Face Studio |  
| :---: | :---: |
| ![Pose Landmarker](screenshots/Screenshot_2026_03_28_15_21_05_073_com_aura_on_device_ai_mnnllm.jpg) | ![Face Landmarker](screenshots/IMG_20260328_154204.jpg) |
Hand and Gesture Detection

https://github.com/user-attachments/assets/77c21d67-6c96-4238-a3af-9809d8c00320

Object Detection 

https://github.com/user-attachments/assets/77cb4ffd-ab2c-4390-a317-33a44f4a5d18



---

## 🛠 Project Structure

- `apps/Android/AuraOnDeviceAi`: The core Android application source code.
- `include/`: Native headers for the MNN engine.
- `source/`: Core engine implementations.
- `transformers/`: Advanced tools for Large Language Model (LLM) conversion, quantization, and optimization.
- `tools/`: Performance profiling and debugging utilities.


---

## 🚀 Getting Started

1. **Clone the Repo**:
   ```bash
   git clone https://github.com/rajureddi/AuraOnDevice-AI_HUB.git
   ```
2. **Open in Android Studio**:
   Navigate to `apps/Android/AuraOnDeviceAi`.
3. **Build & Run**:
   Ensure you have the latest Android NDK installed.

For detailed setup instructions, see the [App README](apps/Android/AuraOnDeviceAi/README.md).

---

## 🤝 Contributing
We welcome contributions! Whether it's optimization, new models, or UI improvements, feel free to open a PR.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Created with ❤️ by [Raju Reddi](https://github.com/rajureddi)
