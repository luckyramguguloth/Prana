"""
Model Conversion Utility: PyTorch -> TorchScript / ONNX / TFLite representation.
"""
import os
import torch
from train_voice_emotion import MobileVoiceEmotionNet

def export_model():
    os.makedirs('ml/exported', exist_ok=True)
    model = MobileVoiceEmotionNet()
    ckpt_path = 'ml/checkpoints/voice_emotion_cnn_bilstm.pt'
    if os.path.exists(ckpt_path):
        model.load_state_dict(torch.load(ckpt_path))
    model.eval()

    dummy_input = torch.randn(1, 16)
    
    # Export TorchScript
    traced = torch.jit.trace(model, dummy_input)
    traced.save('ml/exported/voice_emotion_traced.pt')
    print("Successfully exported TorchScript model to ml/exported/voice_emotion_traced.pt")

if __name__ == '__main__':
    export_model()
