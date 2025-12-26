"""
OCR Service using Tesseract (FREE, local OCR).
"""

import os
from typing import Optional
import numpy as np
from PIL import Image
import io

# Tesseract OCR import
try:
    import pytesseract
    TESSERACT_AVAILABLE = True
except ImportError:
    TESSERACT_AVAILABLE = False
    print("Warning: pytesseract not installed. Run: pip install pytesseract")


class OCRService:
    """
    OCR service using Tesseract (free, local OCR).
    No API keys or credentials required.
    """
    
    def __init__(self, tesseract_path: Optional[str] = None):
        """
        Initialize OCR service.
        
        Args:
            tesseract_path: Optional path to tesseract executable.
                           On Windows: "C:\\Program Files\\Tesseract-OCR\\tesseract.exe"
                           On Linux/Mac: usually just "tesseract" or leave None
        """
        if tesseract_path:
            pytesseract.pytesseract.tesseract_cmd = tesseract_path
        
        self.is_configured = TESSERACT_AVAILABLE
    
    def extract_text(self, image: bytes) -> str:
        """
        Extract text from image bytes using Tesseract.
        
        Args:
            image: Image bytes (JPEG/PNG).
            
        Returns:
            Extracted text string.
        """
        if not TESSERACT_AVAILABLE:
            print("Warning: Tesseract not available. Install with: pip install pytesseract")
            return ""
        
        try:
            # Convert bytes to PIL Image
            pil_image = Image.open(io.BytesIO(image))
            
            # Extract text using Tesseract
            text = pytesseract.image_to_string(
                pil_image,
                lang='pol+eng',  # Polish and English
                config='--psm 6'  # Page segmentation mode 6 (uniform block)
            )
            
            return text.strip()
            
        except Exception as e:
            print(f"Tesseract OCR Error: {e}")
            return ""
    
    def extract_text_from_image(self, image_array: np.ndarray) -> str:
        """
        Extract text from OpenCV image array.
        
        Args:
            image_array: OpenCV image (BGR format).
            
        Returns:
            Extracted text string.
        """
        if not TESSERACT_AVAILABLE:
            print("Warning: Tesseract not available")
            return ""
        
        try:
            # Convert BGR to RGB
            if len(image_array.shape) == 3:
                rgb_image = image_array[..., ::-1]  # BGR to RGB
            else:
                rgb_image = image_array
            
            # Convert to PIL Image
            pil_image = Image.fromarray(rgb_image)
            
            # Extract text
            text = pytesseract.image_to_string(
                pil_image,
                lang='pol+eng',
                config='--psm 6'
            )
            
            return text.strip()
            
        except Exception as e:
            print(f"Tesseract OCR Error: {e}")
            return ""
    
    def extract_text_from_roi(self, image_array: np.ndarray) -> str:
        """
        Extract text from Region of Interest (ROI) - optimized for small crops.
        
        Args:
            image_array: Cropped image (ROI).
            
        Returns:
            Cleaned extracted text.
        """
        if not TESSERACT_AVAILABLE:
            return ""
        
        try:
            # Ensure image is RGB
            if len(image_array.shape) == 3:
                rgb_image = image_array[..., ::-1]
            else:
                rgb_image = image_array
            
            pil_image = Image.fromarray(rgb_image)
            
            # Use PSM 7 for single line text (better for field values)
            # Use PSM 8 for single word (for short fields)
            text = pytesseract.image_to_string(
                pil_image,
                lang='pol+eng',
                config='--psm 7'  # Single line of text
            )
            
            # Clean up the text
            cleaned = text.strip()
            # Remove extra whitespace
            cleaned = ' '.join(cleaned.split())
            
            return cleaned.upper()  # Convert to uppercase
            
        except Exception as e:
            print(f"Tesseract ROI OCR Error: {e}")
            return ""
    
    def is_available(self) -> bool:
        """
        Check if OCR service is available.
        
        Returns:
            True if Tesseract is installed and configured.
        """
        return TESSERACT_AVAILABLE
    
    def verify_installation(self) -> dict:
        """
        Verify Tesseract installation and return info.
        
        Returns:
            Dictionary with installation status and version.
        """
        if not TESSERACT_AVAILABLE:
            return {
                'installed': False,
                'error': 'pytesseract not installed',
                'solution': 'Run: pip install pytesseract'
            }
        
        try:
            version = pytesseract.get_tesseract_version()
            return {
                'installed': True,
                'version': str(version),
                'status': 'Ready'
            }
        except Exception as e:
            return {
                'installed': False,
                'error': str(e),
                'solution': 'Install Tesseract OCR: https://github.com/UB-Mannheim/tesseract/wiki'
            }


def check_tesseract():
    """
    Check if Tesseract is properly installed.
    Run this to diagnose OCR issues.
    """
    service = OCRService()
    info = service.verify_installation()
    
    print("=" * 50)
    print("Tesseract OCR Status:")
    print("=" * 50)
    
    if info['installed']:
        print(f"✓ Tesseract version: {info['version']}")
        print(f"✓ Status: {info['status']}")
    else:
        print(f"✗ Error: {info['error']}")
        print(f"→ Solution: {info['solution']}")
    
    print("=" * 50)
    return info
