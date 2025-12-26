# Instructions for running the ID Card OCR system

## Prerequisites

1. **Java Development Kit 17+**
   - Download from: https://adoptium.net/
   - Verify installation: `java -version`

2. **Python 3.9+** (for backend)
   - Verify installation: `python --version`

3. **Android Studio** (for Android app)
   - Download from: https://developer.android.com/studio

4. **Google Cloud Vision API credentials** (optional, for better OCR)
   - Create project at: https://console.cloud.google.com/
   - Enable Cloud Vision API
   - Download service account JSON

---

## BACKEND Setup

### 1. Navigate to backend directory
```bash
cd backend
```

### 2. Create virtual environment
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows
```

### 3. Install dependencies
```bash
pip install -r requirements.txt
```

### 4. Configure Google Cloud Vision (optional)
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/credentials.json"
# On Windows:
set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\credentials.json
```

### 5. Start the backend server
```bash
python main.py
```

The API will be available at: `http://localhost:8000`

### 6. Test the API
```bash
# Using curl (replace with your base64 images)
curl -X POST http://localhost:8000/health
```

---

## ANDROID Setup

### 1. Open Android Studio
- Open the `android` folder as an existing project

### 2. Sync Gradle
- Android Studio will automatically sync when you open the project
- If not, click: File → Sync Project with Gradle Files

### 3. Configure backend URL
Edit `app/src/main/java/com/idcard/ocr/network/NetworkClient.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_PC_IP:8000/"
```
Replace `YOUR_PC_IP` with your computer's IP address (not `localhost`)

### 4. Build and run
- Connect your Android device or start an emulator
- Click the Run button (▶) in Android Studio

---

## Updating Zone Coordinates

The zone coordinates in `zones_front.json` and `zones_back.json` are **placeholder values**.
You need to update them based on your actual PSD file:

### Method 1: Using your PSD file
1. Open the PSD in Photoshop
2. Select each text layer and note its bounding box:
   - Left, Top, Right, Bottom coordinates
3. Calculate percentages:
   - x = left / document_width
   - y = top / document_height
   - width = (right - left) / document_width
   - height = (bottom - top) / document_height

### Method 2: Using sample images
1. Run the app and capture test images
2. Check the extracted results
3. Adjust zone coordinates in the JSON files
4. Restart the backend

Example zones_front.json:
```json
{
  "document_width": 1000,
  "document_height": 630,
  "zones": {
    "f_nazwisko": {
      "x": 0.05,
      "y": 0.12,
      "width": 0.35,
      "height": 0.06
    }
  }
}
```

---

## Troubleshooting

### Backend issues:
- **Port in use**: Change port in `main.py`: `uvicorn.run(app, host="0.0.0.0", port=8080)`
- **OpenCV error**: Install opencv-python-headless instead of opencv-python

### Android issues:
- **Gradle sync failed**: Invalidate caches: File → Invalidate Caches / Restart
- **Camera permission**: Ensure permission is granted in app settings
- **Network error**: Verify backend URL uses correct IP address (not localhost)

### OCR issues:
- **No text extracted**: Check zone coordinates in zones_*.json
- **Invalid format**: Field validation may be too strict - adjust validators.py
- **Poor quality**: Ensure images are well-lit and not blurred

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | API info |
| `/health` | GET | Health check |
| `/ocr/idcard` | POST | Process front + back |
| `/ocr/front` | POST | Process front only |
| `/ocr/back` | POST | Process back only |

### Request format:
```json
{
  "front_image": "base64_encoded_jpeg...",
  "back_image": "base64_encoded_jpeg..."
}
```

### Response format:
```json
{
  "success": true,
  "data": {
    "f_nazwisko": "KOWALSKI",
    "f_imiona": "JAN MICHAŁ",
    ...
  },
  "message": "Extracted 14/16 fields"
}
```
