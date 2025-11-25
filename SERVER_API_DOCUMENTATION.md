# Server-Side API Documentation for PDF Preview System

## Overview

This document describes the server-side APIs required to support the hybrid PDF preview system in the Android app. The server needs to provide endpoints for:
1. **Preview Mode**: Convert PDF pages to images for preview
2. **Full PDF Access**: Provide authenticated download links
3. **Access Control**: Validate user permissions

## Base URL

```
https://your-server.com/api
```

## Authentication

All endpoints (except public preview) require authentication via:
- **Header**: `Authorization: Bearer <token>`
- Or session-based authentication (cookies)

---

## API Endpoints

### 1. Get PDF Preview Images

**Endpoint:** `GET /notes/{note_id}/preview`

**Description:** Returns the first N pages of a PDF as images for preview mode.

**Parameters:**
- `note_id` (path, required): Unique identifier of the note
- `pages` (query, optional): Number of pages to preview (default: 2, max: 5)
- `quality` (query, optional): Image quality - "low", "medium", "high" (default: "medium")
- `width` (query, optional): Image width in pixels (default: 800)

**Request Example:**
```
GET /api/notes/abc123/preview?pages=2&quality=medium&width=800
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "note_id": "abc123",
  "title": "Sample Note",
  "total_pages": 15,
  "preview_pages": 2,
  "images": [
    {
      "page_number": 1,
      "image_url": "https://your-server.com/previews/abc123_page1.jpg",
      "thumbnail_url": "https://your-server.com/previews/abc123_page1_thumb.jpg",
      "width": 800,
      "height": 1131
    },
    {
      "page_number": 2,
      "image_url": "https://your-server.com/previews/abc123_page2.jpg",
      "thumbnail_url": "https://your-server.com/previews/abc123_page2_thumb.jpg",
      "width": 800,
      "height": 1131
    }
  ],
  "watermark": {
    "enabled": true,
    "text": "Preview Only"
  },
  "access_control": {
    "can_view_full": false,
    "requires_payment": true,
    "payment_amount": 5.99,
    "currency": "USD"
  }
}
```

**Response (Error - 404 Not Found):**
```json
{
  "success": false,
  "error": {
    "code": "NOTE_NOT_FOUND",
    "message": "Note with ID abc123 not found"
  }
}
```

**Response (Error - 403 Forbidden):**
```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "You don't have permission to preview this note"
  }
}
```

**Response (Error - 500 Internal Server Error):**
```json
{
  "success": false,
  "error": {
    "code": "PDF_PROCESSING_ERROR",
    "message": "Failed to process PDF. Please try again later."
  }
}
```

---

### 2. Get Full PDF Download Link

**Endpoint:** `POST /notes/{note_id}/download`

**Description:** Generates a secure, time-limited download link for the full PDF after payment/authentication.

**Headers:**
```
Authorization: Bearer <user_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "payment_id": "pay_1234567890",  // Optional: Payment transaction ID
  "payment_method": "stripe",      // Optional: Payment method used
  "validate_payment": true          // Whether to validate payment before generating link
}
```

**Request Example:**
```
POST /api/notes/abc123/download
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "payment_id": "pay_1234567890",
  "payment_method": "stripe",
  "validate_payment": true
}
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "note_id": "abc123",
  "download_url": "https://your-server.com/downloads/abc123?token=secure_token_here&expires=1699123456",
  "expires_at": "2024-11-25T23:30:56Z",
  "expires_in_seconds": 3600,
  "file_info": {
    "filename": "sample_note.pdf",
    "size_bytes": 2456789,
    "size_mb": 2.35,
    "pages": 15
  }
}
```

**Response (Error - 402 Payment Required):**
```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_REQUIRED",
    "message": "Payment required to download full PDF",
    "payment_amount": 5.99,
    "currency": "USD",
    "payment_url": "https://your-server.com/payment/abc123"
  }
}
```

**Response (Error - 403 Forbidden):**
```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "You don't have permission to download this note"
  }
}
```

