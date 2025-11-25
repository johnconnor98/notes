# Appwrite Database Setup Instructions

## Required Attributes for Collection: `notes_list`

You need to add the following attributes to your Appwrite collection:

### Step 1: Go to Appwrite Console
1. Open https://cloud.appwrite.io
2. Navigate to your project
3. Go to **Databases** → **notes_list** collection
4. Click on **Attributes** tab

### Step 2: Add Required Attributes

Add these attributes (if not already present):

1. **file_path** (String)
   - Type: String
   - Size: 255 (or larger if needed)
   - Required: No (optional)
   - Array: No

2. **thumbnailPath** (String)
   - Type: String
   - Size: 255 (or larger if needed)
   - Required: No (optional)
   - Array: No

### Step 3: Verify Existing Attributes

Make sure these attributes exist:
- `title` (String, Required: Yes)
- `subject` (String, Required: No)
- `semester` (String, Required: No)
- `branch` (String, Required: No)
- `college` (String, Required: No)

### Important Notes:
- After adding attributes, wait a few seconds for them to be available
- The attributes are case-sensitive - use exactly: `file_path` and `thumbnailPath`
- If you get "Unknown attribute" errors, double-check the attribute names match exactly

