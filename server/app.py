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
    # Create table matching your schema: id, notesid, title, subject, semester, branch, college
    c.execute('''CREATE TABLE IF NOT EXISTS notes
                 (id TEXT PRIMARY KEY, notesid TEXT, title TEXT, 
                  subject TEXT, semester TEXT, branch TEXT, college TEXT)''')
    conn.commit()
    
    # Migrate existing database - add missing columns if they don't exist
    try:
        c.execute('ALTER TABLE notes ADD COLUMN notesid TEXT')
    except:
        pass
    try:
        c.execute('ALTER TABLE notes ADD COLUMN subject TEXT')
    except:
        pass
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
        print(f"ID: {row[0] if len(row) > 0 else 'N/A'}")
        print(f"  NotesID: {row[1] if len(row) > 1 else 'N/A'}")
        print(f"  Title: {row[2] if len(row) > 2 else 'N/A'}")
        print(f"  Subject: {row[3] if len(row) > 3 else 'N/A'}")
        print(f"  Semester: {row[4] if len(row) > 4 else 'N/A'}")
        print(f"  Branch: {row[5] if len(row) > 5 else 'N/A'}")
        print(f"  College: {row[6] if len(row) > 6 else 'N/A'}")
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
    
    query += ' ORDER BY id DESC'
    
    print(f"Search query: {query}")
    print(f"Search params: {params}")
    
    c.execute(query, params)
    notes = []
    for row in c.fetchall():
        notes.append({
            'id': row[0] if len(row) > 0 else '',
            'notesid': row[1] if len(row) > 1 else '',
            'title': row[2] if len(row) > 2 else '',
            'subject': row[3] if len(row) > 3 else '',
            'semester': row[4] if len(row) > 4 else '',
            'branch': row[5] if len(row) > 5 else '',
            'college': row[6] if len(row) > 6 else ''
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
        notesid = note_id  # Using note_id as notesid
        
        conn = sqlite3.connect(DATABASE)
        c = conn.cursor()
        c.execute('''INSERT INTO notes 
                     (id, notesid, title, subject, semester, branch, college)
                     VALUES (?, ?, ?, ?, ?, ?, ?)''',
                  (note_id, notesid, title, subject, semester, branch, college))
        conn.commit()
        conn.close()
        
        print(f"✓ Saved metadata to database:")
        print(f"  ID: {note_id}")
        print(f"  NotesID: {notesid}")
        print(f"  Title: {title}")
        print(f"  Subject: {subject}")
        print(f"  Semester: {semester}")
        print(f"  Branch: {branch}")
        print(f"  College: {college}")
        
        return jsonify({
            'id': note_id,
            'notesid': notesid,
            'title': title,
            'subject': subject,
            'semester': semester,
            'branch': branch,
            'college': college
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
    notesid = note_id  # Using note_id as notesid
    filename = f"{note_id}_{file.filename}"
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)
    
    conn = sqlite3.connect(DATABASE)
    c = conn.cursor()
    c.execute('''INSERT INTO notes 
                 (id, notesid, title, subject, semester, branch, college)
                 VALUES (?, ?, ?, ?, ?, ?, ?)''',
              (note_id, notesid, title, subject, semester, branch, college))
    conn.commit()
    conn.close()
    
    print(f"✓ Saved file upload to database:")
    print(f"  ID: {note_id}")
    print(f"  NotesID: {notesid}")
    print(f"  Title: {title}")
    print(f"  Subject: {subject}")
    print(f"  Semester: {semester}")
    print(f"  Branch: {branch}")
    print(f"  College: {college}")
    
    return jsonify({
        'id': note_id,
        'notesid': notesid,
        'title': title,
        'subject': subject,
        'semester': semester,
        'branch': branch,
        'college': college
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