**Response (Error - 401 Unauthorized):**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required"
  }
}
```

---

### 3. Validate Payment/Access

**Endpoint:** `GET /notes/{note_id}/access`

**Description:** Check if user has access to view/download the full PDF.

**Headers:**
```
Authorization: Bearer <user_token>
```

**Request Example:**
```
GET /api/notes/abc123/access
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "note_id": "abc123",
  "has_access": true,
  "access_type": "paid",  // "free", "paid", "subscription", "trial"
  "expires_at": "2024-12-25T23:30:56Z",  // null if permanent
  "downloads_remaining": 3,  // null if unlimited
  "can_preview": true,
  "can_download": true
}
```

**Response (No Access - 200 OK):**
```json
{
  "success": true,
  "note_id": "abc123",
  "has_access": false,
  "access_type": null,
  "expires_at": null,
  "downloads_remaining": null,
  "can_preview": true,
  "can_download": false,
  "payment_required": {
    "amount": 5.99,
    "currency": "USD",
    "payment_url": "https://your-server.com/payment/abc123"
  }
}
```

---

### 4. Process Payment (Optional - if handling payments server-side)

**Endpoint:** `POST /notes/{note_id}/payment`

**Description:** Process payment for note access.

**Headers:**
```
Authorization: Bearer <user_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "payment_method": "stripe",
  "payment_token": "tok_visa_1234",
  "amount": 5.99,
  "currency": "USD"
}
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "payment_id": "pay_1234567890",
  "status": "succeeded",
  "note_id": "abc123",
  "amount": 5.99,
  "currency": "USD",
  "download_url": "https://your-server.com/downloads/abc123?token=secure_token_here&expires=1699123456",
  "expires_at": "2024-11-25T23:30:56Z"
}
```

---

## Server-Side Implementation Requirements

### 1. PDF Processing Library

**Recommended Libraries:**

**Python:**
- `pdf2image` + `Pillow` - Convert PDF pages to images
- `PyPDF2` or `pypdf` - Extract PDF metadata
- `reportlab` - Add watermarks

**Node.js:**
- `pdf-poppler` or `pdf2pic` - Convert PDF to images
- `pdf-lib` - PDF manipulation
- `sharp` - Image processing and watermarking

**Java:**
- Apache PDFBox - PDF processing
- iText - PDF manipulation
- Java ImageIO - Image processing

### 2. Image Generation Process

```python
# Python Example using pdf2image
from pdf2image import convert_from_path
from PIL import Image, ImageDraw, ImageFont
import os

def generate_preview_images(pdf_path, output_dir, pages=2, quality="medium"):
    """
    Convert first N pages of PDF to images with watermark
    
    Args:
        pdf_path: Path to PDF file
        output_dir: Directory to save images
        pages: Number of pages to convert
        quality: Image quality setting
    
    Returns:
        List of image file paths
    """
    # Convert PDF pages to images
    images = convert_from_path(
        pdf_path,
        first_page=1,
        last_page=pages,
        dpi=200 if quality == "high" else 150 if quality == "medium" else 100
    )
    
    image_paths = []
    for i, image in enumerate(images, start=1):
        # Add watermark
        watermarked = add_watermark(image, "Preview Only")
        
        # Save image
        filename = f"page_{i}.jpg"
        filepath = os.path.join(output_dir, filename)
        watermarked.save(filepath, "JPEG", quality=85)
        image_paths.append(filepath)
    
    return image_paths

def add_watermark(image, text):
    """Add watermark text to image"""
    from PIL import ImageDraw, ImageFont
    
    # Create a copy to avoid modifying original
    watermarked = image.copy()
    draw = ImageDraw.Draw(watermarked)
    
    # Try to load font, fallback to default
    try:
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
    
    # Get text dimensions
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    # Calculate position (center of image)
    width, height = watermarked.size
    x = (width - text_width) / 2
    y = (height - text_height) / 2
    
    # Draw semi-transparent watermark
    draw.text(
        (x, y),
        text,
        fill=(255, 255, 255, 128),  # White with 50% opacity
        font=font
    )
    
    return watermarked
```

### 3. Secure Download Link Generation

```python
import hashlib
import time
from datetime import datetime, timedelta

def generate_secure_download_link(note_id, user_id, expires_in_hours=1):
    """
    Generate a secure, time-limited download link
    
    Args:
        note_id: Note identifier
        user_id: User identifier
        expires_in_hours: Link expiration time
    
    Returns:
        Dictionary with download URL and expiration info
    """
    # Generate secure token
    timestamp = int(time.time())
    expires_at = timestamp + (expires_in_hours * 3600)
    
    # Create token hash
    token_data = f"{note_id}:{user_id}:{expires_at}:{SECRET_KEY}"
    token = hashlib.sha256(token_data.encode()).hexdigest()
    
    # Build download URL
    download_url = f"https://your-server.com/downloads/{note_id}?token={token}&expires={expires_at}"
    
    return {
        "download_url": download_url,
        "expires_at": datetime.fromtimestamp(expires_at).isoformat(),
        "expires_in_seconds": expires_in_hours * 3600
    }

