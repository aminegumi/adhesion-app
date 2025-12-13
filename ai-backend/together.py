from flask import Flask, Response, request, jsonify
from flask_cors import CORS
import cv2
import numpy as np
import time
import requests
import json
import re
import os
import sys
import threading

# Add EmotiEffLib to path
EMOTIEFF_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'EmotiEffLib-main')
if EMOTIEFF_PATH not in sys.path:
    sys.path.insert(0, EMOTIEFF_PATH)

app = Flask(__name__, static_url_path='/static')
CORS(app)

# Camera lock for thread safety
camera_lock = threading.Lock()
cam = None

# Load EmotiEffLib emotion recognizer
model_loaded = False
emotion_recognizer = None

try:
    from emotiefflib.facial_analysis import EmotiEffLibRecognizerOnnx
    # Use local ONNX model path
    ONNX_MODEL_PATH = os.path.join(EMOTIEFF_PATH, 'models', 'affectnet_emotions', 'onnx', 'enet_b0_8_best_vgaf.onnx')
    
    if os.path.exists(ONNX_MODEL_PATH):
        # Monkey-patch to use local model
        import emotiefflib.utils as utils
        original_get_model_path_onnx = utils.get_model_path_onnx
        def local_get_model_path_onnx(model_name):
            local_path = os.path.join(EMOTIEFF_PATH, 'models', 'affectnet_emotions', 'onnx', model_name + '.onnx')
            if os.path.exists(local_path):
                return local_path
            return original_get_model_path_onnx(model_name)
        utils.get_model_path_onnx = local_get_model_path_onnx
        
        emotion_recognizer = EmotiEffLibRecognizerOnnx("enet_b0_8_best_vgaf")
        model_loaded = True
        print("✅ EmotiEffLib ONNX Emotion Detection loaded successfully!")
    else:
        print(f"⚠️ ONNX model not found at: {ONNX_MODEL_PATH}")
except Exception as e:
    print(f"⚠️ EmotiEffLib not available: {e}")
    import traceback
    traceback.print_exc()
    model_loaded = False

# Load OpenCV face cascade
face_cascade = None
try:
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    print("✅ OpenCV Face Detection loaded")
except Exception as e:
    print(f"⚠️ OpenCV Face Detection not available: {e}")

# Emotion state
max_emotion = "Neutral"
emotion_confidence = 0.5
emotion_history = []
cached_box = None

# OpenRouter API Configuration
OPENROUTER_API_KEY = os.environ.get('OPENROUTER_API_KEY', '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')
OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
OPENROUTER_MODEL = "google/gemini-2.0-flash-001"

def get_camera():
    """Get or initialize camera"""
    global cam
    with camera_lock:
        if cam is None or not cam.isOpened():
            # Try multiple camera indices
            for idx in [0, 1, 2]:
                cam = cv2.VideoCapture(idx)
                if cam.isOpened():
                    cam.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                    cam.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                    cam.set(cv2.CAP_PROP_FPS, 30)
                    print(f"✅ Camera opened on index {idx}")
                    break
            else:
                print("⚠️ Could not open any camera")
        return cam

def call_openrouter(messages, max_tokens=1000):
    """Call OpenRouter API with given messages"""
    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "http://localhost:5000",
        "X-Title": "Adhesion Mental Health Assistant"
    }
    
    payload = {
        "model": OPENROUTER_MODEL,
        "messages": messages,
        "max_tokens": max_tokens,
        "temperature": 0.7,
    }
    
    try:
        response = requests.post(OPENROUTER_API_URL, headers=headers, json=payload, timeout=30)
        response.raise_for_status()
        result = response.json()
        return result['choices'][0]['message']['content']
    except Exception as e:
        print(f"OpenRouter API error: {e}")
        return "I'm here to support you. How can I help you today?"

def detect_faces(frame):
    """Detect faces using OpenCV cascade"""
    if face_cascade is None:
        return []
    
    try:
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(
            gray, 
            scaleFactor=1.1, 
            minNeighbors=5, 
            minSize=(60, 60)
        )
        return faces
    except:
        return []

def detect_emotion_emotieff(face_img):
    """Detect emotion using EmotiEffLib"""
    global emotion_recognizer
    
    if emotion_recognizer is None or not model_loaded:
        return "Neutral", 0.5
    
    try:
        # Convert BGR to RGB for the model
        face_rgb = cv2.cvtColor(face_img, cv2.COLOR_BGR2RGB)
        
        # Predict emotion
        emotions, scores = emotion_recognizer.predict_emotions(face_rgb, logits=False)
        
        if emotions and len(emotions) > 0:
            emotion = emotions[0]
            # Get confidence for the predicted emotion
            emotion_idx = list(emotion_recognizer.idx_to_emotion_class.values()).index(emotion)
            confidence = float(scores[0][emotion_idx])
            
            # Map EmotiEffLib emotion names to our standard names
            emotion_map = {
                'Anger': 'Angry',
                'Contempt': 'Contempt',
                'Disgust': 'Disgust',
                'Fear': 'Fear',
                'Happiness': 'Happy',
                'Neutral': 'Neutral',
                'Sadness': 'Sad',
                'Surprise': 'Surprise'
            }
            
            return emotion_map.get(emotion, emotion), confidence
        
        return "Neutral", 0.5
    except Exception as e:
        print(f"EmotiEffLib detection error: {e}")
        return "Neutral", 0.5

