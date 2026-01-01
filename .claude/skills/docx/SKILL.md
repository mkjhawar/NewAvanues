---
name: docx
description: Word document (.docx) creation, editing, and analysis with tracked changes and comments support. Use when working with Word files.
---

# DOCX Processing

## Workflows

| Task | Tool | Method |
|------|------|--------|
| Read text | pandoc | `pandoc file.docx -t markdown` |
| Create new | docx-js | TypeScript/JavaScript |
| Edit existing | python-docx | OOXML manipulation |
| Raw access | unzip | Extract XML directly |

## Read Content

### Via Pandoc
```bash
pandoc document.docx -t markdown -o output.md
```

### Via Python
```python
from docx import Document
doc = Document("file.docx")
for para in doc.paragraphs:
    print(para.text)
```

## Create Document (TypeScript)
```typescript
import { Document, Packer, Paragraph, TextRun } from "docx";

const doc = new Document({
  sections: [{
    children: [
      new Paragraph({
        children: [
          new TextRun({ text: "Hello ", bold: true }),
          new TextRun("World"),
        ],
      }),
    ],
  }],
});

const buffer = await Packer.toBuffer(doc);
fs.writeFileSync("output.docx", buffer);
```

## Edit Existing (Python)
```python
from docx import Document

doc = Document("existing.docx")
doc.paragraphs[0].text = "Updated text"
doc.add_paragraph("New paragraph")
doc.save("modified.docx")
```

## Tables
```python
table = doc.add_table(rows=2, cols=3)
table.cell(0, 0).text = "Header 1"
table.style = "Table Grid"
```

## Styles
```python
from docx.shared import Pt, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH

para = doc.add_paragraph()
run = para.add_run("Styled text")
run.bold = True
run.font.size = Pt(14)
para.alignment = WD_ALIGN_PARAGRAPH.CENTER
```

## Images
```python
doc.add_picture("image.png", width=Inches(4))
```

## Raw XML Access
```bash
# .docx is a ZIP archive
unzip -d unpacked document.docx
# Key files:
# - word/document.xml (main content)
# - word/styles.xml (formatting)
# - word/comments.xml (comments)
```

## Tracked Changes
```xml
<!-- Insertions -->
<w:ins w:author="Name" w:date="2025-01-01T00:00:00Z">
  <w:r><w:t>inserted text</w:t></w:r>
</w:ins>

<!-- Deletions -->
<w:del w:author="Name" w:date="2025-01-01T00:00:00Z">
  <w:r><w:delText>deleted text</w:delText></w:r>
</w:del>
```

## Visual Analysis
```bash
# Convert to images for inspection
libreoffice --headless --convert-to pdf document.docx
pdftoppm -jpeg document.pdf preview
```

## Dependencies
```bash
pip install python-docx
npm install docx
brew install pandoc libreoffice poppler
```

## Best Practices

| Practice | Reason |
|----------|--------|
| Preserve unchanged text | Maintains original formatting |
| Keep RSID values | Avoids appearing unprofessional |
| Batch related changes | Groups of 3-10 edits |
| Validate before saving | Ensures XML integrity |