def validate_download_token(note_id, token, expires):
    """
    Validate download token before serving file
    
    Returns:
        True if valid, False otherwise
    """
    # Check expiration
    if int(time.time()) > int(expires):
        return False
    
    # Recreate token and compare
    user_id = get_user_from_token(token)  # Extract from token or session
    token_data = f"{note_id}:{user_id}:{expires}:{SECRET_KEY}"
    expected_token = hashlib.sha256(token_data.encode()).hexdigest()
    
    return token == expected_token
```

### 4. File Storage Structure

```
server/
├── pdfs/                    # Original PDF files (secure, not publicly accessible)
│   ├── abc123.pdf
│   └── def456.pdf
├── previews/                 # Generated preview images (publicly accessible)
│   ├── abc123/
│   │   ├── page_1.jpg
│   │   ├── page_1_thumb.jpg
│   │   ├── page_2.jpg
│   │   └── page_2_thumb.jpg
│   └── def456/
│       └── ...
└── downloads/                # Temporary download links (time-limited access)
    └── (served via secure token validation)
```

### 5. Caching Strategy

**Preview Images:**
- Cache generated preview images
- Regenerate only if PDF is updated
- Cache key: `{note_id}_{pages}_{quality}_{width}`

**Download Links:**
- Do NOT cache download links
- Generate fresh token for each request
- Validate on every download attempt

### 6. Security Considerations

1. **File Access Control:**
   - Store PDFs outside web root
   - Serve via secure download endpoint only
   - Never expose direct file paths

2. **Token Security:**
   - Use strong secret keys
   - Include user ID in token generation
   - Set short expiration times (1-24 hours)

3. **Rate Limiting:**
   - Limit preview requests per user/IP
   - Limit download link generation
   - Prevent abuse

4. **Watermarking:**
   - Add user-specific watermarks (user ID, email)
   - Make watermarks difficult to remove
   - Track watermark for copyright protection

5. **Access Logging:**
   - Log all preview requests
   - Log all download attempts
   - Monitor for suspicious activity

---

## Implementation Checklist

### Backend Setup
- [ ] Install PDF processing library (pdf2image, pdf-poppler, etc.)
- [ ] Install image processing library (Pillow, sharp, etc.)
- [ ] Set up secure file storage structure
- [ ] Configure image cache directory
- [ ] Set up authentication system

### API Endpoints
- [ ] Implement `GET /notes/{id}/preview`
- [ ] Implement `POST /notes/{id}/download`
- [ ] Implement `GET /notes/{id}/access`
- [ ] Implement `POST /notes/{id}/payment` (if needed)

### Security
- [ ] Implement token generation and validation
- [ ] Add rate limiting
- [ ] Set up access logging
- [ ] Configure secure file serving
- [ ] Add watermarking functionality

### Testing
- [ ] Test preview image generation
- [ ] Test download link generation
- [ ] Test token validation
- [ ] Test access control
- [ ] Test watermarking
- [ ] Load testing for image generation

---

## Example Server Implementation (Python/Flask)

```python
from flask import Flask, jsonify, request, send_file
from flask_cors import CORS
import os
from pdf2image import convert_from_path
from PIL import Image, ImageDraw, ImageFont
import hashlib
import time
from datetime import datetime, timedelta

app = Flask(__name__)
CORS(app)

PDF_STORAGE = "pdfs"
PREVIEW_STORAGE = "previews"
SECRET_KEY = os.environ.get("SECRET_KEY", "your-secret-key-here")

