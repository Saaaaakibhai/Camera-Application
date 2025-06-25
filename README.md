# 📸 Camera-Application

**Target SDK:** Android 11 (API 30)

---

## 🚀 How to Use

1. Clone this repository to any folder  
2. Open the project in **Android Studio**  
3. Allow time for **Gradle** to sync and dependencies to install  
4. **Run the app** on a device or emulator and enjoy 🎉  

---

## 📂 Project Features

### 📷 Image Capture & Storage

- Captures photos using the device camera ✅  
- Saves images locally in a designated app folder with unique timestamps ✅  
- Supports high-resolution images with optional compression (using Bitmap API in Android) ✅  
- Adheres to scoped storage compliance for Android 10+ ✅  

### 🖼️ On-Screen Photo Frame Overlay

- Provides a semi-transparent overlay for alignment ✅  
- Dynamically scales for different screen resolutions ✅  
- Adapts based on photo type selection ✅  

### 🧩 Photo Type Selection

- Offers options for various photo types with predefined aspect ratios: ✅  
  - ID Photo (3:2) ✅  
  - Member Photo (4:3) ✅  
  - ID + Member Combo (16:9) ✅  
- Updates frame overlays and provides ratio indicators ✅  

### 💡 User Guidance & Feedback

- Offers real-time feedback for proper alignment ✅  
- Displays tips and instructions below the frame ⚠️  

### 🛠️ Technical Features

- Recommends Jetpack Compose for UI but allows XML layouts ✅  
- Utilizes CameraX for consistent camera behavior ✅  
- Suggests runtime permission handling using AndroidX APIs ✅  
- Optional image validation features like blur and lighting checks ❌  

---

## 📌 Notes

- Make sure to test on real devices for camera behavior consistency  
- Scoped storage requires appropriate permission handling for Android 10+  