def detection():
    """Generator function for video streaming with emotion detection"""
    global max_emotion, emotion_confidence, emotion_history, cached_box
    
    camera = get_camera()
    frame_count = 0
    
    while True:
        with camera_lock:
            if camera is None or not camera.isOpened():
                # Return a placeholder frame
                placeholder = np.zeros((480, 640, 3), dtype=np.uint8)
                cv2.putText(placeholder, "Camera initializing...", (150, 240), 
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
                ret, buffer = cv2.imencode('.jpg', placeholder)
                yield (b'--frame\r\n'
                       b'Content-Type: image/jpeg\r\n\r\n' + buffer.tobytes() + b'\r\n')
                time.sleep(0.5)
                camera = get_camera()
                continue
            
            ret, frame = camera.read()
        
        if not ret or frame is None:
            time.sleep(0.1)
            continue

        frame_count += 1
        display_frame = frame.copy()
        
        # Detect faces and run emotion recognition every 10 frames
        if frame_count % 10 == 0:
            faces = detect_faces(frame)
            
            if len(faces) > 0:
                # Get the largest face
                x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                cached_box = (x, y, w, h)
                
                if model_loaded:
                    # Extract face region with padding
                    padding = 20
                    y1 = max(0, y - padding)
                    y2 = min(frame.shape[0], y + h + padding)
                    x1 = max(0, x - padding)
                    x2 = min(frame.shape[1], x + w + padding)
                    
                    face_img = frame[y1:y2, x1:x2]
                    
                    if face_img.size > 0:
                        emotion, confidence = detect_emotion_emotieff(face_img)
                        max_emotion = emotion
                        emotion_confidence = confidence
                        
                        # Store in history
                        emotion_history.append({
                            'emotion': max_emotion,
                            'confidence': confidence,
                            'timestamp': time.time()
                        })
                        if len(emotion_history) > 50:
                            emotion_history.pop(0)
            else:
                cached_box = None
        
        # Draw on frame using cached detection results
        if cached_box:
            x, y, w, h = cached_box
            
            # Draw rectangle around face - purple color
            cv2.rectangle(display_frame, (x, y), (x+w, y+h), (138, 43, 226), 2)
            
            # Draw emotion label
            label = f"{max_emotion}: {emotion_confidence*100:.0f}%"
            
            # Label background
            label_size = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)[0]
            cv2.rectangle(display_frame, (x, y-30), (x + label_size[0] + 10, y), (138, 43, 226), -1)
            cv2.putText(display_frame, label, (x+5, y-8), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
            
            # Confidence bar
            bar_width = int(w * emotion_confidence)
            cv2.rectangle(display_frame, (x, y+h+5), (x + bar_width, y+h+12), (0, 255, 0), -1)
            cv2.rectangle(display_frame, (x, y+h+5), (x + w, y+h+12), (100, 100, 100), 1)
        
        # Status indicator
        if model_loaded:
            status = f"EmotiEffLib Active | {max_emotion}"
            color = (0, 255, 0)
        else:
            status = "Face Detection Only"
            color = (0, 255, 255)
        
        cv2.putText(display_frame, status, (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

        # Encode and yield frame
        ret, buffer = cv2.imencode('.jpg', display_frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
        if ret:
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + buffer.tobytes() + b'\r\n')
        
        time.sleep(0.033)  # ~30 FPS

def get_system_prompt(current_mood):
    """Get appropriate system prompt based on detected emotion"""
    base_prompt = """You are Serenity, a compassionate Mental Health Assistant. Your role is to:
- Provide emotional support and therapeutic conversations
- Listen with empathy and validate feelings
- Offer practical coping strategies and wellness tips
- Encourage healthy habits and medication adherence
- Never provide medical diagnoses or replace professional help
- ONLY answer questions related to mental health, wellness, emotions, and wellbeing
- If asked about non-health topics (like coding, math, history, etc.), politely say: "I'm Serenity, your mental health companion. I'm here to support your emotional wellbeing. Is there something about your mental health or feelings you'd like to discuss?"

Guidelines:
- Keep responses concise (60-100 words)
- Be warm and caring
- Avoid using asterisks or special formatting
- Be conversational and human-like
- Stay focused on mental health and wellness topics only"""

    mood_specific = {
        'Happy': "\n\nThe user is happy. Celebrate their positive mood!",
        'Sad': "\n\nThe user seems sad. Approach with extra empathy and offer comfort.",
        'Angry': "\n\nThe user seems frustrated. Stay calm and acknowledge their feelings.",
        'Fear': "\n\nThe user seems anxious. Provide reassurance and calming techniques.",
        'Surprise': "\n\nThe user seems surprised. Engage curiously.",
        'Disgust': "\n\nThe user seems uncomfortable. Approach with understanding.",
        'Neutral': "\n\nThe user is calm. Check in on their overall wellbeing.",
        'Contempt': "\n\nThe user seems dismissive. Stay patient and non-judgmental.",
    }
    
    return base_prompt + mood_specific.get(current_mood, mood_specific['Neutral'])

def clean_text(text):
    """Clean the response text"""
    text = text.replace('*', '').replace('_', '')
    text = re.sub(r'\n{3,}', '\n\n', text)
    text = re.sub(r' +', ' ', text)
    return text.strip()

def bot_answer(question, current_mood, conversation_history=None):
    """Generate bot response using OpenRouter"""
    system_prompt = get_system_prompt(current_mood)
    
    messages = [{"role": "system", "content": system_prompt}]
    
    if conversation_history:
        for msg in conversation_history[-10:]:
            messages.append({
                "role": msg.get('role', 'user'),
                "content": msg.get('content', '')
            })
    
    messages.append({"role": "user", "content": question})
    
    response = call_openrouter(messages)
    return clean_text(response)

# Routes
@app.route('/')
def index():
    return jsonify({
        'status': 'Adhesion AI Backend running',
        'emotion_model': 'EmotiEffLib ONNX' if model_loaded else 'Face Detection Only',
        'camera': cam is not None and cam.isOpened() if cam else False
    })

@app.route('/video')
def video():
    """Video streaming route"""
    return Response(detection(), mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/chat', methods=['POST'])
def chat():
    """Handle chat messages"""
    data = request.json
    user_message = data.get('message', '')
    conversation_history = data.get('history', [])
    
    current_emotion = max_emotion or 'Neutral'
    bot_response = bot_answer(user_message, current_emotion, conversation_history)
    
    return jsonify({
        'bot_message': bot_response,
        'detected_emotion': current_emotion,
        'timestamp': time.time()
    })

@app.route('/emotion', methods=['GET'])
def get_emotion():
    """Get current detected emotion"""
    global max_emotion, emotion_confidence, emotion_history
    
    return jsonify({
        'emotion': max_emotion or 'Neutral',
        'confidence': emotion_confidence,
        'model_active': model_loaded,
        'model_type': 'EmotiEffLib' if model_loaded else 'face-detection',
        'history': emotion_history[-10:] if emotion_history else []
    })

@app.route('/motivation', methods=['POST'])
def get_motivation():
    """Generate motivational message"""
    data = request.json
    user_context = data.get('context', 'general wellbeing')
    
    current_emotion = max_emotion or 'Neutral'
    
    prompt = f"""Give a brief, powerful motivational message about {user_context}. 
    Be warm, supportive, and inspiring. Keep it under 80 words. No asterisks."""
    
    messages = [
        {"role": "system", "content": "You are a supportive wellness coach."},
        {"role": "user", "content": prompt}
    ]
    
    response = call_openrouter(messages, max_tokens=150)
    
    return jsonify({
        'motivation': clean_text(response),
        'emotion': current_emotion,
        'timestamp': time.time()
    })

@app.route('/analyze-text', methods=['POST'])
def analyze_text():
    """Analyze text for emotional content"""
    data = request.json
    text = data.get('text', '')
    
    if not text:
        return jsonify({'error': 'No text provided'}), 400
    
    prompt = f"""Analyze this text's emotional tone. Return JSON with:
- primary_emotion: main emotion (happy, sad, angry, fearful, neutral, anxious)
- confidence: number 0-1
- analysis: brief observation (1 sentence)
- recommendation: wellness suggestion (1 sentence)

Text: "{text}"

Return only valid JSON."""

    messages = [
        {"role": "system", "content": "You analyze emotions. Respond only with JSON."},
        {"role": "user", "content": prompt}
    ]
    
    response = call_openrouter(messages, max_tokens=200)
    
    try:
        response = response.strip()
        if response.startswith('```'):
            response = re.sub(r'^```json?\n?', '', response)
            response = re.sub(r'\n?```$', '', response)
        result = json.loads(response)
    except:
        result = {
            'primary_emotion': 'neutral',
            'confidence': 0.5,
            'analysis': 'I sense you have something on your mind.',
            'recommendation': 'Take a moment to breathe and reflect.'
        }
    
    return jsonify(result)

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    camera = get_camera()
    return jsonify({
        'status': 'healthy',
        'model_loaded': model_loaded,
        'model_type': 'EmotiEffLib' if model_loaded else 'none',
        'camera_available': camera.isOpened() if camera else False
    })

if __name__ == '__main__':
    print("\n" + "="*50)
    print("🚀 Starting Adhesion AI Backend with EmotiEffLib...")
    print(f"📸 Emotion Detection: {'✅ EmotiEffLib Active' if model_loaded else '❌ Face Detection Only'}")
    print("="*50 + "\n")
    app.run(host='0.0.0.0', port=5000, debug=True, threaded=True)