@app.route('/api/notes/<note_id>/preview', methods=['GET'])
def get_preview(note_id):
    """Generate preview images for PDF"""
    try:
        pages = int(request.args.get('pages', 2))
        quality = request.args.get('quality', 'medium')
        width = int(request.args.get('width', 800))
        
        # Validate pages limit
        pages = min(pages, 5)  # Max 5 pages
        
        pdf_path = os.path.join(PDF_STORAGE, f"{note_id}.pdf")
        if not os.path.exists(pdf_path):
            return jsonify({
                "success": False,
                "error": {"code": "NOTE_NOT_FOUND", "message": "Note not found"}
            }), 404
        
        # Generate preview images
        preview_dir = os.path.join(PREVIEW_STORAGE, note_id)
        os.makedirs(preview_dir, exist_ok=True)
        
        images = convert_from_path(pdf_path, first_page=1, last_page=pages, dpi=150)
        image_urls = []
        
        for i, image in enumerate(images, start=1):
            # Add watermark
            watermarked = add_watermark(image, "Preview Only")
            
            # Resize if needed
            if width:
                ratio = width / watermarked.width
                new_height = int(watermarked.height * ratio)
                watermarked = watermarked.resize((width, new_height), Image.Resampling.LANCZOS)
            
            # Save image
            filename = f"page_{i}.jpg"
            filepath = os.path.join(preview_dir, filename)
            watermarked.save(filepath, "JPEG", quality=85)
            
            image_urls.append({
                "page_number": i,
                "image_url": f"https://your-server.com/previews/{note_id}/{filename}",
                "thumbnail_url": f"https://your-server.com/previews/{note_id}/thumb_{filename}",
                "width": watermarked.width,
                "height": watermarked.height
            })
        
        return jsonify({
            "success": True,
            "note_id": note_id,
            "preview_pages": pages,
            "images": image_urls,
            "watermark": {"enabled": True, "text": "Preview Only"},
            "access_control": {
                "can_view_full": False,
                "requires_payment": True,
                "payment_amount": 5.99,
                "currency": "USD"
            }
        })
        
    except Exception as e:
        return jsonify({
            "success": False,
            "error": {"code": "PDF_PROCESSING_ERROR", "message": str(e)}
        }), 500

@app.route('/api/notes/<note_id>/download', methods=['POST'])
def get_download_link(note_id):
    """Generate secure download link"""
    # Validate authentication
    token = request.headers.get('Authorization')
    if not token or not validate_auth(token):
        return jsonify({
            "success": False,
            "error": {"code": "UNAUTHORIZED", "message": "Authentication required"}
        }), 401
    
    user_id = get_user_from_token(token)
    
    # Check payment/access
    if not has_access(user_id, note_id):
        return jsonify({
            "success": False,
            "error": {
                "code": "PAYMENT_REQUIRED",
                "message": "Payment required to download full PDF",
                "payment_amount": 5.99,
                "currency": "USD"
            }
        }), 402
    
    # Generate secure download link
    download_info = generate_secure_download_link(note_id, user_id, expires_in_hours=1)
    
    return jsonify({
        "success": True,
        "note_id": note_id,
        **download_info,
        "file_info": {
            "filename": f"{note_id}.pdf",
            "size_bytes": os.path.getsize(os.path.join(PDF_STORAGE, f"{note_id}.pdf")),
            "pages": get_pdf_page_count(note_id)
        }
    })

@app.route('/downloads/<note_id>', methods=['GET'])
def download_pdf(note_id):
    """Serve PDF file with token validation"""
    token = request.args.get('token')
    expires = request.args.get('expires')
    
    if not validate_download_token(note_id, token, expires):
        return jsonify({
            "success": False,
            "error": {"code": "INVALID_TOKEN", "message": "Invalid or expired download link"}
        }), 403
    
    pdf_path = os.path.join(PDF_STORAGE, f"{note_id}.pdf")
    return send_file(pdf_path, as_attachment=True, download_name=f"{note_id}.pdf")

def add_watermark(image, text):
    """Add watermark to image"""
    watermarked = image.copy()
    draw = ImageDraw.Draw(watermarked)
    
    try:
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
    
    width, height = watermarked.size
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    x = (width - text_width) / 2
    y = (height - text_height) / 2
    
    draw.text((x, y), text, fill=(255, 255, 255, 128), font=font)
    return watermarked

if __name__ == '__main__':
    app.run(debug=True, port=5000)
```

---

## Android Integration Notes

The Android app will:
1. Call preview API to get image URLs
2. Display images in ImageView/RecyclerView
3. Show "View Full Document" button
4. On click, check access via `/access` endpoint
5. If no access, show payment dialog
6. After payment, call `/download` to get secure link
7. Download and display full PDF in private cache

---

## Next Steps

1. **Server Implementation:**
   - Choose technology stack (Python/Node.js/Java)
   - Install required libraries
   - Implement API endpoints
   - Set up file storage
   - Add security measures

2. **Android Integration:**
   - Update app to call preview API
   - Display preview images
   - Implement payment/access flow
   - Update PDF viewer for full documents

3. **Testing:**
   - Test preview generation
   - Test download links
   - Test security measures
   - Load testing

