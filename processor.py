"""
Main OCR processing pipeline for ID card scanning.
Uses Tesseract OCR - FREE, no API keys needed!
"""

from typing import Dict, Any, Tuple, Optional
import numpy as np

from ocr.read_zones import ZoneReader
from ocr.detect_document import DocumentDetector
from ocr.normalize import ImageNormalizer
from ocr.perspective import PerspectiveCorrector
from ocr.validators import ResultValidator
from ocr.ocr_service import OCRService


class IDCardOCRProcessor:
    """
    Complete OCR processing pipeline for ID card documents.
    Uses Tesseract OCR - free, local, no API keys required.
    """
    
    # Field mapping: which fields are on front vs back
    FRONT_FIELDS = [
        'f_nazwisko', 'f_imiona', 'f_obywatelstwo', 'f_data_urodzenia',
        'f_plec', 'f_numer_ID', 'f_data_waznosci', 'f_numer_kodu'
    ]
    
    BACK_FIELDS = [
        'b_seria_id', 'b_numer_id', 'b_numer_ident', 'b_data_wydania',
        'b_kto_wydal', 'b_imiona_rodzicow', 'b_nazwisko_rodowe',
        'b_miejsce_urodzenia', 'MRZ'
    ]
    
    def __init__(self, 
                 zones_dir: str = None,
                 tesseract_path: str = None,
                 target_width: int = 1000,
                 target_height: int = 630):
        """
        Initialize IDCardOCRProcessor.
        
        Args:
            zones_dir: Directory containing zones_*.json files.
            tesseract_path: Optional path to tesseract executable.
            target_width: Target document width after normalization.
            target_height: Target document height after normalization.
        """
        self.zone_reader = ZoneReader(zones_dir)
        self.detector = DocumentDetector(target_width, target_height)
        self.normalizer = ImageNormalizer((target_width, target_height))
        self.perspective = PerspectiveCorrector(target_width, target_height)
        self.validator = ResultValidator()
        self.ocr_service = OCRService(tesseract_path)
        
        self.target_width = target_width
        self.target_height = target_height
    
    def check_ocr_status(self) -> dict:
        """
        Check if OCR is properly configured.
        
        Returns:
            Dictionary with OCR status.
        """
        return self.ocr_service.verify_installation()
    
    def process_front(self, image_base64: str) -> Dict[str, str]:
        """
        Process front of ID card.
        
        Args:
            image_base64: Base64 encoded front image.
            
        Returns:
            Dictionary with extracted and validated field values.
        """
        return self._process_side(image_base64, 'front', self.FRONT_FIELDS)
    
    def process_back(self, image_base64: str) -> Dict[str, str]:
        """
        Process back of ID card.
        
        Args:
            image_base64: Base64 encoded back image.
            
        Returns:
            Dictionary with extracted and validated field values.
        """
        return self._process_side(image_base64, 'back', self.BACK_FIELDS)
    
    def _process_side(self, image_base64: str, side: str, fields: list) -> Dict[str, str]:
        """
        Process one side of the ID card.
        
        Args:
            image_base64: Base64 encoded image.
            side: 'front' or 'back'.
            fields: List of field names to extract.
            
        Returns:
            Dictionary with extracted field values.
        """
        # Decode image
        image = self.normalizer.decode_base64(image_base64)
        
        if image is None or image.size == 0:
            return {field: "" for field in fields}
        
        # Detect and correct document perspective
        corrected, detected = self.detector.process(image)
        
        if not detected:
            # Use original image if detection failed
            corrected = self.normalizer.normalize_document(image)
        
        # Get zone definitions
        zones_data = self.zone_reader.load_zones(side)
        zones = zones_data['zones']
        
        # Extract text from each zone
        results = {}
        for field in fields:
            if field not in zones:
                results[field] = ""
                continue
            
            zone = zones[field]
            
            # Calculate absolute coordinates
            x = int(zone['x'] * self.target_width)
            y = int(zone['y'] * self.target_height)
            w = int(zone['width'] * self.target_width)
            h = int(zone['height'] * self.target_height)
            
            # Crop zone
            cropped = self.detector.crop_zone(corrected, x, y, w, h)
            
            if cropped.size == 0:
                results[field] = ""
                continue
            
            # OCR on cropped zone using ROI-optimized method
            text = self.ocr_service.extract_text_from_roi(cropped)
            
            # Clean and validate
            text = self.validator.clean_text(text)
            
            results[field] = text
        
        # Validate specific fields
        results = self.validator.validate_all(results)
        
        return results
    
    def process_both(self, front_base64: str, back_base64: str) -> Dict[str, Any]:
        """
        Process both sides of ID card.
        
        Args:
            front_base64: Base64 encoded front image.
            back_base64: Base64 encoded back image.
            
        Returns:
            Dictionary with all extracted fields and success status.
        """
        front_results = self.process_front(front_base64)
        back_results = self.process_back(back_base64)
        
        # Combine all results
        all_results = {}
        all_results.update(front_results)
        all_results.update(back_results)
        
        # Count successful extractions
        successful = sum(1 for v in all_results.values() if v)
        total = len(all_results)
        
        return {
            'success': True,
            'data': all_results,
            'extracted_fields': successful,
            'total_fields': total,
            'extraction_rate': successful / total if total > 0 else 0
        }
