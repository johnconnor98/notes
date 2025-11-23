from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import sqlite3
import os
from datetime import datetime
import uuid

app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = 'uploads'
THUMBNAIL_FOLDER = 'thumbnails'
DATABASE = 'notes.db'

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)
if not os.path.exists(THUMBNAIL_FOLDER):
    os.makedirs(THUMBNAIL_FOLDER)

def init_db():
    conn = sqlite3.connect(DATABASE)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS notes
                 (id TEXT PRIMARY KEY, title TEXT, filename TEXT, 
                  upload_date TEXT, file_size INTEGER, subject TEXT,
                  semester TEXT, branch TEXT, college TEXT, thumbnail TEXT,
                  file_url TEXT, file_path TEXT, thumbnail_url TEXT)''')
    conn.commit()
    
    # Migrate existing database
    try:
        c.execute('ALTER TABLE notes ADD COLUMN semester TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN branch TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN college TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN thumbnail TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN file_url TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN file_path TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN thumbnail_url TEXT')
    except:
        pass
    
    conn.commit()
    conn.close()

@app.route('/api/notes', methods=['GET'])
def get_notes():
    subject = request.args.get('subject', '')
    semester = request.args.get('semester', '')
    branch = request.args.get('branch', '')
    college = request.args.get('college', '')
    
    conn = sqlite3.connect(DATABASE)
    c = conn.cursor()
    
    # First, log ALL data in database
    c.execute('SELECT * FROM notes')
    all_rows = c.fetchall()
    print("=" * 80)
    print("ALL DATA IN DATABASE:")
    print("=" * 80)
    for row in all_rows:
        print(f"ID: {row[0]}")
        print(f"  Title: {row[1]}")
        print(f"  Filename: {row[2]}")
        print(f"  Upload Date: {row[3]}")
        print(f"  File Size: {row[4]}")
        print(f"  Subject: {row[5] if len(row) > 5 else 'N/A'}")
        print(f"  Semester: {row[6] if len(row) > 6 else 'N/A'}")
        print(f"  Branch: {row[7] if len(row) > 7 else 'N/A'}")
        print(f"  College: {row[8] if len(row) > 8 else 'N/A'}")
        print(f"  Thumbnail: {row[9] if len(row) > 9 else 'N/A'}")
        print(f"  File URL: {row[10] if len(row) > 10 else 'N/A'}")
        print(f"  File Path: {row[11] if len(row) > 11 else 'N/A'}")
        print(f"  Thumbnail URL: {row[12] if len(row) > 12 else 'N/A'}")
        print("-" * 80)
    print(f"Total records: {len(all_rows)}")
    print("=" * 80)
    
    query = 'SELECT * FROM notes WHERE 1=1'
    params = []
    
    if subject:
        query += ' AND subject LIKE ?'
        params.append(f'%{subject}%')
    if semester:
        query += ' AND semester LIKE ?'
        params.append(f'%{semester}%')
    if branch:
        query += ' AND branch LIKE ?'
        params.append(f'%{branch}%')
    if college:
        query += ' AND college LIKE ?'
        params.append(f'%{college}%')
    
    query += ' ORDER BY upload_date DESC'
    
    print(f"Search query: {query}")
    print(f"Search params: {params}")
    
    c.execute(query, params)
    notes = []
    for row in c.fetchall():
        notes.append({
            'id': row[0],
            'title': row[1],
            'filename': row[2],
            'upload_date': row[3],
            'file_size': row[4],
            'subject': row[5] if len(row) > 5 else '',
            'semester': row[6] if len(row) > 6 else '',
            'branch': row[7] if len(row) > 7 else '',
            'college': row[8] if len(row) > 8 else '',
            'thumbnail': row[9] if len(row) > 9 else '',
            'file_url': row[10] if len(row) > 10 else '',
            'file_path': row[11] if len(row) > 11 else '',
            'thumbnail_url': row[12] if len(row) > 12 else ''
        })
    
    print(f"Returning {len(notes)} notes after filtering")
    conn.close()
    return jsonify(notes)

@app.route('/api/notes', methods=['POST'])
def upload_note():
    # Check if this is a metadata-only upload (files already uploaded to Appwrite)
    if 'file_path' in request.form or 'file_url' in request.form:
        # Handle metadata-only upload
        title = request.form.get('title', '')
        subject = request.form.get('subject', '')
        semester = request.form.get('semester', '')
        branch = request.form.get('branch', '')
        college = request.form.get('college', '')
        file_url = request.form.get('file_url', '')
        file_path = request.form.get('file_path', '')
        thumbnail_url = request.form.get('thumbnail_url', '')
        
        if not title:
            return jsonify({'error': 'Title is required'}), 400
        
        note_id = str(uuid.uuid4())
        filename = file_path.split('/')[-1] if file_path else f"{note_id}_file"
        upload_date = datetime.now().isoformat()
        
        conn = sqlite3.connect(DATABASE)
        c = conn.cursor()
        c.execute('''INSERT INTO notes 
                     (id, title, filename, upload_date, file_size, subject, semester, branch, college, 
                      thumbnail, file_url, file_path, thumbnail_url)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)''',
                  (note_id, title, filename, upload_date, 0, subject, semester, branch, college, 
                   '', file_url, file_path, thumbnail_url))
        conn.commit()
        conn.close()
        
        print(f"✓ Saved metadata to database: ID={note_id}, Title={title}, Subject={subject}, File Path={file_path}")
        
        return jsonify({
            'id': note_id,
            'title': title,
            'filename': filename,
            'upload_date': upload_date,
            'file_size': 0,
            'subject': subject,
            'semester': semester,
            'branch': branch,
            'college': college,
            'thumbnail': '',
            'file_url': file_url,
            'file_path': file_path,
            'thumbnail_url': thumbnail_url
        }), 201
    
    # Handle file upload (legacy method)
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    
    file = request.files['file']
    title = request.form.get('title', file.filename)
    subject = request.form.get('subject', 'General')
    semester = request.form.get('semester', '')
    branch = request.form.get('branch', '')
    college = request.form.get('college', '')
    
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400
    
    note_id = str(uuid.uuid4())
    filename = f"{note_id}_{file.filename}"
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)
    
    file_size = os.path.getsize(filepath)
    upload_date = datetime.now().isoformat()
    
    thumbnail = None
    if 'thumbnail' in request.files:
        thumb_file = request.files['thumbnail']
        if thumb_file.filename:
            thumb_filename = f"{note_id}_thumb_{thumb_file.filename}"
            thumb_path = os.path.join(THUMBNAIL_FOLDER, thumb_filename)
            thumb_file.save(thumb_path)
            thumbnail = thumb_filename
    
    conn = sqlite3.connect(DATABASE)
    c = conn.cursor()
    c.execute('''INSERT INTO notes 
                 (id, title, filename, upload_date, file_size, subject, semester, branch, college, thumbnail)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)''',
              (note_id, title, filename, upload_date, file_size, subject, semester, branch, college, thumbnail))
    conn.commit()
    conn.close()
    
    print(f"✓ Saved file upload to database: ID={note_id}, Title={title}, Subject={subject}")
    
    return jsonify({
        'id': note_id,
        'title': title,
        'filename': filename,
        'upload_date': upload_date,
        'file_size': file_size,
        'subject': subject,
        'semester': semester,
        'branch': branch,
        'college': college,
        'thumbnail': thumbnail
    }), 201

@app.route('/api/notes/<note_id>/download', methods=['GET'])
def download_note(note_id):
    conn = sqlite3.connect(DATABASE)
    c = conn.cursor()
    c.execute('SELECT filename FROM notes WHERE id = ?', (note_id,))
    row = c.fetchone()
    conn.close()
    
    if not row:
        return jsonify({'error': 'Note not found'}), 404
    
    filename = row[0]
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    
    if not os.path.exists(filepath):
        return jsonify({'error': 'File not found'}), 404
    
    return send_file(filepath, as_attachment=True)

@app.route('/api/thumbnails/<thumbnail_name>', methods=['GET'])
def get_thumbnail(thumbnail_name):
    filepath = os.path.join(THUMBNAIL_FOLDER, thumbnail_name)
    if not os.path.exists(filepath):
        return jsonify({'error': 'Thumbnail not found'}), 404
    return send_file(filepath)

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=5000, debug=True)

