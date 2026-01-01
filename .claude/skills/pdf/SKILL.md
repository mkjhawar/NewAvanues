---
name: pdf
description: PDF document processing - extraction, creation, merging, splitting, OCR, and form handling. Use when working with PDF files.
---

# PDF Processing

## Libraries

| Library | Purpose |
|---------|---------|
| pypdf | Merge, split, rotate, metadata |
| pdfplumber | Text/table extraction with layout |
| reportlab | Create PDFs (Canvas, Platypus) |
| PyMuPDF | Fast rendering, annotations |

## CLI Tools

| Tool | Usage |
|------|-------|
| qpdf | `qpdf --decrypt input.pdf output.pdf` |
| pdftotext | `pdftotext -layout file.pdf` |
| pdftk | `pdftk *.pdf cat output merged.pdf` |

## Common Operations

### Extract Text
```python
import pdfplumber
with pdfplumber.open("file.pdf") as pdf:
    for page in pdf.pages:
        print(page.extract_text())
```

### Extract Tables
```python
tables = page.extract_tables()
df = pd.DataFrame(tables[0][1:], columns=tables[0][0])
```

### Merge PDFs
```python
from pypdf import PdfMerger
merger = PdfMerger()
for pdf in ["a.pdf", "b.pdf"]:
    merger.append(pdf)
merger.write("merged.pdf")
```

### Create PDF
```python
from reportlab.pdfgen import canvas
c = canvas.Canvas("output.pdf")
c.drawString(100, 750, "Hello World")
c.save()
```

### Split Pages
```python
from pypdf import PdfReader, PdfWriter
reader = PdfReader("input.pdf")
for i, page in enumerate(reader.pages):
    writer = PdfWriter()
    writer.add_page(page)
    writer.write(f"page_{i}.pdf")
```

## OCR (Scanned PDFs)
```python
import pytesseract
from pdf2image import convert_from_path
images = convert_from_path("scanned.pdf")
for img in images:
    text = pytesseract.image_to_string(img)
```

## Form Filling
```python
from pypdf import PdfReader, PdfWriter
reader = PdfReader("form.pdf")
writer = PdfWriter()
writer.append(reader)
writer.update_page_form_field_values(
    writer.pages[0],
    {"field_name": "value"}
)
writer.write("filled.pdf")
```

## Dependencies
```bash
pip install pypdf pdfplumber reportlab PyMuPDF pytesseract pdf2image
brew install poppler tesseract  # macOS
```
